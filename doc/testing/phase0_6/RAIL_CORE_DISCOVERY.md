# Rail Core Discovery (Phase 0.6 - PART A)

Status: COMPLETE (evidence-based inventory of docs + v1 + v2 + persistence +
rendering + placement + validation). READ-ONLY exploration; no product code
changed.

## 1. Documents inventory
- doc/architecture/: RAILWAY_SYSTEM_V2_ARCHITECTURE.md (principles P1-P20,
  1 block=1m, module map, KEEP/MIGRATE/REMOVE), DATA_MODEL.md,
  RAIL_GEOMETRY_DESIGN.md (RailSample contract, arc length, continuity,
  cant), TRAIN_FORMATION_BOGIE_DESIGN.md (distance formation, bogie anchors),
  RENDERING_RUNTIME_DESIGN.md, CONTENT_MODELPACK_DESIGN.md,
  MULTIPLAYER_PERSISTENCE_DESIGN.md, PERFORMANCE_DESIGN.md.
- doc/decisions/: ADR-001..ADR-010 (1 block=1m, bogie anchors, server
  authority, etc.).
- doc/migrations/MIGRATION_PLAN.md; doc/roadmaps/IMPLEMENTATION_ROADMAP.md
  (Phase 1 = Rail Geometry/Network core).
- doc/testing/: TEST_HARNESS_DESIGN, BASELINE_TEST_MATRIX, PHASE0_TEST_RESULTS,
  PHASE1_ENTRY_GATE (OPEN in Phase 0.3), ACCEPTANCE_TEST_STRATEGY.
