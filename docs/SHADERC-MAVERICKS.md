# Shaderc replacement for Minecraft 26.3 on Mavericks

Minecraft creates a Cocoa window and initializes
OpenGL 4.1 on Intel Iris Pro under OS X 10.9.5, using the earlier SDL, FreeType and
OpenAL overrides. It then fails in `libshaderc.dylib` because `____chkstk_darwin`
is missing from Mavericks. There is also a newer libc++ filesystem import
that Mavericks does not provide.

This experimental Intel shaderc v2026.3 build targets macOS 10.9 and removes both
dependencies.

## Install

Extract `shaderc-2026.3-mavericks-experimental.zip` and move **Shaderc-Mavericks**
into your home folder. Add this **JVM argument**:

```
-Dorg.lwjgl.shaderc.libname=/Users/Josh/Shaderc-Mavericks/libshaderc.dylib
```

Keep Java 25 selected and preserve all existing options, including:

```
-Dorg.lwjgl.system.allocator=system
-Dorg.lwjgl.sdl.libname=/Users/Josh/SDL-Mavericks/libSDL3.dylib
-Dorg.lwjgl.freetype.libname=/Users/Josh/FreeType-Mavericks/libfreetype.dylib
-Dorg.lwjgl.openal.libname=/Users/Josh/OpenAL-Mavericks/libopenal.dylib
```

If you choose a different location, use its actual absolute path; `~` is not
expanded by Java. No system library or extracted Minecraft library needs replacing.
Remove only the shaderc override to undo this change.

## Changes

- Build shaderc v2026.3 and its three pinned compiler dependencies for x86_64/macOS 10.9.
- Disable compiler-generated Darwin stack-check instrumentation requiring the
  unavailable `____chkstk_darwin` helper. No fake/no-op replacement symbol is installed.
- Patch glslang's diagnostic filename handling to use its existing fallback when
  `GLSLANG_LEGACY_MACOS` is set, avoiding the newer `std::filesystem::absolute` API.
  Diagnostics may keep a relative filename instead of expanding it to an absolute path.
- Retain the shader compiler, SPIR-V validation/optimization components, and HLSL
  support. Optional test executables and examples are excluded from this build.

Limits: this is not a complete Minecraft launch or a test of every shader,
resource pack, mod, shader compiler feature or target-machine graphics driver.
LWJGL's deprecated Unsafe warning appeared during successful host tests.
Third-party build files suppress warnings, so binary-import checks and execution
tests are material parts of validation, rather than relying on warning absence.

## Sources and reproduction

The package includes the four pinned source archives, source checksums in
`patches/shaderc/sources.json`, the glslang patch, build/test scripts and licenses.
This is a modified build assembled for jdk-macos-legacy; original authors retain
their copyrights. The shaderc archive is v2026.3; compiler dependencies are pinned
by that release's DEPS file.

On an Intel Mac with Python 3, CMake, and an Apple linker supporting 10.9:

```sh
scripts/build-shaderc-mavericks.sh /path/to/fresh-build-workspace
```

Set `CMAKE` to an absolute CMake executable if needed. The script checks archive
hashes, applies the patch and builds `build/libshaderc/libshaderc_shared.dylib`.
In this distribution it uses the included source archives; when used in the
project without them it downloads the same pinned archives. The source workspace
must be fresh to avoid silently reusing changed sources.

To test with LWJGL 3.4.3 core Java/native and shaderc Java JARs:

```sh
scripts/shaderc-test.sh /path/to/jdk/Contents/Home /path/to/lwjgl-jars /absolute/path/to/libshaderc.dylib /path/to/minecraft-26.3-client.jar
```

The final client JAR argument is optional; with it, the test compiles the two
real Minecraft shaders as well. Game assets are read from your existing JAR and
are not distributed in this package.
