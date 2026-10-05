#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
TRIPSYNC_BUILD_ROOT="${TRIPSYNC_BUILD_ROOT:-${TMPDIR:-/tmp}/tripsync-build}"
./gradlew -PtripsyncBuildRoot="$TRIPSYNC_BUILD_ROOT" :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
python3 -m unittest discover -s backend -v
mkdir -p output/apk
cp "$TRIPSYNC_BUILD_ROOT/app/outputs/apk/debug/app-debug.apk" output/apk/TripSync-debug.apk
