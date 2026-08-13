# R16_CONTRACT_TEST_MATRIX — Phase 1-R16

Date: 2026-08-13 JST
Suite: `railv2test.tests.RailsysR16NetworkSuite` (47 tests, registered in
Runner). Gate: any FAILED R16 contract test = R17 NOGO.

## Results
- R16 suite: 47/47 PASS
- Full harness: PASSED=347 FAILED=0 SKIPPED=3 (300 baseline + 47 R16)
- Mutation Guards: 12/12 detected + reverted

## Category -> test mapping

| Category | Test(s) |
|----------|---------|
| A. Closed Loop Geometry Correction | a01 (4 straights + 4 curves), a02 (quarter-circle not chord), a03 (sagitta), a04 (radius) |
| B. Curve Continuity | b01 (position), b02 (tangent), b03 (no inflection/diagonal) |
| C. Symmetry / Closure | c01 (4-corner symmetry), c02 (closure), c03 (bounding box) |
| D. RailNode | d01 (stable nodeId), d02 (duplicate position), d03 (lifecycle) |
| E. RailConnection | e01..e06 (valid/self/position-gap/tangent/gauge/duplicate) |
| F. Endpoint Snap | f01 (unique), f02 (ambiguous), f03 (far) |
| G. Continuous Placement | g01 (snap+connect), g02 (chain of 3) |
| H. Explicit Topology | h01 (loop topology 8+8), h02 (geometry AND topology closed) |
| I. Forward/Reverse Traversal | i01 (forward round trip), i02 (reverse round trip), i03 (step guard) |
| J. Crossing Without Connection | j01 |
| K. R15 ModelPack Regression | k01 (appearance never changes geometry) |
| L. R10F/R13/R14 Regression | l01 (gauge/cant), l02 (mesh continuity), l03 (sleeper/no-NaN) |
| Edge Cases | v01..v08 |

## Tolerances (R16-03 frozen)
- corner length vs true quarter arc: < 0.1 m
- sagitta vs true circle: < 0.05 m
- corner local radius error: < 0.2 m
- position continuity: < 1e-6 m
- tangent continuity: < 1e-3 deg
- closure position: < 1e-6 m, closure tangent: < 1e-3 deg
- connection position tolerance: 0.25 m, tangent: 2.0 deg, gauge: 0.01 m
- node coalesce tolerance: 0.5 m
