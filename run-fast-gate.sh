#!/usr/bin/env bash
# ============================================================================
# run-fast-gate.sh - FAST development gate for the Railway System.
#
# Purpose: normal development-loop validation. NO browser / NO full game launch.
#
# Steps:
#   1. compile check (makeMainOfflineDownload)  [SKIP_BUILD=1 to skip]
#   2. harness tests (pure JVM, no game runtime)
#   3. optional numeric measure task (e.g. ./run-fast-gate.sh r16Measure)
#
# Exit codes: 0 = PASS, 1 = FAIL, 2 = environment error
# ============================================================================
set -uo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT" || exit 2
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"

START=$(date +%s)
say() { echo "[fast-gate] $*"; }
fail() { say "ERROR: $*"; exit 2; }

MEASURE="${1:-}"
say "=== FAST gate start (SKIP_BUILD=${SKIP_BUILD:-0}) ==="

if [ "${SKIP_BUILD:-0}" != "1" ]; then
  say "step 1/3: makeMainOfflineDownload (compile check)..."
  if ! ./gradlew makeMainOfflineDownload; then
    say "=== FAST gate FAIL (build) ==="
    exit 1
  fi
else
  say "step 1/3: build SKIPPED (SKIP_BUILD=1)"
fi

say "step 2/3: harnessTest (pure JVM)..."
if ! ./gradlew harnessTest; then
  say "=== FAST gate FAIL (harness) ==="
  exit 1
fi

if [ -n "$MEASURE" ]; then
  say "step 3/3: measure task '$MEASURE'..."
  if ! ./gradlew "$MEASURE"; then
    say "=== FAST gate FAIL (measure $MEASURE) ==="
    exit 1
  fi
else
  say "step 3/3: (no measure task)"
fi

END=$(date +%s)
say "=== FAST gate PASS (elapsed $((END - START))s) ==="
echo "FAST_GATE_RESULT=PASS"
exit 0