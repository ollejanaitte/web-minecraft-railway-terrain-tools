# R16_MUTATION_GUARD_RESULTS — Phase 1-R16

Date: 2026-08-13 JST
Method: 12 deliberate violations applied to clean sources; the matching R16
test verified to FAIL; restored byte-for-byte (git diff clean). Driver is a
temporary helper in /tmp/opencode (never committed).

## Results: 12 / 12 detected and reverted

| # | Violation | Detected by | Detected |
|---|-----------|-------------|----------|
| 1 | corner handle back to 1.0 (octagonal) | a02_cornerIsQuarterCircleNotChord | YES |
| 2 | corner handle too short (sagitta broken) | a03_cornerSagittaMatchesCircle | YES |
| 3 | closure endpoint tangent shifted 5 deg | c02_closure | YES |
| 4 | duplicate connection allowed | e06_duplicateConnectionRejected | YES |
| 5 | position tolerance inflated (proximity-only) | e03_positionGapRejected | YES |
| 6 | tangent tolerance inflated | e04_tangentMismatchRejected | YES |
| 7 | self connection allowed | e02_selfConnectionRejected | YES |
| 8 | forward traversal broken | i01_forwardTraversalRoundTrip | YES |
| 9 | reverse traversal broken | i02_reverseTraversalRoundTrip | YES |
| 10 | crossing auto-connected | j01_crossingWithoutConnection | YES |
| 11 | node coalesce disabled (no shared joints) | h01_closedLoopTopology | YES |
| 12 | asset id changes corner geometry (F4) | k01_modelPackAppearanceNeverChangesGeometry | YES |

## Post-verification
- Full harness after restore: 342 PASS / 0 FAIL / 3 SKIP.
- All mutated files byte-identical (git diff empty).
- Protected files untouched.
