# Known Failures Registry (Phase 0)

Recorded, NOT fixed. Fixes belong to later phases per the v2 architecture.
Each entry: ID, behavior, reproduction, expected v2 behavior, owner phase,
status.

| ID | Area | Current v1 behavior | Reproduction | Expected v2 behavior | Owner | Status |
|----|------|---------------------|--------------|------------------------|-------|--------|
| BL-TRAIN-001 | formation | follower teleports/wraps ahead of leader at segment boundary | leader crosses node; follower progress wraps 0.x->~1.0 on new segment | follower stays on previous piece at leaderDistance-k*spacing, no wrap | Phase 2 | KNOWN FAILURE (pinned by KnownFailureDocumentationTest) |
| BL-TRAIN-002 | formation | curve spacing drift (raw-t not arc length) | multi-car train on Bezier | arc-length spacing | Phase 2 | KNOWN FAILURE |
| BL-TRAIN-003 | pose | yaw snaps at segment end (look-ahead clamps) | vehicle near piece end | pose from bogie chord, continuous | Phase 2 | KNOWN FAILURE |
| BL-TRAIN-004 | identity | train ID collision after restart (static nextTrainId) | restart with NBT-loaded trains | persistent formation ids | Phase 2 | KNOWN FAILURE (inferred) |
| BL-NET-001 | multiplayer | no rail-graph sync to clients; static shared only in single-JVM | 2 clients: client cannot resolve rail | server-authoritative sync | Phase 10 | KNOWN FAILURE |
| BL-ROUTE-001 | routing | greedy nearest-node route can oscillate between nodes | loop layout, route set | A*/plan route with history | Phase 5 | KNOWN FAILURE |
| BL-OCC-001 | signaling | occupancy is whole-segment boolean, leader-only; tail not tracked | long train, second train behind | interval occupancy, release by tail | Phase 5 | KNOWN FAILURE |
| BL-STATIC-001 | architecture | RailSystemManager static global graph mirror (multi-world) | two worlds / integrated server | per-world v2 store | Phase 1 | KNOWN FAILURE (design debt) |
| BL-TRACK-001 | multiplayer | tracker update freq 3 ticks; coarse interpolation for rail motion | remote client observing train | higher-rate pose batches | Phase 10 | KNOWN FAILURE (perf/quality) |
| BL-CMD-002 | commands | /railsys clear kills ALL rail vehicles in world | two players' trains | scoped to user/target | Phase 3 | KNOWN FAILURE (safety) |

## Policy
- These are baseline facts; they do NOT block Phase 1.
- Phase 2/5/10 gates enable the corresponding @Disabled harness tests.
- New failures found during Phase 0 testing must be added here.
