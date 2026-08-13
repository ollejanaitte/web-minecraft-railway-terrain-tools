# R14_PRODUCTION_3D_RAIL — Phase 1-R14 Implementation Summary

Date: 2026-08-13 JST
Scope: Production 3D Rail pipeline + Standard Closed-Loop Production Rail Test
Course. R10F/R12/R13 foundations preserved.

## 1. Production pipeline (unified)

```
RailSegment (R13, authoritative)
  -> derived RailPath (F2, from endpoints)
  -> PathSample / RailLocalFrame (F2 frames + roll)
  -> ProductionRailMeshBuilder (geometry-core) -> mesh sections
  -> RailsysProduction3DRenderer (game GL front-end)
```

- Entry is ALWAYS a production RailSegment (R13 stable id + gauge snapshot).
- No renderer-only centerline; no validation geometry reuse; no asset-driven
  RailPath mutation; no datum recompensation; no phantom path; preview and
  confirmed share the same F2 pipeline (R10F F2.4/F5).

## 2. Railsys-native Asset Boundary (R14-08)

`RailProfile` (geometry-core) is the stable GEOMETRY/APPEARANCE contract:
head/web/foot dims, gauge, sleeper/fastener/ballast, colours, materialId.
R15 RTM ModelPack adapter targets this type. The mesh is derived from
RailPath frames + profile; changing the profile NEVER changes the RailPath
(F4 — verified by g03).

## 3. Rail profile (R14-02)

- Per rail (left/right at +-gauge/2 along frame right): foot (wide base), web
  (narrow middle), head (wide top). Each rail span is emitted as a CLOSED 3D
  prism: a 4-corner rectangle cross-section swept between two adjacent frames
  (4 longitudinal side faces + 2 end caps), so every rail is watertight with no
  degenerate/duplicate corners.
- Cross-section orientation follows RailLocalFrame (forward/right/up + roll);
  cant is frame attitude, never centerline deformation (R10F/R12).
- Numeric tests: profile dims (c01), left/right symmetry (c02).

## 4. Sleeper/Fastener (R14-03)

- Sleepers placed at real distance s = 0, spacing, 2*spacing, ... (NOT sample
  index). Measured sampling-independent (334 sleepers at 0.05/0.1/0.25/0.5m
  steps for 200m; floor(200/0.6)+1 = 334).
- Each sleeper is DRAWN at its exact distance-based world position (not the
  nearest sample frame), with orientation from the nearest sample frame.
- Half-open section clipping: a sleeper at a section boundary belongs to
  exactly one section (no double-count); the path-end sleeper is included once.
- Numeric tests: s01 (distance-based count = floor(len/spacing)+1), s02
  (sampling-independent), m04 (terminal s=total emitted when length exactly
  divisible by section length), m05 (no boundary double-count).

## 5. Gauge (R14-04)

- Left/right rail centres at +-gauge/2 along frame right; gauge changes the
  look, never the centerline (g03). Gauge preserved under cant (g02) and at
  narrow/standard/upper values (g04).

## 6. Composite geometry (R14-05)

- Straight/Curve/Gradient/Cant and combinations verified numerically: frame
  orthonormal (f01), cant sign (f02), gradient following (f03), composite
  curve+gradient+cant (comp01), no NaN (m03).

## 7. Mesh segmentation (R14-06) + Cache/Rebuild contract (R14-07)

- Long rails split into fixed-length sections (default 32m). Section boundary
  shares the exact same PathSample as the next section's first sample — no gap,
  no frame jump (m02). Mesh is DERIVED/CACHE, never authoritative.
- Rebuild triggers (documented): geometry/cant/gauge/profile/sleeper/asset
  change. Rebuild-free: camera, unrelated rail, signalState/occupied only.
- Culling/LOD/large-network tuning deferred to R24.

## 8. Standard Closed-Loop Course (R14-12)

`StandardClosedLoopCourse` (geometry-core) builds a rounded rectangle =
4 straights + 4 smooth 90-degree F2 corners as production RailSegments:

- Course A (flat, cant 0): courseA(cx,cz,w,l,r,gauge,asset).
- Course B (cant): courseB(...,maxCantDeg,asset) — straight cant 0, corner
  cant non-zero.
- Exact tangent-point geometry: every adjacent pair shares the exact endpoint
  and tangent (position + tangent closure exact by construction).
- 8 segments: 4 NORMAL (straight) + 4 CURVE (corner).
- Loop total for 40x80 r=10 ≈ 216.6 m (F2 corner arc ~14.16 m, not circular).

## 9. Closed-Loop Geometry contract (R14-13)

Numeric tests: loop01 (8 segments, 4 straight + 4 curve), loop02 (position +
tangent closure at every join incl. last->first), loop03 (start/end frame
continuity, no drift), loop05 (gauge continuity around loop), loop06 (F2
total length). Course B cant verified (loop04). Topology (RailNode/
RailConnection) is R16; vehicle running is Phase 2 — not implemented.

## 10. Game wiring

- `RailsysProduction3DRenderer` renders each ACTIVE store segment (GL
  front-end over the pure mesh builder).
- `RailsysProductionRailStore.registerClosedLoopCourse` builds the course with
  real issued ids.
- `/railsys3 testloop [w] [l] [r]` builds the course; RenderGlobal renders all
  active store segments.
