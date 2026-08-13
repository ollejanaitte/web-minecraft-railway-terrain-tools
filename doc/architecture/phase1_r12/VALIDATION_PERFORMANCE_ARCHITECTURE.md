# R12-J: Validation / Performance Architecture

Phase 1-R12 design freeze. Inputs: REQ-P0-11 (validation limits),
REQ-P1-07/08/09 (culling, overlap, obstacle), R11-F (edge cases), R10F F2
guards, R11-C/D (limits from RTM config).

## 1. Goals

- Production implementation is protected by validation at every write point.
- Scale strategy defined; numeric limits "measure to confirm" in R13, NOT
  guessed and frozen now.

## 2. Validation (frozen policy)

### 2.1 Guards (frozen, from F2 + R10F)

- min length: geometry rejects < EPS (F2). Production min rail length:
  additive config `minRailLengthM` (default to F2 EPS guard; R13 confirms).
- max length: `maxRailLengthM` (RTM evidence default 64, max 256 — R13
  confirm Eaglercraft-safe value; recommend 64 default).
- max height delta / gradient: `maxHeightDeltaM` (RTM evidence default 8;
  R13 confirm).
- cant range: controller already [-45,45] (F3); keep.
- NaN/zero guard: RailMath guards (F2) — keep.

### 2.2 Structural validation (frozen rules)

| Check | Rule |
|-------|------|
| overlap | overlapping parallel rails allowed only with allowCrossing (REQ-P1-08) |
| crossing | crossing without connection gated by allowCrossing |
| connection tolerance | F2 POSITION_TOLERANCE_M (1e-4) + ANGLE_TOLERANCE_DEG (0.5) |
| gauge mismatch | warning (REQ-P2-01) + optional block (policy) |
| obstacle | optional black-frame check (REQ-P1-09) |
| invalid topology | node/segment consistency (R12-B §8) |
| invalid switch | degenerate branch / missing tongue -> reject at creation |
| missing asset | fallback + warning (REQ-P1-05) |
| missing connector | INVALID flag + repair (R12-G) |
| orphan id | prune on repair |

### 2.3 When validation runs

- At CONFIRM (placement), at EDIT COMMIT (R12-G), at LOAD (R12-F repair), and
  on manual /repair. Validation is a pure function over the data model.

## 3. Performance (frozen policy + R13-measure)

### 3.1 Caches (REPLACEABLE implementations)

- Mesh cache (R12-D): keyed (railId, assetVersion, rebuildId); chunked.
- Asset cache: RailAssetRegistry (already exists).
- ModelPack cache: parsed definitions (R12-E).
- Spatial index: nodeId/segment/endpoint lookup (R12-B) — hash grid or tree.
- Network lookup: node adjacency (R12-B).
- Frame cache: per-rail sampled frames (keyed by spacing) — optional.

### 3.2 Scaling strategy

- Culling by distance (REQ-P1-07); render chunk grouping.
- LOD: full / mid / far (R12-D).
- Rebuild invalidation: edit/asset/route-change bumps rebuildId.
- Switch dynamic mesh: only movable parts re-mesh per animation frame
  (R12-C/D).
- Connector lookup: by connectorId + targetRailId index.
- Large network: chunked mesh + spatial index + lazy frame sampling.

### 3.3 Numeric limits to MEASURE in R13 (not frozen now)

- spacing (0.5 vs 1.0 m), LOD distances, chunk size, cache budget, sample
  count per switch animation, max segments per node.
- These are recorded as OPEN with R13 owner (see OPEN_QUESTIONS).

## 4. Requirement trace

- REQ-P0-11: §2.1.
- REQ-P1-07: culling/LOD §3.
- REQ-P1-08: overlap/crossing §2.2.
- REQ-P1-09: obstacle §2.2.
- R10F F2/F3: existing guards retained.

## 5. Open questions

- Eaglercraft-safe max length/height (R13 benchmark).
- Spatial index choice (R13).
- Cache budgets (R13).
