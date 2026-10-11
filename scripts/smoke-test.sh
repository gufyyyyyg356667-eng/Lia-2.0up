#!/usr/bin/env bash
# Install a debug build on a connected phone, launch it and look for crashes.
# Usage: scripts/smoke-test.sh [play|direct]   (default: play)
set -euo pipefail

FLAVOR="${1:-play}"
case "$FLAVOR" in
  play)   PKG="com.Lia.assistant";        APK_GLOB="app/build/outputs/apk/play/debug/*.apk" ;;
  direct) PKG="com.Lia.assistant.direct"; APK_GLOB="app/build/outputs/apk/direct/debug/*.apk" ;;
  *) echo "Unknown flavor: $FLAVOR (use play or direct)"; exit 2 ;;
esac

APK=$(ls $APK_GLOB 2>/dev/null | head -1 || true)
if [[ -z "$APK" ]]; then
  echo "No APK found. Build first: ./gradlew assemble${FLAVOR^}Debug"
  exit 2
fi

command -v adb >/dev/null || { echo "adb not found. Install Android platform-tools."; exit 2; }
adb get-state >/dev/null 2>&1 || { echo "No device connected. Enable USB debugging."; exit 2; }

echo "Installing $APK"
adb install -r "$APK" >/dev/null

echo "Clearing logcat and launching $PKG"
adb logcat -c
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 8

if ! adb shell pidof "$PKG" >/dev/null 2>&1; then
  echo "FAIL: app is not running after launch"
  adb logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime" | tail -20 || true
  exit 1
fi

CRASHES=$(adb logcat -d | grep -cE "FATAL EXCEPTION|ANR in $PKG" || true)
if [[ "$CRASHES" -gt 0 ]]; then
  echo "FAIL: $CRASHES crash or ANR lines found"
  adb logcat -d | grep -E "FATAL EXCEPTION|ANR in" | tail -20
  exit 1
fi

echo "PASS: $PKG launched and stayed up for 8 s with no crashes."
echo "Manual checks still needed: onboarding, key entry, voice start/stop, phone actions, edge glow."
