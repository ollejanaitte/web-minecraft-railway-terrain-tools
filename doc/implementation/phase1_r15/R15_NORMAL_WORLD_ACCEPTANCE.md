# R15_NORMAL_WORLD_ACCEPTANCE — Phase 1-R15

Date: 2026-08-13 JST
Driver: `doc/implementation/phase1_r15/r15_closed_loop.mjs` (headless Chrome/CDP,
normal Superflat world "RailsysR15"). PASS when the script completes without
FATAL and the console-verified lines match.

## Verified flow
1. Embedded Railsys-native bundle auto-imports: `railsys: modelpack bundle
   import registered=90/90`.
2. `/railsys15 assets` lists `nr01-nb-rails:1435mm_nb_concrete[PARTIAL]` etc.
3. `/railsys15 use nr01-nb-rails:1435mm_nb_concrete` -> current asset set.
4. `/railsys15 testloop` -> `built 8 segments, total 216.64m
   asset=nr01-nb-rails:1435mm_nb_concrete` (Standard Closed Loop).
5. `/railsys15 testloop_compact` -> compact loop (20x30, r6 at (70,0)).
6. `/railsys3 status` -> `prod=16` (8 standard + 8 compact), stable ids
   rail-1..rail-16.
7. Camera frames compact loop; screenshots captured (below).

## Screenshots (SHA-256, recorded after final run)
| File | SHA-256 |
|------|---------|
| SS-R15-01_CLOSED_LOOP_OVERVIEW.png | 64da97c4...2b62882c |
| SS-R15-02_MODELPACK_STRAIGHT.png | (see git) |
| SS-R15-03_MODELPACK_CURVE.png | (see git) |
| SS-R15-04_STRAIGHT_CURVE_BOUNDARY.png | (see git) |
| SS-R15-05_RAIL_TEXTURE_CLOSEUP.png | (see git) |
| SS-R15-06_ASSET_SELECTOR.png | a9f2236e...77b6ae |
| SS-R15-07_BEFORE_AFTER_DEFAULT.png | (see git) |
| SS-R15-07_BEFORE_AFTER_MODELPACK.png | (see git) |
| SS-R15-BEFORE_DEFAULT.png | 979fcbfc...2bbd8b63 |
| SS-R15-AFTER_MODELPACK.png | 3f290817...49fc14901 |

(Full SHA-256 list is stored in git history for the screenshots directory.)

## Acceptance results
- 8 Segment loop: PASS (console: `built 8 segments, total 216.64m`)
- 全周ModelPack appearance: PASS (asset=nr01-nb-rails:1435mm_nb_concrete)
- Straight / Curve / Straight->Curve boundary: PASS (Luna vision)
- Rail close-up / texture / UV: PASS (no purple/black missing texture)
- Sleeper/rail component appearance: PASS (ballast + rail drawn)
- no major gap / twist / catastrophic overlap: PASS (Luna)
- loop geometry unchanged / closure unchanged / gauge unchanged /
  stable railId unchanged: PASS (geometry-core invariance tests g01/r12 + prod=16)

## Luna vision result (GPT-5.6 Luna)
- Overview/straight/curve/boundary/closeup: continuous rounded-rectangle loop,
  grey rails with grey/brown ballast, no obvious defects. VERDICT: PASS.
- BEFORE vs AFTER (same camera): SAME_GEOMETRY_DIFFERENT_APPEARANCE. VERDICT: PASS.
- Asset selector GUI: list of assets with [LOADED]/[PARTIAL] tags.
  VERDICT: SELECTOR_VISIBLE. PASS.

## Numeric invariance (pixel-level)
- Rail footprint pixels BEFORE=284095 AFTER=284029 (~identical => Geometry SAME).
- Grey (concrete) appearance pixels BEFORE=511 AFTER=6893 (Appearance CHANGED).

## Asset Selector UX (Shift+Right-click)
- Shift+Right-click on AIR while holding the marker wand opens
  `RailsysAssetSelector` (Railsys-native GUI; pack + asset + compatibility +
  current selection). Block clicks keep the R7/R10 confirm contract.
- `/railsys15 open` opens the same GUI (headless command path).
