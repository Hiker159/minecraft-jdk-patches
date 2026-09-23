# SDL3 replacement for Minecraft 26.3 on Mavericks

This is an **experimental, modified SDL 3.4.14 x86_64 build targeting OS X 10.9**.
It addresses the Metal dependency in Minecraft 26.3's bundled SDL library.

## Install

1. Extract `sdl3-3.4.14-mavericks-experimental.zip`.
2. Move the extracted `SDL-Mavericks` folder into your home folder. The library's full path will then be
   `/Users/YOURNAME/SDL-Mavericks/libSDL3.dylib`.
3. Keep using the existing Java 25 Mavericks build. In the launcher's **JVM
   arguments**, preserve your other arguments and add these two options:

```
-Dorg.lwjgl.system.allocator=system
-Dorg.lwjgl.sdl.libname=/Users/Josh/SDL-Mavericks/libSDL3.dylib
```

If you put the folder elsewhere, change the second argument to the library's
actual absolute path. Java does not expand `~` in this property. If the path has
spaces, quote the whole argument as your launcher requires. The suggested home
folder location avoids spaces.

Launch Minecraft 26.3 and use the OpenGL renderer. This build disables Vulkan
and Metal support. Keep the system allocator setting: it addresses the separate
jemalloc missing-clock failure. To undo the SDL change, remove only the `org.lwjgl.sdl.libname` argument.

## Changes

- Build the same SDL 3.4.14 version identified in Minecraft's LWJGL 3.4.3 native JAR.
- Disable Metal, Vulkan, SDL's GPU subsystem, and camera capture. The corresponding
  public entry points remain exported, but these optional facilities are unavailable.
- Retain Cocoa windows, OpenGL, audio, joystick backends, clipboard, and tray support.
- Use SDL's existing Mach clock/gettimeofday paths rather than newer clock APIs.
- Replace macOS 10.12 block-timer constructors with a compatible timer adapter.
- Use ordered window enumeration for mouse focus; guard window tabbing and newer
  touch/controller APIs; use legacy string and status-item methods.

This is an SDL library replacement, not a Minecraft mod. Minecraft's inspected
startup method loads SDL through LWJGL's normal loader, which accepts the
`org.lwjgl.sdl.libname` override.

## Reproduce

Source: https://github.com/libsdl-org/SDL/archive/refs/tags/release-3.4.14.tar.gz

Source SHA-256:
`9d57b178fb297e121ef2605275937b7afaa7cd24d99ce1f95953e69e7a2535d6`

With a clean extracted source tree, CMake, and an Intel Mac with a linker that
supports 10.9, run from this project's root:

```sh
scripts/build-sdl-mavericks.sh /path/to/SDL-3.4.14 /path/to/sdl-build
```

The script applies `patches/sdl3/mavericks.patch` before building. Set the `CMAKE`
environment variable to an absolute CMake executable path if needed. All source
changes are in that patch; this is not an unmodified upstream SDL release.
SDL's original zlib license is included as `SDL-LICENSE.txt` in the archive.

To run the matching graphics test with the six LWJGL 3.4.3 core/SDL/OpenGL JARs:

```sh
scripts/sdl-graphics-test.sh /path/to/jdk/Contents/Home /path/to/lwjgl-jars /absolute/path/to/libSDL3.dylib --fullscreen
```

The optional test temporarily captures the mouse and enters fullscreen, then
returns to windowed mode and exits. The test requires Java/native JAR pairs for
`lwjgl`, `lwjgl-sdl`, and `lwjgl-opengl`, all version 3.4.3.