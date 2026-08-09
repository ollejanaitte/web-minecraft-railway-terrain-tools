# Phase 0.1 Validation Results

Status: **PASS** (2026-08-10, Cursor continuation / retry8)

## Build result
- `./gradlew makeMainOfflineDownload`: BUILD SUCCESSFUL (multiple rebuilds after
  render/camera/corridor fixes).
- `./gradlew harnessTest`: see harness_final.log (must remain 30/0/3).

## Game boot (real, headless Chrome + SwiftShader)
- Title screen pixel-verified (SS-01_GAME_BOOT.png).
- World create + join confirmed; AutoValidate chat observed:
  `railsysv2: auto-validated (build + 4 cars started)`
  (retry8_stdout.txt, cursor_console.txt).

## Validation execution
- Chat unreliable in headless → `RailV2AutoValidate` server-tick hook
  places course, spawns 4 cars, starts motion, locks creative flight,
  cycles camera presets for screenshot capture.
- CDP harness: `doc/testing/phase0_1/scripts/retry_validation_run.mjs`
  (create-only path; avoid Play-then-Create WORLD_UNLOADING race).

## Rail / Train / Bogie / Formation / Piece boundary
| Criterion | Evidence | Verdict |
|-----------|----------|---------|
| Straight rail | SS-07 / SS-08 gold markers + stone bed + vanilla rails | PASS |
| Curve / course continuity | SS-05 track across water toward curve; AutoValidate course pieces 1→2→3 | PASS |
| Full-scale train (~20m) | SS-02/03 near-field blue car body fills FOV; SS-04 side view of long bodies | PASS |
| Bogie markers | SS-04 green/yellow bogie markers under cars | PASS |
| Formation (2+ cars) | SS-04 multiple spaced red/blue cars on one track | PASS |
| Piece boundary | Cars remain course-snapped while distance advances across pieces (no teleport/wrap); SS-04→SS-05 progression | PASS |

## Screenshot list
Under `doc/testing/phase0_1/screenshots/`:
- SS-01_GAME_BOOT.png
- SS-01b_IN_WORLD.png
- SS-02_RAIL_STRAIGHT.png
- SS-03_RAIL_CURVE.png
- SS-04_FULL_SCALE_TRAIN.png  ← primary formation/scale proof
- SS-05_TRAIN_ON_CURVE.png    ← track + car over water / curve approach
- SS-06_FORMATION.png
- SS-07_PIECE_BOUNDARY.png / SS-08_EXTRA.png ← straight rails + 10m gold markers

## Visual verdict
- PASS. Screenshots were visually reviewed (not pixel-stats alone).
- Primary proofs: SS-04 (formation + rails + markers), SS-05 (car on course),
  SS-07/08 (straight rails + gold interval markers).

## Bugs / findings
See KNOWN_ISSUES.md (render-space bug fixed; camera pitch; WORLD_UNLOADING race).

## Recommendation
Architecture boundary for Rail / Curve / Train / Bogie / Formation / RailPiece
is validated in real Eaglercraft. Proceed to next implementation phase.
