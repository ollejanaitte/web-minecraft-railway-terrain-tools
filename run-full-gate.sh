#!/usr/bin/env bash
# ============================================================================
# run-full-gate.sh - FULL Visual Validation gate for the Railway System.
#
# Purpose: phase-completion / real-game confirmation. Runs an acceptance
# Node script that boots Chrome + Eaglercraft, enters a world, drives chat
# commands and captures screenshots.
#
# Safety:
#   - Hard global timeout via the `timeout` command (FULL_TIMEOUT, default 1500s).
#     The gate can NEVER wait forever.
#   - HW Vulkan (ANGLE) is the standard GPU path; SwiftShader is only a
#     documented fallback (GPU_MODE=swiftshader).
#   - Cleans up ONLY its own validation Chrome profile (never user processes).
#
# Usage:
#   ./run-full-gate.sh <acceptance.mjs> [--skip-build]
# Env:
#   SKIP_BUILD=1            skip the compile/build step
#   FULL_TIMEOUT=<secs>     global watchdog (default 1500)
#   GPU_MODE=vulkan|swiftshader
#   CDP_PORT=..., RUN_ID=... forwarded to the acceptance script
#
# Exit codes: 0 = PASS, 1 = FAIL, 2 = environment error
# ============================================================================
set -uo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT" || exit 2
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"

START=$(date +%s)
say() { echo "[full-gate] $*"; }
fail() { say "ERROR: $*"; exit 2; }

SCRIPT="${1:-}"
[ -n "$SCRIPT" ] || fail "usage: ./run-full-gate.sh <acceptance.mjs> [--skip-build]"
[ -f "$SCRIPT" ] || fail "acceptance script not found: $SCRIPT"
[ -x /opt/google/chrome/chrome ] || fail "/opt/google/chrome/chrome not found"

SKIP_BUILD="${SKIP_BUILD:-0}"
GPU_MODE="${GPU_MODE:-vulkan}"
FULL_TIMEOUT="${FULL_TIMEOUT:-1500}"
command -v node >/dev/null 2>&1 || fail "node not found"
command -v timeout >/dev/null 2>&1 || fail "timeout command not found (coreutils)"

say "=== FULL gate start ==="
say "script=$SCRIPT gpu_mode=$GPU_MODE full_timeout=${FULL_TIMEOUT}s"

# --- stale validation process cleanup (project profiles only) --------------
STALE=$(pgrep -f "profiles/run-\|profiles/validation-\|instrumented" 2>/dev/null | grep -v $$ | wc -l)
if [ "$STALE" -gt 0 ]; then
  say "WARNING: $STALE stale validation chrome processes; killing (project only)"
  pkill -TERM -f "profiles/run-" 2>/dev/null
  pkill -TERM -f "profiles/validation-" 2>/dev/null
  sleep 2
  pkill -9 -f "profiles/run-" 2>/dev/null
  pkill -9 -f "profiles/validation-" 2>/dev/null
fi

# --- build gate (optional) ---------------------------------------------------
if [ "$SKIP_BUILD" != "1" ]; then
  say "build: makeMainOfflineDownload..."
  if ! ./gradlew makeMainOfflineDownload; then
    say "=== FULL gate FAIL (build) ==="
    exit 1
  fi
  say "build OK"
else
  say "build SKIPPED (SKIP_BUILD=1)"
fi

# --- run acceptance with a hard global watchdog ------------------------------
say "running: timeout --foreground ${FULL_TIMEOUT}s node $SCRIPT"
if GPU_MODE="$GPU_MODE" timeout --foreground "$FULL_TIMEOUT" node "$SCRIPT"; then
  rc=0
else
  rc=$?
  if [ "$rc" -eq 124 ]; then
    say "=== FULL gate FAIL (GLOBAL TIMEOUT after ${FULL_TIMEOUT}s) ==="
  else
    say "=== FULL gate FAIL (acceptance exit $rc) ==="
  fi
fi

# --- final cleanup (our own profiles only) ------------------------------------
say "cleanup..."
pkill -TERM -f "profiles/run-" 2>/dev/null || true
pkill -TERM -f "profiles/validation-" 2>/dev/null || true
sleep 2
pkill -9 -f "profiles/run-" 2>/dev/null || true
pkill -9 -f "profiles/validation-" 2>/dev/null || true

END=$(date +%s)
say "elapsed=$((END - START))s rc=$rc"
if [ "$rc" = "0" ]; then
  say "=== FULL gate PASS ==="
  echo "FULL_GATE_RESULT=PASS"
  exit 0
else
  echo "FULL_GATE_RESULT=FAIL"
  exit 1
fi