#!/usr/bin/env bash
set -euo pipefail

# Visual check: PODAURA_PIP_INSPECT_SECONDS=15 bash scripts/test-macos-pip.sh
# Confirm artwork and moving color bars before and after the player window closes.
# PODAURA_PIP_SHAPES="portrait" limits a visual inspection to one shape.
# Run on an unlocked desktop: AVKit completion callbacks depend on window animations.
if [[ "$(ioreg -n Root -d 1)" == *'"CGSSessionScreenIsLocked"=Yes'* ]]; then
    echo "Unlock this Mac before running the native PiP smoke test." >&2
    exit 1
fi
caffeinate -d -w "$$" &
repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"
command -v ffmpeg >/dev/null
fixture_dir="$repo_dir/shared/build/pip-smoke"
mkdir -p "$fixture_dir"

ffmpeg -hide_banner -loglevel error -y \
    -f lavfi -i "sine=frequency=440:sample_rate=48000:duration=60" \
    -c:a aac "$fixture_dir/audio.m4a"
ffmpeg -hide_banner -loglevel error -y \
    -f lavfi -i "testsrc2=size=640x640:rate=1" \
    -frames:v 1 "$fixture_dir/cover.png"
ffmpeg -hide_banner -loglevel error -y \
    -i "$fixture_dir/audio.m4a" -i "$fixture_dir/cover.png" \
    -map 0:a -map 1:v -c copy -disposition:v attached_pic "$fixture_dir/cover.m4a"

./gradlew :shared:linkDebugTestMacosArm64
for shape in ${PODAURA_PIP_SHAPES:-wide square portrait}; do
    case "$shape" in
        wide) size=640x360 ;;
        square) size=640x640 ;;
        portrait) size=360x640 ;;
        *) echo "Unknown PiP shape: $shape" >&2; exit 1 ;;
    esac
    ffmpeg -hide_banner -loglevel error -y \
        -f lavfi -i "testsrc2=size=$size:rate=24" \
        -i "$fixture_dir/audio.m4a" -t 60 -c:v libx264 -preset ultrafast \
        -pix_fmt yuv420p -c:a copy "$fixture_dir/$shape.mp4"
    ffmpeg -hide_banner -loglevel error -y -i "$fixture_dir/$shape.mp4" \
        -t 10 -c copy "$fixture_dir/unsupported.flv"
    # App preferences and AppKit are process-scoped; isolate each native session.
    for test_name in \
        unsupportedSystemVideoLeavesInlinePlaybackAvailable \
        nativeVideoEndDoesNotPauseMpvBeforePlaylistCompletion \
        nativePipSurvivesWindowCloseAndMediaChanges; do
        PODAURA_PIP_TEST_VIDEO="$fixture_dir/$shape.mp4" \
        PODAURA_PIP_TEST_AUDIO="$fixture_dir/audio.m4a" \
        PODAURA_PIP_TEST_IMAGE="$fixture_dir/cover.png" \
        PODAURA_PIP_TEST_COVER="$fixture_dir/cover.m4a" \
        PODAURA_PIP_TEST_UNSUPPORTED="$fixture_dir/unsupported.flv" \
            shared/build/bin/macosArm64/debugTest/test.kexe \
            "--ktest_filter=*MacosPictureInPictureTest.$test_name"
    done
done
