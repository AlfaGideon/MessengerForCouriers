# Экип — native Android prototype

The Android APK is a native Java Android application. It does not load a website, use a WebView, or depend on Vite assets at runtime. Screens, navigation, video rendering, room directory, and controls are built from Android Views.

## Native video feed

Use the single plus/empty-state action to open Android's system video picker. The selected file is copied into the app's private storage and remains available after restart. The feed uses a native `TextureView`/`MediaPlayer` presentation, crops to fill the phone screen, supports tap to pause/play and vertical swipes between local clips, and has no download control. Files remain on the device and are not uploaded or visible to other people.

## Voice rooms

The APK contains a permanent directory with a common room and city rooms for Saint Petersburg, Moscow, Kazan, Yekaterinburg, and Novosibirsk. Joining opens a native room screen. Remote participants, microphone transport, speaker detection, clip sharing, and app-link routing require a deployed backend/signaling service; those are not implemented in this client-only prototype. The UI indicates this rather than simulating remote users.

## Android updates

The GitHub workflow publishes a new debug APK and checksum. Android installs app-code updates through its package installer with user confirmation; an Android application cannot silently replace its own APK from an arbitrary download. The earlier web-bundle updater is no longer used by the native APK.

## Build

Requirements: JDK 21, Android SDK platform 36, and build-tools 36.0.0.

```bash
bash scripts/build-android-apk.sh
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`.
