# Project Status And Roadmap

Audience:

```text
Small local LLMs such as GPT-OSS 20B or Llama 3.1 8B.
```

Current point:

```text
Phase 6-F-FIX3 is next.
```

This document is a project status and roadmap summary.
It is not an implementation task.

## 1. Project Overview

This project modifies:

```text
EaglercraftX 1.8 workspace
```

Goal:

```text
Create an RTM-like railway system that runs in browser Minecraft.
```

Main direction:

- Build a custom RailSystem.
- Do not rely on vanilla Minecart behavior for the final railway.
- Use custom graph data for rails.
- Use custom `EntityRailVehicle` for trains.

Core concepts:

```text
RailGraph = railway data
RailNode = control point
RailSegment = rail between two nodes
RailCurveData = curve control data
EntityRailVehicle = custom train entity
```

Important:

```text
RailSystem and Minecart are separate systems.
```

Do not break Minecart/Wrench features while working on RailSystem.

## 2. Phase Status Legend

Use these status names:

```text
DONE = implemented and build passed
PARTIAL = implemented as simple version, not production complete
BUGFIX_NEEDED = exists but has known serious bug
NOT_STARTED = not implemented
```

## 3. Implemented Phase List

### Phase 5-A: Minecart Link Base

Status:

```text
DONE
```

Implemented:

- Wrench item.
- Minecart `linkedPrevEntityId`.
- Minecart `linkedNextEntityId`.
- Wrench right-click link flow.
- Server-side link handling.

Notes:

- EntityID-based minimum implementation.
- UUID/NBT/DataWatcher not implemented for Minecart links.

### Phase 5-B: Minecart Follow

Status:

```text
DONE
```

Implemented:

- Simple linked Minecart follow motion.
- Distance control.
- Speed clamp.

Notes:

- No full train physics.
- No custom rail physics.

### Phase 5-C: RailSystem Data Structure

Status:

```text
DONE
```

Implemented:

- `RailNode`.
- `RailSegment`.
- `RailSegmentType`.
- `RailGraph`.
- `RailPosition`.
- `RailSystemManager`.

### Phase 5-D: Curve Data

Status:

```text
DONE
```

Implemented:

- `RailCurveData`.
- Cubic Bezier curve support.
- `RailSegment.getPoint(t)`.
- Length recalculation by sampling.
- NBT base for curve data.

### Phase 5-D2: Debug Rendering

Status:

```text
PARTIAL
```

Implemented:

- RailGraph debug drawing in RenderGlobal.
- Nodes and segments can be visualized.

Notes:

- Debug rendering has had visibility issues before.
- Marker blocks became more reliable for debugging.

### Phase 5-D3 / D3b / D3c: Test Commands And Markers

Status:

```text
DONE
```

Implemented:

- `/railsys clear`.
- `/railsys testline`.
- `/railsys testcurve`.
- Marker blocks.
- Temporary standard rail block placement.

### Phase 5-E / E2: Debug Vehicle Marker

Status:

```text
DONE
```

Implemented:

- Vehicle marker visualization.
- `/railsys vehicle`.
- Redstone block vehicle marker.

### Phase 5-F / F2 / F3: EntityRailVehicle

Status:

```text
DONE
```

Implemented:

- `EntityRailVehicle`.
- Movement on `RailSegment`.
- Basic render.
- Simple train-like model.
- Direction yaw from rail path.

### Phase 5-G: RailSystem Save

Status:

```text
DONE
```

Implemented:

- `RailSystemSavedData`.
- `RailGraph` NBT save/load.
- World graph access through `RailSystemManager`.

### Phase 5-H: RailWand

Status:

```text
DONE
```

Implemented:

- `RailWand`.
- Four-point curve creation.
- Marker block placement.
- Graph dirty marking.

### Phase 6-A: Train Formation

Status:

```text
PARTIAL
```

Implemented:

- `trainId`.
- `carIndex`.
- `trainLength`.
- `carSpacing`.
- `isLeadCar`.
- `/railsys spawntrain`.

Known issue:

- Follower spacing needs Phase 6-F-FIX3.

