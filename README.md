# CarDash

A home-screen app that turns an old phone (a OnePlus 5 on Android 10) into a car dashboard. It is sideloaded, not published.

## What it does

- **Home screen:** the app is the phone's launcher, laid out for landscape. It has an Apps drawer and a Settings screen.
- **Car on/off detection:** either the charger or the car's hands-free Bluetooth turns the car "on". "Off" happens once both signals are gone for 20 seconds; the delay covers the USB power dip while the engine cranks. If your socket stays powered with the engine off, turn on *Charger stays on with engine off* in Settings, and Bluetooth alone decides.
- **Car On routine:** turns on Bluetooth and Wi-Fi (to join your phone's hotspot), Do Not Disturb and auto brightness, then wakes the screen and shows the dashboard. The screen stays on while the car is on.
- **Car Off routine:** pauses media, turns off DND and Wi-Fi, and turns the screen off. Bluetooth stays on, because the app needs it to detect the car next time.
- **Dashboard:**
  - A map preview using OSM/CARTO tiles, so no API key is needed.
  - A GPS speedometer.
  - A now-playing card that works with any player.
  - One-tap Home/Work buttons that open Google Maps or Waze already navigating.
  - Quick toggles, a clock, battery level and battery temperature.
- **Heat alert:** a banner and a notification when the battery reaches the warning temperature (45 °C by default, adjustable), and a critical alert 5 °C above that.
- **Day/night theme:** switches automatically at sunrise and sunset for your GPS position. It can be forced to Day or Night.
- **Manual car mode:** the power button among the quick toggles forces On or Off, so you can test without the car. The next real charger or Bluetooth change hands control back to automatic detection.

## Why some choices are unusual

- **`targetSdk = 28` (Android 9):** `setWifiEnabled()` and `BluetoothAdapter.enable()` only work for apps that target API 28 or lower. That's fine for a sideloaded app, but never publish this one.
- **Device Owner:** this lets the app:
  - remove the lock screen,
  - turn the screen off (`lockNow`),
  - grant its own location permission,
  - pin itself as the home app.

  Device Owner needs a phone with **no accounts**. That means no Play Store, so sideload Spotify, Maps and Waze.
- **Platform `LocationManager` instead of Play services:** the app doesn't depend on Google.

## Setup

1. Factory-reset the phone and skip Google sign-in.
2. Enable Developer options, then USB debugging.
3. From this folder, in Git Bash:

```bash
./setup.sh --build
```

The script:
- installs the APK and makes the app Device Owner,
- grants location, media-session access, Do Not Disturb access and WRITE_SETTINGS,
- excludes the app from battery optimisation,
- sets it as the home app and removes the lock screen.

4. On the phone, open **Settings** (the gear icon), pick the car's Bluetooth device and enter your destinations.

## Undo

Use **Settings → Release Device Owner…** on the phone. After that you can uninstall the app normally. A Device Owner app can't be uninstalled until it's released, or the phone is factory-reset.

## Build and test

```bash
./gradlew assembleDebug testDebugUnitTest
```

The unit tests cover the car on/off logic, sunrise/sunset times, heat levels, speed rounding and destination storage.
