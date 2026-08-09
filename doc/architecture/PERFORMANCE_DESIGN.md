# Performance / Large World Design

Design-only (Phase -1). Browser (TeaVM) memory + single-thread budget
drives the design (P18).

---

## 1. Budgets (targets, to be measured)

- Tick budget: railway update <= 25% of a 20 TPS server tick (<= 12.5 ms)
  at "large" scale; async/offload not available (single thread) -> keep
  work chunked.
- Render budget: trains + rails + signals within normal WebGL budget;
  LOD + culling to keep draw calls bounded.
- Memory: per-piece arc table capped (split ~ len*32, max 384 => ~1.6 KB
  floats/piece); cache pose samples for active pieces only.
- Bandwidth: sum of sync packets <= 8 KB/s per client budget
  (pose batches + deltas); rail manifest sent once on join.

---

## 2. Large network strategy

- SpatialIndex: grid/quad-tree over pieces (cell ~ 64-128 m). Query for
  trains, collision, signals, chunk activation.
- ActiveRailCache: only pieces near active trains/editors are sampled/
  resolved each tick; LRU eviction.
- Inactive pieces: no per-tick cost; rail world blocks render as normal
  chunks; geometry lazily built (arc table on first touch).
- Trains keep path chunks loaded (chunkHold set) for long formations [R-I].

---

## 3. Update frequency

- Movement: 20 TPS server.
- Pose sync: 2-4 Hz per formation (configurable); state deltas on change.
- Signal/switch updates: on change only.
- Editor jobs: chunked (e.g. 4096 blocks/tick, mirroring v1 WorldEdit).

---

## 4. Entity count

- Logical bogies = no entities (ADR-004): a 16-car train = 1 light entity
  per car (16) instead of 48 (RTM-style bogie entities). Cuts tracking/
  packet cost.
- Seats/rider handling via the car entity.

---

## 5. Model / content memory

- Native meshes (rv2m) loaded once, cached by hash; no per-instance copy.
- Texture budget: LRU with size cap (e.g. 256 MB-ish configurable; scale
  down mipmaps).
- Converted content only; no OBJ/MQO parse at runtime.

---

## 6. Soak/scale gates

- 100 km route (thousands of pieces), 8-16 trains, 16-car formations:
  tick stays in budget; no unbounded memory growth (ARC table LRU).
- Save/load at scale completes in bounded time.
- Client FPS maintained with many pieces/trains in view (LOD).

See doc/testing/ACCEPTANCE_TEST_STRATEGY.md for measurable gates.
