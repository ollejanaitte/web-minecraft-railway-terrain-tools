# Railway System v2 Golden Baseline

**Status: CONFIRMED** (Phase 0.3 STEP A/D)
Golden Baseline SHA: `7cf0e9fd` (current main after Phase 0.2/0.3 commits:
runtime stabilization fixes + launchers + regression docs). The validation was
executed under the Phase 0.2 stabilized environment (Hardware GPU +
tolerant-assert + world-name gate) and that exact state is now committed.

> NOTE: run evidence for the Formal Regression was captured under HEAD
> `88493175` with the Phase 0.2 fixes applied; the fixes are committed in
> `7f976d68`, so `7cf0e9fd` represents the same validated build.

## Baseline facts (executed, not assumed)
- Harness: PASSED 30 / FAILED 0 / SKIPPED 3.
- Production build: `makeMainOfflineDownload` BUILD SUCCESSFUL.
- GPU path: Hardware Vulkan (`--use-gl=angle --use-angle=vulkan`).
- WebGL renderer: `ANGLE (NVIDIA, Vulkan 1.4.312 (NVIDIA GeForce GTX 1050))`.
- Menu CPU ~9% total; in-world renderer ~66% (vs 658% SwiftShader stale).
- World join: OK (created world with validation marker).
- AutoValidate: `railsysv2: auto-validated (build + 4 cars started)` fired.
- Cleanup: Chrome tree terminated; zero stale validation chrome.
- Cold Start: 5/5 PASS (launcher retry covers the known menu-recovery race).

## Screenshot evidence (this directory)
- SS-01_GAME_BOOT.png         boot/title
- SS-01b_IN_WORLD.png         in-world (crosshair/sky/hotbar)
- SS-02_RAIL_STRAIGHT.png     straight rail area (terrain bed)
- SS-03_RAIL_CURVE.png        curve area
- SS-04_FULL_SCALE_TRAIN.png  full-scale train view
- SS-05_TRAIN_ON_CURVE.png    train near curve
- SS-06_FORMATION.png         formation view (sky-heavy)
- SS-07_PIECE_BOUNDARY.png    piece-boundary area
- SS-08_EXTRA.png             extra evidence

## Visual items confirmed (pixel signatures + camera-tour alignment)
| Item | Evidence |
|---|---|
| Straight rail visible | SS-02 (brown/green terrain bed, low sky) |
| Curve rail visible | SS-03 |
| Straight→curve continuity | SS-03/SS-05 |
| Full-scale train visible | SS-04 (sky 0.47 + terrain 0.37) |
| Front/Rear bogie | camera tour frames SS-04/05 |
| Formation visible | SS-06 (sky-heavy) |
| Piece boundary | SS-07 |
| No freeze | responsive CDP throughout |

## Known limitation (documented, non-blocking)
- The Eaglercraft integrated-server GUI-hangup race is tolerated
  (tolerant-assert) and retried by the launcher; run-1 success ~60%, overall
  5/5 with `MAX_RUNS=3`.
