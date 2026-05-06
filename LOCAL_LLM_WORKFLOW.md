# Local LLM Workflow

Audience:

```text
Small local LLMs such as GPT-OSS 20B or Llama 3.1 8B.
```

Purpose:

```text
Safe work procedure for the RailSystem project.
```

Current point:

```text
Phase 6-F-FIX3 is next.
```

This file is a workflow document only.
Do not edit Java code unless a separate task asks you to.

## 1. Pre-Work Checklist

Before changing anything:

1. Confirm the task name.
2. Confirm the exact files allowed for the task.
3. Read the existing code around the target method.
4. Read the relevant handoff documents.
5. Do not edit unrelated files.
6. Do not edit generated files.
7. Do not run destructive git commands.

Generated files and directories to avoid:

```text
build/
target_lwjgl_desktop/
target_teavm_javascript/
target_teavm_wasm_gc/
.gradle-local/
```

If a file is not part of the task, do not edit it.

## 2. Recommended Reading Order

Read documents in this order:

1. `LOCAL_LLM_HANDOFF.md`
2. `BUGFIX_TASKS_FOR_LOCAL_LLM.md`
3. `ENTITY_RAIL_VEHICLE_SPEC.md`
4. `COMMAND_RAILSYSTEM_SPEC.md`
5. `RAILSYSTEM_ARCHITECTURE.md`
6. `TEST_PLAN.md`

Do not skip `BUGFIX_TASKS_FOR_LOCAL_LLM.md` when working on Phase 6-F-FIX3.

## 3. Small Change Rules

Follow these rules:

- Fix one problem at a time.
- Do not do large refactors.
- Do not rewrite entire classes.
- Do not rename packages.
- Do not rename commands.
- Do not touch Minecart files.
- Do not rewrite `RailGraph`.
- Do not add external libraries.

Bad change:

```text
Rewrite EntityRailVehicle movement from scratch.
```

Good change:

```text
Change follower spacing formula inside EntityRailVehicle.onUpdate().
```

## 4. Edit Procedure

Use this exact workflow for code edits:

1. Find the target file.
2. Find the target method.
3. Read 30 to 80 lines around the method.
4. Identify the smallest safe change.
5. Add null checks if using objects from graph or world.
6. Check `worldObj.isRemote`.
7. Preserve DataWatcher fields.
8. Preserve NBT keys.
9. Save the file.
10. Build.

Important server/client rule:

```text
Server side changes real game state.
Client side should only render or read synchronized state.
```

If changing RailGraph:

```java
RailSystemManager.markDirty(world);
```

must be called after server-side persistent graph changes.

## 5. Build Procedure

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

If Gradle fails with:

```text
java.net.SocketException: Operation not permitted
```

then Gradle was blocked by sandbox permissions.
Run the same Gradle command with permission outside the sandbox.

Do not call the work complete unless the build succeeds.

## 6. Basic Manual Test Procedure

Run these commands in game:

```text
/railsys clear
/railsys testline 60
/railsys spawntrain 4 0.05
/railsys start
/railsys stop
```

Expected:

- `clear` removes RailGraph data and debug markers.
- `testline 60` creates a visible straight line.
- `spawntrain 4 0.05` creates four stopped vehicles.
- `start` starts the train slowly.
- `stop` stops the train.
- Follower cars stay aligned.

If testing Phase 6-F-FIX3, also test:

```text
/railsys clear
/railsys testline 60
/railsys spawntrain 4
/railsys start
```

Expected:

- Spawn positions are around 15, 12, 9, and 6 blocks from the start.
- Train does not move before `/railsys start`.
- Train moves very slowly after `/railsys start`.
- Rear cars do not lag behind.

## 7. Common Failures

### Failure: Follower cars drive themselves

Cause:

```text
Follower car updates progress using its own speed.
```

Fix:

```text
Follower must copy lead car segmentId and calculate offset from lead progress.
```

