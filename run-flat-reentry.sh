#!/usr/bin/env bash
# Phase 0.5 re-entry proof: relaunch on the saved flat-world profile and re-enter
# the SAME world, retrying until AutoValidate reproduces.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT" || exit 2
PHASE="$ROOT/doc/testing/phase0_5"
LOGD="$PHASE/logs"
STAMP="$(date +%Y%m%d_%H%M%S)"
MAX_RUNS="${MAX_RUNS:-6}"
GPU_MODE="${GPU_MODE:-auto}"
CDP_PORT="${CDP_PORT:-9396}"
PROFILE="${FLAT_PROFILE:-$PHASE/profiles/flat-20260810_150845-2}"
mkdir -p "$LOGD"
say() { echo "[run-flat-reentry] $*"; }
[ -d "$PROFILE" ] || { say "ERROR: saved flat profile missing: $PROFILE"; exit 2; }
rm -f "$PROFILE"/Singleton* 2>/dev/null

PASS=0
for ((run=1; run<=MAX_RUNS; run++)); do
  say "=== re-entry run $run/$MAX_RUNS ==="
  rm -f "$PHASE/logs/reentry.log" "$PHASE/screenshots"/SS-FLAT-RE-*.png 2>/dev/null
  if GPU_MODE="$GPU_MODE" CDP_PORT="$CDP_PORT" CHROME_PROFILE="$PROFILE" \
     node "$PHASE/scripts/reentry.mjs" >"$LOGD/reentry_${STAMP}_${run}.log" 2>&1; then
    PASS=1
    say "run $run PASS (re-entry + AutoValidate)"
    break
  fi
  say "run $run FAILED - retrying"
  for p in $(pgrep -f "remote-debugging-port=$CDP_PORT"); do kill -9 "$p" 2>/dev/null; done
  sleep 3
done
if [ "$PASS" = "1" ]; then
  say "=== FLAT RE-ENTRY PASS ==="; echo "FLAT_REENTRY_RESULT=PASS"; exit 0
else
  say "=== FLAT RE-ENTRY FAIL ==="; echo "FLAT_REENTRY_RESULT=FAIL"; exit 1
fi
