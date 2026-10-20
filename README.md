# CarDash

Turns a spare Android phone into a car dashboard, with a companion app for your main phone.

- **CarDash** (`app/`) runs on the car phone (built for a OnePlus 5 on an Android 13 custom ROM, landscape). It's the phone's home screen.
- **CarDash Link** (`link/`) runs on your main phone (Android 12+, tested on Android 16). It talks to the car phone over your main phone's hotspot.
- **`linkproto/`** is the small protocol library both apps share.

Both apps are sideloaded, not published.

## CarDash (car phone)

- **Car on/off detection.** The charger (and optionally the car's Bluetooth) turns the car "on". "Off" waits until the signals have been gone for 20 seconds, to ride out the power dip while the engine cranks.
- **Car On routine.** Wi-Fi on (to join the main phone's hotspot), Do Not Disturb, auto brightness, screen on, dashboard to the front, dashcam on, and music resumes if it was playing at Car Off.
- **Car Off routine.** Pauses music, stops navigation and the dashcam, turns Wi-Fi off, shows a parked screen, then turns the screen off.
- **Dashboard.** A Google map with in-app turn-by-turn navigation (Google Navigation SDK) and place search with live suggestions. The map takes 60% of the screen, or 75% while a route runs. Alongside it:
  - now playing (this phone's player, or the main phone's through Link);
  - Maps | Home | Fav, where Fav holds saved places;
  - quick toggles;
  - front/back dashcam buttons;
  - clock, battery and temperature.
- **Navigate with CarDash, Google Maps or Waze.** If in-app routing fails, it hands off to the chosen app. You can paste coordinates or Google Maps links into the search box.
- **Dashcam.** Front or back camera, 3-minute clips in `Movies/CarDash`, loop recording within a size limit, and a hard jolt locks the current clip. Sound is optional and off by default.
- **Charge limit (root).** Pauses charging at 80% and resumes at 75%, to spare a battery that lives in a hot car.
- **Heat alert.** Shows a banner and notification when the battery gets hot.
- **Day/night theme.** Follows sunset at your GPS position.
- **Settings in tabs.** Car, Navigation, Music, Phone link, Dashcam, Battery and System.

## CarDash Link (main phone)

- **Send places to the car.** In Google Maps tap **Share → Send to car**, and the car starts the route. `maps.app.goo.gl` links are followed to the exact spot. If the car isn't connected yet, the place is queued until it is.
- **Search, Home, favourites and recents** on your phone. Searches run on the car phone's API key, so the main phone needs no key.
- **Music.** The car shows what's playing on your main phone, for example through a Bluetooth AUX receiver, and its buttons control it. Car Off pauses it, and Car On resumes it once the car's Bluetooth audio is back.
- **Dashcam clips.** Browse the car's clips by day, stream them with seeking, save them to the phone, and lock or delete them.
- **Battery-friendly.** The link only runs while your hotspot is on. Pairing is done once, with a 6-digit code.

## Google Maps API key

The map, in-app navigation and place search need a Google Maps Platform key with billing enabled. The free monthly allowances cover personal use. CarDash Link needs no key.

1. In Google Cloud, enable **Maps SDK for Android**, **Navigation SDK** and **Places API (New)**, then create an API key.
2. Restrict the key to Android apps (package `com.cardash.app` plus the SHA-1 of your signing key) and to those three APIs.
3. Add `MAPS_API_KEY=...` to `local.properties` in the project root. That file is git-ignored, so the key never reaches the repository.

Without a key, CarDash still works: a speedometer replaces the map, and routes open in Google Maps or Waze.

## Setup

**Car phone.** Factory-reset it and skip Google sign-in: Device Owner can only be set on a phone with no accounts. Enable USB debugging, plug it in, then run this from Git Bash:

```bash
./setup.sh --build
```

This installs CarDash, makes it Device Owner and the home screen, and grants its permissions. Once that's done, you can sign in to Google and install Google Maps and YouTube Music.

**Main phone.** Enable USB debugging, plug it in, then run:

```bash
./setup-link.sh --build
```

Turn on the hotspot and join the car phone to it. Then tap **Pair** in CarDash Link and enter the code on the car phone under **Settings → Phone link**.

**Removing CarDash.** Use **Settings → System → Release Device Owner…** on the car phone first. A Device Owner app can't be uninstalled until it's released.

## Why some choices are unusual

- **Device Owner.** This lets CarDash:
  - remove the lock screen and turn the screen off;
  - toggle Wi-Fi and Bluetooth (blocked for normal apps since Android 10);
  - grant its own permissions;
  - pin itself as the home app;
  - start the dashcam from the background.
- **Root (Magisk)** is only used for the charge limit, which writes to `/sys/class/power_supply/battery/charging_enabled`. On the OnePlus 5 this also cuts USB power, so the phone runs on battery while it holds at 80%.
- **The link runs over the hotspot, not the internet.** The main phone listens; the car phone connects to its hotspot gateway and answers an HMAC challenge using the token agreed at pairing. Clip videos stream over plain HTTP from the car phone, using a random key that changes every connection. Both stay on your own hotspot.

## Build and test

```bash
./gradlew assembleDebug testDebugUnitTest
```

The APKs end up in `app/build/outputs/apk/debug/` and `link/build/outputs/apk/debug/`. The unit tests cover:
- car on/off logic, the charge limiter and dashcam loop cleanup;
- sunrise/sunset times;
- coordinate and Google Maps link parsing, and geocoding choice;
- the link protocol, share parsing and HTTP byte ranges.

## Attribution

Maps, navigation and place search are provided by Google Maps Platform under its [Terms of Service](https://cloud.google.com/maps-platform/terms). Both apps list their open-source licences in-app (CarDash: Settings → System → Open-source licences; Link: About & licences). The Lexend font is used under the SIL Open Font License; see [`licenses/Lexend-OFL.txt`](licenses/Lexend-OFL.txt).