Do not call lead movement logic for follower cars.

### Failure: carSpacing uses progress units

Cause:

```text
targetProgress = lead.progress - carSpacing * carIndex
```

This treats `carSpacing` as progress.

Fix:

```text
Treat carSpacing as block distance.
Convert block spacing to progress by dividing by RailSegment.getLength().
```

### Failure: Graph changes do not persist

Cause:

```text
RailSystemManager.markDirty(world) was not called.
```

Fix:

```java
RailSystemManager.markDirty(world);
```

after changing RailGraph on server side.

### Failure: `/railsys clear` does not remove vehicles

Cause:

```text
RailGraph.clear() does not remove EntityRailVehicle entities.
```

Fix:

```text
CommandRailSystem clear logic must remove EntityRailVehicle separately.
```

Do not remove Minecarts.

### Failure: Segment transition does not work

Cause:

```text
Segments do not share RailNode ids.
```

Fix:

```text
Use createOrReuseNode() for adjacent test lines.
Use getSegmentsConnectedToNode() to find connected segments.
```

### Failure: Code tries to get Segment from connectedNodeIds

Cause:

```text
connectedNodeIds contains node ids, not segment ids.
```

Fix:

```java
RailGraph.getSegmentsConnectedToNode(nodeId)
```

## 8. Forbidden Actions

Never do these:

```text
git reset
git checkout -- <file>
rm -rf
delete user files
edit generated files
add external dependencies
rename /railsys
rename packages
rewrite RailGraph
edit Minecart files for RailSystem bugs
```

Forbidden unrelated edits:

```text
src/game/java/net/minecraft/entity/item/EntityMinecart.java
src/game/java/net/minecraft/entity/item/EntityMinecartEmpty.java
src/game/java/net/minecraft/entity/item/EntityMinecartContainer.java
src/game/java/net/minecraft/entity/item/EntityMinecartFurnace.java
src/game/java/net/minecraft/entity/item/EntityMinecartHopper.java
src/game/java/net/minecraft/entity/EntityMinecartCommandBlock.java
```

Only touch those files if the user explicitly asks for Minecart work.

## 9. Phase 6-F-FIX3 Task Scope

For Phase 6-F-FIX3, do only these things:

### 1. Start Speed

Confirm current start speed:

```java
/railsys start -> targetSpeed = 0.005D
```

Do not increase it.

### 2. Follower Formation Lock

Fix follower spacing to use block distance.

Target logic:

```java
RailGraph graph = RailSystemManager.getGraphForWorld(this.worldObj);
RailSegment segment = graph.getSegment(leadCar.segmentId);
double segmentLength = segment != null ? segment.getLength() : 0.0D;
double progressSpacing = segmentLength > 0.001D ? this.carSpacing / segmentLength : this.carSpacing;
double targetProgress = leadCar.progress - progressSpacing * (double)this.carIndex;
```

Then wrap targetProgress.

Follower must keep:

```java
this.segmentId = leadCar.segmentId;
this.speed = leadCar.speed;
this.targetSpeed = leadCar.targetSpeed;
```

### 3. Clear Removes Vehicles

`/railsys clear` should remove active `EntityRailVehicle` entities.

Rules:

- Remove only `EntityRailVehicle`.
- Do not remove Minecarts.
- Clear RailGraph.
- Clear marker blocks.
- Call `RailSystemManager.markDirty(world)`.

### 4. Prevent Reappearance After Clear

If `EntityRailVehicle` remains after clear, it may be saved and restored.
Make sure clear calls `setDead()` on matching vehicles server-side.

## 10. Expected Work Cycle

Use this cycle every time:

1. Read the task.
2. Read the relevant spec file.
3. Read the Java method.
4. Make one small change.
5. Build.
6. Run manual test.
7. Record result.
8. Move to next small change only after success.

Do not combine unrelated changes.

Do not say work is complete without:

```text
BUILD SUCCESSFUL
```

