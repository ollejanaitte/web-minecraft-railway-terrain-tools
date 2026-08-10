# Phase 1 Fixed Test Course (Phase 0.6 - PART C, frozen)

Status: FROZEN. Runs on the Flat Validation World (Superflat, COURSE_Y base).
Coordinates are world block coords on the flat world. No product code changed.

## Placement rule
Test course pieces are authored on the Flat Validation World at a fixed base
Y (e.g. 64) with the `eaglerflat` naming so AutoValidate/validation gate
applies; the course is the Phase 1 visual + numerical reference.

## TC-01: 100m Straight
- Geometry: Straight (0,64,0) -> (100,64,0), length 100.0.
- Purpose: exact length + yaw 90 (east) + pitch 0; baseline.
- Numerical: length error <= 1e-6; endpoint at (100,64,0).
- Screenshot: SS-R1-01_STRAIGHT (camera side, yaw 180, pitch 20).

## TC-02: Straight -> 90deg Curve -> Straight
- Straight (0,64,0)->(80,64,0); HorizontalBezier (80,64,0) c1(140,64,0)
  c2(200,64,60) p3(200,64,80) (~157m); Straight (200,64,80)->(280,64,80).
- Purpose: straight/curve/straight continuity; 90deg turn; piece joins.
- Numerical: join positions within 1e-4; yaw continuity at both joins.
- Screenshot: SS-R1-03_STRAIGHT_CURVE_JOIN.

## TC-03: Gentle Curve
- HorizontalBezier with large radius (~90deg over ~300m).
- Purpose: low-curvature accuracy.
- Numerical: arc-length error <= 0.5% (vs adaptive reference); sample
  distance monotonic.
- Screenshot: SS-R1-02_CURVE (curve view).

## TC-04: Tight Curve
- HorizontalBezier small radius (~15m, 90deg).
- Purpose: high-curvature sampling accuracy; renderer resolution.
- Numerical: no self-intersection; length within tolerance; NaN count 0.
- Screenshot: SS-R1-02_CURVE (tight) or dedicated.

## TC-05: S-Curve
- Bezier left then right (S shape) over ~240m.
- Purpose: reverse curvature; tangent/yaw continuity through inflection.
- Numerical: yaw continuous (no jump > tolerance); pitch 0.
- Screenshot: SS-R1-04_S_CURVE.

## TC-06: Piece Boundary
- Three pieces joined at nodes (straight+curve+straight); boundary at
  shared nodes.
- Purpose: boundary crossing position/yaw continuity; no gap.
- Numerical: boundary position diff <= 1e-4 between exit/entry samples;
  yaw diff <= 0.5 deg.
- Screenshot: SS-R1-05_PIECE_BOUNDARY (zoom at a join).

## TC-07: Gradient
- Straight with +10% grade (ey > sy) plus VerticalBezier transitions.
- Purpose: pitch continuity; grade rendering.
- Numerical: pitch within tolerance; grade = dy/ds = 0.1 target.
- Screenshot: SS-R1-06_GRADIENT.

## TC-08: Overview
- Full course (TC-02 plus gradient leg) from high camera.
- Purpose: whole-course visibility; no disappearance.
- Screenshot: SS-R1-07_OVERVIEW.

## Numerical checks (applied to every TC)
See PHASE1_SCOPE_AND_ACCEPTANCE.md for the frozen tolerances.

## Evidence
- Screenshots: doc/testing/phase0_6/screenshots/SS-R1-*.png (real game,
  Flat Validation World, Hardware Vulkan).
- Numerical: harness unit outputs (railv2 harness) recorded in the Phase 1.x
  evidence dir.
