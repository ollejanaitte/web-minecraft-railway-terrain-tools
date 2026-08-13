# R13_CONTRACT_TEST_MATRIX — Phase 1-R13

Date: 2026-08-13 JST
Suite: `railv2test.tests.RailsysR13ProductionDataSuite` (30 tests, registered
in railv2test.Runner). Gate rule: any FAILED R13 contract test = R14 NOGO.

## Results

- R13 suite: 30/30 PASS
- Full harness: PASSED=237 FAILED=0 SKIPPED=3 SUCCESS (207 baseline + 30)

## Contract → test mapping

| Contract | Test(s) |
|----------|---------|
| Production Rail Data Model | a01..a05 |
| Stable Rail ID | b01..b06 |
| Preview→Confirm exact handoff | c01..c07 |
| Rail-level Validation | d01..d10 |
| Numeric Limits | e01..e02 |

### a01..a05 Production Data
- a01 segment created from final preview (id carried, length, kind)
- a02 endpoint anchor authoritative (support surface)
- a03 derived path rebuilds identically
- a04 world store keeps active rails
- a05 metadata + schema-reserved signal/occupied hooks

### b01..b06 Stable ID
- b01 preview has no stable id
- b02 confirm assigns stable id
- b03 second confirm different id
- b04 duplicate id rejected
- b05 retired id not reused
- b06 malformed id rejected (null/empty/prefix-only/non-numeric/zero/negative)

### c01..c07 Preview→Confirm
- c01 preview fingerprint stable
- c02 confirm fingerprint == preview fingerprint
- c03 preview length == confirmed length
- c04 preview endpoints == confirmed endpoints
- c05 preview cant == confirmed cant
- c06 promoted preview is the EXACT object
- c07 no second geometry pipeline (derived == fromMarkers)

### d01..d10 Validation
- d01 valid passes
- d02 too short rejected
- d03 too long rejected
- d04 zero length rejected
- d05 NaN rejected
- d06 gradient > 45 rejected
- d07 cant > 45 rejected
- d08 gauge out of [0.6,1.8] rejected
- d09 missing asset rejected
- d10 retired lifecycle rejected

### e01..e02 Limits
- e01 limits frozen (min>0, max>min, 45/45, gauge 0.6/1.8)
- e02 255m boundary accepted (<=256)

## Regression (unchanged, must stay green)

- Foundation Contract Suite (30) — R10F F1-F6 source+golden guards.
- Existing 207-test harness baseline.
