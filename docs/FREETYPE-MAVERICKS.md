# FreeType replacement for Minecraft 26.3 on Mavericks

This experimental FreeType 2.14.3 replacement targets Intel OS X 10.9. It includes
HarfBuzz 14.3.0 with its CoreText backend disabled, libpng 1.6.55, and Brotli 1.1.0
as static dependencies. It retains OpenType shaping/auto-hinting, PNG glyph support,
and WOFF2 decoding. It uses Mavericks' system zlib and bzip2 libraries. No CoreText
or other Apple graphics framework is required by this replacement.

## Install

Extract `freetype-2.14.3-mavericks-experimental.zip`, then move **FreeType-Mavericks**
into your home folder. On the reported machine, add this **JVM argument**:

```
-Dorg.lwjgl.freetype.libname=/Users/Josh/FreeType-Mavericks/libfreetype.dylib
```

Keep both existing workarounds:

```
-Dorg.lwjgl.system.allocator=system
-Dorg.lwjgl.sdl.libname=/Users/YOURNAME/SDL-Mavericks/libSDL3.dylib
```

Keep using the Java 25 Mavericks build. Preserve all the other launcher arguments.
If you choose a different folder, use the actual absolute path; `~` is not
expanded by Java. Remove the FreeType argument to undo this change.