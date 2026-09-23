#!/bin/bash
set -euo pipefail
if [[ $# -lt 3 || $# -gt 4 ]]; then
    echo "Usage: $0 /path/to/java-home /path/to/lwjgl-3.4.3-jars /absolute/path/to/libSDL3.dylib [--fullscreen]" >&2
    exit 2
fi
project=$(cd "$(dirname "$0")/.." && pwd)
java_home=$(cd "$1" && pwd)
libraries=$(cd "$2" && pwd)
sdl_library=$(cd "$(dirname "$3")" && pwd)/$(basename "$3")
[[ -f "$sdl_library" ]] || { echo "SDL library not found" >&2; exit 1; }
for name in lwjgl lwjgl-sdl lwjgl-opengl; do
    [[ -f "$libraries/$name-3.4.3.jar" && -f "$libraries/$name-3.4.3-natives-macos.jar" ]] || {
        echo "Missing LWJGL 3.4.3 Java/native JAR for $name" >&2; exit 1;
    }
done
shift 3
"$java_home/bin/java" -XstartOnFirstThread --enable-native-access=ALL-UNNAMED \
    -Dorg.lwjgl.system.allocator=system "-Dorg.lwjgl.sdl.libname=$sdl_library" \
    --class-path "$libraries/lwjgl-3.4.3.jar:$libraries/lwjgl-3.4.3-natives-macos.jar:$libraries/lwjgl-sdl-3.4.3.jar:$libraries/lwjgl-sdl-3.4.3-natives-macos.jar:$libraries/lwjgl-opengl-3.4.3.jar:$libraries/lwjgl-opengl-3.4.3-natives-macos.jar" \
    "$project/tests/SDLGraphicsSmoke.java" "$@"
