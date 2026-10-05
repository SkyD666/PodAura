#!/usr/bin/env python3
"""Prepare pinned MPVKit frameworks or CA certificates in the shared build directory."""
import argparse
import concurrent.futures
import fcntl
import json
from pathlib import Path
import shutil
import subprocess
import sys

from artifacts import DEST, MANIFEST, download, prepare_framework


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--platform', choices=('ios', 'macos'))
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--headers-only', action='store_true')
    mode.add_argument('--cert-only', action='store_true')
    args = parser.parse_args()
    if not args.cert_only and args.platform is None:
        parser.error('--platform is required unless --cert-only is used')
    DEST.mkdir(parents=True, exist_ok=True)
    manifest = json.loads(MANIFEST.read_text())
    artifacts = manifest['artifacts']
    if args.headers_only:
        artifacts = [item for item in artifacts if item['name'] == 'Libmpv']
    with (DEST / '.lock').open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        if args.cert_only:
            download(manifest['caCertificate'], DEST / 'downloads/cacert.pem')
            return
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
            list(pool.map(lambda item: prepare_framework(
                item, DEST, platform=args.platform, headers_only=args.headers_only), artifacts))
        for sdk in (('macos',) if args.platform == 'macos' else ('iphoneos', 'iphonesimulator')):
            shutil.copy2(Path(__file__).with_name('podaura.h'),
                         DEST / sdk / 'Libmpv.framework/Headers/mpv/podaura.h')
        if not args.headers_only:
            subprocess.run([sys.executable, str(Path(__file__).with_name('build.py')),
                            '--platform', args.platform], check=True)


if __name__ == '__main__':
    main()
