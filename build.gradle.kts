import deps.DependencyConfig
import deps.Loaders
// Fully qualifying this below would not work: in a build script `java` resolves to Gradle's
// own java extension, not to the package.
import java.util.zip.ZipFile

plugins {
    id("dev.isxander.modstitch.base") version "clefal-version"
    id("dev.isxander.modstitch.publishing") version "clefal-version"
    // Bundles the small libraries the mod uses (see the msShadow block below).
    id("dev.isxander.modstitch.shadow") version "clefal-version"
    id ("org.jetbrains.kotlin.jvm") version "2.1.10"
    id ("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
}

fun prop(name: String, consumer: (prop: String) -> Unit) {
    (findProperty(name) as? String?)?.let(consumer)
}


val modv = property("mod_version") as String


val loader = when {
    modstitch.isLoom -> "fabric"
    modstitch.isModDevGradleRegular -> "neoforge"
    modstitch.isModDevGradleLegacy -> "forge"
    else -> throw IllegalStateException("Unsupported loader")
}

val minecraft = property("deps.minecraft") as String

// Targets that carry their own copies of the libraries NirvanaLib used to provide, and so need
// no library mod at runtime. Older targets keep NirvanaLib: 1.21.4 and below use its
// RenderTypeCreator, and leaving the rest untouched keeps the already published targets'
// dependency profile exactly as it is.
val vendoredLibs = minecraft == "26.1.2"

modstitch {
    minecraftVersion = minecraft

    // Alternatively use stonecutter.eval if you have a lot of versions to target.
    // https://stonecutter.kikugie.dev/stonecutter/guide/setup#checking-versions
    javaVersion = when (minecraft) {
        "1.20.1" -> 17
        "1.21.1" -> 21
        "1.21.4" -> 21
        "1.21.8", "1.21.10", "1.21.11" -> 21
        "26.1.2" -> 25
        else -> throw IllegalArgumentException("Please store the java version for $minecraft in build.gradle.kts!")
    }

    // If parchment doesnt exist for a version yet you can safely
    // omit the "deps.parchment" property from your versioned gradle.properties
    parchment {
        prop("deps.parchment") {
            if (minecraft == "1.21.1") minecraftVersion.set("1.21")
            mappingsVersion = it
        }
    }

    // This metadata is used to fill out the information inside
    // the metadata files found in the templates folder.
    val mid = "lootbeams"
    metadata {
        modId = mid
        modName = "Loot Beams Refork"
        modVersion = modv
        modGroup = "com.clefal"
        modAuthor = "Clefal"
        modDescription =
            "Loot items, guided by light!"
        modLicense = "MIT"
        fun <K : Any, V : Any> MapProperty<K, V>.populate(block: MapProperty<K, V>.() -> Unit) {
            block()
        }
        replacementProperties.populate {
            // You can put any other replacement properties/metadata here that
            // modstitch doesn't initially support. Some examples below.
            // This fork's own tracker, not the upstream one: a crash in a build upstream never
            // made should not send reports to its author.
            put("mod_issue_tracker", "https://github.com/BalancedCracker/Loot-Beams-Refork/issues")
            // Resource pack format (this mod ships assets only, no data).
            val pformat = when (property("deps.minecraft")) {
                "1.20.1" -> 15
                "1.21.1" -> 34
                "1.21.4" -> 46
                "1.21.8" -> 64
                "1.21.10" -> 69
                "1.21.11" -> 70.0
                "26.1.2" -> 84
                else -> throw IllegalArgumentException("Please store the resource pack version for ${property("deps.minecraft")} in build.gradle.kts! https://minecraft.wiki/w/Pack_format")
            }
            put("pformat", pformat.toString())
            // Since 26.1, packs newer than format 64 must declare min_format/max_format
            // instead of pack_format. A bare integer N means N.0 for min and N.* for max.
            put(
                "pack_format_fields", when {
                    stonecutter.current.parsed >= "26.1" -> "\"min_format\": $pformat,\n    \"max_format\": $pformat"
                    else -> "\"pack_format\": $pformat"
                }
            )

            put("target_minecraft", minecraft)
            //put("target_lib", property("deps.lib") as String)
            put(
                "target_loader", when (loader) {
                    "neoforge" -> property("deps.neoforge") as String
                    else -> ""
                }
            )
            put("loader", loader)
            put(
                "target_fabricloader", when (loader) {
                    "fabric" -> property("deps.fabric_loader") as String
                    else -> ""
                }
            )
            put("fzzy_config_version", property("deps.fzzy_config_version") as String)
            put("lib_version", property("deps.lib_version") as String)
            // The library-mod dependency entry, or nothing at all when the libraries are
            // bundled. Each loader's manifest has its own syntax, and only ever reads its own.
            put(
                "nirvana_depends", when {
                    vendoredLibs -> ""
                    loader == "fabric" -> "\n    \"nirvana_lib\": \">=${property("deps.lib_version")}\","
                    else -> """
                        |
                        |[[dependencies.${mid}]]
                        |modId = "nirvana_lib"
                        |mandatory = true
                        |versionRange = "[${property("deps.lib_version")},)"
                        |ordering = "AFTER"
                        |side = "CLIENT"
                    """.trimMargin()
                }
            )
        }
    }

    // Fabric Loom (Fabric)
    loom {
        // It's not recommended to store the Fabric Loader version in properties.
        // Make sure its up to date.
        fabricLoaderVersion = if (minecraft == "26.1.2") "0.19.5" else "0.16.11"
        configureLoom {
            runs {
                all {
                    ideConfigGenerated(true)
                }
                //accessWidenerPath.set(file("../../src/main/resources/${mid}.accesswidener"))
            }
        }
    }

    // ModDevGradle (NeoForge, Forge, Forgelike)
    moddevgradle {

        prop("deps.forge") { forgeVersion = it }
        prop("deps.neoform") { neoFormVersion = it }
        prop("deps.neoforge") { neoForgeVersion = it }
        prop("deps.mcp") { mcpVersion = it }


        // Configures client and server runs for MDG, it is not done by default
        defaultRuns()

        // This block configures the `neoforge` extension that MDG exposes by default,
        // you can configure MDG like normal from here
        configureNeoForge {
            //setAccessTransformers("../../src/main/resources/META-INF/accesstransformer.cfg")
            validateAccessTransformers = false
            afterEvaluate {
                runs.all {
                    val upperName = name.replaceFirstChar {
                        it.uppercaseChar()
                    }
                    tasks.named<JavaExec>("run$upperName") {
                        javaLauncher.set(
                            javaToolchains.launcherFor {
                                languageVersion = JavaLanguageVersion.of(project.modstitch.javaVersion.get())
                                vendor = JvmVendorSpec.JETBRAINS
                            }
                        )
                    }
                    jvmArguments.add("-XX:+AllowEnhancedClassRedefinition")
                    disableIdeRun()

            }
                //gameDirectory = file("run")
            }
        }
    }

    mixin {
        // You do not need to specify mixins in any mods.json/toml file if this is set to
        // true, it will automatically be generated.
        addMixinsToModManifest = true
        when {
            isModDevGradleLegacy -> configs.register("${mid}-1.20.1")
            minecraft == "1.21.1" -> configs.register("${mid}-1.21")
            minecraft == "1.21.4" -> configs.register("${mid}-1.21.4")
            minecraft == "1.21.10" || minecraft == "1.21.11" || minecraft == "26.1.2" -> configs.register("${mid}-1.21.10")
            else -> configs.register("${mid}-default")
        }


        // Most of the time you wont ever need loader specific mixins.
        // If you do, simply make the mixin file and add it like so for the respective loader:
        // if (isLoom) configs.register("examplemod-fabric")
        // if (isModDevGradleRegular) configs.register("examplemod-neoforge")
        // if (isModDevGradleLegacy) configs.register("examplemod-forge")
    }
}
base {
    val meta = modstitch.metadata
    archivesName = "${meta.modName.get()}-${loader}-${minecraft}"
}

// Compile each target with JDK 21, or newer when the target needs it (26.1 = 25).
// modstitch only sets source/target compatibility, so without a toolchain javac
// comes from the Gradle daemon JVM. The daemon is pinned to Java 25 for Fabric
// Loom on 26.1 (see gradle/gradle-daemon-jvm.properties), and javac 25 rejects
// some 1.21.x compat-mod class files that javac 21 accepts. JDK 21 (not 17) is
// the floor because the 1.20.1 NirvanaLib jars are compiled for Java 21, which
// javac 17 cannot read even with source/target 17.
java {
    toolchain {
        languageVersion = modstitch.javaVersion.map { JavaLanguageVersion.of(maxOf(it, 21)) }
    }
}

// Libraries bundled into the mod jar, relocated so they cannot clash with another mod's copy.
// The sources import the plain coordinates; relocation rewrites those references at jar time.
msShadow {
    relocatePackage.set("me.clefal.lootbeams.relocated")
    // Option/Tuple/pattern matching, used throughout the mod. Apache-2.0.
    dependency("io.vavr:vavr:0.11.0", mapOf("io.vavr" to "io.vavr"))
    // Backs the mod's own internal EVENT_BUS. Only NeoForge ships this bus (Forge 1.20.1 has
    // net.minecraftforge.eventbus instead), so everything else needs a bundled copy -- and on
    // NeoForge it must stay un-relocated, or the loader would no longer recognise
    // @SubscribeEvent on our @EventBusSubscriber classes.
    if (!modstitch.isModDevGradleRegular) {
        dependency("net.neoforged:bus:8.0.5", mapOf("net.neoforged.bus" to "net.neoforged.bus")) {
            // The bus drags in ASM, log4j and modlauncher, and only the bus itself has a
            // relocation rule, so those would land in the jar under their original names and
            // shadow the game's own copies. An unrelocated ASM is fatal: Mixin then fails to
            // verify its own classes and the game dies before the title screen. The platform
            // provides all three anyway.
            exclude(group = "org.ow2.asm")
            exclude(group = "org.apache.logging.log4j")
            exclude(group = "cpw.mods", module = "modlauncher")
            // Declared below with its own relocation rule.
            exclude(group = "net.jodah")
        }
        dependency("net.jodah:typetools:0.6.3", mapOf("net.jodah" to "net.jodah"))
    }
}

// Loom's no-remap platform (26.1 Fabric) has no remapJar to hand the shadowed jar to, so
// modstitch leaves it as a "dev-fat" jar in build/devlibs while the real `jar` task stays
// disabled. Publish the shadowed jar as the normal artifact instead.
if (findProperty("modstitch.platform") == "fabric-loom") {
    tasks.named<AbstractArchiveTask>("shadowJar") {
        archiveClassifier = ""
        destinationDirectory = layout.buildDirectory.dir("libs")
    }
}

// Guard for the bundled libraries. Anything in the jar outside these roots is a copy of some
// library under its original name, which shadows the game's own copy: that is how an
// unrelocated ASM once made Mixin fail to verify itself and killed the client before the title
// screen. A dev run cannot catch it, because runClient loads the mod from build/classes with
// Gradle's own classpath and never touches the shaded jar -- so check the jar itself.
val allowedJarRoots = listOf(
    "me/clefal/lootbeams/", // the mod, including the libraries relocated underneath it
    "assets/",
    "data/",
    "META-INF/",
    "org/jspecify/",        // annotations only, arrives with vavr, inert at runtime
)
val verifyJarContents = tasks.register("verifyJarContents") {
    group = "verification"
    description = "Fails if the mod jar bundles packages that could shadow the game's libraries."
    // Not modstitch.finalJarTask on the no-remap Loom platform: there it still points at the
    // `jar` task, which the shadow plugin disables, so the check would depend on nothing and
    // happily inspect a stale jar from an earlier build.
    val finalJarTaskName = when {
        findProperty("modstitch.platform") == "fabric-loom" -> "shadowJar"
        else -> modstitch.finalJarTask.name
    }
    val finalJar = tasks.named<AbstractArchiveTask>(finalJarTaskName).flatMap { it.archiveFile }
    inputs.file(finalJar)
    doLast {
        val jarFile = finalJar.get().asFile
        val offenders = ZipFile(jarFile).use { zip ->
            zip.entries().asSequence()
                .map { it.name }
                .filter { !it.endsWith("/") }
                .filter { name ->
                    // Root level holds only our own manifests and mixin configs, so a class
                    // there is always someone else's.
                    if (name.contains('/')) allowedJarRoots.none(name::startsWith)
                    else name.endsWith(".class")
                }
                .map { if (it.contains('/')) it.substringBeforeLast('/') + "/" else it }
                .distinct()
                .sorted()
                .toList()
        }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "${jarFile.name} bundles unexpected entries:\n" +
                    offenders.joinToString("\n") { "  $it" } +
                    "\nEither relocate them in the msShadow block, or exclude them from the " +
                    "dependency that pulls them in."
            )
        }
    }
}
tasks.named("check") { dependsOn(verifyJarContents) }

