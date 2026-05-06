# Local LLM Handoff

This document is for a small local coding model such as GPT-OSS 20B or Llama 3.1 8B.
Follow the instructions exactly. Do not guess broad refactors.

## Current Point

The project is an EaglercraftX 1.8 workspace with a custom RailSystem and train Entity work in progress.

Current phase:

```text
Phase 6-F-FIX3 is next.
Phase 6-F-FIX2 was just completed.
```

Last confirmed build:

```text
./gradlew makeMainOfflineDownload
BUILD SUCCESSFUL
```

Important current behavior:

- `/railsys testline 60` creates a 60 block test line.
- `/railsys spawntrain 4` spawns 4 custom rail vehicles on the selected RailSegment.
- Spawned train should be stopped at spawn time.
- `/railsys start` starts the train slowly.
- `/railsys stop` stops the train.
- `/railsys speed <value>` sets target speed.
- Player can ride `EntityRailVehicle`.
- HUD shows train data while riding.
- Segment switching, route, station, addcar, removecar, unlink exist as simple implementations.

## Repository Root

Use this directory:

```text
/Users/user/Desktop/Test_v1_Eaglercraft1.8_workspace
```

## Build Command

Run exactly:

```sh
cd /Users/user/Desktop/Test_v1_Eaglercraft1.8_workspace
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=.gradle-local
./gradlew makeMainOfflineDownload
```

Expected result:

```text
BUILD SUCCESSFUL
```

If Gradle fails with:

```text
java.net.SocketException: Operation not permitted
```

that is a sandbox permission problem, not a Java code problem. Run the same Gradle command with permission to execute outside the sandbox.

## Main Files

Only edit these files unless a task explicitly says otherwise:

```text
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
src/game/java/net/minecraft/command/CommandRailSystem.java
src/game/java/net/minecraft/rail/RailGraph.java
src/game/java/net/minecraft/client/gui/GuiIngame.java
src/game/java/net/minecraft/client/renderer/entity/RenderRailVehicle.java
```

Do not edit Minecart code unless the user explicitly asks:

```text
src/game/java/net/minecraft/entity/item/EntityMinecart.java
src/game/java/net/minecraft/entity/item/EntityMinecartEmpty.java
src/game/java/net/minecraft/entity/item/EntityMinecartContainer.java
src/game/java/net/minecraft/entity/item/EntityMinecartFurnace.java
src/game/java/net/minecraft/entity/item/EntityMinecartHopper.java
src/game/java/net/minecraft/entity/EntityMinecartCommandBlock.java
```

## Important Classes

### EntityRailVehicle

File:

```text
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
```

Purpose:

- Custom train vehicle Entity.
- Moves on `RailSegment` by `segmentId + progress`.
- Supports train formation fields.
- Supports riding.
- Supports speed control.
- Syncs selected fields through DataWatcher.

Important fields:

```java
public int segmentId = -1;
public double progress = 0.0D;
public double speed = 0.0D;
public double targetSpeed = 0.0D;
public double maxSpeed = 0.02D;
public double acceleration = 0.0005D;

public int trainId = -1;
public int carIndex = 0;
public int trainLength = 1;
public double carSpacing = 0.2D;
public boolean isLeadCar = true;
public boolean forward = true;
```

Important behavior:

- Lead car updates its own speed and progress.
- Non-lead cars must not self-drive.
- Non-lead cars must follow the lead car.
- Current follower formula after Phase 6-F-FIX2:

```java
targetProgress = lead.progress - carSpacing * carIndex;
```

Warning:

`carSpacing` is a progress value, not a block distance. This is likely the next bug source.

### CommandRailSystem

File:

```text
src/game/java/net/minecraft/command/CommandRailSystem.java
```

Purpose:

- Implements `/railsys` command group.
- Creates test rail data.
- Spawns train vehicles.
- Controls speed and train management.

Important commands:

```text
/railsys clear
/railsys testline [length]
/railsys testcurve
/railsys vehicle [progress]
/railsys spawnvehicle
/railsys spawntrain [count] [spacing]
/railsys start
/railsys stop
/railsys speed <value>
/railsys addcar
/railsys removecar
/railsys unlink
/railsys route <nodeId>
/railsys station
/railsys switch [segmentId|clear]
```

Current spawntrain behavior:

- Uses `getInitialTrainCarProgress(segment, i)`.
- Intended initial positions:

```text
car 0: 15 blocks from start
car 1: 12 blocks from start
car 2: 9 blocks from start
car 3: 6 blocks from start
```

### RailGraph

File:

```text
src/game/java/net/minecraft/rail/RailGraph.java
```

Purpose:

- Holds RailNode and RailSegment graph.
- Stores switches, train route target nodes, station nodes.
- Tracks occupied segments in memory.

Do not replace this class with a new graph system.

### GuiIngame

File:

```text
src/game/java/net/minecraft/client/gui/GuiIngame.java
```

Purpose:

- Draws HUD.
- Rail HUD is drawn only while riding `EntityRailVehicle`.

### RenderRailVehicle

File:

```text
src/game/java/net/minecraft/client/renderer/entity/RenderRailVehicle.java
```

Purpose:

- Draws simple train model.
- Draws coupler lines between cars.

Do not add texture dependencies unless explicitly requested.

## Hard Rules

Do:

- Keep changes small.
- Use existing class names and methods.
- Add null checks.
- Keep `worldObj.isRemote` separation.
- Build after every change.
- Preserve existing command names.
- Preserve existing save keys unless intentionally adding new keys.

Do not:

- Do not rewrite RailSystem from scratch.
- Do not remove existing commands.
- Do not remove Minecart/Wrench features.
- Do not change package names.
- Do not add external libraries.
- Do not edit generated files in `build/` or `target_*`.
- Do not use destructive git commands.
- Do not reset or revert unrelated files.

## Build Verification

After changes, run:

```sh
./gradlew makeMainOfflineDownload
```

Required result:

```text
BUILD SUCCESSFUL
```

## Manual Test Checklist

Run in game:

```text
/railsys clear
/railsys testline 60
/railsys spawntrain 4
```

Expected:

- 4 cars appear.
- Cars are stopped.
- Cars are near 15, 12, 9, 6 blocks from segment start.
- Cars do not move before `/railsys start`.

Then run:

```text
/railsys start
```

Expected:

- Train starts very slowly.
- Cars stay in formation.
- Rear cars do not lag behind.
- HUD updates while riding.

Then run:

```text
/railsys stop
```

Expected:

- Train slows/stops.
- Formation remains stable.

