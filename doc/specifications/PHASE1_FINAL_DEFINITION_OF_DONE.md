# Railsys Phase 1 Final Definition of Done

- Status: **FREEZE**
- Date: 2026-08-12 JST
- Final owner: R16 Phase 1 Final Validation

## 1. Required complete user journey

A normal user, without a validation world or proof-only command, can complete:

```text
START_WEB_MINECRAFT.sh
  -> enter World
  -> /railsys3 wand
  -> receive the Railsys wand
  -> right click POS1
  -> see the POS1 direction arrow
  -> right click POS2
  -> see the POS2 direction arrow
  -> automatic production RailPath preview
  -> optionally edit direction, curve, gradient, cant, and asset
  -> Shift + right click Confirm
  -> see preview-identical Real 3D Hybrid rail
  -> connect the next RailPath
  -> create and operate one turnout
  -> save and exit the world
  -> restart using START_WEB_MINECRAFT.sh
  -> reload the same world
  -> see the same rail geometry, assets/versions, cant, connections,
     turnout topology, and active route restored
```

Command fallbacks for confirm/cancel/clear and touch input must also work. No
step may require normal fog/render changes or validation camera/renderer hooks.

## 2. Integrated functional definition

Phase 1 is not complete until all of the following coexist in one production
flow:

- straight;
- curve;
- gradient;
- cant;
- distance-based continuous rail;
- normal-world marker placement and editing;
- Real 3D Hybrid Rail Asset / ModelPack with fallback;
- multiple persistent rails;
- connected rail topology;
- one persistent, route-aware turnout.

Train production implementation is explicitly excluded from Phase 1. Signals,
stations, catenary, advanced turnouts, routing/interlocking, and production
train movement are Phase 2 or later.

## 3. Evidence and quality definition

Final R16 PASS requires all of these without waiver:

- numerical tests for geometry, frames, cant, continuous section boundaries,
  asset gauge/profile, save regeneration, graph topology, and turnout routes;
- current harness PASS with zero unexpected skip/fail;
- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew makeMainOfflineDownload`
  SUCCESS;
- normal launcher GUI walkthrough;
- concise screenshots for marker/preview/confirm, Real 3D cases, asset
  difference/fallback, connected rail, turnout states, and save/restart restore;
- supporting Vision review and Sol's integrated review;
- recorded WebGL reference performance meets the R11/R16 frozen budget;
- R1-R9 and all subsequent stage regressions pass;
- validation-only render/camera/probe logic does not leak into normal worlds;
- no normal fog workaround, renderer-only fake preview, or return to fixed
  repeated whole-track segments;
- preserved files and unrelated user files/worlds remain untouched;
- secret scan clean and Discord webhook runtime-only;
- clean-room provenance complete; no proprietary RTM/NR01 asset/source copied;
- checkpoint commits safely reflected to GitHub main;
- local `main == origin/main == GitHub main` at final completion.

## 4. Final verdict rule

Only when every item above is verified may the release state be:

```text
R10: GO
R11: GO
R12: GO
R13: GO
R14: GO
R15: GO
R16: GO
PHASE 1 VERDICT: PASS
```

An unverified mandatory item yields `NOGO` or `PARTIAL`, never an inferred PASS.
