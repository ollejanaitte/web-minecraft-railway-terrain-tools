# REGRESSION_RESULTS.md — Phase 1.1 (final)

| Check | Result |
|-------|--------|
| Harness | PASSED=49 FAILED=0 SKIPPED=3 |
| Production Build (`makeMainOfflineDownload`) | SUCCESS |
| ProductionGeometryTest T01–T15 + extras | ALL PASS |
| Independent 64k arc-length reference | within 0.5% / 0.1 m |
| NaN / Infinity | 0 |
| Unexpected exceptions | 0 (expected rejects only) |
| dirty v1 MD5 | PRESERVED (9afd512d…, 77ce0d66…) |
| Phase 0.1 RailV2 spike | retained |
| Flat Validation World reached | YES (EaglerFlatValidate, Superflat) |
| Hardware Vulkan | YES (NVIDIA GeForce GTX 1050 / ANGLE Vulkan) |
| AutoValidate fired | YES |
| Geometry debug evidence | placed + visible in screenshots |
| SS-R1_1 screenshots | 8 / 8 captured |
| Visual Review | PASS (all 8 items) |
| Cleanup | PASS (no stale chrome) |
| Secret leak | none |

## Phase 0.6 numerical acceptance (re-run final)

| Check | Tolerance | Result |
|-------|-----------|--------|
| Straight length error | ≤ 1e-6 m | PASS (exact) |
| Curve arc-length error | ≤ 0.5% (0.1 m floor) | PASS |
| Join position | ≤ 1e-4 m | PASS |
| Yaw/pitch/roll continuity | ≤ 0.5° | PASS |
| Distance sampling | ≤ 1e-3 m | PASS |
| Boundary jump | ≤ 1e-4 m | PASS |
| NaN / Infinity | 0 | PASS |
| Unexpected exception | 0 | PASS |

## Verdict linkage

Numerical + build + Flat-World visual all green → **Phase 1.1 PASS**.