- Phase 0.1 spike docs: doc/testing/phase0_1/*.
- Phase 0.2/0.3 runtime+regression: doc/testing/phase0_2/*, phase0_3 in
  railway_v2_regression/.
- Phase 0.5 flat world: doc/testing/phase0_5/*.

Key extracted facts from docs:
- 1 block = 1 metre (P1, CONFIRMED-RTM).
- Geometry (shape of one piece) separated from Network (connectivity/routing)
  (P7).
- Distance-based formation, metres as sole authority; segment progress only a
  per-piece lookup key (P5, P6).
- Arc length tables per piece, cached (P10).
- Vertical curve + cant/roll first-class (P11, P12).
- Switch = real geometry with branches, not "next segment id" (P13).
- Server authoritative, client read-only mirror (P15, P16).
- Save schema v2 with version + v1 migration (P17).
- Phase 1 roadmap: RailGeometry (straight/bezier/vertical), ArcLengthTable,
  RailSample, RailPiece, RailNode adjacency, A*, WorldRailData schema v2 +
  v1 migration read.

## 2. Railway v1 code inventory (net.minecraft.rail)
| Class | Role | Reuse in v2 | Debt to NOT carry |
|---|---|---|---|
| RailSegmentType | enum STRAIGHT/CURVE/SLOPE/SWITCH | concept | SLOPE/SWITCH unused |
| RailNode | graph vertex pos+adjacency | adjacency concept | duplicate adjacency |
| RailCurveData | 6 control coords, raw-t Bezier | control-point concept | raw-t, no arc length, endpoints not owned |
| RailSegment | edge, getPoint(t) 32-chord len | piece concept | normalized progress, no tangent/pitch/roll |
| RailPosition | (segmentId, progress) | RailV2Sample replaces | normalized progress |
| RailGraph | nodes+segments+switch+route+occ+station+NBT | split into Network/Piece | monolithic, whole-segment occupancy, greedy route, versionless NBT |
| RailSystemManager | static GLOBAL_GRAPH mirror | per-world store | dual identity, NBT-roundtrip copyFrom |
| RailSystemSavedData | WorldSavedData "rail_system" | schema v2 replaces | versionless |
| ItemRailWand | 4-click Bezier placement | becomes v2 tool | duplicate nodes, untracked markers, no undo |
| EntityRailVehicle | progress-based movement+formation+sync | adapter (Phase 2) | follower teleport, world-scan, speed in progress/tick, DataWatcher-as-physics |
| CommandRailSystem | /railsys ops | v2 debug commands | debug-marker visuals, baked constants |
| RenderRailVehicle | yaw-only procedural car | Phase 2/9 | no pitch/roll, no model |

v1 units: metres/blocks; speed in normalized progress/tick (segment-length
dependent); yaw only (no pitch/roll); +0.5D y offsets baked.

## 3. RailV2 spike inventory (net.minecraft.railv2)
| Class | Role | Status |
|---|---|---|
| RailV2Geometry | interface lengthM/pieceId/sampleByDistance | core contract (matches ideal) |
| RailV2Sample | distanceM,x,y,z,yaw,pitch,roll,pieceId | matches RailSample (roll always 0) |
| RailV2Straight | exact length, graded, yaw/pitch | reusable |
| RailV2Bezier | cubic, 256-split arc length + tForDistance binary search | prototype arc-length table |
| RailV2Course | 3-piece course (80 straight + ~157 bezier + 80 straight), placeRails | validation course; reuse for test course |
| RailV2CourseMath | wrapYaw | trivial |
| RailV2AutoValidate | server-tick hook, world-name gate (eaglervalidate/eaglerflat), camera tour | validation-only; DO NOT blend into production |
| EntityRailV2Car | 20m car, bogies +/-7m, distance formation | Phase 2 seed (bogie anchors proven) |
| RenderRailV2Car | procedural body + bogie markers | Phase 2/9 seed |
| CommandRailV2Validate | /railsysv2 build/spawn/start/tp | validation-only |

v2 spike facts: meters; yaw = atan2(tx,tz) wrap (-180,180]; pitch =
atan2(dy,hypot(dx,dz)) +up; roll 0 everywhere; Bezier 256-split polyline +
binary search reparameterization; boundary owned by earlier piece (clamp);
course total ~316m; car pose = bogie chord midpoint (+0.5 y), yaw negated on
entity rotation, NOT negated in renderer (note the sign difference).

## 4. Persistence findings (v1)
- RailSystemSavedData "rail_system": versionless NBT
  {NextNodeId,NextSegmentId,Nodes[],Segments[],Switches[],TrainTargets[],Stations[]}.
- Load via MapStorage; static GLOBAL_GRAPH mirror with NBT-roundtrip copyFrom;
  server readers get data.getGraph(), client readers get stale static graph.
- No version, no migration hook, no per-dimension key.
- v1 migration path (from docs): RailSegment STRAIGHT -> Straight geometry;
  RailSegment CURVE + RailCurveData -> horizontal Bezier (same points,
  recompute arc length); progress semantics replaced.

## 5. Rendering findings (v1 + v2)
- v1: debug-marker blocks (gold/diamond/redstone) + F3 debug overlay (lines/
  polyline); RailVehicle rendered as unlit procedural cuboids, yaw only.
- v2: placeRails() writes stone bed (5-wide, 3-deep) + 3 vanilla rail lines +
  gold markers every 10m + 10-high air corridor; cars rendered as flat-color
  cuboids + bogie marker cubes.
- Known rendering gaps for Phase 1 (geometry-correct but invisible):
  - vanilla `Blocks.rail` only renders as a single line at y (no distinct
    left/right rails/sleepers); visually reads as a rail but not left/right.
  - chunk force-loading is done manually in placeRails (world dependency).
  - no z-fighting handling documented; no frustum/visibility budget.
  - piece boundary: bed/rail continuity relies on 1m sampling step.

## 6. Placement findings
- v1: ItemRailWand (4-click Bezier, static per-UUID map, no undo), /railsys
  testline/testcurve/testloop/vehicle/spawn.
- v2: CommandRailV2Validate build/spawn/tp only (no interactive placement).
- Phase 1 minimum placement prototype is NOT present yet; the ideal is
  start/end + direction + geometry selection + preview + confirm/cancel.

## 7. Flat Validation World findings (Phase 0.5)
- Name "EaglerFlatValidate" -> Superflat (GuiCreateWorld hook on
  "eaglerflat"); AutoValidate gate accepts eaglervalidate|eaglerflat.
- run-flat-validation.sh (retry), SS-FLAT-01..07 + SS-FLAT-RE evidence.
- Known: Select World UI re-entry is flaky (layout variance); same-profile
  re-create reproduces validation. Phase 1 Rail Core must NOT depend on the
  re-entry UI path; it uses the flat world create/validate pipeline.
- Standard Phase 1 visual proof = Flat Validation World + Hardware Vulkan +
  real screenshots.

## 8. Dependency map (conceptual)
```
Placement ─► RailGeometry Definition ─► RailPiece ─► RailNetwork/Connectivity
                                              │
                 RailPath (accumulated metres, piece traversal, boundaries)
                                              │
                          Distance Sampling (sampleByDistance)
                                          ├─► Rendering (visible rails)
                                          └─► Persistence (schema v2)
Phase 2 boundary (train-facing):
  RailPath ─► Distance Sampling ─► Bogie anchors ─► Train body pose
```
Phase 0.6 freezes the rail core; train side is bounded to a Phase 2-facing
API (distance -> (piece, localMetres) -> sample) only.

## 9. Gap analysis (Ideal vs v2 Spike vs v1)
| Feature | Ideal (Phase -1) | v1 | v2 Spike | Reuse | Replace | Missing | Phase |
|---|---|---|---|---|---|---|---|
| Distance unit | metre | block=m | metre | all | - | - | - |
| Geometry API | length/sampleByDistance | getPoint(t) | lengthM/sampleByDistance | v2 | v1 | - | 1.1 |
| Arc length | per-piece table | raw-t/32-chord | 256-split polyline | v2 (adaptive) | v1 | adaptive split, table cache | 1.1 |
| Tangent/yaw/pitch | yes | yaw only | yes (yaw,pitch) | v2 | v1 | roll/cant | 1.1 |
| Piece | geometry+blocks+state | segment+progress | course piece | v2 concept | v1 segment | block set, switch state | 1.2 |
| Path | route history + resolver | greedy route | course.resolve | v2 resolve | v1 routing | A*, path history, branches | 1.2 |
| Rendering | visible rails | debug markers | stone bed + rails | v2 bed | v1 markers | distinct left/right + sleepers | 1.3 |
| Placement | editor tool | wand | command-only | wand concept | v1 wand state | preview/confirm | 1.4 |
| Persistence | schema v2 + migration | versionless | none | - | v1 | v2 schema, migration read | 1.5 |
| Vertical curve | first-class | SLOPE unused | none | - | - | vertical Bezier (data model now) | 1.1 (data) |
| Cant/roll | first-class | none | roll=0 | - | - | data model now; physics later | 1.1 (data) |
| Switch | real geometry | routing map | none | - | v1 switch map | branch geometry (Phase 3) | 2+ |

Phase 1 gaps: geometry core, arc length, piece, path, visible rails, minimal
placement, persistence v2 + v1 migration read, numerical+visual acceptance.
Phase 2+ gaps: train core, switch geometry, signals, stations, catenary,
content/modelpack.

## 10. Contradictions / notes found in docs
- v2 spike yaw sign: EntityRailV2Car sets `rotationYaw = wrapYaw(-yaw)`
  (negated) while RenderRailV2Car rotates by the un-negated yaw. This is a
  spike inconsistency to resolve in the Phase 1 sample yaw convention
  (recommend: sample.yaw = world heading as computed; rendering applies MC
  entity convention separately).
- Roadmap Phase 1 (from Phase -1) includes RailNetwork + A* + migration read;
  Phase 0.6 may freeze a slightly narrower Phase 1 (geometry/piece/path/visual/
  placement/persistence) if A* is deferred - to be decided in PART C.
- Placeholder package naming in the roadmap uses `railv2.*` subpackages; the
  spike uses flat `net.minecraft.railv2.*`. Phase 1 should adopt subpackages.

## 11. Unresolved questions (to resolve in PART C)
1. A* route search in Phase 1 or deferred to Phase 1.2+? (roadmap includes it)
2. Vertical curve: full implementation in Phase 1 or data-model-only?
3. Cant/roll: data model only in Phase 1 (sample field + roll=0) or a basic
   cant profile?
4. Renderer: vanilla rail blocks only, or a custom visible-rail renderer
   (left/right rail + sleepers)?
5. Persistence schema v2 exact fields and the v1 "rail_system" migration read
   scope.
6. Sample yaw sign convention (spike inconsistency above).
7. Placement prototype command form vs wand form.
