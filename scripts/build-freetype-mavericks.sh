#!/bin/bash
set -euo pipefail
[[ $# == 1 ]] || { echo "Usage: $0 /path/to/build-workspace (set CMAKE if needed)" >&2; exit 2; }
[[ $(uname -s) == Darwin && $(uname -m) == x86_64 ]] || { echo "Use an Intel Mac build host." >&2; exit 1; }
mkdir -p "$1"
root=$(cd "$1" && pwd)
cmake_bin=${CMAKE:-cmake}
jobs=${JOBS:-4}
fetch() {
    local name=$1 url=$2 sha=$3
    if [[ ! -f "$root/$name.tar.gz" ]]; then
        curl --fail --location --retry 2 "$url" -o "$root/$name.tar.gz"
    fi
    [[ $(shasum -a 256 "$root/$name.tar.gz" | awk '{print $1}') == "$sha" ]] || {
        echo "Source checksum mismatch: $name" >&2; exit 1;
    }
    [[ ! -e "$root/$name" ]] || { echo "Use a fresh workspace; source exists: $name" >&2; exit 1; }
    mkdir "$root/$name"
    tar -xzf "$root/$name.tar.gz" -C "$root/$name" --strip-components=1
}
fetch freetype-2.14.3 https://github.com/freetype/freetype/archive/refs/tags/VER-2-14-3.tar.gz dc49de6b01a266eef4876a4dd34d9842c475d3e28ff2eff63bd2fb760ab56261
fetch harfbuzz-14.3.0 https://github.com/harfbuzz/harfbuzz/archive/refs/tags/14.3.0.tar.gz 566e996a1b40486954fb7110ffe6eb88a0f7958bb466cdb023b0302618acea4a
fetch libpng-1.6.55 https://github.com/pnggroup/libpng/archive/refs/tags/v1.6.55.tar.gz 71a2c5b1218f60c4c6d2f1954c7eb20132156cae90bdb90b566c24db002782a6
fetch brotli-1.1.0 https://github.com/google/brotli/archive/refs/tags/v1.1.0.tar.gz e720a6ca29428b803f4ad165371771f5398faba397edf6778837a18599ea13ff
common=(-DCMAKE_BUILD_TYPE=Release -DCMAKE_OSX_ARCHITECTURES=x86_64 -DCMAKE_OSX_DEPLOYMENT_TARGET=10.9 -DCMAKE_POSITION_INDEPENDENT_CODE=ON "-DCMAKE_INSTALL_PREFIX=$root/deps")
"$cmake_bin" -S "$root/harfbuzz-14.3.0" -B "$root/hb-build" "${common[@]}" \
    -DCMAKE_CXX_FLAGS=-Wunguarded-availability -DBUILD_SHARED_LIBS=OFF \
    -DHB_HAVE_CORETEXT=OFF -DHB_HAVE_FREETYPE=OFF -DHB_BUILD_SUBSET=OFF \
    -DHB_BUILD_RASTER=OFF -DHB_BUILD_VECTOR=OFF -DHB_BUILD_GPU=OFF
"$cmake_bin" --build "$root/hb-build" -j "$jobs"
"$cmake_bin" --install "$root/hb-build"
"$cmake_bin" -S "$root/libpng-1.6.55" -B "$root/png-build" "${common[@]}" \
    -DPNG_SHARED=OFF -DPNG_STATIC=ON -DPNG_FRAMEWORK=OFF -DPNG_TESTS=OFF -DPNG_TOOLS=OFF
"$cmake_bin" --build "$root/png-build" -j "$jobs"
"$cmake_bin" --install "$root/png-build"
"$cmake_bin" -S "$root/brotli-1.1.0" -B "$root/brotli-build" "${common[@]}" -DBUILD_SHARED_LIBS=OFF
"$cmake_bin" --build "$root/brotli-build" -j "$jobs"
"$cmake_bin" --install "$root/brotli-build"
"$cmake_bin" -S "$root/freetype-2.14.3" -B "$root/ft-build" "${common[@]}" \
    -DCMAKE_SHARED_LINKER_FLAGS=-Wl,-ld_classic -DCMAKE_C_FLAGS=-Wunguarded-availability \
    -DBUILD_SHARED_LIBS=ON -DFT_DYNAMIC_HARFBUZZ=OFF -DFT_REQUIRE_HARFBUZZ=ON \
    -DFT_REQUIRE_PNG=ON -DFT_REQUIRE_BROTLI=ON -DFT_REQUIRE_ZLIB=ON -DFT_REQUIRE_BZIP2=ON \
    "-DCMAKE_PREFIX_PATH=$root/deps" "-DPNG_LIBRARY=$root/deps/lib/libpng16.a" \
    "-DBROTLIDEC_LIBRARIES=$root/deps/lib/libbrotlidec.a;$root/deps/lib/libbrotlicommon.a"
"$cmake_bin" --build "$root/ft-build" -j "$jobs"
echo "Built $root/ft-build/libfreetype.dylib. Audit and test before using on Mavericks."
