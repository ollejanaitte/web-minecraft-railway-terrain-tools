#!/usr/bin/env bash
# ============================================================================
# run-validation-workflow.sh - Phase 1.2.2 Validation Workflow Launcher
#
# GUI / headless both supported (GUI is the default when DISPLAY is set).
# Uses a validation-only profile (NEVER runtime/profiles/game).
# RUN ID / stage / heartbeat / timeout / retry are printed by the engine.
#
# Usage:
#   ./run-validation-workflow.sh              # auto: gui if DISPLAY else headless
#   GUI_MODE=headless ./run-validation-workflow.sh
#   GUI_MODE=gui    ./run-validation-workflow.sh
#
# Env:
#   GUI_MODE=auto|gui|headless
#   CDP_PORT=9402 (default)
#   CREATE_RETRIES=3 (default)
#   STAGE_TIMEOUT_SCALE=1.0 (default)
#   VISION_MODEL=kimi|codex-luna  (Vision Observer model)
#   DISCORD_WEBHOOK_URL=...        (human-assist notifications; runtime only)
#   NO_LAUNCH=1                    (attach to an existing validation chrome)
# ============================================================================
set -uo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT" || exit 2
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"

PHASE="$ROOT/doc/testing/phase1_2_2"
LOGD="$PHASE/logs"
RUN_ID="$(date +%Y%m%d_%H%M%S)"
mkdir -p "$LOGD" "$PHASE/screenshots" "$PHASE/profiles"

say() { echo "[run-validation-workflow] $*"; }
fail() { say "ERROR: $*"; exit 2; }

[ -x /opt/google/chrome/chrome ] || fail "chrome missing at /opt/google/chrome/chrome"
[ -f "$ROOT/target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html" ] \
  || fail "offline HTML missing (run ./gradlew makeMainOfflineDownload)"

GUI_MODE="${GUI_MODE:-auto}"
if [ "$GUI_MODE" = "auto" ]; then
  if [ -n "${DISPLAY:-}" ]; then
    GUI_MODE=gui
  else
    GUI_MODE=headless
  fi
fi
say "RUN_ID=$RUN_ID GUI_MODE=$GUI_MODE"

# Stale validation process check (only our phase profile names, never user chrome)
for p in $(pgrep -f "doc/testing/phase1_2_2/profiles/run-" 2>/dev/null || true); do
  say "WARN stale validation chrome $p - killing (project-only)"
  kill -9 "$p" 2>/dev/null || true
done

# Build gate (optional, default on)
if [ "${SKIP_BUILD:-0}" != "1" ]; then
  say "build: makeMainOfflineDownload..."
  ./gradlew makeMainOfflineDownload >>"$LOGD/build_${RUN_ID}.log" 2>&1 || fail "build failed (see $LOGD/build_${RUN_ID}.log)"
  say "build OK"
fi

export RUN_ID GUI_MODE VALIDATION_PROFILE="$PHASE/profiles/run-$RUN_ID"
node "$PHASE/scripts/validation_workflow.mjs" 2>&1 | tee "$LOGD/run_${RUN_ID}.log"
rc=$?

if [ "$rc" = "0" ]; then
  say "=== VALIDATION PASS (RUN $RUN_ID) ==="
  echo "VALIDATION_RESULT=PASS RUN=$RUN_ID GUI_MODE=$GUI_MODE"
else
  say "=== VALIDATION FAIL/STOP (RUN $RUN_ID, exit $rc) ==="
  echo "VALIDATION_RESULT=FAIL RUN=$RUN_ID GUI_MODE=$GUI_MODE exit=$rc"
fi
exit "$rc"
