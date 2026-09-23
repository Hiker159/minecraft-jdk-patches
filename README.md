# Minecraft compatibility patches for OS X Mavericks

Native library replacements and launcher settings for running Minecraft Java
Edition on unsupported versions of Mac OS X, alongside the jdk-macos-legacy Java ports.
For Java downloads, installation, and source builds, see the
Java JDK 25 repository (published separately).

These packages address game libraries that require newer macOS APIs even after
Java itself starts successfully. They are experimental compatibility builds,
not a Minecraft mod or a replacement for the game or launcher.

This is specifically targeted towards Minecraft 1.21.11 and greater. Older versions of Minecraft running on the idk-macos-legacy java port are unaffected. 

## Requirements

- An Intel Mac running Mavericks or greater.
- A Minecraft launcher that already runs on Mavericks and allows a custom Java executable
  and JVM arguments.
- The appropriate Java runtime: Java 21 for releases requiring Java 21, or the
  Java 25 port for the tested Minecraft 26.3. Java 22 has also been reported
  working, but compatibility with individual mods and launchers may differ.

## What versions of the game?
| Version | Needs Patches | What Patch |
| --- | --- | --- |
| 1.20 | No | --- |
| 1.21.10 | No | --- |
| 1.21.11 | Semi | Needs a JVM argument for Multiplayer |
| 26.1+ | Yes | Requires Modified Game Libraries |

## Downloads

| Component | Package | Compatibility change |
| --- | --- | --- |
| SDL 3.4.14 | `sdl3-3.4.14-mavericks-experimental.zip` | Removes required Metal dependencies and adapts newer Cocoa APIs |
| FreeType 2.14.3 | `freetype-2.14.3-mavericks-experimental.zip` | Avoids unavailable CoreText dependencies |
| OpenAL Soft 1.23.1 | `openal-soft-1.23.1-mavericks-experimental.zip` | Uses an audio-library baseline compatible with the older C++ runtime |
| shaderc 2026.3 | `shaderc-2026.3-mavericks-experimental.zip` | Removes unavailable Darwin stack-check and C++ filesystem dependencies |

Checksums are in [SHA256](SHA256). These replacements were
prepared for the Minecraft 26.3 / LWJGL 3.4.3 setup inspected during this project.
Minecraft 26.1+ need these patches for the game to run.
Minecraft 1.21.11 needs only a JVM argument to prevent multiplayer from crashing the game
Minecraft 1.21.10 and earlier are unaffected, and these patches are not necessary.

## Install the patches for Minecraft 26.3

1. Close Minecraft and select the modified Java 25 bundle in
   the launcher's Java executable setting.
2. Extract the four ZIPs. Move their folders into your home folder:
   `SDL-Mavericks`, `FreeType-Mavericks`, `OpenAL-Mavericks`, and `Shaderc-Mavericks`.
3. Open that installation's **JVM Arguments** field. Preserve existing compatible
   arguments and append the settings below, separated by spaces.
4. Replace `YOUR_USERNAME` with the actual name of your home folder before saving.

```text
-Dorg.lwjgl.system.allocator=system
-Dorg.lwjgl.sdl.libname=/Users/YOUR_USERNAME/SDL-Mavericks/libSDL3.dylib
-Dorg.lwjgl.freetype.libname=/Users/YOUR_USERNAME/FreeType-Mavericks/libfreetype.dylib
-Dorg.lwjgl.openal.libname=/Users/YOUR_USERNAME/OpenAL-Mavericks/libopenal.dylib
-Dorg.lwjgl.shaderc.libname=/Users/YOUR_USERNAME/Shaderc-Mavericks/libshaderc.dylib
-Dio.netty.transport.noNative=true
```

The allocator setting bypasses the bundled jemalloc dependency on
`_clock_gettime_nsec_np`. The four library settings select the replacement files.
The last setting selects Java networking instead of Netty's native transport.
Remove any existing `-XX:+UseZGC` option: these Java ports exclude ZGC.

## Multiplayer crash in Minecraft 1.21.11 and later

The supplied 1.21.11 and 26.3 logs fail while loading
`libnetty_transport_native_kqueue_x86_64*.dylib`, because `_clock_gettime` is absent
from Mavericks. This can happen while pinging servers, connecting, or opening
to LAN.

Append this setting to **each affected installation**, even if it does not need
the four replacement libraries above:

```text
-Dio.netty.transport.noNative=true
```

Restart Minecraft, then test server-list pings, joining a server, and opening a
world to LAN with another client connecting. No networking replacement library
or mod is needed for this fallback. See the
[multiplayer diagnosis and verification](docs/MULTIPLAYER-MAVERICKS.md).

## Troubleshooting

| Symptom | Action |
| --- | --- |
| Missing `_clock_gettime_nsec_np` from `libjemalloc.dylib` | Check that `-Dorg.lwjgl.system.allocator=system` is present |
| SDL reports missing Metal or newer Cocoa APIs | Check the SDL override and extracted `libSDL3.dylib` path |
| FreeType reports missing CoreText symbols | Check the FreeType override |
| OpenAL reports `bad_variant_access` or another newer C++ dependency | Check the OpenAL override |
| shaderc reports missing `____chkstk_darwin` | Check the shaderc override |
| Multiplayer fails with `_clock_gettime` from Netty kqueue | Add `-Dio.netty.transport.noNative=true` and restart |
| Java rejects `UseZGC` | Remove that option; use a collector supported by the port, such as G1 |
| A replacement library cannot be found | Verify the absolute path and the spelling/case of each folder and filename |

Keep complete launcher output and macOS crash reports when reporting a new
failure, together with the Minecraft version, selected Java version, and JVM
arguments.

## Undo the changes

Close the game and remove the corresponding added JVM arguments. Once no
installation points to the replacement folders, they can be removed. The setup
does not require replacing macOS system libraries or editing Minecraft's cached
native libraries. Removing a workaround may restore the original crash.

## Source builds and validation

Build scripts target Intel macOS 10.9. They require the
component-specific sources and tools described in the linked guides, including
CMake and an Apple linker that supports the old deployment target.

| Component | Build script | Detailed guide |
| --- | --- | --- |
| SDL3 | `scripts/build-sdl-mavericks.sh` | [SDL source, patch, and graphics tests](docs/SDL3-MAVERICKS.md) |
| FreeType | `scripts/build-freetype-mavericks.sh` | [FreeType dependencies and font tests](docs/FREETYPE-MAVERICKS.md) |
| OpenAL | `scripts/build-openal-mavericks.sh` | [OpenAL source and audio tests](docs/OPENAL-MAVERICKS.md) |
| shaderc | `scripts/build-shaderc-mavericks.sh` | [Pinned compiler sources and shader tests](docs/SHADERC-MAVERICKS.md) |

Host validation includes native dependency audits, SDL window/context tests,
font rendering, OpenAL playback/loopback checks, and shader compilation. The
Netty fallback test is `tests/NettyFallbackSmoke.java`. Successful host tests are
separate from the user-reported Mavericks gameplay results above. Some component
guides retain the historical diagnosis from before the latest successful test.

## Licenses and attribution

This project is licensed under the GPL v2 license. For more information, see the attached license.
