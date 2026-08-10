# Phase 0.2 Validation Guide (AI / automated)

One-command validation: **`./run-validation.sh`**

## Purpose
Reproduce the Railway System v2 Visual Proof end-to-end on the current HEAD:
build → launch → world join → AutoValidate → screenshots → console → cleanup,
with a machine verdict.

## Prerequisites
- Built offline HTML (script builds it unless `SKIP_BUILD=1`).
- Chrome 151+ and hardware GPU (auto-detected), SwiftShader fallback.
- Node.js 18+ and Python3 + PIL (screenshot analysis).

## Usage
```bash
./run-validation.sh                  # full: build + validate + cleanup
SKIP_BUILD=1 ./run-validation.sh     # skip rebuild (faster iteration)
MAX_RUNS=3 ./run-validation.sh       # retries per cold start (default 3)
GPU_MODE=swiftshader ./run-validation.sh  # force fallback
KEEP_PROFILE=1 ./run-validation.sh   # keep profile for debugging
```

Exit codes: `0` = PASS (AutoValidate observed), `1` = FAIL (all runs),
`2` = environment error.

## Flow (per cold-start attempt)
1. Stale-process check + port check (9366 default).
2. `makeMainOfflineDownload` (unless `SKIP_BUILD=1`).
3. Headless Chrome, hardware GPU, Eaglercraft offline HTML.
4. Boot → title/menu (click-through dialogs; state-based detection).
5. Singleplayer → Create New World named **`EaglerValidate`** (char-by-char
   typing; `Input.insertText` is unreliable on the canvas field).
6. Wait for `railsysv2: auto-validated` console marker + in-world signature
   (sky + hotbar + terrain).
7. Capture SS-01..SS-08 under `doc/testing/phase0_2/screenshots/`.
8. Cleanup Chrome (project processes only) + stale-process re-check.

## Result location
- Launcher log: `doc/testing/phase0_2/logs/launcher_*.txt`
- Per-run log: `doc/testing/phase0_2/logs/validation_<stamp>_<run>.log`
- Screenshots: `doc/testing/phase0_2/screenshots/SS-0*.png`
- Console capture: `doc/testing/phase0_2/cursor_console.txt`

## Stability notes
- The Eaglercraft integrated-server GUI-hangup can leave the client in
  WORLD_UNLOADING; a spurious IPC ACK previously crashed the client. Fixed in
  product code:
  - `IntegratedServerState.assertState` → tolerant (logs, does not throw).
  - `SingleplayerServerController.ensureReady` → recovers from
    WORLD_UNLOADING before a fresh world init.
- If a run still fails to reach in-world (menu-recovery state), the launcher
  retries with a fresh profile (`MAX_RUNS`). Observed 5/5 PASS across cold
  starts.

## AutoValidate gate
Validation only fires when the world name contains **`eaglerValidate`**
(see `RailV2AutoValidate`). Normal worlds are unaffected.

## Do not
- Kill unrelated user processes during cleanup (only the profile/port-scoped
  Chrome tree is terminated).
- Commit webhook URLs.
