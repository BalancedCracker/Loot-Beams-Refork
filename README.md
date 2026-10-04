# Loot Beams Refork (expt builds)

Dropped items send up a beam of light coloured by their rarity, so they are easier to spot.

This is a personal fork of [TUsama/Loot-Beams-Refork](https://github.com/TUsama/Loot-Beams-Refork),
kept building against current Minecraft versions. It is **not an official release**: builds from
this repository carry a version like `3.4.7+expt.1`, where `3.4.7` is the upstream version this
fork is based on. Report problems with these builds here, not upstream.

## Targets

One source tree builds every version, via [Stonecutter](https://stonecutter.kikugie.dev/) and
[modstitch](https://github.com/isXander/modstitch).

| Minecraft | Loaders | Required mods |
| --- | --- | --- |
| 26.1.2 | NeoForge, Fabric | Fzzy Config (plus Fabric API, and Mod Menu for the config button) |
| 1.21.11, 1.21.10, 1.21.8, 1.21.4, 1.21.1 | NeoForge, Fabric | Fzzy Config, Nirvana Lib, Common Networking |
| 1.20.1 | Forge, Fabric | same as above |

The 26.1 targets bundle the few libraries they need instead of depending on Nirvana Lib, which has
no Fabric build for 26.1. Older targets are left exactly as upstream published them.

## Building

Gradle has to run on **Java 25** — Fabric Loom refuses to set up Minecraft 26.1 otherwise. The
toolchain is pinned in `gradle/gradle-daemon-jvm.properties`, so a plain invocation is enough:

```bash
./gradlew :26.1.2-neoforge:build :26.1.2-fabric:build
```

Jars land in `versions/<target>/build/libs/`. `build` also runs `verifyJarContents`, which fails if
the jar bundles anything that could shadow one of the game's own libraries.

## Licensing

This fork is MIT licensed (see `LICENSE`). Upstream dedicates its work to the public domain under
CC0-1.0, which permits this; CC0 is a poor fit for code mainly because it does not waive patent
rights.

Note that the built jars bundle third-party libraries, which keep their own terms:
[vavr](https://github.com/vavr-io/vavr) (Apache-2.0) and, on every loader except NeoForge,
[NeoForged's event bus](https://github.com/neoforged/Bus) (LGPL-2.1-only) with
[typetools](https://github.com/jhalterman/typetools) (Apache-2.0). Distributing these jars more
widely would mean shipping those notices too.
