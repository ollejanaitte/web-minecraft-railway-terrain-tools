# Rendering / Runtime Design

Design-only (Phase -1). Client-only responsibility; never mutates server
state.

---

## 1. Renderers

### TrainRenderer (per car)
- Uses TrainPose + TrainState + TrainDefinition.
- Body model (native mesh, part transforms), bogies (front/rear poses),
  wheels (rotation from wheelRotationSpeed), doors (state transforms),
  pantograph (up/down), rollsign/type sign, lights (emissive overlays:
  light0/light1/light2), interior lights.
- Interpolated pose between server snapshots; yaw wrap; pitch/roll lerp.

### BogieRenderer
- Renders bogie models at front/rear bogie world poses (rotation around Y
  + wheel spin). Logical anchors provide pose; no bogie entity.

### RailRenderer
- Rail pieces are world blocks: rail core + ballast blocks + collision
  (RailPiece.worldBlocks) rendered as normal blocks; rail cap/top model via
  definition; preview overlay before placement (reuse RenderGlobal pattern).
- Switch pieces render branch geometry per state.

### SignalRenderer / WireRenderer / ObjectRenderer
- Signals: model + lights (emissive) + rotateBody.
- Wires: quad/line strips between connectors with sag (points from
  WireSection).
- Objects (machine/ornament/container/...): model + part animations +
  DataMap state.

### Debug overlay
- Extend RenderGlobal debug: rail samples, path distance, signals,
  formation spacing, occupancy intervals.

---

## 2. Model / texture cache

- ModelCache: keyed by content hash (defId + model file hash); holds GPU
  buffers (positions/uv/normals/indices) per part.
- TextureCache: WebGL textures keyed by path; LRU budget (see
  PERFORMANCE_DESIGN.md); mipmaps optional.
- Interpolation data cached per car (last two snapshots).

---

## 3. Rendering flags

- transparency: AlphaBlend material -> blend enable for that pass.
- emissive: Light material -> unlit additive pass (head/tail/interior).
- culling: doCulling per definition; smoothing: normals computed at import.
- LOD: distance-based (near/far/billboard-dummy), plus culling by
  renderAABB [C-MP].
- Lighting: standard Eaglercraft lighting for opaque; emissive unlit.
- State interpolation for part transforms (door/pantograph lerp).

---

## 4. Per-frame pipeline

1. Collect visible cars/pieces/signals/objects (frustum + distance).
2. Resolve interpolated poses/state.
3. Sort transparent after opaque.
4. Emissive pass last.
5. Debug overlay.

---

## 5. Constraints

- Single-threaded; per-frame vertex upload budget; reuse pooled buffers.
- Avoid per-frame JSON parse; content parsed once at load.
- Renderer never changes authoritative state.
