# Rail Core Architecture Options (Phase 0.6 - PART B)

Status: COMPLETE. Comparison of design candidates with recommendation.
No product code changed.

## 1. Geometry representation

### Option G-A: normalized-t center (v1 style)
- Sample by t in [0,1]; length approximated (32-chord).
- Pros: trivial; matches v1.
- Cons: non-uniform metre speed on curves; spacing/speed segment-length
  dependent; no accurate arc length. REJECTED (v1 debt confirmed by Phase -1
  P6 and the v2 spike's success with distance).

### Option G-B: physical distance center (metres)
- Sample by distanceM; per-piece arc-length table maps metres->parameter.
- Pros: metres = sole authority (P5/P6); matches v2 spike (lengthM,
  sampleByDistance); train spacing/speed correct; RTM parity.
- Cons: needs arc-length implementation + table cache.
- RECOMMENDED.

### Option G-C: precomputed full lookup table
- Build a dense polyline/table once, reuse for all samples.
- Pros: fast; deterministic.
- Cons: memory (Web/Eaglercraft budget); no analytic tangent; coarse near
  high curvature unless adaptive. Subsumed by G-D's cached per-piece table.

### Option G-D: adaptive arc-length (base split + refinement where curvature high)
- base split = clamp(round(length*32), 8, 384); refine where curvature high.
- Pros: accurate long/short curves; bounded memory; RTM-style (RAIL_GEOMETRY
  §3). Cons: slightly more complexity.
- RECOMMENDED as the ArcLengthTable policy (G-B + adaptive split).

Decision: **G-B with G-D arc-length policy.** Straight = exact closed form.

## 2. Curve definition

### C-A: 3D cubic Bezier (full X/Y/Z control points) - v2 spike approach
- Pros: simple; already prototyped (RailV2Bezier); handles any spatial path.
- Cons: Y and X/Z are coupled; a "horizontal curve with separate gradient"
  is awkward; vertical-curve-first-class (P11) wants a dedicated vertical
  piece.

### C-B: horizontal Bezier (X/Z) + separate vertical profile (Y along distance)
- Pros: P11 vertical curve first-class; gradient transitions smooth and
  decoupled from horizontal; cleaner for cant.
- Cons: more classes (HorizontalCurve, VerticalCurve).
- RECOMMENDED for Phase 1 data model: horizontal cubic Bezier + vertical
  piece; the v2 spike's 3D Bezier is kept as the *validation course* shape but
  Phase 1 production geometry separates them.

Decision: **C-B** (horizontal + vertical separated). Phase 1.1 implements
Straight + HorizontalBezier + VerticalBezier; a 3D Bezier helper may remain
for the course.

## 3. Rail Piece / Path

### P-A: segment-local progress (v1) - REJECTED (P6, follower teleport).

### P-B: piece-local metres + global accumulated distance (v2 Course pattern)
- Piece owns geometry + id + length; Path resolves globalM -> (piece,
  localM); boundaries clamp to piece end (earlier piece owns boundary).
- Pros: proven by RailV2Course.resolve; continuous; deterministic.
- Cons: none significant.
- RECOMMENDED.

### P-C: linked piece graph + route/path abstraction
- Adds RailNetwork adjacency + A* route + path history for switches.
- Pros: needed for switch/branching (Phase 3); route history fixes follower
  teleport (Phase 2).
- Cons: larger scope.
- RECOMMENDED as a Phase 1.2 addition (piece registry + adjacency + route
  query), with A* route search deferred to Phase 2/3 unless cheap to add.

Decision: **P-B core (resolve globalM) in 1.2 + a light RailNetwork registry
(adjacency + simple next-piece selection) + RailPath (ordered traversal +
accumulated distance + boundary resolver).** A* deferred to Phase 2+ (route
history can be added then without breaking the contract).

## 4. Rendering

### R-A: block-only (vanilla rail blocks) - v2 spike approach
- Pros: simple; world-integrated; cheap.
- Cons: single vanilla rail line (no distinct left/right/sleepers); visual
  reads as "a rail line" but weak for curve/gradient continuity proof.

### R-B: generated geometry renderer (custom rail mesh left/right/sleepers)
- Pros: clear left/right rails + sleepers; strong visual proof; decoupled
  from block writes.
- Cons: needs a renderer + chunk/visibility integration; more Phase 1 scope.

### R-C: hybrid - validation bed blocks + production renderer
- Pros: block bed for world presence + custom renderer for crisp rails.
- Cons: two sources of truth (blocks + render) to keep aligned.
- RECOMMENDED as the Phase 1.3 target: a RailRenderer drawing left rail /
  right rail / sleepers from geometry samples, plus a validation stone bed
  (blocks) on the Flat Validation World. Acceptance = "clearly visible as
  rails in a real screenshot".

Decision: **R-C (hybrid)**. Phase 1.3 renders visible left/right rails +
sleepers from the geometry; the Flat Validation World keeps a block bed.
High-quality RTM model rendering stays Phase 7+.

## 5. Placement

### PL-A: command-only (start/end + geometry params)
- Pros: minimal; scriptable; no new item.
- Cons: not in-world intuitive.
- RECOMMENDED as the Phase 1.4 minimum prototype.

### PL-B: wand two-point (v1 style)
- Pros: in-world.
- Cons: item plumbing; duplicate-node debt in v1.
- Deferred; wand concept retained for a later editor.

### PL-C: preview + confirm
- Pros: better UX.
- Cons: extra render/UI scope.
- RECOMMENDED to add preview in 1.4 if cheap (sample polyline preview);
  full editor UI is Phase 3+.

Decision: **PL-A (command prototype: start/end/direction/geometry select)
with optional preview; confirm/cancel; invalid-geometry feedback.** Full
editor deferred.

## 6. Persistence

### S-A: extend v1 RailSystemSavedData - REJECTED (versionless, mixed graph).

### S-B: new v2 WorldRailData with schema version
- WorldRailData NBT v2 {version, pieces[], ...}; versioned; migration bridge
  reads v1 "rail_system".
- Pros: P17; clean; forward-extensible (switch/signal/train refs later).
- Cons: migration util needed.
- RECOMMENDED.

### S-C: JSON/text schema
- Pros: readable.
- Cons: larger, NBT is native here.
- Rejected for world data (NBT native); JSON reserved for content packs.

Decision: **S-B**: `WorldRailData` schema v2 (NBT), version field, Piece IDs,
geometry type + params, endpoints, connection, migration read of v1
"rail_system" (straight->Straight, curve+controls->HorizontalBezier).

## 7. Rejected alternatives (summary)
- G-A normalized-t, P-A segment progress, S-A v1 saveddata extension,
  R-A-only (insufficient visual), full editor UI in Phase 1, JSON world data.

## 8. Selected architecture (recommendation)
```
railv2.geometry  RailGeometry (lengthM, pieceId, sampleByDistance),
                 RailSample (distanceM,x,y,z,yaw,pitch,roll,pieceId),
                 Straight, HorizontalBezier, VerticalBezier,
                 ArcLengthTable (cumulative metres + paramAt, adaptive split)
railv2.network   RailPiece, RailNetwork (registry+adjacency), RailPath
                 (ordered traversal + global distance resolver + boundary)
railv2.render    RailRenderer (left/right rail + sleepers from samples)
railv2.persist   WorldRailData (schema v2) + V1Migration read
placement       /railsv2-style command prototype (start/end/direction/type)
validation       Flat Validation World + run-flat-validation.sh + SS-R1-*
```
Decision rationale: follows Phase -1 P5/P6/P7/P10/P11/P12/P15/P16/P17; the v2
spike already proves the distance/sample/arc-length core; keep validation
(spike) isolated from production.
