# RTM <-> Railsys GAP ANALYSIS — Phase 1-R11

Date: 2026-08-13 JST
Purpose: compare RTM behaviour with Railsys R10F (frozen Foundation) and
assign a Railsys-side decision for each gap. Being different from RTM does NOT
make Railsys wrong.

Legend (gap): MATCH / DIFFERENT / PARTIAL / NOT IMPLEMENTED / UNKNOWN /
NOT APPLICABLE / VERSION-SPECIFIC.
Decision: KEEP RAILSYS / ADOPT BEHAVIOUR / ADAPT FOR RAILSYS / DEFER / REJECT /
NEEDS R12 DESIGN.

---

## A. Placement & editing

| RTM | Railsys R10F | Gap | Decision | Phase |
|-----|--------------|-----|----------|-------|
| Red/blue markers, center/edge | single POS1/POS2 pair | PARTIAL | ADAPT FOR RAILSYS (add NORMAL/JUNCTION + CENTER/EDGE) | R-P1 |
| Two facing markers + confirm | POS1/POS2 + auto-preview + confirm | MATCH | KEEP RAILSYS (F5) | frozen |
| Facing/direction contract | start=+POS1, end=-POS2 | MATCH | KEEP RAILSYS (F2) | frozen |
| Marker GUI numeric edit (post-confirm) | /railsys3 edits (pre-confirm only) | PARTIAL | ADAPT (confirmed-rail editing R-P0) | R-P0 |
| Wrench visual handles | numeric edits | PARTIAL | ADAPT (visual handles EXTENSIBLE) | R-P1 |
| Max length/height limits | none | NOT IMPLEMENTED | ADAPT (config max length/height) | R-P0 |
| Continuous placement / snap | no network | NOT IMPLEMENTED | ADOPT (Rail Network R-P0) | R-P0 |

## B. Geometry

| RTM | Railsys R10F | Gap | Decision | Phase |
|-----|--------------|-----|----------|-------|
| Smooth curve from handles | HorizontalBezierGeometry (Hermite->Bezier) | MATCH | KEEP RAILSYS (F2) | frozen |
| Gradient via endpoint height | gradient via anchor pitch/endpoint y | MATCH | KEEP RAILSYS (F2) | frozen |
| Cant (numeric, sign flips) | constant cant (right rail lower) | MATCH concept | KEEP RAILSYS (F3); EXTENSIBLE profile | frozen/P1 |
| Cant Center/Edge/Random | constant only | PARTIAL | ADAPT FOR RAILSYS (profile fields EXTENSIBLE) | R-P1 |
| 0.5m sampling | 1.0m default (configurable) | DIFFERENT | KEEP RAILSYS (spacing configurable) | frozen/P1 |
| Support-surface datum | F1 anchor y = support surface | MATCH | KEEP RAILSYS (F1) | frozen |

## C. Network / Switch

| RTM | Railsys R10F | Gap | Decision | Phase |
|-----|--------------|-----|----------|-------|
| RailPosition endpoint persisted | AnchorDefinition (not persisted in network) | PARTIAL | ADAPT (persistence contract R-P0) | R-P0 |
| RailMap per segment | RailPath/RailPiece | MATCH | KEEP RAILSYS (F2) | frozen |
| Connection via shared position + canConnect | RailConnection.validate | MATCH | KEEP RAILSYS (F2) | frozen |
| SwitchType 4 kinds | none | NOT IMPLEMENTED | ADOPT (switch types) | R-P0/P1 |
| Point root/main/branch | none | NOT IMPLEMENTED | ADOPT | R-P0/P1 |
| Route = redstone per tick | none | NOT IMPLEMENTED | ADAPT FOR RAILSYS (powered input mapping) | R-P0 |
| Route NOT persisted (derived) | n/a | DIFFERENT | ADOPT BEHAVIOUR (derive from input on load) | R12 |
| Switch animation ~80 ticks + tongues | none | NOT IMPLEMENTED | ADAPT (Railsys-specific visual) | R-P1 |

## D. ModelPack / Asset

| RTM | Railsys R10F | Gap | Decision | Phase |
|-----|--------------|-----|----------|-------|
| pack.json + ModelRail JSON | embedded prototype JSON | PARTIAL | ADAPT (pack load contract) | R-P0 |
| mqo + renderer scripts | procedural RailAsset | DIFFERENT | NEEDS R12 DESIGN (compat adapter B/C) | R12 |
| Per-asset renderer JS | declarative RailAssetDefinition | DIFFERENT | REJECT JS execution; ADAPT declarative | R12 |
| Implicit gauge | explicit gaugeM | DIFFERENT | KEEP RAILSYS explicit gauge | R12 revisit |
| Missing pack dummy/black | registry fallback | MATCH concept | KEEP RAILSYS + warning | R-P1 |

## E. Persistence

| RTM | Railsys R10F | Gap | Decision | Phase |
|-----|--------------|-----|----------|-------|
| Per-tile NBT endpoints+property | minimal WorldRailData restore | PARTIAL | ADAPT (full data contract) | R-P0 |
| Switch geometry + derived route | none | NOT IMPLEMENTED | ADOPT | R-P0/P1 |
| Formations WorldSavedData | none | NOT APPLICABLE (Phase 2) | DEFER | P2 |
| Version migration (fixRTM) | none | NOT IMPLEMENTED | ADAPT (schemaVersion) | R-P1 |

## F. Signal / Crossing / Connector

| RTM | Railsys R10F | Gap | Decision | Phase |
|-----|--------------|-----|----------|-------|
| Signal model blocks + wiring | none | NOT IMPLEMENTED | DEFER logic; R-P1 rail hooks | P2/P1 |
| Rail signal/occupancy (getSignal/isCollidedTrain) | none | NOT IMPLEMENTED | ADAPT (signalState/occupied data) | R-P1 |
| Crossing gate | none | NOT IMPLEMENTED | DEFER (needs detection) | P2 |
| Connection {x,y,z,type,wireName} | none | NOT IMPLEMENTED | ADAPT (connection data contract) | R-P1 |
| Connector models | none | NOT IMPLEMENTED | DEFER | P2 |

## G. Vehicle

| RTM | Railsys R10F | Gap | Decision | Phase |
|-----|--------------|-----|----------|-------|
| Nearest RailMap follow | F2 geometry sufficient | PARTIAL | KEEP RAILSYS F2 + add route resolution | R-P0/R12 |
| Formation persistence | none | NOT APPLICABLE | DEFER | P2 |

---

## Summary

| Gap class | Count | Primary actions |
|-----------|-------|-----------------|
| MATCH | 11 | keep frozen Foundation |
| PARTIAL | 10 | adapt/extend within frozen semantics |
| NOT IMPLEMENTED | 12 | new R-P0/R-P1 features |
| DIFFERENT | 5 | mostly KEEP RAILSYS or R12 design |
| NOT APPLICABLE / DEFER | 4 | Phase 2 |

Phase 1 completion requires the NOT IMPLEMENTED R-P0 items (network, switch,
confirmed editing, delete, persistence, limits) and the R-P1 rail-side data
hooks. RTM-difference alone never overrides a frozen contract.
