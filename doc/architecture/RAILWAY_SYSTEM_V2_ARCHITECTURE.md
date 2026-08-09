# Railway System v2 - Full Architecture

Status: DRAFT (Phase -1). Not yet implemented.
Inputs: Phase -2 RTM specification (root FINAL_REPORT.txt, commits 777b1a72/1448f2f4),
current /railsys source, Phase -2 confidence classes (CONFIRMED-RTM /
CONFIRMED-MODELPACK / REFERENCE-IMPLEMENTATION / INFERRED / UNKNOWN).

Goal: a railway platform for Web Minecraft / Eaglercraft (TeaVM, browser
single-thread, memory/bandwidth constrained) that can grow to RealTrainMod
parity without breaking down, and that preserves the existing /railsys
prototype (v1) during a staged migration.

Confidence of every claim about RTM behavior is inherited from the Phase -2
report and NOT upgraded here.

---

## 1. Design Principles (decisions with rationale)

| # | Principle | Decision | Rationale |
|---|-----------|----------|-----------|
| P1 | 1 block = 1 metre | YES | RTM is 1:1 [CONFIRMED-RTM]. Avoids unit conversion errors for 10-20m cars; vanilla AABB/rendering operate in block units. |
| P2 | Full-scale trains | YES | RTM parity requires 10/16/18/20m cars, trams, locomotives, freight. All design uses metres. |
| P3 | Separate TrainCar from Bogie | YES | Bogies are the track anchors; car body is derived. Matches RTM [R-I]. Enables realistic pose and spacing. |
| P4 | Body pose from front/rear bogies | YES | yaw/pitch/roll from bogie chord is the RTM method [R-I] and is continuous across piece boundaries. |
| P5 | Distance-based formation | YES | Leader advances path distance; followers at leaderDistance - k*carSpacing. Fixes the v1 follower teleport. |
| P6 | segment progress is NOT formation authority | YES | Keep progress [0,1] only as a per-piece lookup key; spacing/authority is metres. |
| P7 | Rail Geometry separated from Rail Network | YES | Geometry = shape of one piece; Network = connectivity/routing. RTM keeps piece geometry in RailMap and route implicit [R-I]. |
| P8 | Rails as piece-based geometry | YES | Each rail is a RailPiece owning a RailGeometry; block/collision/ballast derived from it. |
| P9 | Routing graph as separate layer | YES (conditional) | Keep a light graph (nodes/adjacency) for route search; pieces provide geometry. v1 RailGraph concept retained but re-scoped. |
| P10 | Arc length adopted | YES | progress<->metres must be accurate for spacing, speed, signals. Use per-piece lookup tables (cached, rebuilt on edit). |
| P11 | Vertical curve first-class | YES | Slope rails exist in RTM [R-I]; vertical Bezier for smooth gradient transitions. |
| P12 | Cant / roll first-class | YES | Per-sample roll; body lean; required for realistic curves. |
| P13 | Switch as real geometry | YES | Turnout is a RailPiece with branch geometry and state, not just "next segment id". |
| P14 | Content (ModelPack) decoupled from core | YES | Train/Rail/Signal definitions are data; core runs definitions. |
| P15 | Server authoritative | YES | All physics/state decided server-side. |
| P16 | Client = read-only mirror + interpolation | YES | Browser clients do not own rail math; entity interpolation for motion. |
| P17 | Save schema versioning | YES | NBT world-data schema v2 with migration from v1 "rail_system". |
| P18 | Web/Eaglercraft constraints built in | YES | Memory budget, single thread, no Nashorn -> restricted scripting, pre-converted models. |
| P19 | Legacy /railsys = prototype v1, staged migration | YES | Freeze v1, run v2 alongside via adapter, then deprecate. |
| P20 | No RTM proprietary assets | YES | Clean-room; original models/textures/sounds only. |

These are formalized in ADRs (doc/decisions/).

---

## 2. Scale / Coordinate / Unit Convention

- World: Minecraft block coordinates (double, metres). +X east, +Z south, +Y up.
- 1 block = 1 m. [CONFIRMED-RTM]
- Train local frame: origin at car body center; +Z forward (nose),
  +X right, +Y up. (RTM forward orientation: verify per model at Phase 3;
  forward axis is a Phase -1 open item -> default +Z, configurable per
  content definition.)
