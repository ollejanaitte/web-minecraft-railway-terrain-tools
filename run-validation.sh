#!/usr/bin/env bash
# ============================================================================
# run-validation.sh - One-command AI validation for the Railway System v2.
#
# Phase 0.2 stabilized validation path:
#   1. build check (makeMainOfflineDownload)
#   2. harness check (harnessTest) [optional, off by default]
#   3. headless Chrome on HARDWARE GPU (ANGLE-Vulkan) + Eaglercraft offline
#   4. create "EaglerValidate" world -> RailV2AutoValidate fires (gated)
#   5. capture SS-01..SS-08 + console + logs
#   6. cleanup chrome/server, report result
#
# AutoValidate is OFF for any non-"EaglerValidate" world (normal play safe).
# Falls back to SwiftShader only when hardware Vulkan is unavailable.
#
# Exit codes: 0 = PASS (AutoValidate observed), 1 = FAIL, 2 = environment error
# ============================================================================
set -uo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT" || exit 2

JAVA_HOME_DEFAULT=/usr/lib/jvm/java-17-openjdk-amd64
export JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"

PHASE="$ROOT/doc/testing/phase0_2"
LOGD="$PHASE/logs"
STAMP="$(date +%Y%m%d_%H%M%S)"
MAX_RUNS="${MAX_RUNS:-3}"
GPU_MODE="${GPU_MODE:-auto}"
CDP_PORT="${CDP_PORT:-9366}"
KEEP_PROFILE="${KEEP_PROFILE:-0}"

mkdir -p "$LOGD"

say() { echo "[run-validation] $*"; }
fail() { say "ERROR: $*"; exit 2; }

say "=== Phase 0.2 validation run ($STAMP) ==="
say "root=$ROOT gpu_mode=$GPU_MODE max_runs=$MAX_RUNS"

# --- env / tool checks -----------------------------------------------------
command -v node >/dev/null 2>&1 || fail "node not found"
command -v python3 >/dev/null 2>&1 || fail "python3 not found"
[ -x /opt/google/chrome/chrome ] || fail "/opt/google/chrome/chrome not found"

HTML="$ROOT/target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html"
[ -f "$HTML" ] || fail "offline HTML missing: $HTML (run ./gradlew makeMainOfflineDownload)"

# --- GPU mode detection (auto -> vulkan, fallback swiftshader) -------------
if [ "$GPU_MODE" = "auto" ]; then
  if ls /dev/dri/renderD* >/dev/null 2>&1 && \
     ls /usr/share/vulkan/icd.d/nvidia_icd.json >/dev/null 2>&1; then
    GPU_MODE=vulkan
  else
    GPU_MODE=swiftshader
  fi
fi
say "using GPU_MODE=$GPU_MODE"

# --- stale process / port checks -------------------------------------------
STALE=$(pgrep -f "profiles/validation-" 2>/dev/null | grep -v $$ | wc -l)
if [ "$STALE" -gt 0 ]; then
  say "WARNING: $STALE stale validation chrome found; killing (project processes only)"
  pkill -TERM -f "profiles/validation-" 2>/dev/null
  sleep 3
  pkill -9 -f "profiles/validation-" 2>/dev/null
fi
if ss -tln 2>/dev/null | grep -q ":$CDP_PORT "; then
  fail "CDP port $CDP_PORT already in use (run: ss -tlnp | grep $CDP_PORT)"
fi

# --- optional build gate ---------------------------------------------------
if [ "${SKIP_BUILD:-0}" != "1" ]; then
  say "build: makeMainOfflineDownload..."
  if ! ./gradlew makeMainOfflineDownload >>"$LOGD/build_$STAMP.log" 2>&1; then
    fail "build FAILED (see $LOGD/build_$STAMP.log)"
  fi
  say "build OK"
fi

# --- run with retry --------------------------------------------------------
PASS=0
for ((run=1; run<=MAX_RUNS; run++)); do
  say "=== validation run $run/$MAX_RUNS ==="
  PROFILE="$PHASE/profiles/validation-$STAMP-$run"
  rm -rf "$PROFILE" "$PHASE/retry_run.log" "$PHASE/screenshots"/SS-0*.png 2>/dev/null
  if GPU_MODE="$GPU_MODE" CDP_PORT="$CDP_PORT" \
     CHROME_PROFILE="$PROFILE" KEEP_PROFILE="$KEEP_PROFILE" \
     node "$PHASE/scripts/retry_vulkan.mjs" >"$LOGD/validation_${STAMP}_${run}.log" 2>&1; then
    PASS=1
    say "run $run PASS"
    break
  fi
  code=$?
  say "run $run FAILED (exit $code) - cleaning up and retrying"
  pkill -TERM -f "validation-$STAMP-$run" 2>/dev/null
  sleep 3
  pkill -9 -f "validation-$STAMP-$run" 2>/dev/null
done

# --- final cleanup ---------------------------------------------------------
say "cleanup..."
pkill -TERM -f "profiles/validation-" 2>/dev/null || true
sleep 2
pkill -9 -f "profiles/validation-" 2>/dev/null || true

if [ "$PASS" = "1" ]; then
  say "=== VALIDATION PASS ==="
  echo "VALIDATION_RESULT=PASS"
  exit 0
else
  say "=== VALIDATION FAIL after $MAX_RUNS runs ==="
  echo "VALIDATION_RESULT=FAIL"
  exit 1
fi
