# Курьерка — native Android prototype

The Android APK is a native Java Android application. It does not load a website, use a WebView, or depend on Vite assets at runtime. Screens, navigation, video rendering, room directory, and controls are built from Android Views.

## Native video feed

Use the single plus/empty-state action to open Android's system video picker. The selected file is copied into the app's private storage and remains available after restart. The feed uses a native `TextureView`/`MediaPlayer` presentation, crops to fill the phone screen, supports tap to pause/play and vertical swipes between local clips, and has no download control. Files remain on the device and are not uploaded or visible to other people.

## Voice rooms and city access

The bottom navigation has a prominent round microphone action. The room list is filtered to exactly two entries: the common room and the locally selected city. Choose a city in **Settings → Your city**; the current UI does not list other cities. This is client-side filtering only—real access control, remote participants, and microphone transport require authenticated backend/signaling enforcement, which is not yet deployed.

## Backend prototype (not yet connected to the APK)

A Node.js service lives in `backend/`. The Android client can register/login, exchange persistent text messages for all seven categories over authenticated HTTPS polling, and sync voice-room presence/mute state. Configure the endpoint in **Ещё → Сервер сообщества**; the default is the current temporary test endpoint.

Important: the server only relays WebRTC signaling and mute/presence state—it does **not** transmit voice audio. The current backend uses a single-process JSON store and is a test prototype, not ready for public recommendation or production data. A stable public deployment, real WebRTC audio/TURN, moderation, backups, and privacy workflows are still required.

## Brand and visual system

The launcher, splash screen, and empty-feed hero use the square raccoon courier logo based on the supplied “Курьерка · Санкт-Петербург” reference. The “ЧАТ” word was removed; Android launchers may still apply their own icon mask to the square artwork.

## Android updates and permanent signing

The native Settings screen shows a rounded progress dialog while checking and downloading the APK, verifies its SHA-256, then hands the cached package to Android's standard installer using a read-only content URI. Android still requires the user to approve installation; silent self-installation is not permitted. The updater is enabled only when a persistent signing keystore is configured.

In GitHub repository **Settings → Secrets and variables → Actions**, add these repository secrets:

- `EKIP_SIGNING_KEYSTORE_B64` — one-line Base64 encoding of your private `.jks` or `.p12` keystore.
- `EKIP_SIGNING_STORE_PASSWORD` — keystore password.
- `EKIP_SIGNING_KEY_ALIAS` — alias, for example `ekip-release`.
- `EKIP_SIGNING_KEY_PASSWORD` — private-key password (use the same value as the store password if keytool asks you to).

Create a key locally with JDK `keytool` (Linux/macOS):

```bash
keytool -genkeypair -v -keystore ekip-release.jks -storetype JKS \
  -alias ekip-release -keyalg RSA -keysize 3072 -validity 10000
base64 -w0 ekip-release.jks
```

Copy the Base64 output straight into `EKIP_SIGNING_KEYSTORE_B64` and the passwords/alias into their matching secrets. On macOS, use `base64 < ekip-release.jks | tr -d '\n'` instead of `base64 -w0`. Store a backup of `ekip-release.jks` and its passwords offline. Never commit the keystore or send it in chat; losing it makes future in-place updates impossible. After saving all four secrets, run **Actions → Build and publish Android APK → Run workflow**. Future workflow builds use this same key and can update over the installed app without uninstalling; Android displays its normal install confirmation.

The APK currently on devices was signed with a temporary CI key that was not retained. Because Android rejects updates signed by a different key, the first switch to your permanent key requires one uninstall/reinstall. Uninstalling erases app-private local video copies, so keep original videos elsewhere before this one-time migration. The agent cannot create repository Actions secrets through the connected GitHub integration in this session (GitHub returned HTTP 403); add them directly in repository settings.

## Build

Requirements: JDK 21, Android SDK platform 36, and build-tools 36.0.0.

```bash
bash scripts/build-android-apk.sh
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`.
