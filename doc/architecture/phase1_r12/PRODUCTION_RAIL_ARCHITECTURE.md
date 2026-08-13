# Phase 1-R12 Production Rail Architecture — Master Document

Date: 2026-08-13 JST
Status: DESIGN FREEZE (Phase 1-R12)
Scope: Railsys Production Rail Architecture HOW. Design only — no production
implementation. R10F Foundation Contract is preserved verbatim.

## 0. Phase boundary

- R10F = Foundation Contract (F1-F6 frozen).
- R11  = WHAT / WHY (RTM behaviour, gap, requirements).
- **R12  = HOW / DESIGN FREEZE (this document set).**
- R13+ = PRODUCTION IMPLEMENTATION (roadmap in PHASE1_IMPLEMENTATION_ROADMAP.md).

## 1. Design inputs (R10F + R11)

### 1.1 Frozen Foundation (R10F) — must not be broken

- F1 Coordinate / Support-Surface Anchor (AnchorDefinition SSoT; +1 only at
  selectOnFace UP boundary; no renderer datum compensation).
- F2 Production Geometry / RailPath (fromMarkers; direction contract
  start=+POS1, end=-POS2; arc length; sampling; frame; cant rolls frame not
  centerline; continuity; preview/confirm identity).
- F3 Editing Semantics (anchor position invariance; edit ranges; influence
  matrix).
- F4 Rail Asset / Geometry Isolation (asset = look; geometry = RailPath).
- F5 Placement Lifecycle (auto preview; confirm promotion; cancel/clear
  non-destructive; delete separate).
- F6 Client / Server Authority (server-authoritative world/inventory;
  client-local placement UI).

### 1.2 R11 Requirements (input trace basis)

- P0: REQ-P0-01..11 (data, network, switch+route, switch animation, 3D rail,
  ModelPack load, RTM compat, confirmed editing, delete/replace/repair,
  persistence, validation limits).
- P1: REQ-P1-01..09.
- P2/DEFER: signal logic, detectors, crossing automation, connector models,
  trains, ATS/ATC, undo/redo etc.

Full trace: `REQUIREMENT_TRACEABILITY_MATRIX.md`.

## 2. Architecture overview

The Production Railsys is designed as clean layers, preserving the frozen
Foundation contracts:

```
            ┌────────────────────────────────────────────────────┐
            │  R12-G  Editing / Delete / Repair  (operations)   │
            ├────────────────────────────────────────────────────┤
            │  R12-H Infrastructure Connector (signal/crossing) │
            ├────────────────────────────────────────────────────┤
            │  R12-B Rail Network (topology, stable identity)   │
            ├────────────────────────────────────────────────────┤
            │  R12-A Production Rail Data Model (authoritative) │
            ├────────────────────────────────────────────────────┤
            │  F1/F2 Geometry Core  (RailPath, frames, cant)    │  ← frozen
            └────────────────────────────────────────────────────┘
              R12-D 3D Rail render  ·  R12-E ModelPack / import
              R12-F Persistence     ·  R12-I Vehicle interface
              R12-J Validation/Performance
```

Authoritative vs derived:
- **Authoritative data**: stable IDs, segment endpoints (AnchorDefinition
  semantics), connections, junctions/routes, asset references, metadata.
- **Derived data**: RailPath geometry (rebuilt from endpoints on load), local
  frames, samples, mesh.
- **Cache**: frames, tables, mesh, spatial index (rebuildable).

## 3. Stable identity (cross-cutting decision)

- `railId` (issued @confirm), `nodeId` (@first connection), `connectorId`
  (@infrastructure attach). Transient placement has no stable id.
- IDs are opaque, non-reused after delete (delete marks retired; a new rail
  gets a new id). Monotonic per world.
- All persistence references use IDs, NOT coordinates (position is derived).

## 4. Core data model sketch

```text
RailWorldData (world-bound, persisted)
  schemaVersion
  rails[]        RailSegment | Junction
  nodes[]        RailNode (explicit membership, R12-A §4.3)
  connections[]  RailConnection (pairwise bookkeeping)
  connectors[]   InfrastructureConnector

RailSegment
  id: railId
  kind: NORMAL | SLOPE | CURVE | ...
  endpoints: [RailEndpoint, RailEndpoint]
  assetId + assetVersion
  gaugeM (from asset, denormalized for validation)
  cant: CantProfile
  geometry params (handles etc.)
  signalState?, occupied?  (R-P1 hooks)

RailEndpoint
  anchor: AnchorDefinition (x/y/z/yaw/pitch/lengthH/lengthV)  ← F1/F2 semantics
  markerType: NORMAL | JUNCTION; placement: CENTER | EDGE
  nodeId (may be null for free end)

Junction (R12-C)
  id: railId
  rootEndpoint + branch endpoints
  points[] (route branches)
  routeInput (powered source)
  routeInput (powered source)
  committedRoute (derived)

RailConnection
  nodeId
  endpointA (railId+side), endpointB (railId+side)
  created at snap/connect
```

## 5. Subsystem responsibilities (pointer)

| Subsystem | Doc |
|-----------|-----|
| Data model + identity + lifecycle | PRODUCTION_RAIL_DATA_MODEL.md |
| Network topology + snap | RAIL_NETWORK_ARCHITECTURE.md |
| Switch/junction/route/animation states | SWITCH_JUNCTION_ANIMATION_ARCHITECTURE.md |
| 3D rail mesh/render | PRODUCTION_3D_RAIL_ARCHITECTURE.md |
| ModelPack + RTM compat | MODELPACK_COMPATIBILITY_ARCHITECTURE.md |
| Persistence/save/reload | PERSISTENCE_ARCHITECTURE.md |
| Edit/delete/repair | EDIT_DELETE_REPAIR_ARCHITECTURE.md |
| Infrastructure connector | INFRASTRUCTURE_CONNECTOR_ARCHITECTURE.md |
| Vehicle interface | VEHICLE_INTERFACE_CONTRACT.md |
| Validation/performance | VALIDATION_PERFORMANCE_ARCHITECTURE.md |
| Freeze classification | DESIGN_FREEZE_CLASSIFICATION.md |
| Contract decisions | R10F_CONTRACT_CHANGE_DECISIONS.md |
| Open items | OPEN_QUESTIONS.md |
| Roadmap | PHASE1_IMPLEMENTATION_ROADMAP.md |

## 6. Design decisions summary (frozen at R12)

1. Connection is EXPLICIT production data (not just coordinate proximity) —
   REQ-P0-02.
2. Route state is derived from a powered input (RTM-style) and NOT persisted;
   switch geometry + input source persist. Mid-animation state is not saved —
   REQ-P0-03/04, R11-B.
3. Stable IDs at their own lifecycle events (railId@confirm, nodeId@connect,
   connectorId@attach); position is derived — REQ-P0-01.
4. ModelPack compatibility = runtime import adapter (B) with optional offline
   converter (C); RTM renderer JS is NEVER executed at runtime — REQ-P0-07,
   R11-C2.
5. Persistence is world-global (not chunk-local), schema-versioned; rails
   rebuilt from endpoints on load — REQ-P0-10.
6. Rail data carries signal/occupancy hooks (signalState, occupied) and a
   connection data model; full signal/crossing LOGIC is Phase 2 — REQ-P1-06,
   R11-E.
7. Geometry remains F1/F2 RailPath as the single source; 3D rail mesh is
   derived per-frame from RailLocalFrame — REQ-P0-05.
8. Placement validation limits (length/height) are additive; defaults from
   R11 evidence (length default 64, max 256; height default 8, max 256) with
   "measure to confirm" note — REQ-P0-11.
