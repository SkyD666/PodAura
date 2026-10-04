#!/usr/bin/env python3
"""Fetch checksum-pinned MPVKit frameworks and CA certificates into the build directory."""
import argparse
import concurrent.futures
import fcntl
import hashlib
import json
from pathlib import Path
import plistlib
import shutil
import subprocess
import tempfile
import zipfile


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


def prepare(artifact, destination, headers_only=False):
    name = artifact['name']
    marker = destination / 'receipts' / (name + ('-headers' if headers_only else ''))
    expected = 'Headers/mpv/client.h' if headers_only else name
    if marker.exists() and marker.read_text() == artifact['sha256']:
        if all((destination / sdk / f'{name}.framework' / expected).is_file()
               for sdk in ('iphoneos', 'iphonesimulator')):
            return
    downloads = destination / 'downloads'
    downloads.mkdir(exist_ok=True)
    archive = downloads / f'{name}.zip'
    download(artifact, archive)
    with tempfile.TemporaryDirectory(dir=destination) as temporary:
        root = Path(temporary)
        with zipfile.ZipFile(archive) as bundle:
            # Archives are pinned, but still reject paths escaping the extraction root.
            for entry in bundle.infolist():
                if not (root / entry.filename).resolve().is_relative_to(root):
                    raise RuntimeError(f'Invalid archive path: {entry.filename}')
            bundle.extractall(root)
        framework = root / f'{name}.xcframework'
        info = plistlib.loads((framework / 'Info.plist').read_bytes())
        for sdk, variant in [('iphoneos', None), ('iphonesimulator', 'simulator')]:
            library = next(lib for lib in info['AvailableLibraries']
                           if lib['SupportedPlatform'] == 'ios'
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
            else:
                # MoltenVK and some dependencies package a static archive, not a framework.
                target.mkdir()
                shutil.copy2(source, target / name)
    marker.parent.mkdir(exist_ok=True)
    marker.write_text(artifact['sha256'])
    print(f'Prepared {name}', flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--headers-only', action='store_true')
    mode.add_argument('--cert-only', action='store_true')
    args = parser.parse_args()
    here = Path(__file__).resolve().parent
    destination = here.parents[2] / 'shared/build/mpvkit'
    destination.mkdir(parents=True, exist_ok=True)
    manifest = json.loads((here / 'artifacts.json').read_text())
    artifacts = manifest['artifacts']
    if args.headers_only:
        artifacts = [item for item in artifacts if item['name'] == 'Libmpv']
    with (destination / '.lock').open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        if args.cert_only:
            download(manifest['caCertificate'], destination / 'downloads/cacert.pem')
            return
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
            list(pool.map(lambda item: prepare(item, destination, args.headers_only), artifacts))
        for sdk in ('iphoneos', 'iphonesimulator'):
            shutil.copy2(here / 'podaura.h',
                         destination / sdk / 'Libmpv.framework/Headers/mpv/podaura.h')
        if not args.headers_only:
            subprocess.run(['python3', str(here / 'build.py')], check=True)


if __name__ == '__main__':
    main()
