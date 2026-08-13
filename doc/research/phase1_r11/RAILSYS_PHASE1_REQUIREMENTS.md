# RAILSYS PHASE 1 REQUIREMENTS — Phase 1-R11

Date: 2026-08-13 JST
Derived from the R11 RTM study (see `RTM_RAILSYS_GAP_ANALYSIS.md` for source
rows). These are WHAT/WHY requirements. HOW (design freeze) is R12.

Priority: P0 (Phase 1 completion essential) / P1 (strongly needed) / P2 (nice
to have) / DEFER (Phase 2+).

Each requirement: ID / Title / Source Evidence / RTM Behaviour / Railsys
Current State / User Value / Priority / Dependency / R10F Impact / R12 Design
Question / Acceptance Concept.

---

## P0 requirements

### REQ-P0-01: Production Rail Data Model

- Source: SRC-1 (RailPosition), SRC-9 (NBT keys), Gap E/A.
- RTM: per-endpoint RailPosition (block+double+height+direction+anchors+
  switchType) + RailProperty (asset/ballast) persisted.
- Railsys now: AnchorDefinition + RailsysPlacementState (transient); no
  persisted rail data contract.
- User value: confirmed rails are real, restorable world data.
- Dependency: none.
- R10F impact: NONE (additive; reuses F1/F2 anchor semantics).
- R12 question: native data schema (per-endpoint vs per-rail vs per-network).
- Acceptance: place -> save -> quit -> reload -> rail restored identically.

### REQ-P0-02: Rail Network (connection / snap / continuous placement)

- Source: Gap C, R11-B.
- RTM: segments connect via shared positions + canConnect.
- Railsys now: RailNetwork minimal (addPiece/connect/disconnect) + RailPath.
- User value: continuous connected track, flush joins, multi-rail layout.
- Dependency: REQ-P0-01.
- R10F impact: NONE (extends RailNetwork; F2 continuity kept).
- R12 question: network identity (position vs id), snap tolerance.
- Acceptance: two adjacent segments share an endpoint; resolve continuous.

### REQ-P0-03: Switch (junction) creation + route state

- Source: Gap C, R11-B (SwitchType 4 kinds, Point, checkRSInput).
- RTM: blue marker root + red endpoints -> switch; redstone route per tick;
  route derived on load.
- Railsys now: none.
- User value: 分岐 (branching) — Phase 1 goal.
- Dependency: REQ-P0-02.
- R10F impact: none (new type; F2 piece params reserved).
- R12 question: Eaglercraft power input (redstone vs server), switch types to
  ship (Basic first).
- Acceptance: create turnout; switch route by input; reload keeps effective
  route.

### REQ-P0-04: Switch animation

- Source: R11-B 4 (MAX_COUNT 80, tongue offset+yaw, partial-tick).
- RTM: ~4s animated tongue movement; renderer interpolation.
- Railsys now: none.
- User value: visible, believable switching.
- Dependency: REQ-P0-03.
- R10F impact: none (renderer-only; F4 asset=look).
- R12 question: Railsys animation design (tongue transform, timing) — NOT RTM
  constant copy.
- Acceptance: route change animates tongues; no snap.

### REQ-P0-05: Production 3D Rail rendering

- Source: R11-C (0.5m samples, parts base/railL/railR/sleeper/ballast).
- RTM: short 3D model per sample with rail profile + ballast.
- Railsys now: procedural segment boxes (RailAsset).
- User value: believable rail appearance.
- Dependency: none (R10F F4 already separates look).
- R10F impact: none (F4 kept; renderer is REPLACEABLE C2/C3).
- R12 question: rail profile definition (head/web/foot), spacing default.
- Acceptance: straight/curve/gradient/cant rails render with rail+ballast.

### REQ-P0-06: Production ModelPack load contract

- Source: R11-C 3 (pack.json + ModelRail JSON), R10F RailModelPackParser.
- RTM: pack.json + mods/RTM/ModelRail_*.json -> registry.
- Railsys now: embedded prototype JSON -> RailAssetRegistry.
- User value: external packs add assets without code change.
- Dependency: REQ-P0-05.
- R10F impact: none (REPLACEABLE loader C4; F4 semantic boundary kept).
- R12 question: pack schema v2 + load API + validation.
- Acceptance: external Railsys pack loads; missing asset falls back.

### REQ-P0-07: RTM ModelPack compatibility strategy

- Source: R11-C2 / MODELPACK_COMPATIBILITY_MATRIX.
- RTM: packs with mqo + renderer scripts.
- Railsys now: n/a.
- User value: reuse existing RTM rail packs.
- Dependency: REQ-P0-06 (adapter target).
- R10F impact: none (new; never executes RTM JS).
- R12 question: adopt adapter (B) + optional converter (C); JS scripts rejected.
- Acceptance: an RTM ModelRail JSON imports to a Railsys asset definition.

### REQ-P0-08: Confirmed Rail Editing

