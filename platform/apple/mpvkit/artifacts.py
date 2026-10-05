"""Shared pinned downloads and XCFramework extraction for Apple targets."""
import hashlib
from pathlib import Path
import plistlib
import shutil
import subprocess
import tarfile
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[3]
DEST = ROOT / 'shared/build/mpvkit'
MANIFEST = Path(__file__).with_name('artifacts.json')
MPV = {'url': 'https://codeload.github.com/mpv-player/mpv/tar.gz/refs/tags/v0.41.0',
       'sha256': 'ee21092a5ee427353392360929dc64645c54479aefdb5babc5cfbb5fad626209'}
HEADERS = {'url': 'https://codeload.github.com/KhronosGroup/Vulkan-Headers/tar.gz/e3b1eec08173d6b825cd3ac88c885a63b621504a',
           'sha256': 'f492279345cbc10708b64fcd432b3ff6c8246a5837c4db2b649abba00cf82208'}


def download(artifact, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    if not target.exists() or hashlib.sha256(target.read_bytes()).hexdigest() != artifact['sha256']:
        partial = target.with_suffix('.partial')
        subprocess.run(['curl', '-fLsS', '--retry', '3', '--connect-timeout', '30',
                        '--max-time', '900', artifact['url'], '-o', str(partial)], check=True)
        if hashlib.sha256(partial.read_bytes()).hexdigest() != artifact['sha256']:
            partial.unlink()
            raise RuntimeError(f'Checksum mismatch: {target.name}')
        partial.replace(target)


def prepare_framework(artifact, destination, *, platform, headers_only=False):
    sdks = {
        'ios': [('iphoneos', None), ('iphonesimulator', 'simulator')],
        'macos': [('macos', None)],
    }[platform]
    name = artifact['name']
    marker = destination / 'receipts' / (name + ('-macos-v2' if platform == 'macos' else '') + ('-headers' if headers_only else ''))
    expected = 'Headers/mpv/client.h' if headers_only else name
    if marker.exists() and marker.read_text() == artifact['sha256']:
        if all((destination / sdk / f'{name}.framework' / expected).is_file()
               for sdk, _ in sdks):
            return
    downloads = destination / 'downloads'
    downloads.mkdir(exist_ok=True)
    archive = downloads / f'{name}.zip'
    download(artifact, archive)
    with tempfile.TemporaryDirectory(dir=destination) as temporary:
        root = Path(temporary).resolve()
        with zipfile.ZipFile(archive) as bundle:
            # Archives are pinned, but still reject paths escaping the extraction root.
            for entry in bundle.infolist():
                if not (root / entry.filename).resolve().is_relative_to(root):
                    raise RuntimeError(f'Invalid archive path: {entry.filename}')
            bundle.extractall(root)
        framework = root / f'{name}.xcframework'
        info = plistlib.loads((framework / 'Info.plist').read_bytes())
        for sdk, variant in sdks:
            library = next(lib for lib in info['AvailableLibraries']
                           if lib['SupportedPlatform'] == platform
                           and lib.get('SupportedPlatformVariant') == variant
                           and 'arm64' in lib['SupportedArchitectures'])
            source = framework / library['LibraryIdentifier'] / library['LibraryPath']
            target = destination / sdk / f'{name}.framework'
            if headers_only:
                source = source / 'Headers'
                target = target / 'Headers'
            target.parent.mkdir(parents=True, exist_ok=True)
            if target.exists():
                shutil.rmtree(target)
            if source.is_dir():
                shutil.copytree(source, target)
                # zipfile extracts symlinks as text; flatten versioned macOS binaries.
                if (source / 'Versions/A' / name).is_file():
                    shutil.copy2(source / 'Versions/A' / name, target / name)
            else:
                # MoltenVK and some dependencies package a static archive, not a framework.
                target.mkdir()
                shutil.copy2(source, target / name)
    marker.parent.mkdir(exist_ok=True)
    marker.write_text(artifact['sha256'])
    print(f'Prepared {name}', flush=True)


def extract(artifact, archive, destination):
    download(artifact, archive)
    with tarfile.open(archive) as bundle:
        for entry in bundle.getmembers():
            if not (destination / entry.name).resolve().is_relative_to(destination.resolve()):
                raise RuntimeError(f'Invalid source path: {entry.name}')
        bundle.extractall(destination, **({'filter': 'data'} if hasattr(tarfile, 'data_filter') else {}))
