# R17_CONTRACT_TEST_MATRIX — Phase 1-R17

Date: 2026-08-14 JST
Suite: `railv2test.tests.RailsysR17SwitchSuite` (21 tests, registered in
Runner). Gate: any FAILED R17 contract test = R18 NOGO.

## Results
- R17 suite: 21/21 PASS
- Full harness: PASSED=368 FAILED=0 SKIPPED=3 (347 baseline + 21 R17)
- Mutation Guards: 11/11 detected + reverted

## Contract -> test mapping

| Contract | Test(s) |
|----------|---------|
| Divergence angle computation (wrap) | g01 |
| Valid divergence within [2,30] deg | g02 |
| Too-small divergence (< 2 deg) rejected | g03 |
| Too-large divergence (> 30 deg) rejected | g04 |
| Gauge mismatch rejected (0.01m) | g05 |
| Diverging lead path builds finite + tangent continuity | g06 |
| Bad diverging-path input -> null | g07 |
| Junction registered (stable id, node, branch) | j01 |
| Distinct junction ids never reused | j02 |
| Invalid divergence rejected at registration | j03 |
| No branches rejected | j04 |
| Retired junction rejected after remove | j05 |
| Disconnected mainIn/mainOut rejected (no shared node) | j06 |
| Branch not at shared node rejected | j07 |
| Non-finite divergence input rejected | j08 |
| THROUGH route -> mainOut | r01 |
| BRANCH route -> branch | r02 |
| Invalid branch index -> UNKNOWN (no silent wrong route) | r03 |
| In-session route switching THROUGH<->BRANCH | r04 |
| Closed-loop spur junction (THROUGH keeps loop / BRANCH diverts) | l01 |
| Junction never modifies loop geometry (length + endpoints + samples) | l02 |

## Tolerances (R17 frozen)
- switch divergence: [2.0, 30.0] deg
- gauge compatibility: 0.01 m
- diverging lead path: F2 pipeline, must be finite with continuous tangents
