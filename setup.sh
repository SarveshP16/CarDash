#!/usr/bin/env bash
# CarDash setup: installs the app on the car phone over ADB, makes it Device Owner
# and grants every permission in one go — no tapping through settings screens.
#
# Usage:
#   ./setup.sh            install the existing debug APK
#   ./setup.sh --build    build it first (./gradlew assembleDebug)
#
# Before running: factory-reset the phone, skip Google sign-in (Device Owner needs a phone
# with NO accounts), enable Developer options → USB debugging, and plug it in.

set -u

PKG="com.cardash.app"
ADMIN="$PKG/.admin.AdminReceiver"
LISTENER="$PKG/$PKG.media.MediaListenerService"
ACTIVITY="$PKG/.MainActivity"
APK="app/build/outputs/apk/debug/app-debug.apk"

GREEN='\033[1;32m'
YELLOW='\033[1;33m'
RED='\033[1;31m'
NC='\033[0m'

cd "$(dirname "$0")"

echo "
   ____           ____            _
  / ___|__ _ _ __|  _ \\  __ _ ___| |__
 | |   / _\` | '__| | | |/ _\` / __| '_ \\
 | |__| (_| | |  | |_| | (_| \\__ \\ | | |
  \\____\\__,_|_|  |____/ \\__,_|___/_| |_|

                                - CrYsTAxiT
"

step() { echo -e "${GREEN}==>${NC} $1"; }
warn() { echo -e "${YELLOW}!!${NC} $1"; }
fail() { echo -e "${RED}xx${NC} $1"; exit 1; }

# Runs an adb shell command; a failure is a warning, not a stop (some steps are OS-version dependent).
try() {
    local out
    if ! out=$(adb shell "$@" 2>&1) || [[ "$out" == *"Exception"* || "$out" == *"Error"* ]]; then
        warn "'$*' failed: $out"
        return 1
    fi
    [ -n "$out" ] && echo "   $out"
    return 0
}

# --- 0. Build -------------------------------------------------------------------
if [ "${1:-}" = "--build" ]; then
    step "Building debug APK"
    ./gradlew assembleDebug || fail "Build failed"
fi
[ -f "$APK" ] || fail "No APK at $APK. Run ./setup.sh --build"

# --- 1. Device ------------------------------------------------------------------
step "Waiting for the phone (accept the USB debugging prompt on it)"
adb wait-for-device
echo "   $(adb shell getprop ro.product.model | tr -d '\r') — Android $(adb shell getprop ro.build.version.release | tr -d '\r')"

# --- 2. Install -----------------------------------------------------------------
step "Installing $APK"
adb install -r -g "$APK" || fail "Install failed"

# --- 3. Device Owner ------------------------------------------------------------
step "Making CarDash the Device Owner"
owner=$(adb shell dpm list-owners 2>/dev/null | tr -d '\r')
if [[ "$owner" == *"$PKG"* ]]; then
    echo "   Already Device Owner"
else
    accounts=$(adb shell dumpsys account | grep -c "Account {" || true)
    if [ "$accounts" -gt 0 ]; then
        warn "The phone has $accounts account(s). Remove them all (Settings → Accounts), then re-run."
        warn "Continuing without Device Owner: lock screen and silent permissions won't work."
    else
        try dpm set-device-owner "$ADMIN"
    fi
fi

# --- 4. Permissions -------------------------------------------------------------
step "Granting permissions"
try pm grant "$PKG" android.permission.ACCESS_FINE_LOCATION
try pm grant "$PKG" android.permission.ACCESS_COARSE_LOCATION
try pm grant "$PKG" android.permission.BLUETOOTH_CONNECT
try pm grant "$PKG" android.permission.POST_NOTIFICATIONS
try pm grant "$PKG" android.permission.CAMERA
# "install -g" grants every permission, including the microphone. Dashcam sound is off by default:
# CarDash grants itself the microphone only when Settings → Dashcam → Record sound is switched on.
try pm revoke "$PKG" android.permission.RECORD_AUDIO
# Now-playing card: read other apps' media sessions.
try cmd notification allow_listener "$LISTENER"
# Do Not Disturb while driving.
try cmd notification allow_dnd "$PKG"
# Brightness (fallback for when the app isn't Device Owner).
try appops set "$PKG" WRITE_SETTINGS allow

# --- 5. Keep it alive -----------------------------------------------------------
step "Excluding CarDash from battery optimisation (OxygenOS kills background apps)"
try dumpsys deviceidle whitelist "+$PKG"
try cmd appops set "$PKG" RUN_IN_BACKGROUND allow

# --- 6. Phone setup -------------------------------------------------------------
step "Setting CarDash as the home screen"
try cmd package set-home-activity "$ACTIVITY"

step "Turning Bluetooth on and removing the lock screen"
try svc bluetooth enable || true
try locksettings set-disabled true || warn "A PIN/pattern is set: remove it in Settings → Security"

# --- 7. Launch ------------------------------------------------------------------
step "Starting CarDash"
try am start -n "$ACTIVITY"

echo
echo -e "${GREEN}Done.${NC} On the phone, open Settings (gear on the dashboard):"
echo "  1. Car: detection works from the charger alone; optionally pick the car's Bluetooth too."
echo "  2. Navigation: set your Home address and favourites, and choose CarDash / Google Maps / Waze."
echo "  3. Phone link: pair with CarDash Link on your main phone (see ./setup-link.sh)."
echo "  4. Now that CarDash is Device Owner you can sign in to Google: install Google Maps"
echo "     from the Play Store, and YouTube Music (Morphe-patched) — CarDash auto-detects it."
echo "  5. Turn airplane mode off: while it's on, Android won't let apps toggle Wi-Fi."
