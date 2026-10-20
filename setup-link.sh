#!/usr/bin/env bash
# CarDash Link setup: installs the companion app on your MAIN phone over ADB and grants what it
# needs (music access, running in the background, notifications), so there's nothing to tap through.
#
# Usage:
#   ./setup-link.sh            install the existing debug APK
#   ./setup-link.sh --build    build it first (./gradlew :link:assembleDebug)
#
# Before running: enable Developer options → USB debugging on the main phone and plug it in.
# If the car phone is plugged in too, unplug it (or pass -s <serial> via ANDROID_SERIAL).

set -u

PKG="com.cardash.link"
LISTENER="$PKG/$PKG.media.LinkListenerService"
APK="link/build/outputs/apk/debug/link-debug.apk"

GREEN='\033[1;32m'
YELLOW='\033[1;33m'
RED='\033[1;31m'
NC='\033[0m'

cd "$(dirname "$0")"

step() { echo -e "${GREEN}==>${NC} $1"; }
warn() { echo -e "${YELLOW}!!${NC} $1"; }
fail() { echo -e "${RED}xx${NC} $1"; exit 1; }

try() {
    local out
    if ! out=$(adb shell "$@" 2>&1) || [[ "$out" == *"Exception"* || "$out" == *"Error"* ]]; then
        warn "'$*' failed: $out"
        return 1
    fi
    [ -n "$out" ] && echo "   $out"
    return 0
}

if [ "${1:-}" = "--build" ]; then
    step "Building CarDash Link"
    ./gradlew :link:assembleDebug || fail "Build failed"
fi
[ -f "$APK" ] || fail "No APK at $APK. Run ./setup-link.sh --build"

step "Waiting for the main phone (accept the USB debugging prompt on it)"
adb wait-for-device
echo "   $(adb shell getprop ro.product.model | tr -d '\r') — Android $(adb shell getprop ro.build.version.release | tr -d '\r')"

step "Installing $APK"
adb install -r "$APK" || fail "Install failed"

step "Granting access"
# Shows the main phone's music on the car and lets the car control it.
try cmd notification allow_listener "$LISTENER"
try pm grant "$PKG" android.permission.POST_NOTIFICATIONS
# Lets the link start by itself when the hotspot turns on.
try dumpsys deviceidle whitelist "+$PKG"

step "Opening CarDash Link"
try am start -n "$PKG/.MainActivity"

echo
echo -e "${GREEN}Done.${NC} To pair:"
echo "  1. Turn on this phone's hotspot and connect the car phone to it."
echo "  2. In CarDash Link tap Pair; on the car phone open Settings → Phone link and enter the code."
echo "  3. In Google Maps, Share → Send to car sends any place to the car."
