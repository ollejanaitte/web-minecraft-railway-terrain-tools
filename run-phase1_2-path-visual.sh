#!/usr/bin/env bash
# Phase 1.2 RailPath visual validation (Flat World + Hardware Vulkan).
set -uo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT" || exit 2
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
PHASE="$ROOT/doc/testing/phase1_2"
LOGD="$PHASE/logs"
STAMP="$(date +%Y%m%d_%H%M%S)"
MAX_RUNS="${MAX_RUNS:-4}"
GPU_MODE="${GPU_MODE:-auto}"
CDP_PORT="${CDP_PORT:-9392}"
mkdir -p "$LOGD" "$PHASE/screenshots" "$PHASE/profiles"
say() { echo "[run-phase1_2-path-visual] $*"; }
fail() { say "ERROR: $*"; exit 2; }
[ -x /opt/google/chrome/chrome ] || fail "chrome missing"
[ -f "$ROOT/target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html" ] || fail "offline HTML missing"

# cleanup stale chrome on our port/profile
for p in $(pgrep -f "remote-debugging-port=${CDP_PORT}" || true); do kill -9 "$p" 2>/dev/null || true; done
for p in $(pgrep -f "phase1_2/profiles" || true); do kill -9 "$p" 2>/dev/null || true; done

if [ "${SKIP_BUILD:-0}" != "1" ]; then
  say "build..."
  ./gradlew makeMainOfflineDownload >>"$LOGD/build_$STAMP.log" 2>&1 || fail "build failed"
  ./gradlew harnessTest >>"$LOGD/harness_$STAMP.log" 2>&1 || fail "harness failed"
  say "build+harness OK"
fi

PASS=0
for ((run=1; run<=MAX_RUNS; run++)); do
  say "=== path visual run $run/$MAX_RUNS ==="
  PROFILE="$PHASE/profiles/path-$STAMP-$run"
  rm -rf "$PROFILE"
  if GPU_MODE="$GPU_MODE" CDP_PORT="$CDP_PORT" CHROME_PROFILE="$PROFILE" \
     node "$PHASE/scripts/path_validate.mjs" >"$LOGD/path_${STAMP}_${run}.log" 2>&1; then
    PASS=1
    say "run $run PASS"
    break
  fi
  say "run $run FAILED - retrying"
  for p in $(pgrep -f "path-${STAMP}-${run}" || true); do kill -9 "$p" 2>/dev/null || true; done
  sleep 3
done

for p in $(pgrep -f "phase1_2/profiles/path-" || true); do kill -9 "$p" 2>/dev/null || true; done
for p in $(pgrep -f "remote-debugging-port=${CDP_PORT}" || true); do kill -9 "$p" 2>/dev/null || true; done

if [ "$PASS" = "1" ]; then
  say "=== PHASE1.2 PATH VISUAL PASS ==="
  echo "PHASE1_2_PATH_VISUAL=PASS"
  exit 0
else
  say "=== PHASE1.2 PATH VISUAL FAIL ==="
  echo "PHASE1_2_PATH_VISUAL=FAIL"
  exit 1
fi
