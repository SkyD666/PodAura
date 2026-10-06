"""python3 -m unittest discover -s platform/apple/mpvkit -p 'test_*.py'."""
import hashlib
import json
from pathlib import Path
import plistlib
import shutil
import tempfile
import unittest
from unittest.mock import patch
import zipfile

from artifacts import INTEROP_HEADERS, prepare_framework
import prepare


class PrepareHeadersTest(unittest.TestCase):
    def test_all_interop_headers_are_prepared_without_runtime_tools(self):
        with tempfile.TemporaryDirectory() as directory:
            destination = Path(directory).resolve()
            downloads = destination / 'downloads'
            downloads.mkdir()
            artifacts = []
            sdks = [('iphoneos', 'ios', None), ('iphonesimulator', 'ios', 'simulator'),
                    ('macos', 'macos', None)]
            for name, header in INTEROP_HEADERS.items():
                archive = downloads / f'{name}.zip'
                libraries = []
                with zipfile.ZipFile(archive, 'w') as bundle:
                    for sdk, platform, variant in sdks:
                        library = dict(LibraryIdentifier=sdk, LibraryPath=f'{name}.framework',
                                       SupportedPlatform=platform, SupportedArchitectures=['arm64'])
                        if variant:
                            library['SupportedPlatformVariant'] = variant
                        libraries.append(library)
                        prefix = f'{name}.xcframework/{sdk}/{name}.framework/'
                        bundle.writestr(prefix + name, b'upstream runtime')
                        bundle.writestr(prefix + 'Headers/' + header, name.encode())
                    bundle.writestr(f'{name}.xcframework/Info.plist',
                                    plistlib.dumps(dict(AvailableLibraries=libraries)))
                artifacts.append(dict(name=name, sha256=hashlib.sha256(archive.read_bytes()).hexdigest()))
            manifest = destination / 'artifacts.json'
            manifest.write_text(json.dumps(dict(artifacts=artifacts + [dict(name='UnneededRuntime')])))
            with patch.object(prepare, 'DEST', destination), patch.object(prepare, 'MANIFEST', manifest), \
                    patch.object(prepare.subprocess, 'run', side_effect=AssertionError('Runtime tool invoked')):
                for platform in ('ios', 'macos'):
                    with patch('sys.argv', ['prepare.py', '--platform', platform, '--headers-only']):
                        prepare.main()
                        for sdk, family, _ in sdks:
                            if family == platform:
                                (destination / sdk / 'Libavformat.framework/Headers/cached.h').touch()
                        prepare.main()  # Cached headers must not be extracted again.
            for sdk, _, _ in sdks:
                self.assertTrue((destination / sdk / 'Libavformat.framework/Headers/cached.h').is_file())
                for name, header in INTEROP_HEADERS.items():
                    framework = destination / sdk / f'{name}.framework'
                    self.assertEqual((framework / 'Headers' / header).read_bytes(), name.encode())
                    self.assertFalse((framework / name).exists())
                    if name != 'Libmpv':
                        self.assertEqual((destination / sdk / 'include' / name.lower() / header).read_bytes(),
                                         name.encode())
            self.assertFalse((destination / 'native').exists())
            self.assertTrue(all(receipt.name.endswith('-headers')
                                for receipt in (destination / 'receipts').iterdir()))

    def test_headers_never_replace_native_library_or_claim_runtime_receipt(self):
        with tempfile.TemporaryDirectory() as directory:
            destination = Path(directory).resolve()
            archive = destination / 'downloads/Libmpv.zip'
            archive.parent.mkdir()
            libraries = []
            with zipfile.ZipFile(archive, 'w') as bundle:
                for sdk in ('iphoneos', 'iphonesimulator'):
                    library = dict(LibraryIdentifier=sdk, LibraryPath='Libmpv.framework',
                                   SupportedPlatform='ios', SupportedArchitectures=['arm64'])
                    if sdk == 'iphonesimulator':
                        library['SupportedPlatformVariant'] = 'simulator'
                    libraries.append(library)
                    prefix = f'Libmpv.xcframework/{sdk}/Libmpv.framework/'
                    bundle.writestr(prefix + 'Libmpv', b'upstream library')
                    bundle.writestr(prefix + 'Headers/mpv/client.h', b'client header')
                bundle.writestr('Libmpv.xcframework/Info.plist',
                                plistlib.dumps(dict(AvailableLibraries=libraries)))
            artifact = dict(name='Libmpv', sha256=hashlib.sha256(archive.read_bytes()).hexdigest())

            # A fresh header-only preparation must not publish a runtime binary or receipt.
            prepare_framework(artifact, destination, platform='ios', headers_only=True)
            self.assertFalse((destination / 'receipts/Libmpv').exists())
            for sdk in ('iphoneos', 'iphonesimulator'):
                self.assertFalse((destination / sdk / 'Libmpv.framework/Libmpv').exists())
            prepare_framework(artifact, destination, platform='ios')

            # Model native compilation, then Gradle cleaning only its declared header outputs.
            for sdk in ('iphoneos', 'iphonesimulator'):
                framework = destination / sdk / 'Libmpv.framework'
                (framework / 'Libmpv').write_bytes(b'custom library with podaura_output symbols')
                (framework / 'podaura-build').write_text('native fingerprint')
                shutil.rmtree(framework / 'Headers')
            prepare_framework(artifact, destination, platform='ios', headers_only=True)
            prepare_framework(artifact, destination, platform='ios', headers_only=True)
            prepare_framework(artifact, destination, platform='ios')
            for sdk in ('iphoneos', 'iphonesimulator'):
                framework = destination / sdk / 'Libmpv.framework'
                self.assertEqual((framework / 'Libmpv').read_bytes(),
                                 b'custom library with podaura_output symbols')
                self.assertEqual((framework / 'podaura-build').read_text(), 'native fingerprint')
                self.assertEqual((framework / 'Headers/mpv/client.h').read_bytes(), b'client header')


