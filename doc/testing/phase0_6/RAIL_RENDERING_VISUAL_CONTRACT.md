# Rail Rendering & Visual Contract (Phase 0.6 - PART C, frozen)

Status: FROZEN for Phase 1. No product code changed.

## 1. Minimum visible rails (Phase 1 acceptance)
Phase 1 must render rails that are **clearly visible as a railway in a real
game screenshot**:
- Left rail
- Right rail
- Sleepers (ties)
- Optional: validation stone bed (blocks) on the Flat Validation World
- Curve continuity, gradient continuity, piece-boundary continuity visible
  (no gap / no visible break)

"Geometry internal only" = FAIL (per Phase 0.6 rule 38).

## 2. Renderer strategy (hybrid, option R-C)
- A `RailRenderer` draws, from geometry samples, distinct left/right rails
  + sleepers as flat-color geometry (no model assets). Rail gauge ~1.5 m
  (approx), rail height ~0.15 m above bed, sleepers every ~0.7 m.
- The Flat Validation World also carries a block stone bed (from the
  validation course) for world presence + a readable rail look.
- Not in Phase 1: RTM-quality models, textures, pantograph, emissive
  materials (Phase 7+).

## 3. Geometry -> render data
- Sample step for rendering: ~0.5 m (or per 0.25 m near high curvature).
- Rails = two offset curves from the centerline sample: offset = gauge/2
  along the yaw basis (perpendicular horizontal). Y = sample.y + rail height.
- Sleepers = short cross pieces at each sample step (every ~0.7 m).
- Curve resolution: more samples where curvature high (reuse ArcLengthTable
  split or fixed 0.25 m step on curves).
- Piece boundary: renderer must not draw a gap; the last sample of a piece
  and first of the next coincide at the shared node (continuity contract).

## 4. Chunk / visibility
- Rendering driven by geometry (pure client math), not by world block reads
  (avoids the v1 "marker blocks untracked" and chunk-force-load issues).
- Frustum culling: geometry segment bounds; per-piece AABB.
- No z-fighting: rails offset above bed; sleepers below rail tops; rail
  height and offsets chosen to avoid coplanar faces.
- Known gap: vanilla `Blocks.rail` renders a single line and is not a
  substitute for the left/right + sleeper renderer.

## 5. Camera / screenshot contract
- All Phase 1 visual proof from the Flat Validation World (Superflat,
  Hardware Vulkan), real Eaglercraft canvas, no mock/generated images.
- Required screenshots (frozen):
  - SS-R1-01_STRAIGHT
  - SS-R1-02_CURVE
  - SS-R1-03_STRAIGHT_CURVE_JOIN
  - SS-R1-04_S_CURVE
  - SS-R1-05_PIECE_BOUNDARY
  - SS-R1-06_GRADIENT
  - SS-R1-07_OVERVIEW
- Each saved under doc/testing/phase0_6/screenshots/ (Phase 1) or the
  phase0_6 runs/ evidence dir; filenames exact.

## 6. Visual review PASS criteria
- Rails visible; left/right rails distinguishable.
- Sleepers visible (Phase 1 contract).
- Curve does not visibly break; join does not visibly gap.
- No buried rail; no severe floating rail.
- No sudden orientation jump; no missing segment; no rendering
  disappearance.
- Gradient visually coherent; scale reasonable (car/rail scale).

## 7. Non-goals (Phase 1)
- Block-based rail placement as the primary visual (only the validation bed).
- Model/texture content system.
- Signal/wire/catenary rendering.
- Multiplayer sync of render data (client computes from synced geometry).
