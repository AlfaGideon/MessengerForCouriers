#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK" ]]; then
  echo "ANDROID_HOME (Android SDK) is required" >&2
  exit 1
fi
BUILD_TOOLS="$SDK/build-tools/36.0.0"
ANDROID_JAR="$SDK/platforms/android-36/android.jar"
for tool in aapt2 d8 zipalign apksigner; do
  [[ -x "$BUILD_TOOLS/$tool" ]] || { echo "Missing Android build tool: $BUILD_TOOLS/$tool" >&2; exit 1; }
done
[[ -f "$ANDROID_JAR" ]] || { echo "Missing Android platform: $ANDROID_JAR" >&2; exit 1; }

BUILD="$ROOT/android/app/build/manual-apk"
OUTPUT="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
rm -rf "$BUILD"
mkdir -p "$BUILD/generated" "$BUILD/classes" "$BUILD/dex"

cat > "$BUILD/AndroidManifest.xml" <<'MANIFEST'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="ru.ekip.couriers">
  <uses-sdk android:minSdkVersion="24" android:targetSdkVersion="35" />
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.RECORD_AUDIO" />
  <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
  <application android:allowBackup="false" android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round" android:label="@string/app_name" android:supportsRtl="true" android:theme="@style/AppTheme.NoActionBarLaunch" android:usesCleartextTraffic="false">
    <activity android:name="ru.ekip.couriers.MainActivity" android:exported="true" android:supportsPictureInPicture="true" android:resizeableActivity="true" android:configChanges="orientation|keyboardHidden|keyboard|screenSize|locale|smallestScreenSize|screenLayout|uiMode|navigation|density">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
    <provider android:name="ru.ekip.couriers.UpdateFileProvider" android:authorities="ru.ekip.couriers.updates" android:exported="false" android:grantUriPermissions="true" />
  </application>
</manifest>
MANIFEST

"$BUILD_TOOLS/aapt2" compile --dir "$ROOT/android/app/src/main/res" -o "$BUILD/resources.zip"
"$BUILD_TOOLS/aapt2" link \
  --manifest "$BUILD/AndroidManifest.xml" \
  -I "$ANDROID_JAR" \
  --min-sdk-version 24 \
  --target-sdk-version 35 \
  --version-code "${EKIP_APK_VERSION_CODE:-2}" \
  --version-name "${EKIP_APK_VERSION_NAME:-2.0.0}" \
  --auto-add-overlay \
  --java "$BUILD/generated" \
  -o "$BUILD/base.apk" \
  "$BUILD/resources.zip"

mapfile -d '' JAVA_SOURCES < <(find "$ROOT/android/app/src/main/java/ru/ekip/couriers" -type f -name '*.java' -print0)
javac -encoding UTF-8 -source 8 -target 8 -classpath "$ANDROID_JAR" -d "$BUILD/classes" \
  "${JAVA_SOURCES[@]}" "$BUILD/generated/ru/ekip/couriers/R.java"
mapfile -d '' CLASS_FILES < <(find "$BUILD/classes" -type f -name '*.class' -print0)
"$BUILD_TOOLS/d8" --release --min-api 24 --lib "$ANDROID_JAR" --output "$BUILD/dex" "${CLASS_FILES[@]}"
cp "$BUILD/dex/classes.dex" "$BUILD/classes.dex"

(cd "$BUILD" && zip -q -0 base.apk classes.dex)
mkdir -p "$(dirname "$OUTPUT")"
"$BUILD_TOOLS/zipalign" -f -p 4 "$BUILD/base.apk" "$BUILD/aligned.apk"
if [[ -n "${EKIP_SIGNING_KEYSTORE_B64:-}" ]]; then
  : "${EKIP_SIGNING_STORE_PASSWORD:?EKIP_SIGNING_STORE_PASSWORD is required with the signing keystore}"
  : "${EKIP_SIGNING_KEY_ALIAS:?EKIP_SIGNING_KEY_ALIAS is required with the signing keystore}"
  : "${EKIP_SIGNING_KEY_PASSWORD:?EKIP_SIGNING_KEY_PASSWORD is required with the signing keystore}"
  printf '%s' "$EKIP_SIGNING_KEYSTORE_B64" | base64 --decode > "$BUILD/ekip-signing.keystore"
  SIGNING_STORE="$BUILD/ekip-signing.keystore"
  SIGNING_ALIAS="$EKIP_SIGNING_KEY_ALIAS"
  SIGNING_STORE_PASSWORD="$EKIP_SIGNING_STORE_PASSWORD"
  SIGNING_KEY_PASSWORD="$EKIP_SIGNING_KEY_PASSWORD"
  SIGNING_MODE="persistent"
else
  if [[ -n "${EKIP_SIGNING_STORE_PASSWORD:-}${EKIP_SIGNING_KEY_ALIAS:-}${EKIP_SIGNING_KEY_PASSWORD:-}" ]]; then
    echo "EKIP_SIGNING_KEYSTORE_B64 is required when any other signing secret is set" >&2
    exit 1
  fi
  keytool -genkeypair -keystore "$BUILD/temporary.keystore" -storepass android -alias androiddebugkey -keypass android \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Temporary,O=Android,C=US" >/dev/null 2>&1
  SIGNING_STORE="$BUILD/temporary.keystore"
  SIGNING_ALIAS="androiddebugkey"
  SIGNING_STORE_PASSWORD="android"
  SIGNING_KEY_PASSWORD="android"
  SIGNING_MODE="temporary"
  echo "WARNING: No persistent signing secrets found; this APK cannot be updated in place by a later differently-signed APK." >&2
fi
STORE_MAGIC="$(od -An -tx1 -N4 "$SIGNING_STORE" | tr -d ' \n')"
if [[ "$STORE_MAGIC" == "feedfeed" ]]; then SIGNING_STORE_TYPE="JKS"; else SIGNING_STORE_TYPE="PKCS12"; fi
"$BUILD_TOOLS/apksigner" sign --ks "$SIGNING_STORE" --ks-type "$SIGNING_STORE_TYPE" --ks-key-alias "$SIGNING_ALIAS" \
  --ks-pass "pass:$SIGNING_STORE_PASSWORD" --key-pass "pass:$SIGNING_KEY_PASSWORD" --out "$OUTPUT" "$BUILD/aligned.apk"
"$BUILD_TOOLS/apksigner" verify --verbose "$OUTPUT"
echo "APK created ($SIGNING_MODE signing): $OUTPUT"
