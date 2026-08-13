# R17_MUTATION_GUARD_RESULTS — Phase 1-R17

Date: 2026-08-14 JST
Method: 11 deliberate violations applied to clean sources; the matching R17
test verified to FAIL; restored byte-for-byte (git diff clean). Driver is a
temporary helper in /tmp/opencode (never committed).

## Results: 11 / 11 detected and reverted

| # | Violation | Detected by | Detected |
|---|-----------|-------------|----------|
| 1 | min divergence lowered (1 deg accepted) | g03_tooSmallDivergenceRejected | YES |
| 2 | max divergence raised (90 deg accepted) | g04_tooLargeDivergenceRejected | YES |
| 3 | gauge tolerance inflated | g05_gaugeMismatchRejected | YES |
| 4 | divergence wrap broken | g01_divergenceComputed | YES |
| 5 | diverging lead path broken (null) | g06_divergingPathBuildsAndFinite | YES |
| 6 | min-divergence validation disabled | j03_invalidDivergenceRejectedAtRegistration | YES |
| 7 | duplicate junction id issued | j02_duplicateJunctionIdNeverIssued | YES |
| 8 | invalid branch index accepted | r03_invalidBranchIndexUnknown | YES |
| 9 | resolveRoute ignores committed branch | r02_branchRoute | YES |
| 10 | branch route input ignored | r04_routeSwitchInSession | YES |
| 11 | junction mutates loop geometry | l02_switchGeometryNeverTouchesLoop | YES |

## Post-verification
- Full harness after restore: 368 PASS / 0 FAIL / 3 SKIP.
- All mutated files byte-identical (git diff empty).
- Protected files untouched.
- Note: the three post-mutation hardening tests (j06 shared-node, j07 branch
  position, j08 non-finite) and the strengthened l02 were added after the
  mutation run; they are covered by the shared-node/non-finite production
  guards verified by source review (Sol round 1).
