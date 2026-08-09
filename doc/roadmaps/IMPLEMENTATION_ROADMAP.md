# Railway System v2 - Implementation Roadmap

Design-only (Phase -1). Phase ordering chosen so the distance-based core
comes first (it unblocks everything), content/rendering after, and
migration/parity last.

Each phase: purpose, scope, deps, files/packages, new classes, migration
target, acceptance, tests, soak, perf gate, rollback, risks, user decision
points, entry/exit conditions.

Packages: `net.minecraft.rail.*` existing; new `net.minecraft.railv2.*`
(new: `railv2.geometry`, `railv2.network`, `railv2.world`, `railv2.train`,
`railv2.signal`, `railv2.station`, `railv2.crossing`, `railv2.catenary`,
`railv2.content`, `railv2.model`, `railv2.render`, `railv2.net`,
`railv2.persist`, `railv2.perf`, `railv2.legacy`). v1 stays until Phase 11.

---

## Phase 0 - Baseline / Test Harness / v1 Preservation
- Purpose: safe refactor ground; capture v1 behavior as tests; freeze v1.
- Scope: build CI/unit harness (pure JVM), geometry math lib, baseline soak.
- Deps: none.
- New: `railv2.math` (Vec, arc-length helpers), test sources; no production
  behavior change to v1.
- Acceptance: build green; v1 unchanged; math tests pass (straight/bezier
  length vs reference).
- Tests: unit (math), soak: v1 testloop unchanged.
- Perf gate: n/a.
- Entry: current repo. Exit: harness + baseline recorded.

## Phase 1 - Rail Geometry / RailNetwork Core v2
- Purpose: distance-capable geometry + routing layer.
- Scope: RailGeometry (straight/bezier/vertical), ArcLengthTable, RailSample,
  RailPiece, RailNode adjacency, route search (A*), WorldRailData schema v2
  + v1 migration read.
- Deps: Phase 0.
- New classes: as above; `RailNetwork`; migration util.
- Migration target: RailGraph->RailNetwork; RailSegment->RailPiece/Geometry.
- Acceptance: sampleByDistance continuity; length accuracy; migration of
  existing graph to v2 pieces round-trips geometry.
- Tests: unit (arc length, continuity at junctions, migration).
- Soak: none yet (no train).
- Perf gate: piece build time budget.
- Risks: arc precision; migration of old curve data.
- Exit: v2 geometry network loads a migrated v1 world.

## Phase 2 - Train / Formation / Bogie / Distance Movement
- Purpose: fix follower teleport; distance-based formations.
- Scope: TrainFormation, TrainCar, Bogie (logical anchors), Coupler,
  TrainPose, PathPosition + resolver, leader movement, follower walk-back,
  reverse, light car entity, V1Adapter (read-only pose from v2).
- Deps: Phase 1.
- New classes: railv2.train.*, adapter.
- Migration target: EntityRailVehicle movement -> v2; car defs default.
- Acceptance (headline): 8-16 car train, 100+ laps on testloop-course,
  no teleport, no disappearing follower, spacing within tolerance, pose
  continuous, reverse OK, switch both routes later (Phase 3 switch).
- Tests: unit (walker/resolver), soak (loop), manual.
- Perf gate: tick budget with 16-car train.
- Rollback: adapter disabled -> v1 entity.
- Risks: path continuity at nodes; performance of resolve().
- Exit: formations stable; V1Adapter renders v2 pose.

## Phase 3 - Rail Placement / Switch / Collision
- Purpose: rails become world objects you place and collide with.
- Scope: RailPiece world blocks + ballast + collision + core block entity;
  marker/placement tool (straight/bezier/switch); switch geometry
  (basic/single-cross/scissors/diamond); preview; copy/remove/undo.
- Deps: Phase 1, Phase 2.
- New: railv2.world.*, switch geometry, placement commands.
- Migration target: RailWand + debug markers -> v2 tooling; switches real.
- Acceptance: build junction; train follows active branch; placement
  validation; save/load; collision.
- Tests: placement validity, turnout routing both branches, save/load.
- Perf gate: block write budget.
- Risks: geometry of switches; block cost.
- Exit: editor + collision functional.

## Phase 4 - Driving / Cab / Physics
- Purpose: realistic operation.
- Scope: TrainController (notch, reverser, brake ramp, EB), gradient
  resistance, signal speed clamp hook, cab HUD, doors/pantograph/light
  controls, announcements hooks.
- Deps: Phase 2.
- New: railv2.train.TrainController, HUD, input.
- Acceptance: staged throttle, controlled stop distance, doors/panto.
- Tests: physics unit + manual.
- Perf gate: none.
- Exit: precise driving.

