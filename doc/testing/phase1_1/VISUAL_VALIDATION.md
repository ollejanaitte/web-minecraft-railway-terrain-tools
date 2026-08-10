# VISUAL_VALIDATION.md — Phase 1.1

Status: **COMPLETE** (Phase 1.1 resume 2026-08-10, after prior STOP at 2026-08-10)

## Intent

Flat Validation World + Hardware Vulkan + geometry debug centerline markers
→ screenshots `SS-R1_1-01` … `SS-R1_1-08`.

## Method (resume)

- Screen-state driven automation: an instrumented copy of the offline HTML
  bakes in the EaglercraftX `screenChanged` hook
  (`doc/testing/phase1_1/scripts/geom_validate_instrumented.html`, regenerated
  automatically by `geom_validate.mjs` when the built HTML changes).
- The hook exposes the exact current GuiScreen class name, so navigation
  (content warning → default-username note → main menu → create form) and
  create-retry are deterministic instead of pixel-heuristic.
- Camera-tour tags are now announced as chat messages
  (`railsysv2: camera tour=<tag>`) from `RailV2AutoValidate`; the client
  console observes them and the automation captures each geometry camera
  exactly when it is held.
- Renderer: ANGLE Vulkan, NVIDIA GeForce GTX 1050 (confirmed in game console:
  `OpenGL Renderer: ANGLE (NVIDIA, Vulkan 1.4.312 (NVIDIA NVIDIA GeForce GTX 1050...), NVIDIA)`).

## Run metadata (evidence set)

- RUN_ID: `geom-20260810_134441-1` (single successful run; create attempt 1/3)
- World: `EaglerFlatValidate` (Superflat via `eaglerflat` name hook)
- Gate: `railsysv2: worldName=New aEaglerFlatValidateWorld validation=true`
- AutoValidate: `railsysv2: auto-validated (build + 4 cars started)`
- Script exit: SUCCESS shots=8
- Logs: `doc/testing/phase1_1/logs/geom_validate.log`
- Screenshots: `doc/testing/phase1_1/screenshots/`

## Evidence

| # | File | Camera (tour tag) | Expected | Observed | Verdict |
|---|------|-------------------|----------|----------|---------|
| 01 | SS-R1_1-01_STRAIGHT_GEOMETRY.png | geom_straight (0,78,190) yaw 0 | 100 m straight, gold markers, tangent torches, no kink | Gold-marker column forms a clean vertical straight line (slope ≈ -3.2, perp residual ~14 px over 214 px span); no kink; coordinates bounded | PASS |
| 02 | SS-R1_1-02_GENTLE_CURVE.png | geom_gentle (40,82,190) yaw -20 | smooth continuous curve, diamond markers | Diamond markers spread smoothly across a wide arc (bbox x 322-714, y 286-502); continuous, no cusp | PASS |
| 03 | SS-R1_1-03_TIGHT_CURVE.png | geom_tight (180,78,195) yaw -30 | still smooth, no folding, tangent continuous | Emerald markers form a compact smooth arc (bbox x 572-630, y 334-534); no fold/NaN artifact | PASS |
| 04 | SS-R1_1-04_S_CURVE_FIXTURE.png | geom_s_curve (220,80,195) yaw -10 | opposing curvature, continuous join | Redstone markers (x 460-918, lower) + lapis markers (x 506-692, upper) show two opposing arcs; no gap/jump | PASS |
| 05 | SS-R1_1-05_GRADIENT.png | geom_gradient (280,78,190) yaw 0 | elevation change, pitch correct, no vertical kink | Iron markers form straight ascending column (8 m rise over 100 m); straight, bounded, no kink | PASS |
| 06 | SS-R1_1-06_CURVE_GRADIENT.png | geom_curve_grad (320,82,190) yaw -25 | horizontal curve + vertical change, 3D composition | Coal markers trace a rising curve; orientation plausible, no axis/sign inversion | PASS |
| 07 | SS-R1_1-07_LOCAL_FRAME.png | geom_local_frame (420,78,195) yaw 0 | forward markers visible, no frame flip | Gold centerline + glowstone forward markers form consistent parallel lines; handedness continuous, no flip | PASS |
| 08 | SS-R1_1-08_OVERVIEW.png | geom_overview (200,95,250) yaw 180 pitch 45 | whole course coherent | Multiple marker types (emerald, redstone, lapis) visible in one overview; course coherent, no explosion | PASS |

## Visual Review (programmatic)

| Item | Verdict | Reason |
|------|---------|--------|
| Straight | PASS | Gold markers collinear; no kink, no coordinate explosion |
| Gentle curve | PASS | Diamond markers, smooth wide arc |
| Tight curve | PASS | Emerald markers, compact smooth arc, no folding |
| S-curve fixture | PASS | Redstone + lapis opposing arcs, continuous join |
| Gradient | PASS | Iron markers straight ascending line, pitch direction correct |
| Curve + gradient | PASS | Coal markers rising curve, 3D composition plausible |
| Local frame | PASS | Gold centerline + glowstone forward markers, no frame flip |
| Overview | PASS | Whole debug course visible, coherent |

Gate failure conditions checked: line kink (none), visible jump (none), sample
inversion (none), tangent flip (none), geometry explosion (none — all marker
clusters bounded in-frame), invisible expected geometry (none — markers
detected per feature), wrong pitch sign (none), frame flip (none), obvious
coordinate offset (none), NaN/Inf artifact (none — blocks placed with finite
coordinates), debug evidence missing (none).

## Gate impact

Phase 1.1 visual gate satisfied → part of `PHASE 1.1 VERDICT: PASS`.

Prior stop history (2026-08-10): the STOPPED run produced 0 SS-R1_1 screenshots
(menu-recovery race, world create automation). Resume fixed automation (screen
hook + chat tour tags + create retry) and captured all 8.
