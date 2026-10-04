#!/usr/bin/env python3
"""Build PodAura's IOSurface output against the checksum-pinned MPVKit libraries."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tarfile

from prepare import download

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
DEST = ROOT / 'shared/build/mpvkit'
SOURCES = [HERE / name for name in ('build.py', 'iosurface.patch', 'context_podaura.m',
                                  'context_podaura.h', 'podaura.h', 'artifacts.json')]
MPV = {'url': 'https://codeload.github.com/mpv-player/mpv/tar.gz/refs/tags/v0.41.0',
       'sha256': 'ee21092a5ee427353392360929dc64645c54479aefdb5babc5cfbb5fad626209'}
HEADERS = {'url': 'https://codeload.github.com/KhronosGroup/Vulkan-Headers/tar.gz/e3b1eec08173d6b825cd3ac88c885a63b621504a',
           'sha256': 'f492279345cbc10708b64fcd432b3ff6c8246a5837c4db2b649abba00cf82208'}


def extract(artifact, archive, destination):
    download(artifact, archive)
    with tarfile.open(archive) as bundle:
        for entry in bundle.getmembers():
            if not (destination / entry.name).resolve().is_relative_to(destination.resolve()):
                raise RuntimeError(f'Invalid source path: {entry.name}')
        bundle.extractall(destination, **({'filter': 'data'} if hasattr(tarfile, 'data_filter') else {}))


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
    sdk_path = subprocess.check_output(['xcrun', '--sdk', sdk, '--show-sdk-path'], text=True).strip()
    clang = subprocess.check_output(['xcrun', '--sdk', sdk, '--find', 'clang'], text=True).strip()
    triple = 'arm64-apple-ios17.0' + ('-simulator' if sdk == 'iphonesimulator' else '')
    compile_args = ['-target', triple, '-isysroot', sdk_path, '-F' + str(DEST / sdk)]
    names = [x['name'] for x in json.loads((HERE / 'artifacts.json').read_text())['artifacts']
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
               MACOS_SDK=sdk_path, MACOS_SDK_VERSION='0.0', SDKROOT=sdk_path)
    # Suppress macOS SDK feature detection: the cross compiler already targets iOS.
    env['MACOS_SDK_VERSION'] = '17.0'
    out = work / 'out'
    subprocess.run(['meson', 'setup', str(out), str(source), '--cross-file', str(cross),
                    '--buildtype=release', '-Ddefault_library=static', '-Dauto_features=disabled',
                    '-Dlibmpv=true', '-Dcplayer=false', '-Dgpl=false', '-Dvulkan=enabled',
                    '-Diconv=enabled', '-Dzlib=enabled', '-Dlua=disabled', '-Dgl=disabled',
                    '-Dvideotoolbox-pl=enabled', '-Daudiounit=enabled', '-Duchardet=enabled',
                    '-Dswift-build=disabled'], env=env, check=True)
    subprocess.run(['ninja', '-C', str(out)], env=env, check=True)
    target = DEST / sdk / 'Libmpv.framework'
    shutil.copy2(out / 'libmpv.a', target / 'Libmpv')
    shutil.copy2(HERE / 'podaura.h', target / 'Headers/mpv/podaura.h')


def main():
    marker = DEST / 'receipts/podaura-output'
    fingerprint = hashlib.sha256(b''.join(p.read_bytes() for p in SOURCES)).hexdigest()
    if marker.exists() and marker.read_text() == fingerprint and all(
        (DEST / sdk / 'Libmpv.framework/podaura-build').exists() and
        (DEST / sdk / 'Libmpv.framework/podaura-build').read_text() == fingerprint
        for sdk in ('iphoneos', 'iphonesimulator')):
        return
    for tool in ('meson', 'ninja', 'pkg-config'):
        if not shutil.which(tool): raise RuntimeError(f'{tool} is required to build iOS libmpv')
    directory = DEST / 'native/source'
    if directory.exists(): shutil.rmtree(directory)
    directory.mkdir(parents=True)
    extract(MPV, DEST / 'downloads/mpv-v0.41.0.tar.gz', directory)
    extract(HEADERS, DEST / 'downloads/vulkan-headers.tar.gz', directory)
    source = directory / 'mpv-0.41.0'
    headers = next(directory.glob('Vulkan-Headers-*'))
    subprocess.run(['patch', '-p1', '-i', str(HERE / 'iosurface.patch')], cwd=source, check=True)
    for name in ('context_podaura.m', 'context_podaura.h'):
        shutil.copy2(HERE / name, source / 'video/out/vulkan' / name)
    shutil.copy2(HERE / 'podaura.h', source / 'include/mpv/podaura.h')
    for sdk in ('iphoneos', 'iphonesimulator'):
        out = DEST / 'native' / sdk / 'out'
        if out.exists(): shutil.rmtree(out)
        build(sdk, source, headers)
        (DEST / sdk / 'Libmpv.framework/podaura-build').write_text(fingerprint)
    marker.write_text(fingerprint)


if __name__ == '__main__': main()
