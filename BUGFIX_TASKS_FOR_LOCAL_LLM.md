# Bugfix Tasks For Local LLM

This file describes the next work item only.

Current point:

```text
Phase 6-F-FIX3 is next.
```

Do not implement future features unless the user explicitly asks.

## Task Summary

Fix train formation spacing so rear cars never drift or appear incorrectly spaced.

The likely problem is that `carSpacing` is currently treated as a progress value.
For long or short segments, progress spacing does not equal block spacing.

The requested train should feel stable and practical:

- Spawned train starts stopped.
- `/railsys start` starts very slowly.
- All cars stay locked to the lead car.
- Cars should be separated by a fixed block distance, not a progress percentage.

## Files To Edit

Edit only:

```text
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
src/game/java/net/minecraft/command/CommandRailSystem.java
```

Do not edit:

```text
src/game/java/net/minecraft/rail/RailGraph.java
src/game/java/net/minecraft/client/gui/GuiIngame.java
src/game/java/net/minecraft/client/renderer/entity/RenderRailVehicle.java
```

unless the user explicitly asks.

## Current Relevant Code

### EntityRailVehicle follower logic

File:

```text
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
```

Find this section in `onUpdate()`:

```java
} else {
    EntityRailVehicle leadCar = this.findLeadCar();
    if (leadCar != null) {
        this.segmentId = leadCar.segmentId;
        this.trainLength = leadCar.trainLength;
        this.carSpacing = leadCar.carSpacing;
        this.forward = leadCar.forward;
        this.speed = leadCar.speed;
        this.targetSpeed = leadCar.targetSpeed;
        double targetProgress = leadCar.progress - this.carSpacing * (double) this.carIndex;
        while (targetProgress < 0.0D) {
            targetProgress += 1.0D;
        }
        while (targetProgress > 1.0D) {
            targetProgress -= 1.0D;
        }
        this.progress = targetProgress;
    }
}
```

Problem:

```java
lead.progress - carSpacing * carIndex
```

uses progress spacing. It should use block-distance spacing.

## Required Fix

Change follower spacing to use `RailSegment.getLength()`.

Expected formula:

```java
RailGraph graph = RailSystemManager.getGraphForWorld(this.worldObj);
RailSegment segment = graph.getSegment(leadCar.segmentId);
double segmentLength = segment != null ? segment.getLength() : 0.0D;
double progressSpacing = segmentLength > 0.001D ? this.carSpacing / segmentLength : this.carSpacing;
double targetProgress = leadCar.progress - progressSpacing * (double)this.carIndex;
```

Then wrap:

```java
while (targetProgress < 0.0D) targetProgress += 1.0D;
while (targetProgress > 1.0D) targetProgress -= 1.0D;
this.progress = targetProgress;
```

Important:

- Keep `this.segmentId = leadCar.segmentId`.
- Keep `this.speed = leadCar.speed`.
- Keep `this.targetSpeed = leadCar.targetSpeed`.
- Keep `findLeadCar()` `!isDead` check.
- Do not make followers update progress using their own speed.

## Recommended Rename Or Comment

Do not rename fields unless necessary.

Add a comment near follower logic:

```java
// carSpacing is stored in blocks for formation spacing.
```

This helps future models avoid treating `carSpacing` as progress.

## spawntrain Requirements

File:

```text
src/game/java/net/minecraft/command/CommandRailSystem.java
```

Current intended spawn positions:

```text
car 0: 15 blocks from segment start
car 1: 12 blocks from segment start
car 2: 9 blocks from segment start
car 3: 6 blocks from segment start
```

Do not break this.

`getInitialTrainCarProgress(RailSegment segment, int carIndex)` should continue to use:

```java
distanceFromStart = 15.0D - 3.0D * carIndex;
progress = distanceFromStart / segment.getLength();
```

If `spawntrain [count] [spacing]` is used:

- Keep accepting the spacing argument.
- Treat `spacing` as block spacing for runtime formation.
- Default spacing may remain `0.2D` only if the user did not ask to change it.
- If spacing default is too small in game, ask or wait for next task. Do not guess a new default unless instructed.

## Speed Requirements

Do not change unless specifically requested:

```java
targetSpeed for /railsys start = 0.005D
maxSpeed = 0.02D
acceleration = 0.0005D
spawntrain speed = 0.0D
spawntrain targetSpeed = 0.0D
```

## Manual Test

Run:

```text
/railsys clear
/railsys testline 60
/railsys spawntrain 4
```

Expected:

- Train appears stopped.
- Cars appear near 15, 12, 9, 6 blocks from start.

Run:

```text
/railsys start
```

Expected:

- Train starts slowly.
- Rear cars stay locked to lead car.
- No rear car is left behind.
- Formation spacing remains visually stable.

Run:

```text
/railsys stop
```

Expected:

- Train stops.
- Formation remains stable.

## Build Test

Run:

```sh
cd /Users/user/Desktop/Test_v1_Eaglercraft1.8_workspace
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=.gradle-local
./gradlew makeMainOfflineDownload
```

Required:

```text
BUILD SUCCESSFUL
```

## Forbidden Changes

Do not:

- Do not edit Minecart files.
- Do not rewrite RailGraph.
- Do not remove DataWatcher fields.
- Do not remove `/railsys` commands.
- Do not change package names.
- Do not edit generated target/build output.
- Do not add new dependencies.
- Do not use git reset or destructive commands.

