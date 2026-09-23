#!/bin/bash
set -euo pipefail
if [[ $# -ne 2 ]]; then
    echo "Usage: $0 /path/to/clean/SDL-3.4.14 /path/to/build-directory" >&2
    echo "Set CMAKE to a CMake executable if it is not on PATH." >&2
    exit 2
fi
project=$(cd "$(dirname "$0")/.." && pwd)
source_dir=$(cd "$1" && pwd)
mkdir -p "$2"
build_dir=$(cd "$2" && pwd)
[[ $(uname -s) == Darwin && $(uname -m) == x86_64 ]] || {
    echo "Build on an Intel Mac with a linker supporting macOS 10.9." >&2; exit 1;
}
for pair in 'MAJOR 3' 'MINOR 4' 'MICRO 14'; do
    set -- $pair
    grep -Eq "^#define SDL_${1}_VERSION[[:space:]]+$2$" "$source_dir/include/SDL3/SDL_version.h" || {
        echo "Expected SDL 3.4.14 sources" >&2; exit 1;
    }
done
# Validate the entire patch before changing any source file. Use clean source.
(cd "$source_dir" && git apply --check "$project/patches/sdl3/mavericks.patch" && git apply "$project/patches/sdl3/mavericks.patch")
"${CMAKE:-cmake}" -S "$source_dir" -B "$build_dir" \
    -DCMAKE_BUILD_TYPE=Release -DCMAKE_OSX_ARCHITECTURES=x86_64 \
    -DCMAKE_OSX_DEPLOYMENT_TARGET=10.9 \
    -DCMAKE_SHARED_LINKER_FLAGS=-Wl,-ld_classic \
    -DCMAKE_C_FLAGS=-Wunguarded-availability \
    -DCMAKE_OBJC_FLAGS=-Wunguarded-availability \
    -DSDL_METAL=OFF -DSDL_RENDER_METAL=OFF -DSDL_VULKAN=OFF \
    -DSDL_GPU=OFF -DSDL_CAMERA=OFF -DSDL_CLOCK_GETTIME=OFF \
    -DSDL_REVISION=SDL-3.4.14-mavericks-experimental \
    -DSDL_SHARED=ON -DSDL_STATIC=OFF -DSDL_TEST_LIBRARY=OFF -DSDL_TESTS=OFF
"${CMAKE:-cmake}" --build "$build_dir" -j "${JOBS:-6}"
echo "Built $build_dir/libSDL3.dylib; audit and test before using on Mavericks."