// Stonecutter constants for mod loaders.
// See https://stonecutter.kikugie.dev/stonecutter/guide/comments#condition-constants
stonecutter {
    constants.putAll(mapOf<String, Boolean>(
        "fabric" to loader.equals("fabric"),
        "neoforge" to loader.equals("neoforge"),
        "forge" to loader.equals("forge"),
        "vanilla" to loader.equals("vanilla"),
        "legacy" to (minecraft == "1.20.1"),
        "malum" to (modstitch.minecraftVersion.get() == "1.20.1" || (loader.equals("neoforge") && modstitch.minecraftVersion.get() == "1.21.1")),
        "biomancy" to (loader.equals("forge") && (minecraft == "1.20.1")),
        "simplesword" to ((minecraft == "1.21.1") || (minecraft == "1.20.1"))
    ))

    replacements.string(current.version >= "1.21.10" && loader.equals("fabric")) {
        replace("guiGraphics.peekScissorStack()", "guiGraphics.scissorStack.peek()")
    }

    replacements.string(current.parsed >= "1.21.11") {
        replace("net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier")
        replace("renderer.RenderType", "renderer.rendertype.RenderType")
        replace("net.minecraft.Util", "net.minecraft.util.Util")
    }

    replacements.regex(current.parsed >= "1.21.11") {
        replace("\\bResourceLocation\\b" to "Identifier", "\\bIdentifier\\b" to "ResourceLocation")
    }

    // Minecraft 26.1: pure renames/moves that need no per-version code.
    replacements.regex(current.parsed >= "26.1") {
        // GuiGraphics was renamed to GuiGraphicsExtractor.
        replace("\\bGuiGraphics\\b" to "GuiGraphicsExtractor", "\\bGuiGraphicsExtractor\\b" to "GuiGraphics")
        // LevelRenderState moved into the state.level package.
        replace(
            "\\bnet\\.minecraft\\.client\\.renderer\\.state\\.LevelRenderState\\b" to "net.minecraft.client.renderer.state.level.LevelRenderState",
            "\\bnet\\.minecraft\\.client\\.renderer\\.state\\.level\\.LevelRenderState\\b" to "net.minecraft.client.renderer.state.LevelRenderState"
        )
        // LightTexture.FULL_BRIGHT is now LightCoordsUtil.FULL_BRIGHT.
        replace(
            "\\bnet\\.minecraft\\.client\\.renderer\\.LightTexture\\b" to "net.minecraft.util.LightCoordsUtil",
            "\\bnet\\.minecraft\\.util\\.LightCoordsUtil\\b" to "net.minecraft.client.renderer.LightTexture"
        )
        replace("\\bLightTexture\\b" to "LightCoordsUtil", "\\bLightCoordsUtil\\b" to "LightTexture")
    }

    replacements.string("ss_replacement", current.version.equals("1.20.1")) {
        replace("Styles.COMMON", "HelperMethods.getStyle(\"common\")")
        replace("Styles.UNIQUE", "HelperMethods.getStyle(\"unique\")")
        replace("Styles.LEGENDARY", "HelperMethods.getStyle(\"legendary\")")
        replace("Styles.RUNIC", "HelperMethods.getStyle(\"runic\")")

    }
}

