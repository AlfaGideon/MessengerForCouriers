# Экип

Android-first local courier media app. A fresh install opens an empty video library. No sample posts or courier photos are bundled.

## Video upload and persistence

The Android file picker opens the phone's video library. A selected video and its optional caption are stored as a Blob in IndexedDB inside the app's WebView origin. The app restores the media after it is closed and reopened, plays it in the library, and allows local deletion. Storage is private to that installation; clearing app data or uninstalling the app removes those files.

This is real on-device persistence, not a cloud upload. There is no hosted backend or account system in this repository, so the saved video is not visible to other couriers. Shared feeds, chat delivery, and real voice rooms require a deployed API, accounts/database, media storage, and a WebRTC signalling service; these screens intentionally start empty instead of showing fake users or demo rooms.

## Build the APK

The Android shell is a native WebView with a system video picker and the app's own yellow bird launcher icon. The APK build script uses Android SDK tools directly, so it does not need to download a Gradle distribution.

Requirements: Node.js 22, JDK 21, Android SDK platform 36 and build-tools 36.0.0.

```bash
npm ci
npm run android:apk
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`.

The workflow `.github/workflows/android-apk.yml` builds and uploads the debug APK artifact on pushes to the Arena branch.
