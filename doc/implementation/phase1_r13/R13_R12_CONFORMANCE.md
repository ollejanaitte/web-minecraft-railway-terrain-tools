# R13_R12_CONFORMANCE — Phase 1-R13

Date: 2026-08-13 JST
Scope: verify the R13 production-data implementation conforms to the R12
design freeze and does NOT over-reach into later phases.

## Conformance checklist

| R12 item | R13 implementation | Verdict |
|----------|--------------------|---------|
| Production Data Model (R12-A §4) | RailSegment / RailEndpointData / RailWorldData | CONFORMS |
| Stable ID timing (R12-A §3) | id issued at CONFIRM via world issuer; preview has no id | CONFORMS |
| Authoritative/Derived/Cache/Transient separation (R12-A §2) | documented + coded: anchor/id/asset/cant authoritative; path/gauge/blockPos derived; promoted preview transient; no cache in R13 | CONFORMS |
| Confirm handoff / exact promotion (R12-A §3.1/§5.1) | RailFingerprint (endpoints+asset+cant+path identity); promotedPreview = EXACT object; controller confirm never rebuilds | CONFORMS |
| Validation scope (R12-J §2) | RailSegmentValidator: rail-level only; network/snap/junction deferred | CONFORMS |
| Numeric limits measured, not guessed (R12-J §3.3) | limitMeasure tool + R13_LIMIT_MEASUREMENT_RESULTS | CONFORMS |
| Explicit future Node/Connection boundary (R12-A §4.3/4.4) | RailWorldData structured for later registries; NOT implemented | BOUNDARY PRESERVED |
| Signal/occupancy schema-reserved, not persisted (R12-A §2) | RailSegment.signalState/occupied default; not persisted in R13 | CONFORMS |
| gauge snapshot derived (asset authoritative, F4) | gaugeM snapshot; clampGaugeForDefaults | CONFORMS |
| No premature Network | no RailNode/RailConnection functional code (only reserved structure) | CONFORMS |
| No premature Switch | no junction/route/animation code | CONFORMS |
| No premature ModelPack | no import adapter (only assetId reference) | CONFORMS |
| No premature Persistence completion | RailWorldData in-memory only; R23 owns backend | CONFORMS |

## R10F F1-F6 regression

- F1 anchor datum: Foundation suite f1_* PASS (30/30).
- F2 RailPath: Foundation f2_* + golden PASS.
- F3 editing: Foundation f3_* PASS.
- F4 asset isolation: Foundation f4_* PASS (asset = look; gauge snapshot
  derived from asset).
- F5 lifecycle: Foundation f5_* PASS (confirm promotion + non-destructive
  cancel/clear preserved).
- F6 authority: Foundation f6_* PASS (server-authoritative wand untouched;
  production store represents the integrated-world side).
- R10SourceContractTest 29/29 PASS (exact promotion source guards intact).

## Verdict

R12 conformance: PASS. No R12 architecture item was redesigned or violated;
later-phase functionality was not started.
