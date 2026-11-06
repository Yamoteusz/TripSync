#!/bin/sh
set -eu
TRIPSYNC_SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
if [ ! -x "$TRIPSYNC_SDK/emulator/emulator" ]; then
    echo 'Nie znaleziono emulatora. Ustaw ANDROID_SDK_ROOT na katalog Android SDK.' >&2
    exit 1
fi
if [ "$#" -ne 1 ]; then
    echo 'Użycie: sh scripts/start_emulator.sh NAZWA_AVD' >&2
    "$TRIPSYNC_SDK/emulator/emulator" -list-avds
    exit 1
fi
exec "$TRIPSYNC_SDK/emulator/emulator" -avd "$1" -dns-server "${TRIPSYNC_DNS_SERVERS:-1.1.1.1,8.8.8.8}" -no-snapshot-load
