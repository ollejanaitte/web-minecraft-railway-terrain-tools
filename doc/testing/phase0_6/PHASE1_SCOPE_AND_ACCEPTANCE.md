# Phase 1 Scope & Acceptance (Phase 0.6 - PART C, frozen)

Status: FROZEN for Phase 1. No product code changed.

## 1. Phase 1 includes
- Rail Geometry production core (Straight, HorizontalBezier, VerticalBezier).
- ArcLengthTable (adaptive split, cached, metres<->parameter).
- RailSample + sampleByDistance/sampleByProgress.
- RailPiece (id, geometry, endpoints, length, connectivity).
- RailNetwork light (registry + adjacency + next-piece selection).
- RailPath (ordered traversal + global distance resolver + boundary).
- Visible rail rendering (left/right rails + sleepers) from geometry.
- Minimal placement prototype (command: start/end/direction/geometry).
- Persistence: WorldRailData schema v2 + v1 "rail_system" migration read.
- Flat Validation World regression + numerical regression + visual regression.
- Harness unit tests for geometry/arc-length/continuity.

## 2. Phase 1 excludes
- Switch full implementation (branch geometry Phase 3).
- Signal / block / route locking.
- Station / platform / stop point / timetable.
- Catenary / crossing.
- Train Controller / driving / cab.
- Full formation / advanced train core (Phase 2 boundary only).
- ModelPack / content ecosystem / RTM assets.
- Multiplayer sync rewrite.
- A* route search (deferred to Phase 2/3; light next-piece selection only).

## 3. Phase 1 prohibitions
- Do NOT touch dirty v1 files (CommandRailSystem.java, EntityRailVehicle.java).
- Do NOT delete the RailV2 spike wholesale (it is the validation course).
- Do NOT return to normalized segment progress as movement authority.
- Do NOT mix Geometry/Rendering/Placement into one giant class.
- Do NOT PASS on Build success alone; screenshots are required.
- Do NOT PASS without real screenshots / without Flat Validation World.
- Do NOT implement Phase 2 Train Core simultaneously.
- Do NOT extend switches into Phase 1.
- Do NOT copy RTM code/assets.
- Do NOT mix validation-specific hacks into production architecture
  (AutoValidate/camera tour stay isolated in railv2 spike).

## 4. Numerical acceptance (frozen)
| Check | Tolerance | Rationale |
|---|---|---|
| Straight length error | <= 1e-6 m (exact) | closed form |
| Curve arc-length error | <= 0.5% vs adaptive reference (or abs 0.1 m, whichever larger) | 256-split reference vs adaptive |
| Piece join position error | <= 1e-4 m | continuity EPS_POS |
| Tangent/yaw continuity at join | <= 0.5 deg | visual smoothness |
| Pitch continuity at join | <= 0.5 deg | gradient smoothness |
| Roll continuity | <= 0.5 deg (Phase 1 roll=0) | trivially satisfied |
| Distance sampling error | <= 1e-3 m | sampleByDistance round-trip |
| Boundary jump | <= 1e-4 m | no gap/no teleport |
| NaN / Infinity | 0 occurrences | invalid-state policy |
| Invalid state exceptions | 0 unexpected | guard policy |
| Long-distance stability | sample at totalLength stable, no drift | determinism |

## 5. Visual proof (frozen)
- Flat Validation World (Superflat) + Hardware Vulkan + real Eaglercraft.
- Screenshots SS-R1-01..07 (see PHASE1_TEST_COURSE.md and
  RAIL_RENDERING_VISUAL_CONTRACT.md).
- Visual review PASS criteria (rails visible, left/right distinguishable,
  sleepers, no gaps/breaks/buried/floating, no orientation jumps, gradient
  coherent, scale reasonable).

## 6. Phase 1.x sequence (frozen)
- Phase 1.1: Geometry Core (Straight, HorizontalBezier, VerticalBezier,
  ArcLengthTable, RailSample, numerical harness).
  Gate: harness green + numerical acceptance.
- Phase 1.2: Rail Piece / Rail Network / Rail Path (registry, adjacency,
  resolve globalM, boundary).
  Gate: harness continuity tests green.
- Phase 1.3: Visible Rail Renderer (left/right + sleepers on Flat World).
  Gate: SS-R1-01..07 screenshots + visual review.
- Phase 1.4: Placement Prototype (command start/end/direction/type +
  preview).
  Gate: place course on Flat World via command; visual + numerical.
- Phase 1.5: Persistence (WorldRailData v2 + v1 migration read) + Regression
  + Final Gate.
  Gate: save/load round-trip; migration of v1 graph; full regression;
  FINAL_REPORT; commit/push/sync/Discord.
Each Phase 1.x: implement -> unit/harness -> build -> run-validation (Flat
World) -> screenshot -> numerical check -> visual review -> regression ->
evidence -> FINAL_REPORT -> commit -> push -> sync -> Discord.

## 7. Regression baseline
- Harness PASSED 30 / FAILED 0 / SKIPPED 3 must be maintained (no FAIL
  increase).
- Production build SUCCESS.
- Cold-start/validation pipeline (Phase 0.2/0.5) stays green.
