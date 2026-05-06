# EntityRailVehicle Spec

Audience:

```text
Small local LLMs such as GPT-OSS 20B or Llama 3.1 8B.
```

Current point:

```text
Phase 6-F-FIX3 is next.
```

This is a specification document only. Do not edit Java code unless a separate task asks you to.

## Source File

Main file:

```text
src/game/java/net/minecraft/entity/item/EntityRailVehicle.java
```

Do not move this class.
Do not rename the package.

## Class Responsibility

`EntityRailVehicle` is the custom train vehicle Entity for the RailSystem.

It is responsible for:

- Holding current rail position.
- Moving on a `RailSegment`.
- Acting as either a lead car or follower car.
- Supporting train formations.
- Supporting player riding.
- Syncing train state to clients with DataWatcher.
- Saving train state to NBT.

It is not responsible for:

- Creating RailNodes.
- Creating RailSegments.
- Placing marker blocks.
- Parsing `/railsys` commands.
- Rendering the model.
- Minecart behavior.

## Rail Position Fields

These fields define where the vehicle is on the RailSystem:

```java
public int segmentId = -1;
public double progress = 0.0D;
public boolean forward = true;
```

Meaning:

- `segmentId`: id of the current `RailSegment`.
- `progress`: position on the segment.
- `progress = 0.0D`: start node.
- `progress = 1.0D`: end node.
- `forward = true`: moves from start node to end node.
- `forward = false`: moves from end node to start node.

Rules:

- `progress` must stay in `0.0D` to `1.0D`, or be wrapped safely.
- Always null-check `RailSegment`, start node, and end node.
- Do not teleport to unrelated coordinates.

## Speed Fields

These fields control train speed:

```java
public double speed = 0.0D;
public double targetSpeed = 0.0D;
public double maxSpeed = 0.02D;
public double acceleration = 0.0005D;
```

Meaning:

- `speed`: actual current progress-per-tick speed.
- `targetSpeed`: desired speed.
- `maxSpeed`: maximum allowed speed.
- `acceleration`: amount used to move `speed` toward `targetSpeed`.

Rules:

- `speed` must be clamped to `0.0D <= speed <= maxSpeed`.
- `targetSpeed` must be clamped to `0.0D <= targetSpeed <= maxSpeed`.
- `/railsys start` currently sets `targetSpeed = 0.005D`.
- `/railsys stop` sets `targetSpeed = 0.0D`.
- `/railsys speed <value>` accepts `0.0D` to `0.05D`, but `EntityRailVehicle.maxSpeed` still clamps actual speed to `0.02D`.

## Train Formation Fields

These fields define train formation behavior:

```java
public int trainId = -1;
public int carIndex = 0;
public int trainLength = 1;
public double carSpacing = 0.2D;
public boolean isLeadCar = true;
```

Meaning:

- `trainId`: id shared by all cars in the same train.
- `carIndex`: car order in train.
- `carIndex = 0`: lead car.
- `carIndex > 0`: follower car.
- `trainLength`: total number of cars.
- `carSpacing`: spacing between cars.
- `isLeadCar`: true only for the lead car.

Important rule for Phase 6-F-FIX3:

```text
carSpacing must be treated as block distance, not progress distance.
```

Known current bug before Phase 6-F-FIX3:

```java
targetProgress = lead.progress - carSpacing * carIndex;
```

This uses progress spacing. It is wrong for long or short segments.

Expected FIX3 logic:

```java
RailGraph graph = RailSystemManager.getGraphForWorld(this.worldObj);
RailSegment segment = graph.getSegment(leadCar.segmentId);
double segmentLength = segment != null ? segment.getLength() : 0.0D;
double progressSpacing = segmentLength > 0.001D ? this.carSpacing / segmentLength : this.carSpacing;
double targetProgress = leadCar.progress - progressSpacing * (double)this.carIndex;
```

Then wrap `targetProgress`:

```java
while (targetProgress < 0.0D) {
    targetProgress += 1.0D;
}
while (targetProgress > 1.0D) {
    targetProgress -= 1.0D;
}
this.progress = targetProgress;
```

## Lead Car Behavior

A lead car is:

```java
isLeadCar == true
carIndex == 0
```

Lead car behavior:

- Reads rider input.
- Updates `targetSpeed`.
- Moves `speed` toward `targetSpeed`.
- Updates its own `progress`.
- Handles Segment end transition.
- Handles route and station behavior.

Lead car may update:

```java
speed
targetSpeed
segmentId
progress
forward
```

## Follower Car Behavior

A follower car is:

```java
isLeadCar == false
carIndex > 0
```

Follower hard rules:

- Follower must not self-drive.
- Follower must not add `speed` to `progress`.
- Follower must not call `updateLeadProgress()`.
- Follower must follow lead car state every tick.

Follower should copy:

```java
this.segmentId = leadCar.segmentId;
this.trainLength = leadCar.trainLength;
this.carSpacing = leadCar.carSpacing;
this.forward = leadCar.forward;
this.speed = leadCar.speed;
this.targetSpeed = leadCar.targetSpeed;
```

Follower should calculate only its formation offset from the lead car.

If `findLeadCar()` returns null:

```text
Do nothing. Keep current state.
```

## Lead Search

Method:

```java
private EntityRailVehicle findLeadCar()
```

Required checks:

```java
vehicle != this
!vehicle.isDead
vehicle.trainId == this.trainId
vehicle.carIndex == 0
```

Do not return dead vehicles.
Do not return another follower.

## Riding

Relevant methods:

```java
public boolean interactFirst(EntityPlayer playerIn)
public void updateRiderPosition()
public double getMountedYOffset()
```

Expected behavior:

- Right click mounts the player.
- Server side performs mount.
- One player per vehicle is enough.
- If another player is riding, return true and do not replace rider.
- Rider should sit above the vehicle and not inside the model.

Do not implement:

- GUI driving controls.
- Multi-seat support.
- Door logic.

## DataWatcher Sync

DataWatcher fields currently include:

```java
DW_TRAIN_ID
DW_CAR_INDEX
DW_TRAIN_LENGTH
DW_CAR_SPACING
DW_IS_LEAD_CAR
DW_TARGET_SPEED
DW_SPEED
DW_SEGMENT_ID
DW_PROGRESS
```

Expected synced values:

```text
trainId
carIndex
trainLength
carSpacing
isLeadCar
targetSpeed
speed
segmentId
progress
```

Rules:

- Server updates DataWatcher.
- Client reads DataWatcher.
- Client should not simulate independent movement that fights server sync.

## NBT Save

NBT keys include:

```text
SegmentId
Progress
Speed
TargetSpeed
MaxSpeed
Acceleration
TrainId
CarIndex
TrainLength
CarSpacing
IsLeadCar
Forward
StopAtDistanceEnabled
TraveledDistance
StopAtDistance
StationDwellTicks
StationResumeTargetSpeed
VehicleType
```

Do not remove these keys.
Adding new keys is allowed only if required by the task.

## Known Bugs Before Phase 6-F-FIX3

Known issue:

```text
Follower spacing uses progress units instead of block units.
```

Symptom:

- Cars may appear too close or too far depending on Segment length.
- Formation may not match expected block spacing.

Expected fix:

- Convert block spacing to progress using `RailSegment.getLength()`.

## Forbidden Changes

Do not:

- Do not edit Minecart classes.
- Do not remove DataWatcher fields.
- Do not remove NBT keys.
- Do not make follower cars self-drive.
- Do not change package name.
- Do not replace EntityRailVehicle with a new class.
- Do not add external libraries.