### Phase 6-B: Multi-Segment Travel

Status:

```text
PARTIAL
```

Implemented:

- `forward`.
- Segment end transition.
- Connected segment selection.

Limitations:

- Simple next segment selection.
- No robust route pathfinding.

### Phase 6-C: Switch

Status:

```text
PARTIAL
```

Implemented:

- `switchTargetSegmentByNodeId`.
- `/railsys switch`.
- Switch NBT save.

Limitations:

- No visual switch UI.
- No switch animation.

### Phase 6-D: Coupler Rendering

Status:

```text
PARTIAL
```

Implemented:

- Coupler lines between cars.
- Lead/tail visual differences.

Limitations:

- Coupler is visual only.
- No physical coupler.

### Phase 6-E: Riding

Status:

```text
PARTIAL
```

Implemented:

- Right-click riding.
- Rider position update.
- Bounding box adjustment.

Limitations:

- No seats.
- No cab view.
- No door logic.

### Phase 6-F: Driving Control

Status:

```text
BUGFIX_NEEDED
```

Implemented:

- `targetSpeed`.
- `maxSpeed`.
- `acceleration`.
- `/railsys start`.
- `/railsys stop`.
- `/railsys speed <value>`.
- W/S rider input.

Current values:

```java
targetSpeed for /railsys start = 0.005D
maxSpeed = 0.02D
acceleration = 0.0005D
```

Known issue:

- Phase 6-F-FIX3 still needed for formation spacing and clear behavior.

### Phase 6-G: HUD

Status:

```text
DONE
```

Implemented:

- Riding HUD in `GuiIngame`.
- Shows speed.
- Shows targetSpeed.
- Shows trainId.

### Phase 6-H: Riding Improvement

Status:

```text
PARTIAL
```

Implemented:

- Mounted offset adjusted.
- Rider position centered.

Limitations:

- Dismount position is not fully custom.

### Phase 6-I: Formation Management

Status:

```text
PARTIAL
```

Implemented:

- `/railsys addcar`.
- `/railsys removecar`.
- `/railsys unlink`.

Limitations:

- Simple tail add/remove only.
- No advanced consist editor.

### Phase 6-J: Route Target

Status:

```text
PARTIAL
```

Implemented:

- `trainTargetNodeByTrainId`.
- `/railsys route <nodeId>`.
- Simple next segment selection toward target.

Limitations:

- No A*.
- No full route planning.

### Phase 6-K: Signal / Block Occupancy

Status:

```text
PARTIAL
```

Implemented:

- `occupiedSegmentIds`.
- Stops before occupied next segment.

Limitations:

- Runtime only.
- No signal blocks.
- No signal rendering.
- No robust multiplayer traffic control.

### Phase 6-L: Stations

Status:

```text
PARTIAL
```

Implemented:

- `stationNodeIds`.
- `/railsys station`.
- 3 second station dwell.

Limitations:

- No platform block.
- No timetable.
- No passenger logic.

### Phase 6-M: Vehicle Model Organization

Status:

```text
PARTIAL
```

Implemented:

- `VehicleType`.
- Basic color variation.

Limitations:

- No JSON vehicle model packs.
- No texture packs.

### Phase 6-N: Sound

Status:

```text
NOT_STARTED
```

Notes:

- Sound was intentionally skipped.
- Empty implementation is acceptable for now.

### Phase 6-O: Sync Cleanup

Status:

```text
PARTIAL
```

Implemented:

- DataWatcher sync for:

```text
trainId
speed
targetSpeed
segmentId
progress
```

Limitations:

- Not full multiplayer synchronization.
- No custom packets.

## 4. Implemented Main Features

Implemented or partially implemented:

- Straight rail data.
- Curve rail data.
- Cubic Bezier curves.
- RailGraph save/load.
- RailWand curve creation.
- `/railsys` debug and control commands.
- Custom train Entity.
- Basic train model render.
- Direction rotation.
- Train formation.
- Coupler render.
- Riding.
- HUD.
- Multi-segment travel.
- Switch routing.
- Simple route target.
- Simple station stop.
- Simple block occupancy.

