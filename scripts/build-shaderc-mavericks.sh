#!/bin/bash
set -euo pipefail
[[ $# == 1 ]] || { echo "Usage: $0 /path/to/fresh/build-workspace (set CMAKE if needed)" >&2; exit 2; }
[[ $(uname -s) == Darwin && $(uname -m) == x86_64 ]] || { echo "Use an Intel Mac build host." >&2; exit 1; }
project=$(cd "$(dirname "$0")/.." && pwd)
mkdir -p "$1"
root=$(cd "$1" && pwd)
[[ ! -e "$root/source" ]] || { echo "Use a fresh workspace; source already exists." >&2; exit 1; }
fetch() {
    local name=$1 url=$2 sha=$3 dest=$4
    if [[ ! -f "$root/$name.tar.gz" ]]; then
        if [[ -f "$project/source/$name.tar.gz" ]]; then
            cp "$project/source/$name.tar.gz" "$root/$name.tar.gz"
        else
            curl --fail --location --retry 2 "$url" -o "$root/$name.tar.gz"
        fi
    fi
    [[ $(shasum -a 256 "$root/$name.tar.gz" | awk '{print $1}') == "$sha" ]] || {
        echo "Source checksum mismatch: $name" >&2; exit 1;
    }
    mkdir -p "$root/$dest"
    tar -xzf "$root/$name.tar.gz" -C "$root/$dest" --strip-components=1
}
fetch shaderc-v2026.3 https://github.com/google/shaderc/archive/refs/tags/v2026.3.tar.gz ee493ccf1b3038b4ef2fe024664c5eb2dc4bcc1f6b05b33e3909de0e19c81024 source
fetch shaderc-glslang https://github.com/KhronosGroup/glslang/archive/168d452a4f460d24b588fed08477a81c44ee27a1.tar.gz c167b2474af06cbab93abae8249efaf88fb79a09f30488e2a24d399e4258ce7a source/third_party/glslang
fetch shaderc-spirv-headers https://github.com/KhronosGroup/SPIRV-Headers/archive/29981f65241605e08b0ede4cfeb999fe3b723c6a.tar.gz 232899f1ad4104fb5bc377b94596c7621575eee62ad9a9e8f929b63a7dd8a7ad source/third_party/spirv-headers
fetch shaderc-spirv-tools https://github.com/KhronosGroup/SPIRV-Tools/archive/b707790a898e44038547df54580022fc1cf89c3d.tar.gz 05d8af89737bde57571c48dbd36714c9f520a69623e14de72c3be6b600e277d6 source/third_party/spirv-tools
(cd "$root/source" && git apply --check "$project/patches/shaderc/mavericks.patch" && git apply "$project/patches/shaderc/mavericks.patch")
"${CMAKE:-cmake}" -S "$root/source" -B "$root/build" \
    -DCMAKE_BUILD_TYPE=Release -DCMAKE_OSX_ARCHITECTURES=x86_64 \
    -DCMAKE_OSX_DEPLOYMENT_TARGET=10.9 -DCMAKE_SHARED_LINKER_FLAGS=-Wl,-ld_classic \
    '-DCMAKE_CXX_FLAGS=-DGLSLANG_LEGACY_MACOS -fno-stack-check -Wunguarded-availability' \
    -DCMAKE_C_FLAGS=-fno-stack-check -DSHADERC_SKIP_TESTS=ON \
    -DSHADERC_SKIP_EXAMPLES=ON -DSHADERC_SKIP_EXECUTABLES=ON \
    -DSHADERC_ENABLE_WERROR_COMPILE=OFF -DSPIRV_SKIP_TESTS=ON \
    -DSPIRV_SKIP_EXECUTABLES=ON -DENABLE_GLSLANG_BINARIES=OFF
"${CMAKE:-cmake}" --build "$root/build" --target shaderc_shared -j "${JOBS:-6}"
echo "Built $root/build/libshaderc/libshaderc_shared.dylib; audit and test before using on Mavericks."
