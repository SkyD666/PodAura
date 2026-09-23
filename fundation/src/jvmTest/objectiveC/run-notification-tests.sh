#!/bin/sh
set -eu

test_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
bridge_dir="$test_dir/../../jvmMain/objectiveC/macMediaPlayer"
test_build_dir=$(mktemp -d "${TMPDIR:-/tmp}/podaura-notifications-tests.XXXXXX")
trap 'rm -rf "$test_build_dir"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

xcrun --sdk macosx clang \
    -fobjc-arc -fblocks -mmacosx-version-min=11.0 \
    -Wall -Wextra -Werror -UNDEBUG \
    -I "$bridge_dir" \
    "$test_dir/PodAuraNotificationsTests.m" \
    -framework Foundation -framework UserNotifications \
    -o "$test_build_dir/notification-tests"
"$test_build_dir/notification-tests"