## 5. Current Serious Bugs

### Bug 1: Follower spacing

Status:

```text
BUGFIX_NEEDED
```

Problem:

```text
carSpacing is currently used as progress distance in follower logic.
```

Expected:

```text
carSpacing should be treated as block distance.
Convert to progress using RailSegment.getLength().
```

Main file:

```text
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
```

### Bug 2: `/railsys clear` may leave vehicles

Status:

```text
BUGFIX_NEEDED
```

Problem:

```text
RailGraph.clear() does not remove EntityRailVehicle entities.
```

Expected:

```text
/railsys clear should remove EntityRailVehicle entities.
Do not remove Minecart entities.
```

Main file:

```text
src/game/java/net/minecraft/command/CommandRailSystem.java
```

### Bug 3: Start speed tuning

Status:

```text
PARTIAL
```

Current:

```text
/railsys start targetSpeed = 0.005D
maxSpeed = 0.02D
acceleration = 0.0005D
```

Expected:

```text
Keep this slow unless user asks for faster speed.
```

## 6. Not Implemented

Not implemented:

- Train sounds.
- Door system.
- Signal rendering.
- Signal blocks.
- GUI polish.
- Multiplayer full sync.
- Robust pathfinding.
- A* route search.
- Persistent train management.
- Chunk loading support.
- JSON vehicle packs.
- Model packs.
- Texture packs.
- Sound packs.
- Passenger system.
- Timetable system.

## 7. Future Roadmap

### Phase 7: Railway Operations

Planned:

- Signal system.
- ATS/ATC.
- Timetable.
- Station platform.
- Passenger system.
- Train stop position control.

Status:

```text
NOT_STARTED
```

### Phase 8: Multiplayer Stabilization

Planned:

- Better multiplayer sync.
- Optimization.
- Chunk loading support.
- Persistent trains.
- Better occupancy management.

Status:

```text
NOT_STARTED
```

### Phase 9: Content Packs

Planned:

- Vehicle packs.
- Model packs.
- Texture packs.
- Sound packs.
- JSON-based train definitions.

Status:

```text
NOT_STARTED
```

## 8. Recommended Test Commands

Basic test:

```text
/railsys clear
/railsys testline 60
/railsys spawntrain 4
/railsys start
/railsys stop
```

Formation test:

```text
/railsys clear
/railsys testline 60
/railsys spawntrain 4 0.05
/railsys start
```

Curve test:

```text
/railsys clear
/railsys testcurve
/railsys spawntrain 4
/railsys start
```

Station test:

```text
/railsys station
/railsys start
```

Route test:

```text
/railsys route <nodeId>
```

## 9. Important Files

Core files:

```text
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
src/game/java/net/minecraft/command/CommandRailSystem.java
src/game/java/net/minecraft/rail/RailGraph.java
src/game/java/net/minecraft/client/renderer/entity/RenderRailVehicle.java
```

Supporting files:

```text
src/game/java/net/minecraft/rail/RailNode.java
src/game/java/net/minecraft/rail/RailSegment.java
src/game/java/net/minecraft/rail/RailCurveData.java
src/game/java/net/minecraft/rail/RailSegmentType.java
src/game/java/net/minecraft/rail/RailPosition.java
src/game/java/net/minecraft/rail/RailSystemManager.java
src/game/java/net/minecraft/rail/RailSystemSavedData.java
src/game/java/net/minecraft/client/gui/GuiIngame.java
```

Do not edit Minecart files unless explicitly asked.

## 10. Notes For Local LLMs

Follow these rules:

- Make small changes only.
- Build after changes.
- Do not rewrite RailGraph.
- Do not remove existing commands.
- Do not remove existing NBT keys.
- Do not touch Minecart files.
- Do not edit generated files.
- Do not add external dependencies.
- Do not use destructive git commands.

Required build command:

```sh
cd /Users/user/Desktop/Test_v1_Eaglercraft1.8_workspace
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=.gradle-local
./gradlew makeMainOfflineDownload
```

Required result:

```text
BUILD SUCCESSFUL
```

