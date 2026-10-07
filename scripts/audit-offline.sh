#!/usr/bin/env bash
set -euo pipefail

echo "=============================================="
echo "EXP Mango Offline & Privacy Policy Audit"
echo "=============================================="

MANIFEST="app/src/main/AndroidManifest.xml"

if grep -q "android.permission.INTERNET" "$MANIFEST"; then
    echo "FAIL: INTERNET permission found in $MANIFEST!"
    exit 1
fi
echo "PASS: No INTERNET permission in $MANIFEST."

BUILD_GRADLE="app/build.gradle.kts"
FORBIDDEN_DEPS=("retrofit" "okhttp" "ktor" "firebase" "sentry" "analytics" "telemetry" "appcenter" "mixpanel")

for dep in "${FORBIDDEN_DEPS[@]}"; do
    if grep -i -q "$dep" "$BUILD_GRADLE"; then
        echo "FAIL: Prohibited dependency '$dep' found in $BUILD_GRADLE!"
        exit 1
    fi
done
echo "PASS: No prohibited networking/analytics dependencies in $BUILD_GRADLE."

echo "Offline audit passed successfully!"
