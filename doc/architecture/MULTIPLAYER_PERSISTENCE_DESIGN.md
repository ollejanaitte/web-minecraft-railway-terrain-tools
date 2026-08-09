# Multiplayer / Persistence Design

Design-only (Phase -1). Server-authoritative (P15, ADR-007).

---

## 1. Authority model

- Server owns RailNetwork, all train/formation/controller physics, signals,
  switches, crossings, catenary, persistence.
- Client is a read-only mirror: renders from server snapshots; sends only
  input (notch changes, ride actions, edit requests) via commands/packets.
- No client prediction in v2 v1.0 (simplicity); interpolation only.
  (Prediction may be added later behind ADR-007 extension point.)

---

## 2. Sync surface (S -> C)

### Full state (on join / snapshot)
- RailNetwork manifest: pieces (id, defId, start/end, geometry params
  needed to render rail blocks + preview), node adjacency (for editing UI).
- Signals, switches (state), stations, crossings, catenary (render data).
- Formations (trainId, car defs, direction) + current poses.

### Deltas (per tick / dirty)
- TrainState (door/light/panto/notch/signal/destination/announcement/
  custom buttons) - dirty flags, sent on change.
- TrainPose snapshots per car at a configured rate (default 2-4 ticks),
  with per-car position + yaw/pitch/roll for interpolation.
- Switch/signal/crossing state changes (immediate).
- Rail edit events (place/remove) to rebuild client rail render.

### Input (C -> S)
- Notch/reverser/brake/EB/horn/door/pantograph/lights/destination/
  announcements/custom buttons.
- Ride/enter-seat, coupling/uncoupling requests (validated server-side).
- Edit tool commands (place rail, switch, signal, etc.) - permission gated.

---

## 3. Packet design (pseudo)

```
RailwaySyncPacket { seq, type, payload }
type = JOIN_SNAPSHOT | RAIL_MANIFEST | PIECE_DELTA | TRAIN_STATE |
       TRAIN_POSE_BATCH | SIGNAL_STATE | SWITCH_STATE | EDIT_EVENT |
       CONTENT_MANIFEST | INPUT_ACK
```
- Pose batch: one packet per update for all cars of a formation (compact:
  x,y,z floats + yaw/pitch/roll bytes, quantized).
- Bandwidth budget (PERFORMANCE_DESIGN.md): e.g. 16 cars * 4Hz * 24B = 1.5 KB/s
  per train; cap total sync bytes/tick.

---

## 4. Interpolation (client)

- Each car keeps last two pose snapshots; render at
  `lerp(last, cur, alpha)`; yaw wrap handled; pitch/roll linear.
- DataWatcher v1 migration: v1 EntityRailVehicle uses DataWatcher fields
  (trainId, carIndex, ..., segmentId, progress) [R-I]; v2 replaces this
  with pose+state packets. During migration, the V1Adapter can still emit
  v1-style metadata for old clients; new path uses RailwaySync.

---

## 5. Persistence (ADR-008)

WorldRailData (WorldSavedData key `rail_system`, schema v2):
```
{
  "schemaVersion": 2,
  "pieces":   [RailPiece...],
  "nodes":    [RailNode...],
  "switches": [ {pieceId, state} ],
  "signals":  [Signal...],
  "stations":[Station...],
  "crossings":[CrossingGate...],
  "catenary": [WireSection...],
  "formations":[FormationRecord...],
  "objects": [ {x,y,z,defId,dataMap} ]
}
FormationRecord { formationId, defIds[], direction, leaderPathDistance,
                  pathEntries[], trainState }
```
- Arc tables recomputed on load (not stored).
- Content by defId; missing def -> dummy fallback.
- Chunk unload: trains/rails keep logical state; rail world blocks are
  chunk data; on chunk load, re-resolve pieces lazily.
- Long trains keep their path chunks loaded (chunkHold set) [R-I].

### v1 migration
- Read old `rail_system` graph (v1 NBT: nodes[], segments[] with
  curveData, switches, trainTargets, stations).
- Nodes -> RailNode; Segments -> RailPiece (straight/curve), SWITCH from
  switch maps; stations -> Station placeholders; trainTargets dropped into
  route plan when applicable.
- Old EntityRailVehicle (entity NBT) -> FormationRecord via V1Adapter
  (positions from segmentId/progress converted to metres via geometry).
- Old `FINAL_REPORT.md`, debug markers: not data; ignored.
- Schema version field; forward-compat: unknown fields preserved.

---

## 6. Missing-pack / version behavior

- World references defId; server checks ContentRegistry; absent -> dummy +
  log. Clients need the pack for models; manifest tells them; missing model
  shows dummy mesh (never crash).

---

## 7. Concurrency & determinism

- All server logic runs in the main game tick (TeaVM single thread);
  no async physics. Edit/preview jobs are chunked (like v1 WorldEdit jobs).
- Deterministic per tick order: inputs -> controller -> formation movement
  -> signals/occupancy -> sync build.