- Source: R11-D 1 (marker GUI + wrench post-confirm).
- RTM: edit existing rails via markers/wrench; regenerate.
- Railsys now: pre-confirm edits only.
- User value: fix/adjust already-placed rails (Phase 1 goal).
- Dependency: REQ-P0-01.
- R10F impact: none (additive; F3 anchor invariance kept).
- R12 question: selection UX + edit transaction + regeneration.
- Acceptance: select confirmed rail, change endpoint/curve/gradient/cant/
  asset, regenerate.

### REQ-P0-09: Delete / Replace / Repair

- Source: R11-D 2 (breakRail, RailProperty swap).
- RTM: break rail blocks; swap model.
- Railsys now: no delete API (F5 delete is separate by contract).
- User value: remove/replace rails safely (Phase 1 goal).
- Dependency: REQ-P0-01, REQ-P0-02.
- R10F impact: none (delete is a separate operation per F5).
- R12 question: deletion semantics with connected rails/infrastructure.
- Acceptance: delete a confirmed rail; connected rails handled; no crash.

### REQ-P0-10: Persistence / Save / Reload

- Source: R11-D 3, PERSISTENCE_DATA_MATRIX.
- RTM: tile-entity NBT; rails survive reload.
- Railsys now: minimal WorldRailData restore.
- User value: rails survive restart (Phase 1 goal).
- Dependency: REQ-P0-01, REQ-P0-03.
- R10F impact: none (additive data contract).
- R12 question: native schema + versioning + migration.
- Acceptance: save/quit/restart/reload keeps rails + network + switch routes.

### REQ-P0-11: Placement validation limits

- Source: R11-F (railGeneratingDistance default 64 max 256, height 8/256).
- RTM: max length/height config.
- Railsys now: none.
- User value: prevents pathological/invalid rails.
- Dependency: none.
- R10F impact: none (additive validation).
- R12 question: Railsys defaults + obstacle semantics.
- Acceptance: over-long/over-tall placement rejected with clear feedback.

## P1 requirements

### REQ-P1-01: Marker families + center/edge placement

- Source: R11-A A1 (red/blue, center/edge).
- Railsys: add NORMAL/JUNCTION + CENTER/EDGE to marker selection.

### REQ-P1-02: Visual handle editing

- Source: R11-A A5 (green handles).
- Railsys: handle-based visual edit (EXTENSIBLE; numeric stays).

### REQ-P1-03: Cant profile (center/edge/random)

- Source: R11-A A6, R11-D (C_Center/Edge/Random).
- Railsys: EXTENSIBLE CantProfile.

### REQ-P1-04: Switch persistence + switch editing/deletion

- Source: R11-B/D.
- Railsys: persist switch geometry + derived route; edit/delete switches.

### REQ-P1-05: Missing/changed/broken ModelPack handling

- Source: R11-D/F (dummy/black; migration).
- Railsys: fallback + warning + versioned schema + migration.

### REQ-P1-06: Rail signal/occupancy + connection data hooks

- Source: R11-E (getSignal/isCollidedTrain; Connection).
- Railsys: signalState/occupied in rail data; connection data model; stable
  rail id.

### REQ-P1-07: Culling / render distance / LOD

- Source: R11-C (ModelConfig doCulling/renderDistance).

### REQ-P1-08: Duplicate/overlap/intersection validation + allowCrossing

- Source: R11-F.

### REQ-P1-09: Obstacle check (optional)

- Source: R11-A A7 (black frame).

## P2 / DEFER

| ID | Title | Priority | Note |
|----|-------|----------|------|
| REQ-P2-01 | gauge-mismatch warning | P2 | Railsys explicit gauge |
| REQ-P2-02 | undo/redo | P2 | edit transaction later |
| REQ-P2-03 | Signal model rendering + block-section logic | DEFER | Phase 2 (needs detection/trains) |
| REQ-P2-04 | Train detector + signal converters | DEFER | Phase 2 |
| REQ-P2-05 | Crossing gate automation | DEFER | Phase 2 |
| REQ-P2-06 | Connector models (Relay/DSS) | DEFER | Phase 2 |
| REQ-P2-07 | Trains / formations / vehicle simulation | DEFER | Phase 2 |
| REQ-P2-08 | ATS/ATC / timetable / full operations | DEFER | Phase 2+ |
| REQ-P2-09 | mqo/obj import offline converter | DEFER-ish | part of REQ-P0-07 R12 |

## P0/P1/DEFER summary

- P0: 11 requirements (data, network, switch, switch animation, 3D rail,
  ModelPack load, RTM compat, confirmed editing, delete, persistence,
  validation).
- P1: 9 requirements.
- P2/DEFER: signal/crossing/connector/vehicle logic -> Phase 2, with R-P1 rail
  data hooks in Phase 1.

## R12 freeze candidates (from R11)

1. Railsys rail data schema + persistence contract (REQ-P0-01/10).
2. Network identity (position vs id) + snap semantics (REQ-P0-02).
3. Switch data model + route-input mapping (REQ-P0-03).
4. ModelPack v2 schema + load API (REQ-P0-06).
5. RTM ModelPack compatibility adapter (REQ-P0-07).
6. Rail renderer profile (REQ-P0-05) + culling (REQ-P1-07).
7. Connection/infrastructure data model (REQ-P1-06).
