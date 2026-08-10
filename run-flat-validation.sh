#!/usr/bin/env bash
# ============================================================================
# run-flat-validation.sh - Phase 0.5 Flat Validation World proof.
#
# Retries the flat-world validation (create "EaglerFlatValidate" -> Superflat
# via name hook -> AutoValidate -> SS-FLAT screenshots -> cleanup) up to
# MAX_RUNS times with a fresh profile each attempt.
#
# Exit: 0 = PASS, 1 = FAIL (all runs), 2 = environment error.
# ============================================================================
set -uo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT" || exit 2
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
PHASE="$ROOT/doc/testing/phase0_5"
LOGD="$PHASE/logs"
STAMP="$(date +%Y%m%d_%H%M%S)"
MAX_RUNS="${MAX_RUNS:-4}"
GPU_MODE="${GPU_MODE:-auto}"
CDP_PORT="${CDP_PORT:-9390}"
mkdir -p "$LOGD"
say() { echo "[run-flat-validation] $*"; }
fail() { say "ERROR: $*"; exit 2; }
[ -x /opt/google/chrome/chrome ] || fail "chrome missing"
[ -f "$ROOT/target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html" ] || fail "offline HTML missing"

if [ "${SKIP_BUILD:-0}" != "1" ]; then
  say "build..."
  ./gradlew makeMainOfflineDownload >>"$LOGD/build_$STAMP.log" 2>&1 || fail "build failed"
  ./gradlew harnessTest >>"$LOGD/harness_$STAMP.log" 2>&1 || fail "harness failed"
  say "build+harness OK"
fi

PASS=0
for ((run=1; run<=MAX_RUNS; run++)); do
  say "=== flat validation run $run/$MAX_RUNS ==="
  PROFILE="$PHASE/profiles/flat-$STAMP-$run"
  rm -rf "$PROFILE" "$PHASE/logs/flat_validate.log" "$PHASE/screenshots"/SS-FLAT-*.png 2>/dev/null
  if GPU_MODE="$GPU_MODE" CDP_PORT="$CDP_PORT" CHROME_PROFILE="$PROFILE" \
     node "$PHASE/scripts/flat_validate.mjs" >"$LOGD/flat_${STAMP}_${run}.log" 2>&1; then
    PASS=1
    say "run $run PASS"
    break
  fi
  say "run $run FAILED - retrying"
  for p in $(pgrep -f "flat-${STAMP}-${run}"); do kill -9 "$p" 2>/dev/null; done
  sleep 3
done

for p in $(pgrep -f "profiles/flat-"); do kill -9 "$p" 2>/dev/null; done
if [ "$PASS" = "1" ]; then
  say "=== FLAT VALIDATION PASS ==="
  echo "FLAT_VALIDATION_RESULT=PASS"
  exit 0
else
  say "=== FLAT VALIDATION FAIL ==="
  echo "FLAT_VALIDATION_RESULT=FAIL"
  exit 1
fi
