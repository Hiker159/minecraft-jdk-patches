#!/bin/bash
set -euo pipefail
if [[ $# -lt 3 || $# -gt 4 ]]; then
    echo "Usage: $0 /path/to/java-home /path/to/lwjgl-3.4.3-jars /path/to/libopenal.dylib [--loopback]" >&2
    exit 2
fi
project=$(cd "$(dirname "$0")/.." && pwd)
java_home=$(cd "$1" && pwd)
libraries=$(cd "$2" && pwd)
openal=$(cd "$(dirname "$3")" && pwd)/$(basename "$3")
shift 3
"$java_home/bin/java" --enable-native-access=ALL-UNNAMED \
    -Dorg.lwjgl.system.allocator=system "-Dorg.lwjgl.openal.libname=$openal" \
    --class-path "$libraries/lwjgl-3.4.3.jar:$libraries/lwjgl-3.4.3-natives-macos.jar:$libraries/lwjgl-openal-3.4.3.jar" \
    "$project/tests/OpenALSmoke.java" "$@"
