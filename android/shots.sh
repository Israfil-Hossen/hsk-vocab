#!/bin/bash
# Installs the test APK on a running emulator and saves a PNG of each screen into $1.
OUT="${1:-shots}"; mkdir -p "$OUT"
APK=$(ls android/app/build/outputs/apk/debug/*.apk | head -1)
adb install -r "$APK"
adb shell settings put system screen_off_timeout 600000
for s in onboarding today learn level cards dialogue grammar book reader me about book-ur reader-ur reader-bn; do
  adb shell am force-stop com.israfilhossen.dawahchinese
  adb shell am start -W -n com.israfilhossen.dawahchinese/.MainActivity --es screen "$s" > /dev/null
  sleep 5
  adb exec-out screencap -p > "$OUT/$s.png"
  echo "shot $s $(stat -c %s "$OUT/$s.png")"
done
adb logcat -d -s AndroidRuntime:E > "$OUT/crash.txt" || true
