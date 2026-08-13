# R11-F: RTM Vehicle Interface / Error / Limit / Edge Cases

Phase 1-R11 deliverable. Evidence: SRC-1 (RTM jar), SRC-5 (車両), SRC-6
(train JSON), SRC-9.

Legend: OBSERVED / KNOWN / INFERRED / UNKNOWN.

---

## 1. Vehicle Interface (train on rails)

### 1.1 How RTM trains use the rail network

- Trains are `EntityTrainBase` + bogies; a `Formation` (id + FormationEntry[])
  links cars into a train; `FormationData` is a WorldSavedData (persists
  formations).
- Each tick the train resolves the NEAREST rail map: `SwitchType.getRailMap(
  Entity)` / `TileEntityLargeRailCore.getRailMap(Entity)`. Switch tiles return
  the branch-specific map by proximity to the switch point; diamond/scissors
  return nearest of both routes.
- Trains follow RailMap samples (position, yaw, height, pitch). Bogies use
  bogieModel2/3 + bogiePos; cars spaced by trainDistance.

Evidence: SRC-1 (Formation/FormationData/EntityTrainBase/EntityBogie),
SRC-5 (車両), SRC-6 (train JSON). Class: KNOWN.

### 1.2 Rail-side interface the vehicle needs

| Rail capability | RTM usage | Railsys F2 capability |
|-----------------|-----------|-----------------------|
| Centerline position | RailMap.getRailPos | PathSample.sample (x,y,z) — MATCH |
| Tangent / direction | getRailRotation (yaw) | PathSample.sample.tx/ty/tz + yaw — MATCH |
| Local frame (forward/right/up) | derived in renderer/vehicle | RailLocalFrame — MATCH (Railsys richer) |
| Gauge | implicit HALF_GAUGE | RailAssetDefinition.gaugeM — MATCH |
| Cant/roll | via rail pitch/height; vehicle lean (rolling coeff) | frame.rollDeg — MATCH |
| Gradient | getRailPitch/getRailHeight | sample.pitchDeg / y — MATCH |
| Segment connection | RailMap chain via shared positions | RailPath entries + continuity — MATCH |
| Route/switch | getRailMap(Entity) branch selection | switch route API (R-P0, R11-B) — NOT YET |
| Endpoint / end-of-track | run out of rail map -> stop | resolve clamp; end sample — PARTIAL |
| Forward/reverse | setTrainDirection | PathSample travel direction — MATCH |
| Occupancy | isCollidedTrain / signal | R-P1 data hook |
| Events | onGetElectricity etc | R-P1 event hooks |
| Route selection | nearest point/rail map | switch API — R-P0 |

### 1.3 Conclusion

Railsys F2 geometry already provides everything a vehicle needs for position/
tangent/frame/gauge/cant/gradient/forward-reverse. Missing for Phase 2
vehicles: switch route resolution (R-P0), occupancy/signal hooks (R-P1),
persisted rail network (R-P0). Railsys RailLocalFrame is richer than RTM's
yaw-only renderer rotation, which is an advantage.

## 2. Error / Limit / Edge Cases

| Case | RTM evidence | Class | Railsys implication |
|------|--------------|-------|---------------------|
| Very short rail | length min not documented; RailMap zero-length guarded | UNKNOWN | Railsys rejects < EPS (F2) — keep |
| Sharp curve | no hard limit documented; handle-driven | UNKNOWN | Railsys angle-continuity 0.5deg join check |
| Steep gradient | railGeneratingHeight max (default 8, max 256) | KNOWN | Railsys R-P0 max height delta |
| Excessive cant | no range found; negative reverses | UNKNOWN | Railsys R-P1 cant range ([-45,45] controller already) |
| Duplicate rail | overlapping same-position rail? UNKNOWN | UNKNOWN | Railsys R-P1 duplicate detection |
| Rail intersection | crossings via DiamondCross / crossing blocks; block overlap check canPlaceRail | KNOWN/INFERRED | Railsys R-P1 crossing/overlap validation |
| Disconnected rail | canConnect; broken endpoints orphan | KNOWN/INFERRED | Railsys R-P0 network integrity |
| Near-miss snap | not documented | UNKNOWN | Railsys R-P0 snap tolerance |
| Gauge mismatch | no check (content issue) | KNOWN | Railsys explicit gauge: validation option R-P2 |
| Missing ModelPack | dummy config; black rails reported | KNOWN | Railsys fallback + warning (R-P1) |
| Broken ModelPack | load failure / dummy | INFERRED | Railsys R-P1 graceful skip + report |
| Incompatible ModelPack | schema drift | INFERRED | Railsys R-P1 versioned schema |
| Save data mismatch | version migration (fixRTMRailMapVersion) | KNOWN | Railsys R-P1 schemaVersion migration |
| Switch inconsistent state | route derived each tick; no stored state to corrupt | KNOWN | Railsys R-P0 derived route (matches RTM) |
| Missing infrastructure reference | Connection.isAvailable(world) checks target | KNOWN (SRC-1) | Railsys R-P1 isAvailable check |
| Missing signal/crossing target | position-based; may dangle | INFERRED | Railsys R-P1 reference validation |
| Chunk/world lifecycle | onChunkUnload; tile restore | KNOWN | Railsys world-bound restore (R-P0) |

## 3. Railsys R10F edge-case status

- Very short: rejected (F2). Non-finite: rejected. Continuity: checked.
- No length/height/obstacle limits. No duplicate/intersection/gauge-mismatch
  checks. No missing-pack version handling beyond registry fallback.
- No network integrity / switch state / infrastructure reference checks.

## 4. Requirement Candidates

- R-P0: placement validation limits (max length, max height delta; optional
  obstacle check) — from RTM GeneratingDistance/Height.
- R-P1: duplicate/overlap/intersection validation; crossing support flag
  (allowCrossing).
- R-P1: snap tolerance for network connection.
- R-P1: missing/broken/incompatible ModelPack graceful handling + versioned
  schema + migration.
- R-P1: infrastructure reference availability check (Connection.isAvailable
  analogue).
- R-P2: gauge-mismatch warning.

## 5. Open Questions

- RTM zero/min-length behaviour UNKNOWN (Railsys keeps its own guard).
- Sharp-curve / cant limits UNKNOWN (Railsys defines its own).
- Exact RTM end-of-track vehicle behaviour (stop vs derail) UNKNOWN.
- Chunk-boundary rail placement constraints UNKNOWN.
