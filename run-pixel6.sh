#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_SDK_PATH="${ANDROID_HOME:-${HOME}/Library/Android/sdk}"
PIXEL6_ADB="${ANDROID_SDK_PATH}/platform-tools/adb"
PIXEL6_EMULATOR="${ANDROID_SDK_PATH}/emulator/emulator"
PIXEL6_APK="${PROJECT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
PIXEL6_LOG="${PROJECT_DIR}/.gradle/pixel6-emulator.log"

if [[ ! -x "$PIXEL6_ADB" || ! -x "$PIXEL6_EMULATOR" ]]; then
    printf 'Android SDK tools not found in %s\nSet ANDROID_HOME to your Android SDK directory.\n' "$ANDROID_SDK_PATH" >&2
    exit 1
fi

pixel6_serial() {
    local serial state avd_name
    while read -r serial state; do
        [[ "$serial" == emulator-* && "$state" == device ]] || continue
        avd_name="$("$PIXEL6_ADB" -s "$serial" emu avd name 2>/dev/null | head -n 1 | tr -d '\r' || true)"
        if [[ "$avd_name" == Pixel_6 ]]; then
            printf '%s\n' "$serial"
            return 0
        fi
    done < <("$PIXEL6_ADB" devices | tail -n +2)
    return 1
}

if pgrep -f 'qemu-system-aarch64-headless -avd Pixel_6' >/dev/null; then
    printf 'Stopping the headless Pixel_6 emulator...\n'
    if PIXEL6_RUNNING_SERIAL="$(pixel6_serial)"; then
        "$PIXEL6_ADB" -s "$PIXEL6_RUNNING_SERIAL" emu kill >/dev/null
    else
        pkill -f 'qemu-system-aarch64-headless -avd Pixel_6'
    fi
    for ((attempt=0; attempt<30; attempt++)); do
        pgrep -f 'qemu-system-aarch64-headless -avd Pixel_6' >/dev/null || break
        sleep 1
    done
fi

if ! pgrep -f 'qemu-system-aarch64 -avd Pixel_6' >/dev/null; then
    printf 'Opening the visible Pixel_6 emulator...\n'
    mkdir -p "${PROJECT_DIR}/.gradle"
    "$PIXEL6_EMULATOR" -avd Pixel_6 -gpu host -no-snapshot-load \
        >"$PIXEL6_LOG" 2>&1 </dev/null &
    PIXEL6_EMULATOR_PID=$!
fi

PIXEL6_SERIAL=""
for ((attempt=0; attempt<90; attempt++)); do
    PIXEL6_SERIAL="$(pixel6_serial || true)"
    if [[ -n "$PIXEL6_SERIAL" && "$("$PIXEL6_ADB" -s "$PIXEL6_SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then
        break
    fi
    sleep 2
done

if [[ -z "$PIXEL6_SERIAL" || "$("$PIXEL6_ADB" -s "$PIXEL6_SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != 1 ]]; then
    printf 'Pixel_6 did not finish booting. Emulator log: %s\n' "$PIXEL6_LOG" >&2
    [[ -f "$PIXEL6_LOG" ]] && tail -n 30 "$PIXEL6_LOG" >&2
    exit 1
fi

printf 'Building PURRFOLIO...\n'
cd "$PROJECT_DIR"
ANDROID_HOME="$ANDROID_SDK_PATH" ./gradlew :app:assembleDebug

printf 'Installing and launching PURRFOLIO on %s...\n' "$PIXEL6_SERIAL"
"$PIXEL6_ADB" -s "$PIXEL6_SERIAL" install -r "$PIXEL6_APK"
"$PIXEL6_ADB" -s "$PIXEL6_SERIAL" shell am force-stop com.purrfolio.mco1
"$PIXEL6_ADB" -s "$PIXEL6_SERIAL" shell am start -n com.purrfolio.mco1/.MainActivity

if [[ -n "${PIXEL6_EMULATOR_PID:-}" ]]; then
    printf 'PURRFOLIO is running. Keep this terminal open while using the emulator; press Ctrl+C to close it.\n'
    wait "$PIXEL6_EMULATOR_PID"
fi