class MacosFrameworkTest(unittest.TestCase):
    def test_versioned_binary_and_root_headers_survive_headers_only_then_runtime(self):
        with tempfile.TemporaryDirectory() as temporary:
            destination = Path(temporary)
            downloads = destination / 'downloads'
            downloads.mkdir()
            archive = downloads / 'Libmpv.zip'
            root = 'Libmpv.xcframework/'
            framework = root + 'macos-arm64_x86_64/Libmpv.framework/'
            with zipfile.ZipFile(archive, 'w') as bundle:
                bundle.writestr(root + 'Info.plist', plistlib.dumps({'AvailableLibraries': [{
                    'LibraryIdentifier': 'macos-arm64_x86_64', 'LibraryPath': 'Libmpv.framework',
                    'SupportedPlatform': 'macos', 'SupportedArchitectures': ['arm64', 'x86_64'],
                }]}))
                bundle.writestr(framework + 'Headers/mpv/client.h', 'client header')
                bundle.writestr(framework + 'Libmpv', 'Versions/Current/Libmpv')
                bundle.writestr(framework + 'Versions/A/Libmpv', b'!<arch>\nstatic library')
            artifact = {'name': 'Libmpv', 'sha256': hashlib.sha256(archive.read_bytes()).hexdigest()}
            prepare_framework(artifact, destination, platform='macos', headers_only=True)
            prepare_framework(artifact, destination, platform='macos')
            target = destination / 'macos/Libmpv.framework'
            self.assertEqual((target / 'Headers/mpv/client.h').read_text(), 'client header')
            self.assertEqual((target / 'Libmpv').read_bytes(), b'!<arch>\nstatic library')
            # A cached headers-only invocation must never overwrite the custom runtime.
            (target / 'Libmpv').write_bytes(b'rebuilt mpv')
            prepare_framework(artifact, destination, platform='macos', headers_only=True)
            self.assertEqual((target / 'Libmpv').read_bytes(), b'rebuilt mpv')
            self.assertFalse((destination / 'iphoneos').exists())


if __name__ == '__main__':
    unittest.main()