- Yaw: Minecraft convention, degrees, 0 = +Z, clockwise positive when viewed
  from above; wrap [-180,180).
- Pitch: +up positive, degrees; wrap [-180,180).
- Roll: positive = right-side down (lean into curve), degrees, unbounded but
  small in practice.
- Distances: metres (double). Speeds: blocks/tick (m/tick); convenience
  conversions: 1 m/tick = 20 m/s = 72 km/h (i.e. km/h / 72 = blocks/tick,
  matching RTM's SignalLevel speed formula [R-I]).
- Accelerations: blocks/tick^2; RTM uses "km/h per s * 0.0006944" [C-MP].
- Precision: doubles for position; floats for per-frame pose; epsilon
  EPS = 1e-6 m for chord/direction guards; progress tolerance 1e-4.
- Interpolation: linear position + slerp-like yaw wrap + linear pitch/roll
  over partial ticks (see RENDERING_RUNTIME_DESIGN.md).
- Vehicle sizing design targets: 10m tram ~ trainDistance 5; 16m ~8;
  18m ~9; 20m ~10; bogie spacing ~ car length*0.7; width 2.75m class.

---

## 3. System Overview

Modules (server S / client C / both B):

```
RailwaySystem (B)
  rail.geometry      RailGeometry, RailLine(Straight/Bezier/Vertical/Switch), ArcLengthTable
  rail.network       RailNetwork, RailNode, RailConnection, RailRoute (light graph, S)
  rail.world         RailPiece, RailBlockEntity (world blocks + collision + ballast), S+C
  rail.path          RailPath, PathPosition (route history + distance resolver), S
  train              TrainFormation, TrainCar, Bogie, Coupler, TrainPose, TrainController, S
  train.state        TrainState (door/light/panto/notch/direction/...) synced S->C
  signal             Signal, SignalAspect, SignalBlock, SignalWire, SignalConverter, S
  station            Station, Platform, StopPoint, Timetable(optional ext), S
  crossing           CrossingGate, S
  catenary           WireSection, Insulator, Connector, PantographContact, S
  objects            InstalledObject (machine/ornament/container/...), S+C
  content            ContentRegistry, TrainDefinition, RailDefinition,
                     SignalDefinition, MachineDefinition, WireDefinition,
                     ObjectDefinition, SoundDefinition, AnimationDefinition,
                     ScriptDefinition, pack loader, S+C (models C)
  model              ModelLoader (v2 native + import), Mesh, Material, S+C
  texture            TextureManager, rollsign, lights, cache, C
  sound              SoundEngine (spatial, loop, event), C (triggers S)
  script             RestrictedScriptEngine, S/C
  animation          AnimationState, PartTransform, C (+ S state)
  render             TrainRenderer, RailRenderer, SignalRenderer, WireRenderer,
                     ObjectRenderer, debug overlay, C
  net                RailwaySync, Snapshot/Delta packets, C+S
  persist            WorldRailData (schema v2), FormationStore, migration, S
  perf               SpatialIndex, ActiveRailCache, update budget, S/C
  legacy             V1Adapter (freeze + translate old data/commands), S+C
```

See doc/diagrams/architecture.txt for the ASCII diagram.

---

## 4. Core Data Ownership

- RailNetwork: owns pieces registry, adjacency, routes, switches, signals,
  stations, crossing, catenary refs (server). Single source of truth.
- RailPiece: owns geometry + world block set + collision + state (switch).
- TrainFormation: owns ordered TrainCars + coupling + leader path position.
- TrainCar: owns definition ref + front/rear Bogie + pose output.
- TrainController: owns notch/reverser/speed; reads signals ahead.
- SignalBlock: owns occupancy interval + aspect + linked hardware.
- ContentDefinition: immutable content; shared by all instances.
- Client mirrors: read-only copies for rendering; never authoritative.

---

## 5. Key Data Flows

### Movement (server, per tick)
1. Formation front car: TrainController computes speed (notch physics +
   gradient + signal limit).
2. Leader advances pathDistance += dir * speed * dt; PathPosition resolves
   (piece, localMetres); on piece end, route/switch selects next piece and
   appends path entry (continuous distance).
3. For each car k: targetDistance = leaderDistance - k*carSpacing; walk
   backward across path entries to (piece, localMetres); set front bogie.
4. Rear bogie placed at bogieSpan behind front bogie.
5. Body pose = function(frontBogie, rearBogie) (see TRAIN_FORMATION_BOGIE_DESIGN.md).
6. Signals/occupancy updated by head/tail positions; released by tail.

### Rendering (client)
- Server sends TrainState + pose snapshots (interpolated) and rail/world
  block state; client interpolates and renders from definitions.

### Content
- pack.json + Model<Type>_*.json -> ContentRegistry; definitions referenced
  by id in world data; missing definition -> fallback dummy.

### Persistence
- WorldRailData NBT v2: {version, pieces[], switches[], signals[], trains/
  formations[], stations[], crossings[], catenary[]}; migrate from v1
  "rail_system".

---

## 6. Module Responsibilities (summary)

See dedicated docs:
- doc/architecture/DATA_MODEL.md
- doc/architecture/RAIL_GEOMETRY_DESIGN.md
- doc/architecture/TRAIN_FORMATION_BOGIE_DESIGN.md
- doc/architecture/CONTENT_MODELPACK_DESIGN.md
- doc/architecture/MULTIPLAYER_PERSISTENCE_DESIGN.md
- doc/architecture/RENDERING_RUNTIME_DESIGN.md
- doc/architecture/PERFORMANCE_DESIGN.md
- doc/migrations/MIGRATION_PLAN.md
- doc/roadmaps/IMPLEMENTATION_ROADMAP.md
- doc/testing/ACCEPTANCE_TEST_STRATEGY.md

---

## 7. KEEP / MIGRATE / REMOVE (summary)

Full matrix in FINAL_REPORT.txt section 10 and doc/migrations/MIGRATION_PLAN.md.

- KEEP (concept): RailNode adjacency, station node flag concept, switch
  target concept (becomes piece state), testloop as regression course,
  debug marker rendering, RailWand 4-point Bezier creation (becomes a v2
  placement tool), WorldSavedData pattern.
- MIGRATE/REWRITE: RailGraph (geometry+route mixed -> split), RailSegment
  (progress -> arc-length geometry), RailCurveData (raw t -> arc-length
  reparameterized), EntityRailVehicle (movement+formation+sync -> delegate
  to v2 Train/Formation; becomes adapter), CommandRailSystem (test builders
  -> v2 editor commands), occupancy (bool per segment -> distance intervals
  with tail release), route (greedy -> A*/path with switch), station (node
  flag -> Station+StopPoint), DataWatcher sync (-> dedicated RailwaySync),
  RailSystemManager (static graph -> per-world v2 store).
- REMOVE/DEPRECATE (as sole mechanism): segment-progress formation,
  follower-on-leader recompute, trainId/carIndex world-scan management,
  debug-marker-based rail visuals (replaced by RailPiece blocks),
  global static graph mirror, entire v1 EntityRailVehicle movement.

---

## 8. Web/Eaglercraft Constraints (design drivers)

- TeaVM single-threaded, no threads/reflection; keep per-tick work bounded.
- Memory: avoid huge arc tables; store per-piece lookup (split ~ len*32,
  capped; see RAIL_GEOMETRY_DESIGN.md); cache computed pose samples.
- No Nashorn: scripting is a restricted, allow-listed expression/script
  system (see CONTENT_MODELPACK_DESIGN.md).
- OBJ/MQO runtime parse is costly: use pre-converted v2 native format for
  delivery; OBJ/MQO import is an offline tool (see CONTENT_MODELPACK_DESIGN.md).
- Rendering: WebGL via Eaglercraft; emissive via unlit materials;
  transparency via alpha blend; LOD + culling budgets defined in
  PERFORMANCE_DESIGN.md.
- Networking: bandwidth budget; delta + snapshot; interpolation.

---

## 9. Open Items (deferred to Phase 0/1 or user decision)

- Forward axis of content models (+Z default; per-definition config).
- Bogie as logical anchor vs light entity (ADR-004; recommendation: logical
  anchors with a thin render/seat entity, not physics entities).
- Script scope (ADR-006; recommendation: restricted, no full JS).
- v2 native model format details (ADR-010).
- Whether ATO/timetable is core or external (design as extension hooks).
