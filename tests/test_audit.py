import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('audit', Path(__file__).resolve().parents[1] / 'scripts/audit-macos.py')
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)

class AuditTests(unittest.TestCase):
    def inspect(self, target='10.9', symbols='', dependencies='', arch='x86_64', modern=False):
        def command(*args):
            if args[0] == 'lipo': return arch
            if args[0] == 'nm': return symbols
            if args[1] == '-L': return 'example:\n' + dependencies
            return ('Load command 1\ncmd LC_BUILD_VERSION\nminos ' if modern else
                    'Load command 1\ncmd LC_VERSION_MIN_MACOSX\nversion ') + target + '\nsdk 26.2\n' + ('tool LD\nversion 1225.1.2\n' if modern else '')
        with patch.object(audit, 'run', command):
            return audit.audit(Path('example'))
    def test_mavericks_target_not_sdk(self):
        self.assertEqual(self.inspect(), [])
        self.assertEqual(self.inspect(modern=True), [])
    def test_new_target_rejected(self):
        self.assertTrue(self.inspect('10.10'))
        self.assertTrue(self.inspect('11.0', modern=True))
    def test_missing_api_rejected(self):
        self.assertTrue(self.inspect(symbols='_clock_gettime\n'))
        self.assertTrue(self.inspect(symbols='____chkstk_darwin\n'))
        self.assertTrue(self.inspect(symbols='__ZTISt18bad_variant_access\n'))
        self.assertTrue(self.inspect(symbols='_kCTFontOpenTypeFeatureTag\n'))
        self.assertTrue(self.inspect(symbols='_clock_gettime_nsec_np\n'))
        self.assertTrue(self.inspect(symbols='_fstatat$INODE64\n'))
        self.assertTrue(self.inspect(symbols='_futimens\n'))
        self.assertFalse(self.inspect(symbols='_legacy_clock_gettime\n'))
    def test_metal_and_host_dependency(self):
        self.assertTrue(self.inspect(dependencies='\t/System/Library/Frameworks/Metal.framework/Versions/A/Metal (compatibility version 1.0.0)'))
        self.assertTrue(self.inspect(dependencies='\t/usr/local/lib/libfreetype.dylib (compatibility version 1.0.0)'))
    def test_wrong_arch(self):
        self.assertTrue(self.inspect(arch='arm64'))
        self.assertTrue(self.inspect(arch='x86_64 arm64'))

if __name__ == '__main__': unittest.main()
