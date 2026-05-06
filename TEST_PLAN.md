# Test Plan

Audience:

```text
Small local LLMs such as GPT-OSS 20B or Llama 3.1 8B.
```

Current point:

```text
Phase 6-F-FIX3 is next.
```

Use this test plan after any RailSystem or train bugfix.

## Build Test

Run:

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

If build fails with:

```text
java.net.SocketException: Operation not permitted
```

that is a sandbox permission problem. It is not a Java compile error.
Run the same command with permission to run Gradle outside the sandbox.

## Phase 6-F-FIX3 Manual Test

### Test 1: Basic Spawn

Commands:

```text
/railsys clear
/railsys testline 60
/railsys spawntrain 4
```

Expected:

- Four `EntityRailVehicle` cars appear.
- Train is not moving.
- HUD is not required unless riding.
- Cars appear near these distances from the testline start:

```text
car 0: 15 blocks
car 1: 12 blocks
car 2: 9 blocks
car 3: 6 blocks
```

Failure signs:

- Train moves immediately after spawn.
- Cars spawn at the same position.
- Cars spawn far away from the rail.
- Cars spawn outside the 60 block testline.

Files to inspect on failure:

```text
src/game/java/net/minecraft/command/CommandRailSystem.java
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
```

Methods to inspect:

```java
CommandRailSystem.spawnTrain(...)
CommandRailSystem.getInitialTrainCarProgress(...)
EntityRailVehicle.setRailProgress(...)
EntityRailVehicle.updateRailPosition(...)
```

### Test 2: Slow Start

Commands:

```text
/railsys start
```

Expected:

- Train starts slowly.
- Lead car does not jump forward.
- Rear cars do not lag behind.
- Cars remain in formation.

Expected speed:

```text
targetSpeed should be 0.005D after /railsys start
```

Files to inspect on failure:

```text
src/game/java/net/minecraft/command/CommandRailSystem.java
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
```

Methods to inspect:

```java
CommandRailSystem.setTrainSpeed(...)
EntityRailVehicle.setTargetSpeed(...)
EntityRailVehicle.updateSpeedTowardsTarget(...)
EntityRailVehicle.updateLeadProgress(...)
```

### Test 3: Follower Formation Lock

Setup:

```text
/railsys clear
/railsys testline 60
/railsys spawntrain 4
/railsys start
```

Observe for at least 20 seconds.

Expected:

- Follower cars stay locked to lead car.
- Rear cars do not get left behind.
- Cars do not overlap.
- Formation spacing remains stable.

Important expected implementation:

```text
Follower cars must use lead car segmentId and progress.
Follower cars must not self-drive.
carSpacing must be converted from block distance to progress using segment.getLength().
```

Files to inspect on failure:

```text
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
```

Methods to inspect:

```java
EntityRailVehicle.onUpdate()
EntityRailVehicle.findLeadCar()
EntityRailVehicle.updateRailPosition()
```

### Test 4: Stop

Commands:

```text
/railsys stop
```

Expected:

- Lead car target speed becomes `0.0D`.
- Train slows/stops.
- Follower cars remain aligned while stopping.

Files to inspect on failure:

```text
src/game/java/net/minecraft/command/CommandRailSystem.java
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
```

### Test 5: Speed Command

Commands:

```text
/railsys speed 0.005
/railsys speed 0.01
/railsys speed 0
```

Expected:

- Train responds to target speed changes.
- Actual speed is clamped by `maxSpeed`.
- Followers remain locked to lead car.

Do not expect speed above:

```text
EntityRailVehicle.maxSpeed = 0.02D
```

### Test 6: Riding and HUD

Steps:

1. Right click an `EntityRailVehicle`.
2. Confirm player mounts vehicle.
3. Run `/railsys start`.
4. Watch HUD.

Expected HUD:

```text
RailVehicle
speed: <value>
target: <value>
trainId: <id>
```

Expected riding behavior:

- Player stays on vehicle.
- Player is not inside the model.
- Player does not get left behind.

Files to inspect on failure:

```text
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
src/game/java/net/minecraft/client/gui/GuiIngame.java
```

Methods to inspect:

```java
EntityRailVehicle.interactFirst(...)
EntityRailVehicle.updateRiderPosition()
EntityRailVehicle.getMountedYOffset()
GuiIngame.renderRailVehicleHud()
```

### Test 7: Clear Removes Vehicles

Commands:

```text
/railsys clear
```

Expected:

- Marker blocks are removed.
- RailGraph is cleared.
- `EntityRailVehicle` entities should be removed.
- After waiting several seconds, old train cars should not reappear.

Known requirement:

```text
If clear does not remove EntityRailVehicle, fix CommandRailSystem.clear behavior.
```

Files to inspect on failure:

```text
src/game/java/net/minecraft/command/CommandRailSystem.java
```

Methods to inspect:

```java
CommandRailSystem.processCommand(...)
CommandRailSystem.clearDebugMarkers(...)
```

## Regression Tests

Run these after Phase 6-F-FIX3:

```text
/railsys clear
/railsys testcurve
/railsys spawntrain 4
/railsys start
/railsys stop
```

Expected:

- No crash.
- Curve segment still works.
- Train still renders.

Run:

```text
/railsys addcar
/railsys removecar
/railsys unlink
```

Expected:

- Commands do not crash.
- Lead car remains valid unless unlinked.

Run:

```text
/railsys station
/railsys route <nodeId>
/railsys switch
```

Expected:

- Commands do not crash.
- Invalid node or invalid segment gives an error message, not a crash.

## General Failure Guide

If the train does not move:

Inspect:

```java
CommandRailSystem.setTrainSpeed(...)
EntityRailVehicle.targetSpeed
EntityRailVehicle.updateSpeedTowardsTarget()
EntityRailVehicle.updateLeadProgress()
```

If follower cars drift:

Inspect:

```java
EntityRailVehicle.onUpdate()
EntityRailVehicle.findLeadCar()
RailSegment.getLength()
```

If HUD does not show:

Inspect:

```java
GuiIngame.renderRailVehicleHud()
EntityRailVehicle DataWatcher values
```

If build fails:

Inspect the first Java compile error. Do not guess.

## Forbidden During Testing

Do not:

- Do not edit generated files.
- Do not use `git reset`.
- Do not delete user files.
- Do not change unrelated Minecart files.
- Do not change command names.

