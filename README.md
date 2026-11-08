# Экип

Android-first app for couriers. A fresh install opens an empty video library. No sample posts or courier photos are bundled.

## On-device videos

The Android file picker opens the phone's video library. A selected video and its optional caption are stored in IndexedDB inside the app's WebView. The app restores the media after it is closed and reopened, plays it in the library, and allows local deletion. Storage is private to that installation; clearing app data or uninstalling the app removes those files.

This is on-device persistence, not a cloud upload. There is no hosted backend or account system in this repository, so saved videos are not visible to other couriers. Shared feeds and chat delivery require a deployed API, accounts/database, and media storage.

## Persistent voice-room directory

The app includes a common room and permanent city-room entries for Saint Petersburg, Moscow, Kazan, Yekaterinburg, and Novosibirsk. The room list does not depend on someone creating a room or being present. At this stage, tapping a room opens it locally on the device; it does not connect microphones or show remote participants. Real cross-device voice requires a deployed WebRTC signalling/media service, which is not included here.

## Android over-the-air updates

The Android shell checks the public GitHub OTA manifest on launch and can download a versioned web bundle over HTTPS. The native updater verifies the bundle SHA-256, rejects unexpected download hosts and unsafe ZIP paths, extracts it inside app storage, and serves the new interface on the next WebView load. The Settings screen also has a manual update check. OTA only updates the web interface; changes to native Android code still require installing a new APK. OTA metadata and APKs are published by `.github/workflows/android-apk.yml` to `release/` on the Arena branch.

## Build the APK

The Android shell is a native WebView with a system video picker and the app's yellow bird launcher icon. The build script uses Android SDK tools directly, so it does not need to download a Gradle distribution.

Requirements: Node.js 22, JDK 21, Android SDK platform 36 and build-tools 36.0.0.

```bash
npm ci
npm run android:apk
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`.