tasks.named<Copy>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}




// All dependencies should be specified through modstitch's proxy configuration.
// Wondering where the "repositories" block is? Go to "stonecutter.gradle.kts"
// If you want to create proxy configurations for more source sets, such as client source sets,
// use the modstitch.createProxyConfigurations(sourceSets["client"]) function.
dependencies {
    val loaderEnum = when {
        modstitch.isLoom -> Loaders.LOOM
        modstitch.isModDevGradleLegacy -> Loaders.FORGE
        modstitch.isModDevGradleRegular -> Loaders.NEOFORGE
        else -> throw IllegalArgumentException("unknown loader")
    }
    val fzzyConfigVersion = findProperty("deps.fzzy_config_version")
    val fzzyMinecraftVersion = when (minecraft) {
        "1.21.1" -> "1.21"
        "1.21.4" -> "1.21.3"
        "1.21.8" -> "1.21.6"
        "1.21.10" -> "1.21.9"
        "26.1.2" -> "26.1"
        else -> minecraft
    }
    var fzzyString : String = "";
    val libVersion = property("deps.lib_version") as String
    fun Dependency?.jij() = this?.also(::modstitchJiJ)
    fun String.implementation() = if (modstitch.isModDevGradleLegacy){
        add("modImplementation", this)
    } else {
        modstitchModImplementation(this)
    }
    fun String.runtimeOnly() = if (modstitch.isModDevGradleLegacy) {
        add("modRuntimeOnly", this)
    } else {
        modstitchModRuntimeOnly(this)
    }
    //fzzy
    modstitch.loom {
        val fabricApi = property("deps.fabric_api") as String
        modstitchModImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApi}+${minecraft}")
        fzzyString = "me.fzzyhmstrs:fzzy_config:${fzzyConfigVersion}+${fzzyMinecraftVersion}";

    }

    modstitch.moddevgradle {
        if (modstitch.isModDevGradleLegacy){
            fzzyString = "me.fzzyhmstrs:fzzy_config:${fzzyConfigVersion}+${fzzyMinecraftVersion}+forge";
        } else {
            if (minecraft == "1.21.8"){
                fzzyString = "me.fzzyhmstrs:fzzy_config:${fzzyConfigVersion}+1.21.7+neoforge";
            } else {
                fzzyString = "me.fzzyhmstrs:fzzy_config:${fzzyConfigVersion}+${fzzyMinecraftVersion}+neoforge"
            }

        }

    }

    modstitchModCompileOnly(fzzyString)
    (fzzyString).runtimeOnly()

    // The bundled libraries (see msShadow above) also have to be on the compile classpath.
    // Only NeoForge provides the event bus itself, so every other loader needs the artifact.
    modstitchImplementation("io.vavr:vavr:0.11.0")
    if (!modstitch.isModDevGradleRegular) {
        modstitchImplementation("net.neoforged:bus:8.0.5")
        modstitchImplementation("net.jodah:typetools:0.6.3")
    }

    // Targets that still use NirvanaLib. common-network is NirvanaLib's own requirement, so it
    // is only needed alongside it.
    if (!vendoredLibs) {
        ("maven.modrinth:nirvana-library:${loader}-${minecraft}-${libVersion}").implementation()
        ("maven.modrinth:common-network:${property("deps.common_network")}").runtimeOnly()
    }
    //loader-specified deps
    DependencyConfig.getDependencies(loaderEnum, minecraft).forEach { dep ->
        dependencies.add(dep.configuration, dep.notation, dep.options)
    }
    //lombok
    modstitchCompileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testCompileOnly("org.projectlombok:lombok:1.18.42")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.42")

}

