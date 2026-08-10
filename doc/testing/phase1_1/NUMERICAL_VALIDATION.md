# NUMERICAL_VALIDATION.md — Phase 1.1

## Fixed tolerances (Phase 0.6)

| Check | Tolerance |
|-------|-----------|
| Straight length | ≤ 1e-6 m |
| Curve arc-length | ≤ 0.5% (or 0.1 m floor) vs independent dense integration |
| Join position | ≤ 1e-4 m |
| Yaw/pitch/roll continuity | ≤ 0.5° |
| Distance sampling | ≤ 1e-3 m |
| Boundary jump | ≤ 1e-4 m |
| NaN/Inf | 0 |
| Unexpected exception | 0 |

## Reference method

Curve length: independent 65536-split polyline of the **same** cubic XZ (+ linear Y per Phase 0.6). Does **not** use production `ArcLengthTable` as oracle.

## Test cases (ProductionGeometryTest)

| ID | Result |
|----|--------|
| T01 Straight 100m | PASS |
| T02 Short straight | PASS |
| T03 Gentle curve | PASS |
| T04 Tight curve | PASS |
| T05 S-curve join fixture | PASS |
| T06 Up gradient | PASS |
| T07 Down gradient | PASS |
| T08 Curve+gradient | PASS |
| T09 Endpoints | PASS |
| T10 Out-of-range clamp | PASS |
| T11 Determinism | PASS |
| T12 Dense sampling | PASS |
| T13 Degenerate reject | PASS |
| T14 LocalFrame orthonormal | PASS |
| T15 Long 2000m | PASS |
| productionDistanceRoundTrip | PASS |
| fromAnchorsHermiteMapping | PASS |
| verticalBezierGeometry | PASS |
| nanDistanceThrows | PASS |

## Harness summary

```
PASSED=49 FAILED=0 SKIPPED=3
RESULT: SUCCESS
```

## Worst errors observed

- Straight length: exact (diff 0 within 1e-6)
- Arc-length vs 64k independent: within 0.5% / 0.1 m band (PASS)
- Join fixture: position within 1e-4; angles within 0.5°
- NaN count: 0
- Unexpected exceptions: 0 (expected IllegalArgument/IllegalState only on invalid input tests)
