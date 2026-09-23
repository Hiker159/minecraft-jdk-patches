# OpenAL replacement for Minecraft 26.3 on Mavericks

This experimental replacement is a fresh **OpenAL Soft 1.23.1** build for Intel
OS X 10.9. It uses C++14 and the native CoreAudio backend. This uses
an older OpenAL version to avoid the newer C++ runtime requirement. It is not
an update to the system's C++ library or a rebuild of Minecraft's newer OpenAL.

## Install

Extract `openal-soft-1.23.1-mavericks-experimental.zip` and move **OpenAL-Mavericks**
into your home folder. Add this to the launcher's **JVM arguments**:

```
-Dorg.lwjgl.openal.libname=/Users/Josh/OpenAL-Mavericks/libopenal.dylib
```

Keep the Java 25 Mavericks runtime and these existing arguments:

```
-Dorg.lwjgl.system.allocator=system
-Dorg.lwjgl.sdl.libname=/Users/Josh/SDL-Mavericks/libSDL3.dylib
-Dorg.lwjgl.freetype.libname=/Users/Josh/FreeType-Mavericks/libfreetype.dylib
```

Preserve your other JVM options. If your folder differs, use the library's actual
absolute path. Java does not expand `~`. Nothing under `/System/Library` or
`/usr/lib` needs replacing, and Minecraft's extracted native files stay intact.
To undo the audio override, remove only `org.lwjgl.openal.libname`.

## Source and reproduction

The package includes the unmodified upstream source archive and its LGPL license.
No upstream source patch was needed; this is a custom build configuration.
OpenAL Soft retains its authors' copyrights. You can rebuild and replace this
separate dynamic library using the same launcher override.

Source URL:
https://github.com/kcat/openal-soft/archive/refs/tags/1.23.1.tar.gz

Source SHA-256:
`dfddf3a1f61059853c625b7bb03de8433b455f2f79f89548cbcbd5edca3d4a4a`

Extract `source/openal-soft-1.23.1.tar.gz`, then on an Intel Mac with CMake and
an Apple linker supporting macOS 10.9:

```sh
scripts/build-openal-mavericks.sh /path/to/openal-soft-1.23.1 /path/to/build
```

Set `CMAKE` to an absolute CMake executable path if needed. The result is
`/path/to/build/libopenal.dylib`.

To run the included tests with the LWJGL 3.4.3 core Java/native and OpenAL Java JARs:

```sh
scripts/openal-test.sh /path/to/jdk/Contents/Home /path/to/lwjgl-jars /absolute/path/to/libopenal.dylib
scripts/openal-test.sh /path/to/jdk/Contents/Home /path/to/lwjgl-jars /absolute/path/to/libopenal.dylib --loopback
```