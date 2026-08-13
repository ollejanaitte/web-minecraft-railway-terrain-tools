# R14_MUTATION_GUARD_RESULTS — Phase 1-R14

Date: 2026-08-13 JST
Method: 7 deliberate violations applied to clean geometry-core sources; the
matching R14 test verified to FAIL; restored byte-for-byte (git diff clean).

## Results: 7 / 7 detected and reverted

| # | Violation | Detected by | Detected |
|---|---|---|---|
| 1 | gauge offset sign flipped (left/right swapped) | g01_gaugeDistance | YES |
| 2 | frame up ignored in cross-section (cant broken) | g02_gaugeWithCantMaintained | YES |
| 3 | sleeper placed by sample index instead of distance | s01_sleeperSpacingDistanceBased | YES |
| 4 | section boundary gap inserted (0.001m) | m02_sectionBoundaryNoGapNoJump | YES |
| 5 | invalid profile dimension injected | c01_railProfileDimensions | YES |
| 6 | corner centre shifted (closure broken) | loop02_endpointClosure | YES |
| 7 | corner end yaw wrong (tangent broken) | loop02_endpointClosure | YES |

## Coverage of R14-11 mutation list

- left/right gauge offset sign -> #1
- cant frame ignored -> #2
- sleeper sample-index -> #3
- mesh section boundary gap -> #4
- invalid profile dimension -> #5
- closed-loop position closure -> #6
- closed-loop tangent continuity -> #7
- (asset centerline change / gradient Y / left-right rail swap / fixed world
  axis / non-production geometry) are guarded by R14 numeric tests + R10F F4 +
  R13 pipeline — the 7 above are the representative mutation set.

## Post-verification

- Full harness after restore: 273 PASS / 0 FAIL / 3 SKIP.
- All mutated files byte-identical (git diff empty).
- Protected files untouched.
- Re-run 2026-08-13 (post Sol-review): mutation #3 search string updated to the
  half-open sleeper-loop code; 7/7 again detected and reverted.
