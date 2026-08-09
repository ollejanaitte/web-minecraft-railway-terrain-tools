# Railway System v2 - Data Model

Design-only (Phase -1). Pseudo-schemas; no Java implementation.

Units: metres / blocks, degrees, blocks/tick (m/tick), blocks/tick^2.
All position values are world doubles unless marked "local".

---

## 1. Identifiers

- `pieceId` : global monotonic id for each RailPiece (network-wide).
- `nodeId`  : graph node id (routing layer).
- `trainId` : persistent formation id (per world).
- `carIndex`: 0-based index within formation.
- `defId`   : content definition id (e.g. "train:my_e231").
- Versioned ids: content refs stored as strings to survive pack changes.

---

## 2. Rail

### RailNode (routing layer)
```
RailNode {
  nodeId: int
  x,y,z: double            // exact metre position
  connectedPieceIds: [int] // adjacency (pieces, not nodes)
}
```

### RailConnection (edge)
```
RailConnection {
  pieceId: int
  startNodeId: int
  endNodeId: int
}
```
(Conceptually the graph edge = a piece. v1 RailNode.connectedNodeIds is
replaced by piece-based adjacency.)

### RailGeometry (pure shape, immutable)
```
RailGeometry {
  kind: STRAIGHT | CURVE | VERTICAL_CURVE | SWITCH | TURNTABLE
  start: RailPositionLike   // x,y,z + heading(yaw,pitch)
  end:   RailPositionLike
  controlPoints: [ {x,y,z} ... ]   // for Bezier (horizontal) + vertical
  arcLength: double                // total metres (3D)
  table: ArcLengthTable            // progress<->metres lookup
  branches: [RailBranch ...]       // SWITCH only; each branch = sub-geometry
  cantProfile: CantProfile         // roll per metre (optional)
  gradientProfile: GradientProfile
  sample(distanceM) -> RailSample  // x,y,z,yaw,pitch,roll,curvature,...
  nearest(x,z) -> RailSample       // for train spawn/attract
}
```
See RAIL_GEOMETRY_DESIGN.md for RailSample fields.

### RailPiece (world representation)
```
RailPiece {
  pieceId: int
  defId: string                   // RailDefinition
  geometry: RailGeometry
  nodeId: startNodeId, endNodeId  // routing refs
  worldBlocks: [ {x,y,z, block, meta, surfaceOffset16} ] // rail + ballast + collision
  corePos: (x,y,z)                // owning block entity
  switchState: bool | null        // SWITCH only
  signalRefs: [signalId]          // occupancy/detection linkage
  stateVersion: int               // for sync dirty tracking
}
```

### RailPath / PathPosition (movement)
```
PathEntry { pieceId, dir(+1/-1), startM, endM }  // continuous metres
RailPath  { entries: [PathEntry] }                 // route history/plan
PathPosition {
  pathDistance: double            // global metres along the path
  resolve() -> { pieceId, dir, localM, progress } // derived
}
```
- Formation stores `leaderPathDistance` + the resolved path.
- follower k: `targetDistance = leaderPathDistance - k*carSpacing`;
  resolve backward across entries.

---

## 3. Train

### TrainDefinition (content, immutable) - see CONTENT_MODELPACK_DESIGN.md
Subset of fields used by runtime:
```
TrainDefinition {
  defId, trainType(EC/DC/EL/DL/SL/CC/TC/N)
  halfLength: double        // trainDistance (m)
  bogiePositions: [[x,y,z],[x,y,z]]  // front/rear offsets
  size: [width,height]      // AABB
  seatSlots: [[x,y,z,type]] // metres
  playerPos: [[x,y,z]...]   // driver seats
  performance: {
    maxSpeeds: [1..5], accelerateions, deccelerations[9],
    emergency: decel, rolling, rollCoeffs, pantoPos, wheelRotation
  }
  lights: head/tail/interior (Light[])
  rollsign, doors, pantograph (parts), sounds, scripts, model refs
}
```

### TrainFormation (runtime)
```
TrainFormation {
  formationId: int
  cars: [TrainCar]            // ordered, front..rear
  leader: TrainCar            // carIndex 0
  controller: TrainController
  path: RailPath
  leaderPathDistance: double
  direction: +1 / -1
  speed: double               // m/tick (shared, control-car set)
  chunkHold: [chunkCoord]     // keep loaded for long trains
}
```

