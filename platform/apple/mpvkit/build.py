#!/usr/bin/env python3
"""Build native libmpv with the selected Apple platform's output patches."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

from artifacts import DEST, MANIFEST, MPV, HEADERS, extract

HERE = Path(__file__).resolve().parent
COMMON_SOURCES = [HERE / 'build.py', HERE / 'artifacts.py', MANIFEST]


def build(sdk, source, headers):
    work = DEST / 'native' / sdk
    include = work / 'include'
    pc = work / 'pkgconfig'
    include.mkdir(parents=True, exist_ok=True)
    pc.mkdir(exist_ok=True)
    versions = {'libass': '0.17.4', 'libplacebo': '7.360.1', 'uchardet': '0.0.8'}
    libraries = {'libavcodec': 'Libavcodec', 'libavfilter': 'Libavfilter',
                 'libavformat': 'Libavformat', 'libavutil': 'Libavutil',
                 'libswresample': 'Libswresample', 'libswscale': 'Libswscale',
                 'libass': 'Libass', 'libplacebo': 'Libplacebo', 'uchardet': 'Libuchardet'}
    for name, framework in libraries.items():
        location = DEST / sdk / f'{framework}.framework/Headers'
        namespace = 'ass' if name == 'libass' else name
        if (location / namespace).is_dir(): location = location / namespace
        link = include / namespace
        if link.is_symlink(): link.unlink()
        link.symlink_to(location, target_is_directory=True)
        if name.startswith(('libav', 'libsw')):
            version_text = ''.join(p.read_text() for p in location.glob('version*.h'))
            prefix = name.upper()
            versions[name] = '.'.join(re.search(rf'#define {prefix}_VERSION_{part}\s+(\d+)',
                                                version_text)[1] for part in ('MAJOR', 'MINOR', 'MICRO'))
        variables = 'pl_has_vulkan=1\n' if name == 'libplacebo' else ''
        cflags = f'-I{include}' + (f' -I{include / namespace}' if name == 'uchardet' else '')
        (pc / f'{name}.pc').write_text(f'Name: {name}\nDescription: pinned MPVKit {name}\n'
            f'Version: {versions[name]}\nCflags: {cflags}\nLibs: -framework {framework}\n{variables}')
    (pc / 'vulkan.pc').write_text(f'Name: Vulkan\nDescription: pinned MoltenVK\nVersion: 1.4.0\n'
        f'Cflags: -I{headers / "include"}\nLibs: -framework MoltenVK\n')
    apple_sdk = 'macosx' if sdk == 'macos' else sdk
    sdk_path = subprocess.check_output(['xcrun', '--sdk', apple_sdk, '--show-sdk-path'], text=True).strip()
    clang = subprocess.check_output(['xcrun', '--sdk', apple_sdk, '--find', 'clang'], text=True).strip()
    triple = ('arm64-apple-macos12.0' if sdk == 'macos' else
              'arm64-apple-ios17.0' + ('-simulator' if sdk == 'iphonesimulator' else ''))
    compile_args = ['-target', triple, '-isysroot', sdk_path, '-F' + str(DEST / sdk)]
    names = [x['name'] for x in json.loads(MANIFEST.read_text())['artifacts']
             if x['name'] != 'Libmpv']
    system = ['AVFoundation', 'CoreAudio', 'AudioToolbox', 'CoreVideo', 'CoreMedia',
              'Metal', 'VideoToolbox', 'QuartzCore', 'IOSurface', 'Foundation', 'CoreFoundation']
    link_args = compile_args + [arg for name in names + system for arg in ('-framework', name)]
    link_args += ['-lbz2', '-liconv', '-lexpat', '-lresolv', '-lxml2', '-lz', '-lc++']
    cross = work / 'cross.ini'
    cross.write_text(f"[binaries]\nc = '{clang}'\nobjc = '{clang}'\n"
        f"ar = 'ar'\nstrip = 'strip'\npkg-config = 'pkg-config'\n"
        f"[host_machine]\nsystem = 'darwin'\ncpu_family = 'aarch64'\ncpu = 'arm64'\nendian = 'little'\n"
        f"[properties]\nneeds_exe_wrapper = true\n[built-in options]\n"
        f'c_args = {compile_args!r}\nobjc_args = {compile_args!r}\n'
        f'c_link_args = {link_args!r}\nobjc_link_args = {link_args!r}\n')
    env = dict(os.environ, PKG_CONFIG_LIBDIR=str(pc), PKG_CONFIG_PATH='',
               MACOS_SDK=sdk_path, SDKROOT=sdk_path,
               MACOS_SDK_VERSION='12.0' if sdk == 'macos' else '17.0')
    out = work / 'out'
    subprocess.run(['meson', 'setup', str(out), str(source), '--cross-file', str(cross),
                    '--buildtype=release', '-Ddefault_library=static', '-Dauto_features=disabled',
                    '-Dlibmpv=true', '-Dcplayer=false', '-Dgpl=false', '-Dvulkan=enabled',
                    '-Diconv=enabled', '-Dzlib=enabled', '-Dlua=disabled', '-Dgl=disabled',
                    '-Dvideotoolbox-pl=enabled', '-Duchardet=enabled',
                    '-Dswift-build=disabled'] + (['-Dcoreaudio=enabled']
                        if sdk == 'macos' else ['-Daudiounit=enabled']), env=env, check=True)
    subprocess.run(['ninja', '-C', str(out)], env=env, check=True)
    target = DEST / sdk / 'Libmpv.framework'
    shutil.copy2(out / 'libmpv.a', target / 'Libmpv')
    shutil.copy2(HERE / 'podaura.h', target / 'Headers/mpv/podaura.h')


def build_platform(platform):
    sdks = ('macos',) if platform == 'macos' else ('iphoneos', 'iphonesimulator')
    patches = ['iosurface.patch'] + (['coreaudio.patch'] if platform == 'macos' else [])
    sources = COMMON_SOURCES + [HERE / name for name in
        patches + ['context_podaura.m', 'context_podaura.h', 'podaura.h']]
    fingerprint = hashlib.sha256(b''.join(p.read_bytes() for p in sources)).hexdigest()
    if all((DEST / sdk / 'Libmpv.framework/podaura-build').exists() and
           (DEST / sdk / 'Libmpv.framework/podaura-build').read_text() == fingerprint
           for sdk in sdks):
        return
    for tool in ('meson', 'ninja', 'pkg-config'):
        if not shutil.which(tool): raise RuntimeError(f'{tool} is required to build Apple libmpv')
    directory = DEST / 'native' / f'{platform}-source'
    if directory.exists(): shutil.rmtree(directory)
    directory.mkdir(parents=True)
    extract(MPV, DEST / 'downloads/mpv-v0.41.0.tar.gz', directory)
    extract(HEADERS, DEST / 'downloads/vulkan-headers.tar.gz', directory)
    source = directory / 'mpv-0.41.0'
    headers = next(directory.glob('Vulkan-Headers-*'))
    for patch in patches:
        subprocess.run(['patch', '-p1', '-i', str(HERE / patch)], cwd=source, check=True)
    for name in ('context_podaura.m', 'context_podaura.h'):
        shutil.copy2(HERE / name, source / 'video/out/vulkan' / name)
    shutil.copy2(HERE / 'podaura.h', source / 'include/mpv/podaura.h')
    for sdk in sdks:
        out = DEST / 'native' / sdk / 'out'
        if out.exists(): shutil.rmtree(out)
        build(sdk, source, headers)
        (DEST / sdk / 'Libmpv.framework/podaura-build').write_text(fingerprint)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--platform', choices=('ios', 'macos'), required=True)
    args = parser.parse_args()
    build_platform(args.platform)


if __name__ == '__main__':
    main()