## Phase 5 - Signal / Block / Route
- Purpose: safe traffic; release-by-tail.
- Scope: SignalBlock occupancy (distance intervals), SignalAspect, signal
  hardware + wire/connector/insulator, converters, route locking, switch
  locking, signal speed restriction, ATS hooks.
- Deps: Phase 2 (distance), Phase 3 (rails), Phase 4 (driving).
- New: railv2.signal.*.
- Acceptance: two trains -> second stops at red, proceeds on green; release
  by tail; switch locking.
- Tests: following distance, deadlock, turnout interlocking.
- Perf gate: occupancy update cheap.
- Risks: deadlock; sync.
- Exit: automatic safe blocks.

## Phase 6 - Station / Operation
- Purpose: scheduled stopping.
- Scope: Station, Platform, StopPoint (exact stop), dwell, door side,
  announcements, Timetable hooks (external ATO-ready).
- Deps: Phase 4, Phase 5.
- New: railv2.station.*.
- Acceptance: train stops within 0.25m, doors correct side, dwell, resume;
  timetable hook present.
- Tests: stop accuracy, dwell, schedule.
- Exit: station ops functional.

## Phase 7 - Crossing / Catenary / Railway Objects
- Purpose: infrastructure.
- Scope: CrossingGate (lights+sound+state, signal integration), Catenary
  (poles/insulators/connectors/wire/sag, pantograph contact), machines/
  ornaments/containers/signboards runtime.
- Deps: Phase 3, Phase 5.
- New: railv2.crossing.*, railv2.catenary.*, objects runtime.
- Acceptance: crossing closes before arrival; wire renders with sag.
- Tests: crossing timing, wire continuity.
- Exit: world feels like a railway.

## Phase 8 - Content / ModelPack
- Purpose: external content without core changes.
- Scope: ContentRegistry, definitions (Train/Rail/Signal/Machine/Wire/
  Object/Sound/Animation/Script), pack loader (Model*.json + pack.json +
  sounds.json), v2 native mesh + converter (OBJ/MQO import offline), JSON
  schema strategy, restricted script engine.
- Deps: Phase 3 (rail defs), Phase 4 (train defs).
- New: railv2.content.*, railv2.model.*, converter tool.
- Acceptance: external pack adds train/rail/signal; no core change; missing
  def fallback.
- Tests: pack load, schema validation, converter round-trip.
- Exit: content ecosystem works.

## Phase 9 - Rendering / Animation / Sound
- Purpose: visual/audio parity.
- Scope: TrainRenderer (pose+state+parts+lights+rollsign), BogieRenderer,
  RailRenderer (blocks+preview), Signal/Wire/Object renderers, AnimationState,
  SoundEngine (events, spatial, loop), debug overlay.
- Deps: Phase 4, Phase 8 (defs), Phase 7 (objects).
- New: railv2.render.*, railv2.sound.*, railv2.animation.*.
- Acceptance: config-defined car renders + animates + sounds; emissive
  lights.
- Tests: render smoke (offscreen), manual.
- Perf gate: render budget.
- Exit: visual parity baseline.

## Phase 10 - Multiplayer / Persistence / Performance
- Purpose: correct shared world at scale.
- Scope: RailwaySync packets (snapshot/delta, pose batches, state),
  interpolation, content manifest, WorldRailData v2 full, chunk keepalive,
  SpatialIndex + ActiveRailCache, packet budget, v1 DataWatcher replaced.
- Deps: all prior.
- New: railv2.net.*, railv2.perf.*.
- Acceptance: 2 clients identical poses/state; save/load byte-stable; 100km
  perf budget; bandwidth budget.
- Tests: 2-client parity, soak, bandwidth meter.
- Exit: production-ready sync.

## Phase 11 - Legacy Migration / Cleanup
- Purpose: remove v1.
- Scope: V1Adapter removal, CommandRailSystem v1 test builders -> v2 debug
  course, old DataWatcher path removal, rail_system_v1_backup cleanup.
- Deps: Phase 10.
- Acceptance: no runtime path uses v1 logic; data fully v2.
- Tests: full regression.
- Exit: clean tree.

## Phase 12 - RTM Parity / Long-duration Verification
- Purpose: final verification.
- Scope: run acceptance + soak suites (loop, S-curve, gradient, cant,
  switch, reverse, save/load, chunk, multiplayer, long-duration),
  performance gates.
- Deps: Phase 11.
- Acceptance: all acceptance criteria (doc/testing/ACCEPTANCE_TEST_STRATEGY.md)
  green; soak 100+ laps stable.
- Exit: Railway System v2 "RTM parity" gate passed.

---

## Ordering rationale vs suggested defaults
Distance-based formation (P5) must precede signals/stations/crossing because
those depend on accurate, stable train positions. Content (Phase 8) after
runtime so definitions are validated against real physics. Rendering after
content so models are definition-driven. Migration late so v1 works until
v2 is proven.
