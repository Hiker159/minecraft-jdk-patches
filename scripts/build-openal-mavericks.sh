#!/bin/bash
set -euo pipefail
if [[ $# -ne 2 ]]; then
    echo "Usage: $0 /path/to/openal-soft-1.23.1 /path/to/build-directory (set CMAKE if needed)" >&2
    exit 2
fi
[[ $(uname -s) == Darwin && $(uname -m) == x86_64 ]] || { echo "Use an Intel Mac build host." >&2; exit 1; }
source_dir=$(cd "$1" && pwd)
mkdir -p "$2"
build_dir=$(cd "$2" && pwd)
"${CMAKE:-cmake}" -S "$source_dir" -B "$build_dir" \
    -DCMAKE_POLICY_VERSION_MINIMUM=3.5 -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_OSX_ARCHITECTURES=x86_64 -DCMAKE_OSX_DEPLOYMENT_TARGET=10.9 \
    -DCMAKE_SHARED_LINKER_FLAGS=-Wl,-ld_classic -DCMAKE_CXX_FLAGS=-Wunguarded-availability \
    -DALSOFT_UTILS=OFF -DALSOFT_EXAMPLES=OFF \
    -DALSOFT_BACKEND_COREAUDIO=ON -DALSOFT_REQUIRE_COREAUDIO=ON \
    -DALSOFT_BACKEND_PIPEWIRE=OFF -DALSOFT_BACKEND_PULSEAUDIO=OFF \
    -DALSOFT_BACKEND_JACK=OFF -DALSOFT_BACKEND_PORTAUDIO=OFF \
    -DALSOFT_BACKEND_SDL2=OFF -DALSOFT_EMBED_HRTF_DATA=ON
"${CMAKE:-cmake}" --build "$build_dir" -j "${JOBS:-6}"
echo "Built $build_dir/libopenal.dylib. Audit and test before using on Mavericks."
