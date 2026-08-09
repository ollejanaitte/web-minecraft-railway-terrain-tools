# Test Fixtures

Source of truth: `src/harness/java/railv2test/fixtures/RailFixtures.java`
These are pure data + reference geometry (Phase 0). Units: metres / blocks,
degrees.

| Fixture | Type | Parameters | Purpose |
|---------|------|-----------|---------|
| FIXTURE_STRAIGHT_100M | straight (piece 1) | (0,64,0)->(0,64,100) along +Z | straight distance/sampling/yaw |
| FIXTURE_CURVE_90_DEG | cubic Bezier (piece 2) | P0(0,64,0) C1(5,64,0) C2(10,64,5) P3(10,64,10) | 90-degree curve, arc length, tangent |
| FIXTURE_S_CURVE_A/B | Bezier (pieces 3,4) | two mirrored quarter curves | S-curve continuity |
| FIXTURE_GRADIENT | straight (piece 5) | (0,64,0)->(0,72,100) | 8% gradient, pitch |
| FIXTURE_VERTICAL_CURVE | vertical Bezier (piece 6) | (0,64,0)->(0,68,80), handles dy0=6,dy1=0 | smooth pitch transition |
| FIXTURE_CANT | cant-wrapped straight (piece 7) | roll ramp to 5 deg mid | roll continuity |
| FIXTURE_MULTI_PIECE | pieces 8,9,10 | straight + curve + straight | junction continuity |
| FIXTURE_LOOP_SIMPLE | pieces 11..18 | 40x40 loop, 4 straight + 4 curve (testloop-like) | loop placement/soak |
| FIXTURE_SWITCH_BASIC_CONTRACT | string | "SWITCH piece: start -> branch A(straight)|branch B(curve); active branch by state; followers follow active branch." | Phase 3 contract |
| FIXTURE_FORMATION_2/8/16 | int | car counts | formation scenarios |
| FIXTURE_CAR_SPACING_M | double | 20.0 | default spacing (20m-class car) |

## Geometric notes
- FIXTURE_CURVE_90_DEG midpoint at t=0.5 = (6.875, 3.125); end tangent yaw 0.
- FIXTURE_MULTI_PIECE junctions connect exactly by construction
  (straight end == next start); yaw continuous (both 90 deg at piece 8/9).
- FIXTURE_LOOP_SIMPLE matches the v1 testloop node layout (size 40, radius 10,
  kappa 0.55228475) so the Phase 2 soak can reuse it as the regression course.

## Future fixtures (Phase 3+)
- FIXTURE_SWITCH_BASIC: real branch geometries (data definition now).
- Long soak course: FIXTURE_LOOP_SIMPLE extended with gradients/cants/switch.
