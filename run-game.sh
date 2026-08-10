#!/usr/bin/env bash
# ============================================================================
# run-game.sh - One-command normal game launch (human play).
#
# Phase 0.2 stabilized normal launch:
#   * Hardware GPU (ANGLE-Vulkan) when available; SwiftShader fallback.
#   * AUTO-VALIDATE IS OFF: the RailV2AutoValidate hook only fires inside a
#     world named "EaglerValidate" (created by run-validation.sh). Any normal
#     world you create/play will NOT build courses, spawn trains, or move the
#     camera.
#   * Uses a persistent profile so saved worlds survive relaunches.
#   * Prints PIDs; stop with:  ./run-game.sh stop   (or the printed PID).
#
# Options:
#   DISPLAY set      -> visible window on your desktop (recommended for play)
#   no DISPLAY       -> headless=new (screenshot/DM-free; still playable via
#                        remote debugging on --remote-debugging-port)
#   GPU_MODE=swiftshader -> force software fallback
#   HEADLESS=1       -> force headless
#   PORT=9222        -> CDP port (only in headless mode)
# ============================================================================
set -uo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT" || exit 2

CHROME=/opt/google/chrome/chrome
[ -x "$CHROME" ] || { echo "run-game: chrome not found at $CHROME" >&2; exit 2; }

HTML="$ROOT/target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html"
[ -f "$HTML" ] || { echo "run-game: offline HTML missing (run ./gradlew makeMainOfflineDownload)" >&2; exit 2; }

PHASE="$ROOT/doc/testing/phase0_2"
PROFILE="${GAME_PROFILE:-$PHASE/profiles/game}"
PORT="${PORT:-9222}"
GPU_MODE="${GPU_MODE:-auto}"
LOG="$PHASE/logs/game_$(date +%Y%m%d_%H%M%S).log"
mkdir -p "$PHASE/profiles" "$PHASE/logs"

case "${1:-}" in
  stop)
    pids=$(pgrep -f "user-data-dir=$PROFILE" 2>/dev/null | grep -v $$)
    if [ -z "$pids" ]; then echo "run-game: no running game"; exit 0; fi
    echo "run-game: stopping $pids"
    # shellcheck disable=SC2086
    kill -TERM $pids 2>/dev/null
    sleep 3
    # shellcheck disable=SC2086
    kill -9 $pids 2>/dev/null || true
    exit 0
    ;;
esac

# duplicate instance check
if pgrep -f "user-data-dir=$PROFILE" >/dev/null 2>&1; then
  echo "run-game: an instance is already running (profile $PROFILE). Stop it first: ./run-game.sh stop" >&2
  exit 2
fi

# GPU mode
if [ "$GPU_MODE" = "auto" ]; then
  if ls /dev/dri/renderD* >/dev/null 2>&1 && \
     ls /usr/share/vulkan/icd.d/nvidia_icd.json >/dev/null 2>&1; then
    GPU_MODE=vulkan
  else
    GPU_MODE=swiftshader
  fi
fi

# flags common to both modes
FLAGS=(--no-sandbox --autoplay-policy=no-user-gesture-required --mute-audio
       --hide-scrollbars --window-size=1280,720 --user-data-dir="$PROFILE")
if [ "$GPU_MODE" = "swiftshader" ]; then
  FLAGS+=(--disable-gpu-sandbox --enable-unsafe-swiftshader --use-gl=angle --use-angle=swiftshader)
else
  FLAGS+=(--use-gl=angle --use-angle=vulkan)
fi

if [ "${HEADLESS:-0}" = "1" ] || [ -z "${DISPLAY:-}" ]; then
  FLAGS+=(--headless=new "--remote-debugging-port=$PORT")
  echo "run-game: HEADLESS mode (no DISPLAY) - CDP on port $PORT"
else
  echo "run-game: VISIBLE window on DISPLAY=$DISPLAY"
fi

echo "run-game: launching Chrome (gpu=$GPU_MODE, profile=$PROFILE)"
# shellcheck disable=SC2086
setsid "$CHROME" "${FLAGS[@]}" "file://$HTML" >"$LOG" 2>&1 < /dev/null &
PID=$!
disown
echo "run-game: PID=$PID  (log: $LOG)"
echo "run-game: to stop: ./run-game.sh stop"
