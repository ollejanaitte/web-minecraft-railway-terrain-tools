# REQUIREMENT TRACEABILITY MATRIX — Phase 1-R12

Date: 2026-08-13 JST
Every R11 requirement is traced to an R12 architecture deliverable. Gate:
all P0 requirements must have a DESIGN ASSIGNED (no DESIGN UNASSIGNED).

Legend:
- Status: DESIGNED (frozen in R12) / PARTIAL (design exists, numeric deferred)
  / DEFERRED (Phase 2) / OPEN
- Phase: R13..R27 (see PHASE1_IMPLEMENTATION_ROADMAP.md)

## P0 (must be fully traced)

| Req | Title | R12 Architecture | Section | Status | Phase | Acceptance gate |
|-----|-------|------------------|---------|--------|-------|-----------------|
| REQ-P0-01 | Production Rail Data Model | PRODUCTION_RAIL_DATA_MODEL.md | §2-6 | DESIGNED | R13 (data), R23 (reload) | place->data record (R13); save->quit->reload identical (R23) |
| REQ-P0-02 | Rail Network | RAIL_NETWORK_ARCHITECTURE.md | §2-8 | DESIGNED | R16 | segments share endpoint; resolve continuous |
| REQ-P0-03 | Switch creation + route | SWITCH_JUNCTION_ANIMATION_ARCHITECTURE.md | §3-4 | DESIGNED | R17 | create turnout; route by input (in-session; reload restore at R23) |
| REQ-P0-04 | Switch animation | SWITCH_JUNCTION_ANIMATION_ARCHITECTURE.md | §5-7 | DESIGNED | R18 | route change animates; no snap |
| REQ-P0-05 | Production 3D Rail | PRODUCTION_3D_RAIL_ARCHITECTURE.md | §2-10 | DESIGNED | R14 | straight/curve/gradient/cant render with rail+ballast |
| REQ-P0-06 | ModelPack load contract | MODELPACK_COMPATIBILITY_ARCHITECTURE.md | §2-3 | DESIGNED | R15 | external pack loads; missing falls back |
| REQ-P0-07 | RTM ModelPack compatibility | MODELPACK_COMPATIBILITY_ARCHITECTURE.md | §1 | DESIGNED | R15 | RTM ModelRail JSON imports to Railsys asset |
| REQ-P0-08 | Confirmed Rail Editing | EDIT_DELETE_REPAIR_ARCHITECTURE.md | §2 | DESIGNED | R21 | select confirmed; edit; regenerate |
| REQ-P0-09 | Delete/Replace/Repair | EDIT_DELETE_REPAIR_ARCHITECTURE.md | §3 | DESIGNED | R22 | delete; connected rails handled; no crash |
| REQ-P0-10 | Persistence/Save/Reload | PERSISTENCE_ARCHITECTURE.md | §2-8 | DESIGNED | R23 | restart keeps rails+network+switch routes |
| REQ-P0-11 | Placement validation limits | VALIDATION_PERFORMANCE_ARCHITECTURE.md | §2.1 | DESIGNED (policy frozen; numeric defaults pending R13 measurement) | R13 | over-long/over-tall rejected |

P0 trace: 11/11 DESIGNED. No DESIGN UNASSIGNED.

## P1 (intentionally assigned)

| Req | Title | R12 Architecture | Status | Phase |
|-----|-------|------------------|--------|-------|
| REQ-P1-01 | Marker families + center/edge | PRODUCTION_RAIL_DATA_MODEL.md §4.2 | DESIGNED | R13 |
| REQ-P1-02 | Visual handle editing | EDIT_DELETE_REPAIR_ARCHITECTURE.md §2.3 (EXTENSIBLE) | DESIGNED | R21 |
| REQ-P1-03 | Cant profile (center/edge/random) | DATA_MODEL §2 (CC-1 EXTENSIBLE) | DESIGNED | R21 |
| REQ-P1-04 | Switch persistence/editing | SWITCH_JUNCTION_ANIMATION_ARCHITECTURE.md §9-10 | DESIGNED | R17/R18/R23 |
| REQ-P1-05 | Missing/changed/broken pack | PERSISTENCE_ARCHITECTURE.md §6 | DESIGNED | R15/R23 |
| REQ-P1-06 | Rail signal/occupancy + connection hooks | INFRASTRUCTURE_CONNECTOR_ARCHITECTURE.md §3 | DESIGNED | R19 |
| REQ-P1-07 | Culling/LOD | PRODUCTION_3D_RAIL_ARCHITECTURE.md §7 | DESIGNED (numeric R13) | R24 |
| REQ-P1-08 | Overlap/crossing + allowCrossing | VALIDATION_PERFORMANCE_ARCHITECTURE.md §2.2 | DESIGNED | R16 |
| REQ-P1-09 | Obstacle check | VALIDATION_PERFORMANCE_ARCHITECTURE.md §2.2 | DESIGNED | R21 |

## P2 / DEFER (assigned / Phase 2)

| Req | Title | R12 Architecture | Status | Phase |
|-----|-------|------------------|--------|-------|
| REQ-P2-01 | gauge-mismatch warning | VALIDATION §2.2 | DESIGNED | R24 |
| REQ-P2-02 | undo/redo | EDIT_DELETE_REPAIR §4 | DEFERRED (extensible) | Phase 2 |
| REQ-P2-03 | Signal model + block-section | INFRASTRUCTURE_CONNECTOR §5 | DEFERRED | Phase 2 |
| REQ-P2-04 | Detector + converters | INFRASTRUCTURE_CONNECTOR §7 | DEFERRED | Phase 2 |
| REQ-P2-05 | Crossing automation | INFRASTRUCTURE_CONNECTOR §6 | DEFERRED | Phase 2 |
| REQ-P2-06 | Connector models (Relay/DSS) | MODELPACK_COMPATIBILITY (accessory) | DEFERRED | Phase 2 |
| REQ-P2-07 | Trains/formations | VEHICLE_INTERFACE_CONTRACT.md | DEFERRED (interface frozen) | Phase 2 |
| REQ-P2-08 | ATS/ATC/timetable | INFRASTRUCTURE_CONNECTOR §7 | DEFERRED | Phase 2+ |
| REQ-P2-09 | mqo/obj offline converter | MODELPACK_COMPATIBILITY §1 (option C) | PARTIAL (tooling) | R15 |

## Trace summary

| Priority | Total | DESIGNED | DEFERRED/OPEN | Notes |
|----------|-------|----------|---------------|-------|
| P0 | 11 | 11 | 0 | gate satisfied |
| P1 | 9 | 9 | 0 | all assigned |
| P2/DEFER | 9 | 2 designed / 7 deferred | 7 Phase 2 | intent recorded |

Conclusion: P0 trace is complete; no P0 requirement lacks an architecture
home. R12 PASS condition on traceability is met.
