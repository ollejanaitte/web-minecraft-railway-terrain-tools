# Legacy /railsys (v1) Migration Plan

Design-only (Phase -1). Goal: never break v1 functionality abruptly;
run v2 alongside v1 via an adapter; deprecate in controlled phases
(ADR-009).

---

## 1. Strategy overview

1. FREEZE v1: current /railsys + EntityRailVehicle become "legacy"
   (no new features; bugfixes only if trivial).
2. BUILD v2 core under new packages (no dependency on v1).
3. ADAPTER: an EntityRailVehicle subclass (or parallel entity) that reads
   v2 TrainFormation pose/state and renders via v2; old /railsys commands
   translate to v2 API for the same operations.
4. DATA MIGRATION: world saved data converted v1->v2 (see below).
5. COMMAND MIGRATION: /railsys kept as an alias layer; new /railsys v2
   (or /rtm-like) commands added; v1-only subcommands (testline/testcurve/
   testloop) moved to a "debug/course" namespace.
6. DEPRECATE: after soak + parity, v1 entity + old code paths removed.

---

## 2. Migration matrix

| Existing (v1)             | Current role                    | v2 Target                  | Action        | Phase |
|---------------------------|---------------------------------|----------------------------|---------------|-------|
| RailGraph                 | graph (nodes+segments+switch+route+occ+station) | RailNetwork (routing layer) | MIGRATE       | 1 |
| RailNode                  | immutable point + adjacency     | RailNode (piece adjacency) | MIGRATE       | 1 |
| RailSegment               | segment + progress + raw-t curve| RailPiece + RailGeometry   | REWRITE       | 1 |
| RailCurveData             | raw Bezier control              | Bezier geometry (arc-len)  | REWRITE       | 1 |
| RailSegmentType           | enum (SLOPE/SWITCH unused)      | geometry kinds             | MIGRATE       | 1 |
| RailPosition (v1, unused) | value object                    | PathPosition/rail pos      | REMOVE/REPLACE| 1 |
| RailSystemManager         | static global graph mirror      | per-world v2 store         | REWRITE       | 1 |
| RailSystemSavedData       | WorldSavedData rail_system      | WorldRailData schema v2    | MIGRATE       | 1 |
| EntityRailVehicle         | movement+formation+sync+render  | TrainFormation + adapter   | REWRITE       | 2 |
| CommandRailSystem         | test builders + ops             | v2 editor + alias          | MIGRATE       | 3/4 |
| ItemRailWand              | 4-point Bezier creation         | v2 placement tool          | MIGRATE       | 3 |
| RenderRailVehicle         | cuboid + coupler line           | v2 TrainRenderer           | REWRITE       | 9 |
| testloop/testline/testcurve| debug builders                 | v2 regression course       | KEEP/MIGRATE  | 0/2 |
| occupancy (segment bool)  | collision avoidance (leader only)| SignalBlock intervals, tail release | REWRITE | 5 |
| route (greedy)            | nearest-node routing            | RailRoute (A*/plan)        | REWRITE       | 5 |
| switch (target segment)   | node->segment map               | RailPiece switch state     | MIGRATE       | 3 |
| station (node flag)       | flag + dwell                    | Station + StopPoint        | MIGRATE       | 6 |
| trainId/carIndex/progress | formation via fields            | formation order + metres   | REMOVE(role)  | 2 |
| DataWatcher sync          | 9 fields                        | RailwaySync pose+state     | MIGRATE       | 10 |
| WorldSavedData            | graph only                      | schema v2 + migration      | MIGRATE       | 1 |
| debug markers (blocks)    | rail visual                     | RailPiece blocks + overlay | REPLACE       | 3 |
| client spawn (type 80)    | S0EPacketSpawnObject            | RailwaySync spawn          | MIGRATE       | 10 |

---

## 3. Data migration v1 -> v2

- Read old `rail_system` NBT: nodes, segments (type + curveData + length),
  switchTargetSegmentByNodeId, trainTargetNodeByTrainId, stations.
- Build RailNode list; for each segment create a RailPiece:
  - STRAIGHT: Straight geometry from start/end nodes.
  - CURVE: horizontal Bezier from RailCurveData (6 control coords) -> recompute
    arc length.
- Convert switches (node->segment) to SWITCH piece state where a node has 2+
  outgoing; otherwise keep as route hint.
- Stations -> Station records (node ref, default dwell).
- TrainTargets -> initial route plan hint (recomputed).
- Old EntityRailVehicle NBT (entity storage): convert segmentId/progress ->
  PathPosition (metres via geometry), direction -> v2 direction; create
  FormationRecord with default defIds (placeholder) and dummy model until
  content available.
- Schema version 1 present -> upgrade to 2; unknown keys preserved.
- Keep old key as `rail_system_v1_backup` until deprecation confirms.

---

## 4. Command migration

- Keep `/railsys` working through Phase 3-4 via V1Adapter (translates to v2
  calls). Add v2 commands: `/railsys place`, `/railsys switch <state>`,
  `/railsys route <...>`, `/railsys stop`, etc. (exact set at Phase 3/4).
- testline/testcurve/testloop -> `/railsys debug ...` + used as regression
  course in Phase 0/2 (KEEP for tests).
- RailWand -> v2 placement tool (still 4-point Bezier + straight + switch).

---

## 5. Deprecation schedule

- Phase 0-1: v1 untouched; tests capture v1 behavior as baseline.
- Phase 2: adapter in place; v2 formation runtime; v1 entity still usable.
- Phase 5-9: commands migrate; v1-only features (route/station/switch)
  aliased to v2.
- Phase 10: v1 DataWatcher sync replaced; old entity deprecated.
- Phase 11: remove v1 EntityRailVehicle movement/CommandRailSystem test
  builders; keep debug course via v2.
- Exit: no runtime path uses v1 logic; save data fully v2.

Rollback: because v2 adds (not replaces) and adapter preserves v1 behavior,
rollback = disable v2 flags. Data migration writes backup key.
