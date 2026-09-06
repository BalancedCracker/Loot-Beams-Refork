# NirvanaLib for Minecraft 26.1.2 / Fabric (local build)

Loot Beams Refork depends on [NirvanaLib](https://github.com/TUsama/NirvanaLib).
As of September 2026 its author has published a **NeoForge** build for 26.1.2
(`neoforge-26.1.2-2.2.0` on Modrinth) but **no Fabric build**. The upstream
repository contains a `versions/26.1.2-fabric` directory, but it is not registered
as a build target and its dependency versions are placeholders.

Until upstream publishes one, the `26.1.2-fabric` target of this project resolves
NirvanaLib from **mavenLocal** (`~/.m2`) under the group `local.nirvanalib`
instead of `maven.modrinth` (modstitch pins the `maven.modrinth` group to the
Modrinth repository, so a local jar cannot shadow it). See `build.gradle.kts`.

## Building the jar

Gradle must run on Java 25 (Fabric Loom refuses to set up 26.1.2 otherwise).

```bash
git clone --recursive -b multiversion https://github.com/TUsama/NirvanaLib.git
cd NirvanaLib
```

Apply these changes to the clone:

1. `settings.gradle.kts`: register the Fabric target and, to keep the build small,
   drop the other targets:
   ```kotlin
   mc("26.1.2", loaders = listOf("fabric"))
   // remove or comment out the mc("1.21.11", ...) ... mc("1.20.1", ...) lines
   vcsVersion = "26.1.2-fabric"
   ```
   and in `stonecutter.gradle.kts`: `stonecutter active "26.1.2-fabric"`.
2. `versions/26.1.2-fabric/gradle.properties`:
   ```properties
   modstitch.platform=fabric-loom
   deps.minecraft=26.1.2
   deps.fabric_api=0.155.2
   deps.common_networking=1.0.23-26.1.2
   ```
   (`fabric-loom` is the no-remap platform; 26.1 is not obfuscated.)
3. `gradle.properties`: `deps.fabric_loader=0.19.5`, and in `build.gradle.kts`
   `fabricLoaderVersion = "0.19.5"`.
4. `src/main/resources/nirvana_lib.accesswidener`: change the header namespace
   from `named` to `official` (`accessWidener v1 official`).
5. `build.gradle.kts`: comment out the `msPublishing { ... }` block. It reads
   Modrinth/CurseForge tokens from hardcoded `D:\` paths at configuration time
   and fails on any other machine.

Then build:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew "Set active project to 26.1.2-fabric"
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :26.1.2-fabric:build
```

The mod jar is written to
`versions/26.1.2-fabric/build/devlibs/Nirvana Lib-fabric-26.1.2-2.2.0-dev-fat.jar`
(with the no-remap platform, modstitch leaves the shadowed jar in `devlibs`).
It contains the relocated vavr / event bus / typetools classes, the mixin
configs and `fabric.mod.json`, i.e. it is a complete mod jar.

## Installing into mavenLocal

```bash
D=~/.m2/repository/local/nirvanalib/nirvana-library/fabric-26.1.2-2.2.0
mkdir -p "$D"
cp "versions/26.1.2-fabric/build/devlibs/Nirvana Lib-fabric-26.1.2-2.2.0-dev-fat.jar" \
   "$D/nirvana-library-fabric-26.1.2-2.2.0.jar"
cat > "$D/nirvana-library-fabric-26.1.2-2.2.0.pom" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>local.nirvanalib</groupId>
  <artifactId>nirvana-library</artifactId>
  <version>fabric-26.1.2-2.2.0</version>
  <packaging>jar</packaging>
</project>
EOF
```

## Runtime

Players (and the `runClient` dev run) need the same jar next to Loot Beams in
the `mods` folder, together with Fabric API, Fzzy Config and Common Networking
for 26.1.2. Once upstream publishes a Fabric 26.1.2 build, switch the group in
`build.gradle.kts` back to `maven.modrinth` and delete this file.
