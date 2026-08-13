# PHASE1_IMPLEMENTATION_ROADMAP — Phase 1-R12

Date: 2026-08-13 JST
Formal R13+ implementation order derived from the R12 architecture dependency
graph. Each phase defines objective / dependencies / input contracts / scope /
non-goals / entry+exit gates / validation / risk.

Dependency principle: geometry/data first, then appearance, then topology,
then operations, then persistence, then UX integration.

## Phase list

### R13 — Production Rail Data Model + Placement limits
- Objective: implement the R12-A data model (RailSegment/Endpoint/RailNode/
  Connection; railId at confirm, nodeId at connect, connectorId at attach) +
  validation limits (R12-J §2.1).
- Dependencies: R10F Foundation (frozen).
- Input contracts: PRODUCTION_RAIL_DATA_MODEL, VALIDATION_PERFORMANCE.
- Scope: data types, confirm promotion writes world data, min/max length/
  height guards, marker families CENTER/EDGE (REQ-P1-01).
- Non-goals: no network, no 3D rail, no persistence format freeze.
- Entry: R10F suite green. Exit: place->data record; over-long rejected.
- Validation: Foundation suite + new data-model tests + normal world.

### R14 — Production 3D Rail
- Objective: R12-D renderer (profile extrusion/segmented, frames, asset look).
- Dependencies: R13 (data), R10F F4.
- Input: PRODUCTION_3D_RAIL_ARCHITECTURE.
- Scope: rail profile, sleeper/ballast, frames, mesh cache, spacing measure.
- Exit: straight/curve/gradient/cant rails render; asset switch look-only.

### R15 — ModelPack / RTM Compatibility
- Objective: R12-E adapter + Railsys-native pack contract.
- Dependencies: R14 (renderer), R13 (asset registry).
- Input: MODELPACK_COMPATIBILITY_ARCHITECTURE.
- Scope: pack load API, import adapter (JSON->definition), optional mqo
  converter, fallback, error reporting, license surfacing. JS never executed.
- Non-goals: switch models, signal models (later).
- Exit: an RTM ModelRail JSON imports to a Railsys asset and renders.

### R16 — Rail Network / Continuous Connection
- Objective: R12-B topology (nodes, explicit connections, snap, traversal).
- Dependencies: R13, R10F F2.
- Input: RAIL_NETWORK_ARCHITECTURE.
- Scope: node/connection data, snap, resolveNext/traverse, spatial index,
  crossing/overlap validation (REQ-P1-08).
- Exit: two segments share a node; continuous resolution; crossing policy.

### R17 — Switch / Junction
- Objective: R12-C topology + route (Basic first).
- Dependencies: R16.
- Input: SWITCH_JUNCTION_ANIMATION_ARCHITECTURE.
- Scope: junction data, branch endpoints, route input (server value), derived
  committedRoute, vehicle resolveRoute.
- Exit: turnout created; route switches by input IN-SESSION. (Full reload
  restore of switch routes is gated at R23 where persistence lands; R17 uses
  in-memory restore only / scoped test double.)
- Entry/Exit note: "reload keeps route" belongs to R23, not R17.

### R18 — Switch Animation
- Objective: R12-C animation (tongue/movable parts, smoothstep, partial-tick).
- Dependencies: R17, R14 (renderer).
- Input: SWITCH_JUNCTION_ANIMATION_ARCHITECTURE §5-7, PRODUCTION_3D_RAIL §8.
- Scope: movable parts, animation state machine, render integration,
  committed-route traversal consistency.
- Exit: route change animates in-session; no snap; committedRoute guides
  vehicles. (Mid-animation RELOAD reset is deferred to R23 where persistence
  exists; R18 tests in-memory reset only.)

### R19 — Infrastructure Connector
- Objective: R12-H connector data + rail hooks + event catalogue.
- Dependencies: R13/R16 (stable ids + network), R10F F6.
- Input: INFRASTRUCTURE_CONNECTOR_ARCHITECTURE.
- Scope: connector data, signal/occupancy fields, attach UX, persistence data
  (in-memory; full reload restore at R23).
- Non-goals: signal/crossing LOGIC (Phase 2).
- Exit: attach a connector to a rail; delete invalidates (reload restore at
  R23).

### R20 — Signal/Crossing Rail-side Integration
- Objective: connector-driven rail-side interfaces for signal/crossing models.
- Dependencies: R19, R15.
- Scope: signal/crossing model attachment, protectedRouteRef, approach zone
  data, event dispatch stub. LOGIC stays Phase 2.
- Exit: signal/crossing models attach and read rail hooks.

### R21 — Confirmed Rail Editing
- Objective: R12-G transaction (select->edit->preview->validate->commit).
- Dependencies: R13, R16.
- Input: EDIT_DELETE_REPAIR_ARCHITECTURE.
- Scope: endpoint/direction/handle/curve/gradient/cant/asset edits; cant
  profile (REQ-P1-03); visual handles (REQ-P1-02); obstacle check (REQ-P1-09).
- Exit: edit confirmed rail; preview; commit; connection integrity kept.

### R22 — Delete / Replace / Repair
- Objective: R12-G §3.
- Dependencies: R21, R19.
- Scope: delete with dependency check, disconnect, asset/geometry replace,
  repair (orphans, missing asset, broken connection).
- Exit: delete/replace/repair safe; connectors handled.

### R23 — Persistence
- Objective: R12-F world store (versioned, save authoritative, derive rest).
- Dependencies: R13..R22 data all persisted.
- Input: PERSISTENCE_ARCHITECTURE.
- Scope: RailWorldData, schema+migration, restore pipeline, missing-pack
  fallback, mid-anim policy, full reload restore of network + switch routes
  (the reload gates deferred from R17/R19 now complete).
- Exit: save/quit/restart/reload restores network+switch routes.

### R24 — Performance / Large Network
- Objective: R12-J scaling (culling/LOD, caches, spatial index tuning).
- Dependencies: R14..R23.
- Scope: measure + tune spacing/LOD/chunk budgets, index choice, gauge-mismatch
  warning (REQ-P2-01).
- Exit: large network renders at target fps; metrics recorded.

### R25 — Complete Rail UX Integration
- Objective: unify placement/edit/switch/connector UX into the normal-world
  flow (START_WEB_MINECRAFT.sh -> placement -> ... -> save -> reload -> edit).
- Dependencies: R13..R24.
- Exit: the full Phase 1 user journey works end-to-end.

### R26 — Final Regression + R27 Phase 1 Acceptance
- Objective: full regression (Foundation suite + all R13-25 tests + golden),
  Normal World Acceptance, performance soak.
- Dependencies: R25.
- Exit: all Phase 1 goals met; Phase 1 complete verdict.

## Dependency graph (summary)

```
R13 ──> R14 ──> R15 ─┐
  │        │        │
  └─> R16 ─┼──> R17 ─> R18
         │ │        │
         │ └────────┴──> R19 ─> R20
         │
         └──────────────> R21 ─> R22
                              │
R19 ──────────────────────────┼
                              v
                           R23 ─> R24 ─> R25 ─> R26 ─> R27
```

## Non-goals across R13-R27

- No Phase 2 logic (signal block sections, detectors, crossing automation,
  trains, ATS/ATC, timetable) — deferred.
- No change to R10F FROZEN contracts without a Contract Change Proposal.
- No RTM renderer JS execution; no RTM asset bundling.

## Risks

- R15 mqo fidelity (mitigate: converter iteration + fallback).
- R17 Eaglercraft route input mapping (mitigate: server value abstraction).
- R24 scale on low-end web (mitigate: measure-first, LOD).
