#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
python3 -m unittest discover -s backend -v
mkdir -p output/apk
cp app/build/outputs/apk/debug/app-debug.apk output/apk/TripSync-debug.apk
