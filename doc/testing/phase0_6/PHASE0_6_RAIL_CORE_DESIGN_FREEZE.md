# Phase 0.6 Rail Core Design Freeze (PART C + D)

Status: FROZEN. Verdict: **PHASE 1 DESIGN GATE = OPEN**.
No product code changed in Phase 0.6.

## Selected architecture (summary)
- Distance-centered geometry (metres), ArcLengthTable (adaptive split),
  RailSample (distanceM,x,y,z,yaw,pitch,roll,pieceId).
- Straight (exact) + HorizontalBezier (X/Z) + VerticalBezier (Y profile).
- RailPiece (id, geometry, endpoints, length, connectivity) + light
  RailNetwork (registry/adjacency/next-piece) + RailPath (globalM resolver,
  boundary owned by earlier piece).
- Rendering: hybrid visible rails (left/right + sleepers) from geometry +
  Flat World validation bed.
- Placement: command prototype (start/end/direction/geometry) + preview.
- Persistence: WorldRailData schema v2 (NBT, versioned) + v1 migration read.
- Validation: Flat Validation World (Superflat, Hardware Vulkan) + real
  screenshots SS-R1-01..07 + numerical + visual regression.

## Frozen contracts (see dedicated docs)
- RAIL_GEOMETRY_CONTRACT.md — units, geometry/sample/piece/path API,
  arc length, continuity, gradient, cant, error policy.
- RAIL_RENDERING_VISUAL_CONTRACT.md — visible rails, renderer strategy,
  visual review PASS.
- PHASE1_TEST_COURSE.md — TC-01..08 coordinates, purposes, checks, shots.
- PHASE1_SCOPE_AND_ACCEPTANCE.md — scope, exclusions, prohibitions,
  numerical tolerances, 1.x sequence.
- RAIL_CORE_DISCOVERY.md / RAIL_CORE_ARCHITECTURE_OPTIONS.md — evidence +
  option rationale.

## Phase 1 Design Gate review (PART D)
Phase 1 implementer must answer without ambiguity:
- Rail Geometry = shape of one piece; sampleByDistance(localM) -> RailSample.
- sample API = RailGeometry.sampleByDistance(double) (+ pieceId, lengthM).
- distance unit = metre (1 block = 1 m).
- arc length = per-piece ArcLengthTable (adaptive split, metres<->param).
- Piece = id + geometry + endpoints + length + connectivity.
- Path = ordered traversal + global distance resolver + boundary owner rule.
- boundary = earlier piece owns its end; resolver snaps to shared node.
- Rendering = visible left/right rails + sleepers from geometry (not blocks).
- Placement = command prototype (start/end/direction/geometry) + preview.
- Persistence = WorldRailData v2 (versioned NBT) + v1 migration read.
- Phase 1 includes: geometry core, arc length, piece, path, visible rails,
  minimal placement, persistence, regression, screenshots.
- Phase 1 excludes: switch, signal, station, catenary, train controller,
  full formation, modelpack, A* (deferred), multiplayer rewrite.
- Numerical PASS: table in PHASE1_SCOPE_AND_ACCEPTANCE.md (0 NaN, error
  tolerances).
- Visual PASS: rails visible/distinguishable/sleepers/no gaps/breaks.
- Test Course: TC-01..08 on Flat Validation World.
- Screenshots: SS-R1-01..07 real game.
- Phase 1.x: 1.1 Geometry -> 1.2 Piece/Path -> 1.3 Renderer -> 1.4 Placement
  -> 1.5 Persistence/Regression/Final Gate.

## Unresolved issues (non-blocking, deferred)
- A* route search deferred to Phase 2/3 (light next-piece selection only).
- Switch geometry (branch) deferred to Phase 3 (data model reserves type).
- Full cant physics deferred (data model only).
- Sample yaw sign resolved (sample carries raw heading; entity/render
  conventions applied at use site).
- Placement UI beyond command prototype deferred.

## Final Report
See doc/testing/phase0_6/FINAL_REPORT_PHASE0_6.txt.
