# CommandRailSystem Spec

Audience:

```text
Small local LLMs such as GPT-OSS 20B or Llama 3.1 8B.
```

Current point:

```text
Phase 6-F-FIX3 is next.
```

This is a command specification document only.

## Source File

Main file:

```text
src/game/java/net/minecraft/command/CommandRailSystem.java
```

Do not move this class.
Do not rename `/railsys`.

## Command List

Existing commands:

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
/railsys switch
/railsys switch <segmentId>
/railsys switch clear
```

Do not remove any command.
Do not change command names.

## `/railsys clear`

Purpose:

- Clear RailSystem graph.
- Remove debug marker blocks.
- Should remove active `EntityRailVehicle` entities.

Current expected behavior:

```text
RailSystem graph cleared, removed markers: X
```

Important requirement:

```text
clear should also remove EntityRailVehicle instances so old trains do not reappear or remain in the world.
```

If this is not implemented yet, it is a known bug/future bugfix task.

Do not remove Minecart entities.
Only remove `EntityRailVehicle`.

## `/railsys testline [length]`

Purpose:

- Create a straight RailSegment for testing.

Arguments:

```text
length: optional integer, 5 to 100
default: 30
```

Expected behavior:

- Creates a straight segment from player position in +X direction.
- Reuses nearby start/end RailNode when possible.
- Places visible marker blocks and rail blocks.
- Marks RailSystem data dirty.

Do not change:

- Default length behavior unless user asks.
- Node reuse behavior.

## `/railsys testcurve`

Purpose:

- Create a curve RailSegment for testing.

Expected behavior:

- Creates start node.
- Creates end node.
- Creates `RailCurveData`.
- Creates curve segment.
- Places marker blocks.
- Marks RailSystem data dirty.

Do not remove this command.

## `/railsys vehicle [progress]`

Purpose:

- Place a visible block marker at a position on the selected segment.

Arguments:

```text
progress: optional double
default: 0.5
```

Expected behavior:

- Selects curve segment if present, otherwise newest segment.
- Places marker block at `RailSegment.getPoint(progress, start, end)`.

## `/railsys spawnvehicle`

Purpose:

- Spawn one `EntityRailVehicle`.

Expected behavior:

- Uses selected segment.
- Spawned single vehicle can move normally.
- Does not create a train formation.

## `/railsys spawntrain [count] [spacing]`

Purpose:

- Spawn a train formation.

Arguments:

```text
count: optional integer, 1 to 8, default 3
spacing: optional double, 0.05 to 0.4, default 0.2
```

Current expected spawn state:

```text
All cars speed = 0.0D
All cars targetSpeed = 0.0D
Train does not move until /railsys start
```

Important current initial placement:

```text
carIndex 0: 15 blocks from segment start
carIndex 1: 12 blocks from segment start
carIndex 2: 9 blocks from segment start
carIndex 3: 6 blocks from segment start
```

Implementation requirement:

Method:

```java
private double getInitialTrainCarProgress(RailSegment segment, int carIndex)
```

Should calculate:

```java
double distanceFromStart = 15.0D - 3.0D * (double) carIndex;
double progress = distanceFromStart / segment.getLength();
```

Then clamp to `0.0D` to `1.0D`.

Do not break the 15/12/9/6 block initial placement.

Important for Phase 6-F-FIX3:

```text
The spacing argument should be treated as block distance for runtime formation spacing.
```

Do not treat `spacing` as progress spacing in new code.

## `/railsys start`

Purpose:

- Start controlled train.

Expected behavior:

```java
targetSpeed = 0.005D;
```

Target train selection:

- If player is riding `EntityRailVehicle`, control that train.
- Otherwise control nearest lead car within range.

Do not increase default start speed unless user asks.

## `/railsys stop`

Purpose:

- Stop controlled train.

Expected behavior:

```java
targetSpeed = 0.0D;
```

Do not delete the train.

## `/railsys speed <value>`

Purpose:

- Set target speed for controlled train.

Arguments:

```text
value: double, 0.0 to 0.05
```

Important:

- Entity `maxSpeed` may clamp actual speed lower.
- Current `EntityRailVehicle.maxSpeed = 0.02D`.

## `/railsys addcar`

Purpose:

- Add one car to the tail of the controlled train.

Expected behavior:

- New car gets same `trainId`.
- New car gets next `carIndex`.
- Existing trainLength values update.
- New car should be follower.

Do not create a new trainId.

## `/railsys removecar`

Purpose:

- Remove last tail car from controlled train.

Expected behavior:

- Does not remove lead car.
- Updates trainLength.

Do not remove all cars.

## `/railsys unlink`

Purpose:

- Break train formation.

Expected behavior:

- Reset train formation fields.
- Stop affected cars.
- Clear route target for old train id.

## `/railsys route <nodeId>`

Purpose:

- Set target node for controlled train.

Arguments:

```text
nodeId: integer, must exist in RailGraph
```

Expected behavior:

- Train chooses a Segment that moves closer to target node when possible.
- This is not full A* pathfinding.

## `/railsys station`

Purpose:

- Register nearest RailNode as station.

Expected behavior:

- Finds nearest node within search distance.
- Adds station marker.
- On arrival, lead car stops for about 3 seconds.
- Then it resumes.

## `/railsys switch`

Purpose:

- Show nearest node switch status.

Expected behavior:

- Lists connected segments.
- Shows selected segment.

## `/railsys switch <segmentId>`

Purpose:

- Set switch target segment for nearest node.

Expected behavior:

- Only accepts segment connected to the node.
- Invalid segment gives error.

## `/railsys switch clear`

Purpose:

- Clear switch target for nearest node.

## Forbidden Changes

Do not:

- Do not remove commands.
- Do not rename commands.
- Do not edit Minecart commands.
- Do not change `/railsys spawntrain` initial placement 15/12/9/6 unless user asks.
- Do not make `/railsys clear` remove non-RailSystem user builds except recorded markers and `EntityRailVehicle`.
- Do not add external dependencies.

