# R15_MUTATION_GUARD_RESULTS — Phase 1-R15

Date: 2026-08-13 JST
Method: 9 deliberate violations applied to clean sources; the matching R15
test verified to FAIL; restored byte-for-byte (git diff clean). Driver is a
temporary helper in /tmp/opencode (never committed).

## Results: 9 / 9 detected and reverted

| # | Violation | Detected by | Detected |
|---|-----------|-------------|----------|
| 1 | traversal guard disabled (.. accepted) | z01_traversalRejected | YES |
| 2 | absolute path guard disabled | z02_absolutePathRejected | YES |
| 3 | unknown renderer accepted as SUPPORTED | r01_rendererMapping | YES |
| 4 | stable asset id made non-deterministic | i01_stableAssetId | YES |
| 5 | duplicate asset id allowed | i02_duplicateIdRejected | YES |
| 6 | MQO texture path double-slash not normalized | m01_mqoParsed | YES |
| 7 | asset gauge mutation changes geometry | g01_assetSwitchGeometryInvariant | YES |
| 8 | missing MQO accepted as LOADED | m03_missingMqoFallsBack | YES |
| 9 | asset replace changes cant (F4 violated) | r12_assetReplaceInvariance | YES |

## Coverage of R15-15 mutation list
- traversal guard disabled -> #1
- renderer JS execution / unknown treated SUPPORTED -> #3
- duplicate asset ID allowed -> #5
- asset switch RailPath rebuild / railId change -> #7 (gauge->geometry) + #9 (cant)
- texture path validation disabled -> #6
- unsupported renderer treated SUPPORTED -> #3
- malformed MQO silent accept -> covered by m02 (broken MQO => no objects) + #8
- ModelPack ZIP stage into repo -> covered by Git Policy (clean-room) + docs
- asset change alters gauge/centerline -> #7

## Post-verification
- Full harness after restore: 296 PASS / 0 FAIL / 3 SKIP.
- All mutated files byte-identical (git diff empty).
- Protected files untouched.
