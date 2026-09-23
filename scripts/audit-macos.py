#!/usr/bin/env python3
"""Reject known Mavericks binary blockers. This is not a runtime certification."""
import argparse
import pathlib
import re
import subprocess
import sys

BAD_SYMBOLS = {
    '____chkstk_darwin',
    '__ZNSt3__14__fs10filesystem10__absoluteERKNS1_4pathEPNS_10error_codeE',
    '_clock_gettime', '_clock_gettime_nsec_np', '_clock_getres', '_clonefile', '_fclonefileat',
    '_futimens', '_utimensat', '_fchmodat', '_openat', '_fstatat',
    '_fstatat$INODE64', '_fdopendir', '_fdopendir$INODE64',
    '_disconnectx', '_getentropy', '_os_unfair_lock_lock', '_os_unfair_lock_unlock',
    '__ZTISt18bad_variant_access', '__ZTVSt18bad_variant_access',
    '_kCTFontOpenTypeFeatureTag', '_kCTFontOpenTypeFeatureValue',
    '_OBJC_CLASS_$_CAMetalLayer', '_OBJC_CLASS_$_NSStatusBarButton',
    '_OBJC_CLASS_$_NSAccessibilityElement',
}
MACH_MAGICS = {b'\xcf\xfa\xed\xfe', b'\xce\xfa\xed\xfe', b'\xfe\xed\xfa\xcf',
               b'\xfe\xed\xfa\xce', b'\xca\xfe\xba\xbe', b'\xbe\xba\xfe\xca',
               b'\xca\xfe\xba\xbf', b'\xbf\xba\xfe\xca'}

def run(*args):
    return subprocess.check_output(args, text=True, stderr=subprocess.STDOUT)

def version(value):
    parts = tuple(map(int, value.split('.')))
    return parts + (0,) * (3 - len(parts))

def audit(path):
    errors = []
    if run('lipo', '-archs', str(path)).strip() != 'x86_64':
        errors.append('not an Intel x86_64-only binary')
    load = run('otool', '-l', str(path))
    minimums = []
    for block in load.split('Load command '):
        if 'LC_VERSION_MIN_MACOSX' in block:
            minimums.extend(re.findall(r'^\s*version\s+(\d+\.\d+(?:\.\d+)?)', block, re.M))
        elif 'LC_BUILD_VERSION' in block:
            # "version" in this command describes the linker, not the OS.
            minimums.extend(re.findall(r'^\s*minos\s+(\d+\.\d+(?:\.\d+)?)', block, re.M))
    if not minimums:
        errors.append('no macOS deployment target')
    elif any(version(v) > (10, 9, 0) for v in minimums):
        errors.append('deployment target above Mavericks: ' + ', '.join(minimums))
    own_ids = set()
    for block in load.split('Load command '):
        if 'LC_ID_DYLIB' in block:
            own_ids.update(re.findall(r'^\s*name (.*?) \(offset', block, re.M))
    dependencies = run('otool', '-L', str(path))
    for line in dependencies.splitlines()[1:]:
        dep = line.strip().split(' (')[0]
        if dep in own_ids:
            continue
        if any(x in dep for x in ('/Metal.framework/', '/MetalKit.framework/')):
            errors.append('depends on Metal: ' + dep)
        if dep.startswith(('/opt/', '/usr/local/', '/Users/')):
            errors.append('depends on a build-host library: ' + dep)
    symbols = run('nm', '-u', str(path))
    for symbol in BAD_SYMBOLS:
        if re.search(r'(?<!\S)' + re.escape(symbol) + r'(?!\S)', symbols):
            errors.append('imports post-Mavericks API: ' + symbol)
    return errors

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('image', type=pathlib.Path, help='JDK image, .jdk bundle, or native library directory')
    args = p.parse_args()
    if not args.image.is_dir():
        p.error('image must be a directory')
    count = 0
    failed = False
    for file in sorted(args.image.rglob('*')):
        if file.is_symlink() or not file.is_file():
            continue
        with file.open('rb') as stream:
            if stream.read(4) not in MACH_MAGICS:
                continue
        # Java class files share the fat Mach-O magic. Verify the file type.
        if "Mach-O" not in run("file", "-b", str(file)):
            continue
        count += 1
        try:
            errors = audit(file)
        except subprocess.CalledProcessError as e:
            errors = ['inspection failed: ' + e.output.strip()]
        for error in errors:
            print(f'FAIL {file}: {error}')
            failed = True
    if not count:
        print('FAIL: no Mach-O binaries found')
        return 1
    print(f'Inspected {count} Mach-O files. Runtime testing on Mavericks is still required.')
    return int(failed)

if __name__ == '__main__':
    sys.exit(main())
