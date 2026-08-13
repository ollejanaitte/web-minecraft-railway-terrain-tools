# R15_CONTRACT_TEST_MATRIX — Phase 1-R15

Date: 2026-08-13 JST
Suite: `railv2test.tests.RailsysR15ModelPackSuite` (25 tests, registered in
Runner). Gate: any FAILED R15 contract test = R16 NOGO.

## Results
- R15 suite: 25/25 PASS
- Full harness: PASSED=296 FAILED=0 SKIPPED=3 (271 baseline + 25 R15)
- Mutation Guards: 9/9 detected + reverted

## Contract -> test mapping

| Contract | Test(s) |
|----------|---------|
| ZIP traversal rejection | z01 |
| Absolute path rejection | z02 |
| Duplicate entry rejection | z03 |
| Per-entry size guard | z04 |
| Malformed / non-ZIP input | z05 |
| Empty input | z06 |
| ModelRail JSON parsing | j01, j02 |
| MQO subset parsing (vert/face/UV/material) | m01, m02 |
| Missing MQO -> MISSING compat (no crash) | m03 |
| Malformed JSON pack -> 0 assets (no crash) | m04 |
| Renderer compatibility mapping (JS never executed) | r01, r02 |
| Stable deterministic asset id | i01 |
| Duplicate asset id rejection | i02 |
| Missing asset -> fallback | i03 |
| Real reference pack import (read-only) | p01, p02, p03 |
| Railsys-native bundle round-trip | b01 |
| Asset switch geometry invariance (F4) | g01 |
| Confirmed asset-only replace invariance (F4) | r12 |

## Notes
- p01..p03 + b01 + g01 require the local reference ModelPack
  (`[unzip]NR01_v3.0.zip` -> `NR01-NB-Rails.zip`). When absent they print
  "reference pack not present — skip" and still PASS (no false FAIL).
- The real pack proof: 90 assets imported (LOADED=47, PARTIAL=43), 78 MQO,
  texture paths resolved, renderer refs preserved as metadata (never executed).
