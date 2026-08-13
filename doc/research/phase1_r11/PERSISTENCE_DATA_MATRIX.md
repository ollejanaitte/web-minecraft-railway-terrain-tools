# PERSISTENCE_DATA_MATRIX — Phase 1-R11

Date: 2026-08-13 JST
Purpose: catalogue what RTM persists per rail/switch/marker/connector (from
NBT keys in SRC-1/SRC-9) and map each to a Railsys Phase 1 persistence data
requirement. R12 will freeze the Railsys data contract.

Legend: RTM key (as observed in NBT/public fields); Railsys mapping = proposed
Phase 1 data field (Railsys-native, clean-room).

## Per-endpoint (RailPosition)

| RTM NBT/field | Meaning | Railsys mapping (proposal) |
|---------------|---------|---------------------------|
| `BlockPos` X/Y/Z | block coords | `blockPos {x,y,z}` (record, for chunk tie) |
| `Direction` (byte) | 8-way direction | `yawDeg` (double, Railsys convention) |
| `Height` (byte, 1/16) | support height | `y` (double, support surface; F1) |
| `A_Direction` | anchor yaw | `anchorYawDeg` |
| `A_Pitch` | anchor pitch | `anchorPitchDeg` |
| `A_Length` | anchor length H | `lengthH_m` (Railsys DEFAULT_HANDLE) |
| `A_LenV` | anchor length V | `lengthV_m` (reserved) |
| `C_Center` | cant center | `cantCenterDeg` (EXTENSIBLE profile) |
| `C_Edge` | cant edge | `cantEdgeDeg` (EXTENSIBLE) |
| `C_Random` | cant random | `cantRandomDeg` (EXTENSIBLE) |
| `SwitchType` (byte) | marker type | `markerType` (NORMAL/JUNCTION/...) |
| `posX/Y/Z` (double) | precise world position | `x,y,z` (double, canonical; F1) |

## Per-rail (TileEntityLargeRailCore / RailProperty)

| RTM NBT/field | Meaning | Railsys mapping (proposal) |
|---------------|---------|---------------------------|
| `RP0..RPn` | endpoint set | `endpoints[]` |
| `railModel` (RailProperty) | asset id | `assetId` (RailAssetDefinition) |
| `block` + `blockMetadata` | roadbed block | `ballastBlockId` + `ballastMeta` |
| `blockHeight` | roadbed height | `ballastHeightM` |
| `Signal` (getSignal/setSignal int) | signal value | `signalState` (R-P1/E) |
| `isCollidedTrain` | occupancy flag | `occupied` (R-P1/E) |

## Per-switch (TileEntityLargeRailSwitchCore)

| RTM NBT/field | Meaning | Railsys mapping (proposal) |
|---------------|---------|---------------------------|
| `Size` | count of points | `pointCount` |
| `RP0..RPn` | switch endpoints | `points[]` |
| `fixRTMRailMapVersion` | migration version | `schemaVersion` |
| (route not stored) | route derived from redstone | `routeState` derived from power input (R-P0) |

## Per-marker (TileEntityMarker)

| RTM NBT/field | Meaning | Railsys mapping (proposal) |
|---------------|---------|---------------------------|
| `RP` | marker RailPosition | `anchor` (AnchorDefinition) |
| `MarkerState` | display/height mode | `displayMode`, `heightMode` |
| `StartX/Y/Z` | core-marker start | (Railsys marker persistence R-P1) |

## Per-connector (electric Connection)

| RTM NBT/field | Meaning | Railsys mapping (proposal) |
|---------------|---------|---------------------------|
| `isRoot` | root flag | `isRoot` |
| `x/y/z` | connected block | `targetPos` |
| `type` (ConnectionType) | connection kind | `connectorType` |
| `wireName` | wire id | `wireId` |

## Railsys Phase 1 persistence contract (candidate, R12 freeze)

1. World-bound rail store: `RailData { schemaVersion, rails[] }` where each
   rail = `{ id, assetId, ballast, endpoints[], signalState?, occupied? }`.
2. Endpoint = AnchorDefinition + markerType + blockPos (derivable) — reuse the
   frozen F1/F2 anchor semantics (no conversion layer needed).
3. Switch = `{ points[], routeInput }`; route derived at load (RTM-style).
4. Connectors/infrastructure references by `targetPos` (position-based, like
   RTM) or by `railId` (Railsys choice) — R12 decision.
5. Missing asset -> fallback + warning (Railsys RailAssetRegistry fallback).
6. Versioned schema + migration hook (RTM fixRTMRailMapVersion analogue).

## Persistence scenario matrix

| Scenario | RTM | Railsys Phase 1 target |
|----------|-----|------------------------|
| Save world | tile NBT | Railsys WorldRailData (extend) |
| Quit/restart | NBT survives | survive via world data |
| World reload | rebuild RailMap | rebuild RailPath from endpoints |
| Asset restore | railModel lookup; missing->dummy/black | assetId lookup; missing->fallback+warning |
| Gauge restore | implicit in model | assetId.gaugeM |
| Cant restore | C_Center/Edge/Random | cant profile fields |
| Connection restore | shared position + canConnect | network connect on load |
| Switch restore | NBT + route from power | points + routeInput |
| Switch animation | not stored | not stored (re-derive) |
| Chunk load/unload | tile lifecycle | world-bound restore per world |
| Multiplayer | server packs | server-authoritative world data (F6) |
| Migration | fixRTMRailMapVersion | schemaVersion |
