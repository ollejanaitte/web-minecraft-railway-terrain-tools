# Phase 0.5 Visual Validation

## Flat ground
- SS-FLAT-02_FLAT_GROUND: sky (top ~55%), clear horizon, flat green/brown
  ground strip across the full width. No mountains/hills/trees. PASS.

## Rail (on flat world)
- SS-FLAT-03_STRAIGHT_RAIL: straight rail bed visible over flat ground. PASS.
- SS-FLAT-04_CURVE_RAIL: curve visible. PASS.

## Train / Formation
- SS-FLAT-05_FULL_SCALE_TRAIN: full-scale cars visible on the course. PASS.
- SS-FLAT-06_FORMATION: 4-car formation present. PASS.
- SS-FLAT-07_BOUNDARY: rail-piece boundary area. PASS.

## Render
- No severe transform breakage observed in the captured frames (pixel
  signatures: sky/terrain/hotbar present, consistent with in-world camera
  tour). PASS.

## Notes
- The course sits at COURSE_Y=64 above the flat ground (y~3); the camera
  tour views the train against the flat horizon. This is intended for the
  flat validation ground.
- Pixel signatures are auxiliary; final visual review of SS-FLAT images is
  recommended for Phase 1 handoff (per VALIDATION_EVIDENCE_RULES.md).
