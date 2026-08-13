# R13_MUTATION_GUARD_RESULTS — Phase 1-R13

Date: 2026-08-13 JST
Scope: prove the R13 contract suite REALLY fails when a production-data
contract is violated, then revert byte-for-byte.

## Method

Each mutation was applied to a clean geometry-core source, the full harness
was run, the matching R13 test was verified to FAIL, and the file was restored
to exact prior bytes (`git diff --exit-code` clean afterwards). Driver is a
temporary helper (never committed).

## Results: 11 / 11 violations detected and reverted

| # | Deliberate violation | Detected by | Detected |
|---|---|---|---|
| 1 | Zero id accepted (`RailId` guard relaxed) | `b06_malformedIdRejected` | YES |
| 2 | Ids collide (`RailIdIssuer` always returns 1) | `b03_secondConfirmDifferentStableId` | YES |
| 3 | Duplicate id allowed (`RailWorldData` guard disabled) | `b04_duplicateIdRejected` | YES |
| 4 | Retired id allowed (guard disabled) | `b05_retiredIdNotReused` | YES |
| 5 | Too-short check disabled | `d02_tooShortRejected` | YES |
| 6 | Too-long check disabled | `d03_tooLongRejected` | YES |
| 7 | Cant limit disabled (incl. NaN) | `d07_cantLimitRejected` | YES |
| 8 | Fingerprint ignores asset id | `c02_confirmFingerprintEqual` | YES |
| 9 | Exact promotion broken (promoted preview = null) | `c06_promotedPreviewIsExactObject` | YES |
| 10 | Promoted/derived mismatch check disabled | `d13_promotedVsDerivedMismatchRejected` | YES |
| 11 | NaN cant accepted at construction | `d11_nanCantRejected` | YES |

Coverage: preview-stable-id (design + b06 + f02 source guard), confirm
different geometry (c06 + f01 source guard), duplicate/retired id (#3/#4),
NaN/limit validation disabled (#5/#6/#7 + d05/d11), fingerprint comparison
disabled (#8), phantom-path guard (#10).

## Post-verification

- Full harness after final restore: PASSED=245 FAILED=0 SKIPPED=3.
- All mutated files byte-identical (`git diff` empty on each after restore).
- Protected files untouched and byte-identical.
