# Экип

Mobile-first Android/Web application shell for a courier community. First install starts with an empty media library; no sample courier photos, posts, chats, or rooms are seeded.

## Run locally

```bash
npm ci
npm run dev
```

Production web build:

```bash
npm run build
```

## Video storage

The video picker accepts video files from the phone/gallery. It saves the selected video blob and caption to IndexedDB inside the app's WebView origin, restores saved items after restart, plays them in the feed, and supports deleting them. This is **on-device storage only**: the file is not uploaded to a cloud server and cannot be seen by other users. Removing the app/clearing app data removes its local videos.

## Android APK

The Capacitor Android project is in `android/` (`ru.ekip.couriers`). To build a debug APK with Android Studio/JDK 21 and Android SDK 36 installed:

```bash
npm ci
npm run android:apk
```

The output path is `android/app/build/outputs/apk/debug/app-debug.apk`.

A GitHub Actions workflow at `.github/workflows/android-apk.yml` builds the APK on the current Arena branch and publishes it as a downloadable workflow artifact.

## Product limitations

There is no backend service or account system in this repository. Therefore uploaded videos are private to the device; cross-user feeds, real messaging, server-backed rooms, and live voice/WebRTC are not enabled. Those require a deployed API, user/authentication model, media storage, database, and voice signalling service. This build does not claim those features are live.
