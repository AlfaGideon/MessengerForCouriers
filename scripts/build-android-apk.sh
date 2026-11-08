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
mkdir -p "$BUILD/generated" "$BUILD/classes" "$BUILD/dex" "$BUILD/assets"

cat > "$BUILD/AndroidManifest.xml" <<'MANIFEST'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="ru.ekip.couriers">
  <uses-sdk android:minSdkVersion="24" android:targetSdkVersion="35" />
  <uses-permission android:name="android.permission.INTERNET" />
  <application android:allowBackup="true" android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round" android:label="@string/app_name" android:supportsRtl="true" android:theme="@style/AppTheme.NoActionBarLaunch" android:usesCleartextTraffic="false">
    <activity android:name="ru.ekip.couriers.MainActivity" android:exported="true" android:configChanges="orientation|keyboardHidden|keyboard|screenSize|locale|smallestScreenSize|screenLayout|uiMode|navigation|density">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>
MANIFEST

"$BUILD_TOOLS/aapt2" compile --dir "$ROOT/android/app/src/main/res" -o "$BUILD/resources.zip"
"$BUILD_TOOLS/aapt2" link \
  --manifest "$BUILD/AndroidManifest.xml" \
  -I "$ANDROID_JAR" \
  --min-sdk-version 24 \
  --target-sdk-version 35 \
  --version-code 1 \
  --version-name 1.0.0 \
  --auto-add-overlay \
  --java "$BUILD/generated" \
  -o "$BUILD/base.apk" \
  "$BUILD/resources.zip"

javac -encoding UTF-8 -source 8 -target 8 -classpath "$ANDROID_JAR" -d "$BUILD/classes" \
  "$ROOT/android/app/src/main/java/ru/ekip/couriers/MainActivity.java"
mapfile -d '' CLASS_FILES < <(find "$BUILD/classes" -type f -name '*.class' -print0)
"$BUILD_TOOLS/d8" --release --min-api 24 --lib "$ANDROID_JAR" --output "$BUILD/dex" "${CLASS_FILES[@]}"
cp "$BUILD/dex/classes.dex" "$BUILD/classes.dex"
cp -R "$ROOT/android/app/src/main/assets/public" "$BUILD/assets/public"

(cd "$BUILD" && zip -q -0 base.apk classes.dex && zip -q -r base.apk assets)
mkdir -p "$(dirname "$OUTPUT")"
"$BUILD_TOOLS/zipalign" -f -p 4 "$BUILD/base.apk" "$BUILD/aligned.apk"
keytool -genkeypair -keystore "$BUILD/debug.keystore" -storepass android -alias androiddebugkey -keypass android \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
"$BUILD_TOOLS/apksigner" sign --ks "$BUILD/debug.keystore" --ks-key-alias androiddebugkey \
  --ks-pass pass:android --key-pass pass:android --out "$OUTPUT" "$BUILD/aligned.apk"
"$BUILD_TOOLS/apksigner" verify --verbose "$OUTPUT"
echo "APK created: $OUTPUT"