// NOTE: Modrinth/CurseForge auto-publishing is disabled for local/dev builds.
// The original config eagerly read access tokens from hardcoded Windows paths
// (D:\curseforge-key.txt, D:\modrinth-key.txt) during Gradle configuration,
// which crashes the build on any machine without those exact files. Re-enable
// by restoring this block (with real, machine-appropriate token paths) only
// when you actually intend to publish.
/*
msPublishing {

    mpp {
        changelog = file("../../changelog.md")
            .readLines()
            .joinToString("\n") { line ->
                if (line.isNotBlank()) {
                    "$line</br>"
                } else {
                    line
                }
            }
        type = STABLE


        afterEvaluate {
            file = modstitch.finalJarTask.flatMap { it.archiveFile }
            this@mpp.displayName.set(file.map { it.asFile.name })
        }
        //dryRun = true
        val cfOptions = curseforgeOptions {
            accessToken = file("D:\\curseforge-key.txt").readText()
            projectId = "1150640"
            minecraftVersions.add(minecraft)
            clientRequired = true
            serverRequired = false
            javaVersions.set(listOf(JavaVersion.toVersion(modstitch.javaVersion.get())))
            requires("nirvana-library")
        }

        // Modrinth options used by both Fabric and Forge
        val mrOptions = modrinthOptions {
            accessToken = file("D:\\modrinth-key.txt").readText()
            version = "${loader}-${minecraft}-${modstitch.metadata.modVersion.get()}"
            projectId = "rp7ooqvq"
            minecraftVersions.add(minecraft)
            requires("nirvana-library")
        }

        curseforge("toCurseForge") {
            from(cfOptions)
        }


        modrinth("toModrinth") {
            from(mrOptions)
        }


    }

}
*/