### TrainCar (runtime)
```
TrainCar {
  carIndex: int
  def: TrainDefinition
  frontBogie: Bogie
  rearBogie: Bogie
  pose: TrainPose             // derived each tick
  trainState: TrainState      // door/light/panto/etc (per formation, some per car)
  couplerFront / couplerRear: Coupler
}
```

### Bogie (logical anchor; ADR-004)
```
Bogie {
  car: TrainCar
  isFront: bool
  pathPos: PathPosition       // resolved to (pieceId, dir, localM)
  yaw, pitch, roll: double    // tangent at anchor
  rotation: double            // wheel rotation (visual)
}
```

### Coupler
```
Coupler { gapMetres: double }   // coupling gap = halfLenA + halfLenB
```
Distance between car centers = sum of half-lengths + gap.

### TrainPose (output)
```
TrainPose { x,y,z, yaw, pitch, roll }   // body, derived from bogies
```

### TrainState (synced)
```
TrainState {
  notch: int(-8..5), reverser(front/center/back)
  doors: close/open_left/open_right/open_all
  lights: off/head/head_tail; interior: off/on/rainbow
  pantograph: down/up (front/back)
  destination: int, announcement: int
  signal: int(aspect level), directionInFormation: int
  doorSideOverride, customButtons: [bool]
}
```

### TrainController (physics)
```
TrainController {
  notch, reverser, brakeNotch, emergency
  speed, targetSpeed, brakeCount   // brake ramp (notch*-18 tick ramp) [R-I]
  computeAccel(decel, gradient, signalLimit, def) -> speedDelta
}
```
Physics in core; performance numbers in definition; server-script hooks
(useVariableAcceleration/Deceleration [C-MP]) via restricted script.

---

## 4. Signal / Operation

```
SignalAspect { level: int(1..6), label, speedLimit(m/tick) }  // 6=proceed..1=stop [R-I]
SignalBlock {
  blockId, pieceId, intervalM: [startM,endM]
  owner: trainId | null     // released by tail
  aspect: SignalAspect
  linkedSignals: [signalId], linkedSwitches: [pieceId]
}
Signal { signalId, defId, pos, aspect, wireIn/out, stateVersion }
SignalConverter { type(NOT/AND/OR/...), inputs, output }  // logic layer [R-I]
```

## Station
```
Station {
  stationId, name, nodeRef/pieceRef, platforms: [Platform]
  StopPoint { pieceId, localM, doorsSide(left/right/both), toleranceM }
  dwellTicks, announcements: [annId]
}
Timetable(ext) { trips, stopTimes }   // external subsystem hooks
```

## Crossing / Catenary
```
CrossingGate { pieceId, gates, lights, sound, state(open/closing/closed), linkedSignal }
WireSection { id, fromConnector, toConnector, defId, sag, points[] }
Insulator / Connector { type(relay/input/output), wirePos, linked }
```

---

## 5. Persistence (WorldRailData v2)

```
world_rail_data {                    // replaces/extends "rail_system"
  schemaVersion: 2
  pieces: [RailPiece]
  nodes: [RailNode]
  switches: [ {pieceId,state} ]
  signals: [Signal]
  stations: [Station]
  crossings: [CrossingGate]
  catenary: [WireSection]
  formations: [FormationRecord]       // cars + defIds + leaderDistance + path (resumable)
  objectStates: [ {pos, defId, dataMap} ]
}
FormationRecord { formationId, defIds:[...], direction, leaderPathDistance,
                  pathEntries:[...], trainState }
```
- Geometry/arc tables recomputed on load (not stored) to avoid drift.
- Content referenced by defId string; missing def -> fallback dummy.
- v1 migration: read old "rail_system" graph; convert nodes->nodes,
  segments->RailPiece(straight/curve from RailCurveData), switch/route/
  station maps -> v2 state. Entities (EntityRailVehicle) migrated via
  V1Adapter until deprecated.

## 6. Networking mirror (client)
Client keeps read-only: pieces (for rail render), TrainPose snapshots +
TrainState, signal aspects, switch states, object states. No client-side
physics authority. See MULTIPLAYER_PERSISTENCE_DESIGN.md.
