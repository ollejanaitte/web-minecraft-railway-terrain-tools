# Manual Baseline Checklist (Tier 3)

Run against the production build (`makeMainOfflineDownload` offline HTML) in
a browser, single-player. Record results in PHASE0_TEST_RESULTS.md.
Each item: Expected v1 behavior (baseline) — NOT necessarily correct.

## Setup
1. Build: `./gradlew makeMainOfflineDownload` (JAVA_HOME java-17).
2. Open `target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html`.
3. Enter a single-player world. Enable cheats (commands perm 2).

## Commands / rail
- [ ] `/railsys help` -> subcommand list shown.
- [ ] `/railsys testloop` -> "Created test rail loop: size=40x40 radius=10
  segments=8 startNode=..." and markers appear.
- [ ] `/railsys testcurve` -> curve segment + control markers.
- [ ] `/railsys testline 30` -> straight + markers.
- [ ] F3 debug overlay shows nodes/segments/curve (RAIL_SYSTEM_DEBUG_RENDER).

## Train
- [ ] `/railsys spawntrain 3` -> 3 cars visible.
- [ ] `/railsys start` -> train accelerates.
- [ ] `/railsys stop` -> decelerates to stop.
- [ ] `/railsys speed 0.01` -> cruises.
- [ ] Right-click a car to ride; W/S throttle works.
- [ ] Observed: when the lead car crosses a node into the next segment, note
      whether follower(s) flicker/teleport (KNOWN FAILURE BL-TRAIN-001).
- [ ] `/railsys station` near a node; train stops ~60 ticks at that node.
- [ ] `/railsys switch <id>` + `route <node>` on a junction; observe route.

## Persistence
- [ ] Place rails + train; exit to title; reload world.
- [ ] Rails persist (rail_system WorldSavedData).
- [ ] Vehicles persist (NBT) at ~same position.

## Renderer / UI
- [ ] Cars render as colored cuboids; coupler line between cars.
- [ ] Ride HUD shows RailVehicle/speed/target/trainId.

## Save results
Record exact commands, timestamps, and observations in PHASE0_TEST_RESULTS.md.
