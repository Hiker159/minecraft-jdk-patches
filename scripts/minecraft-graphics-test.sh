#!/bin/bash
set -euo pipefail
if [[ $# -ne 2 ]]; then
    echo "Usage: $0 /path/to/jdk/Contents/Home /path/to/lwjgl-3.3.3-jars" >&2; exit 2
fi
project=$(cd "$(dirname "$0")/.." && pwd)
java_home=$(cd "$1" && pwd)
libraries=$(cd "$2" && pwd)
for name in lwjgl lwjgl-glfw lwjgl-opengl; do
    [[ -f "$libraries/$name-3.3.3.jar" && -f "$libraries/$name-3.3.3-natives-macos.jar" ]] || {
        echo "Missing Java or native JAR for $name 3.3.3" >&2; exit 1;
    }
done
# This exercises graphics, not the optional jemalloc allocator or FreeType.
"$java_home/bin/java" -XstartOnFirstThread --enable-native-access=ALL-UNNAMED -Dorg.lwjgl.system.allocator=system \
    --class-path "$libraries/*" "$project/tests/MinecraftGraphicsSmoke.java"
