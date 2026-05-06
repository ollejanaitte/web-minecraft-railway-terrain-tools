# RailSystem Architecture

Audience:

```text
Small local LLMs such as GPT-OSS 20B or Llama 3.1 8B.
```

Current point:

```text
Phase 6-F-FIX3 is next.
```

This document explains the custom RailSystem architecture.
It is for safe maintenance and bugfix work.

Do not edit Java code while reading this document.

## 1. Overall Summary

RailSystem is a custom railway system.

Important:

```text
RailSystem is independent from Minecraft Minecart.
```

Do not confuse these systems:

- Minecart uses vanilla minecart classes.
- RailSystem uses custom graph classes and `EntityRailVehicle`.

RailSystem basics:

- `RailGraph` manages RailNodes and RailSegments.
- `RailNode` is a control point in the world.
- `RailSegment` connects two RailNodes.
- `RailCurveData` stores curve control points.
- `EntityRailVehicle` moves using `segmentId + progress`.
- `RailWand` and `/railsys` create or edit RailGraph data.

Vehicle position model:

```text
EntityRailVehicle.segmentId = current segment
EntityRailVehicle.progress = position on that segment
```

Progress meaning:

```text
0.0 = segment start node
1.0 = segment end node
```

## 2. Main Classes

### RailNode

File:

```text
src/game/java/net/minecraft/rail/RailNode.java
```

Purpose:

- Stores one graph node.
- Stores node position.
- Stores connected node ids.

### RailSegment

File:

```text
src/game/java/net/minecraft/rail/RailSegment.java
```

Purpose:

- Stores one rail segment.
- Connects a start RailNode to an end RailNode.
- Calculates position on straight or curved rail.

### RailCurveData

File:

```text
src/game/java/net/minecraft/rail/RailCurveData.java
```

Purpose:

- Stores cubic Bezier curve control points.
- Calculates curve points.
- Saves and loads curve data through NBT.

### RailSegmentType

File:

```text
src/game/java/net/minecraft/rail/RailSegmentType.java
```

Purpose:

- Enum for segment type.

Expected values:

```text
STRAIGHT
CURVE
SLOPE
SWITCH
```

### RailGraph

File:

```text
src/game/java/net/minecraft/rail/RailGraph.java
```

Purpose:

- Owns all RailNodes and RailSegments.
- Stores switch state.
- Stores train route targets.
- Stores station nodes.
- Stores temporary occupied segment ids.

### RailPosition

File:

```text
src/game/java/net/minecraft/rail/RailPosition.java
```

Purpose:

- Small value object.
- Stores `segmentId` and `progress`.

### RailSystemManager

File:

```text
src/game/java/net/minecraft/rail/RailSystemManager.java
```

Purpose:

- Gets RailGraph for a World.
- Marks RailSystem data dirty.
- Provides fallback global graph.

### RailSystemSavedData

File:

```text
src/game/java/net/minecraft/rail/RailSystemSavedData.java
```

Purpose:

- Saves and loads RailGraph using world saved data.

## 3. RailNode Spec

Main fields:

```java
private final int id;
private final double x;
private final double y;
private final double z;
private final List<Integer> connectedNodeIds;
```

Meaning:

- `id`: unique node id in the RailGraph.
- `x/y/z`: world position.
- `connectedNodeIds`: ids of directly connected RailNodes.

Important methods:

```java
getId()
getX()
getY()
getZ()
connectTo(int nodeId)
disconnectFrom(int nodeId)
getConnectedNodeIds()
distanceSqTo(RailNode other)
writeToNBT(NBTTagCompound nbt)
static RailNode readFromNBT(NBTTagCompound nbt)
```

Rules:

- Do not manually edit `connectedNodeIds` outside RailNode methods.
- Use `connectTo()` and `disconnectFrom()`.
- Do not create duplicate node ids.
- Do not remove NBT keys.

NBT saves:

```text
Id
X
Y
Z
ConnectedNodeIds
```

## 4. RailSegment Spec

Main fields:

```java
private final int id;
private final int startNodeId;
private final int endNodeId;
private double length;
private final RailSegmentType type;
private RailCurveData curveData;
```

Meaning:

- `id`: unique segment id in the RailGraph.
- `startNodeId`: id of start node.
- `endNodeId`: id of end node.
- `length`: segment length in blocks.
- `type`: segment type.
- `curveData`: curve data, usually only for `CURVE`.

Important methods:

```java
getId()
getStartNodeId()
getEndNodeId()
getLength()
getType()
getCurveData()
setCurveData(RailCurveData data)
hasCurveData()
getPoint(double t, RailNode start, RailNode end)
recalculateLength(RailNode start, RailNode end)
writeToNBT(NBTTagCompound nbt)
static RailSegment readFromNBT(NBTTagCompound nbt)
```

### STRAIGHT

For `RailSegmentType.STRAIGHT`:

- `curveData` may be null.
- `getPoint(t)` should use linear interpolation between start node and end node.

### CURVE

For `RailSegmentType.CURVE`:

- `curveData` should be non-null.
- `getPoint(t)` should use cubic Bezier curve calculation.
- `recalculateLength()` should approximate curve length by sampling points.

### Segment Start/End Nodes

Segment start and end nodes are important for:

- Vehicle movement direction.
- Segment transition.
- Route decisions.
- Switch decisions.
- Station arrival.

Do not swap `startNodeId` and `endNodeId` casually.

## 5. RailCurveData Spec

File:

```text
src/game/java/net/minecraft/rail/RailCurveData.java
```

RailCurveData stores cubic Bezier control points:

```java
controlX1
controlY1
controlZ1
controlX2
controlY2
controlZ2
```

Bezier curve points:

```text
P0 = start RailNode
P1 = control point 1
P2 = control point 2
P3 = end RailNode
```

Important methods:

```java
getPoint(double t, RailNode start, RailNode end)
writeToNBT(NBTTagCompound nbt)
static RailCurveData readFromNBT(NBTTagCompound nbt)
```

Rules:

- Clamp `t` to `0.0D` to `1.0D`.
- Do not assume control points are block centers.
- Do not remove NBT keys.

## 6. RailGraph Spec

File:

```text
src/game/java/net/minecraft/rail/RailGraph.java
```

Main maps and sets:

```java
private final Map<Integer, RailNode> nodes;
private final Map<Integer, RailSegment> segments;
private final Map<Integer, Integer> switchTargetSegmentByNodeId;
private final Map<Integer, Integer> trainTargetNodeByTrainId;
private final Set<Integer> occupiedSegmentIds;
private final Set<Integer> stationNodeIds;
```

### Node Methods

Important methods:

```java
RailNode createNode(double x, double y, double z)
RailNode createOrReuseNode(double x, double y, double z, double reuseDistance)
RailNode getNode(int id)
Collection<RailNode> getNodes()
RailNode findNearestNode(double x, double y, double z, double maxDistance)
boolean removeNode(int id)
```

Rules:

- Use `createOrReuseNode()` when making connected test lines.
- Node sharing is required for Segment-to-Segment travel.
- If two segments visually touch but do not share a node, vehicles cannot transition normally.

### Segment Methods

Important methods:

```java
RailSegment createSegment(int startNodeId, int endNodeId, RailSegmentType type)
RailSegment createCurveSegment(int startNodeId, int endNodeId, RailCurveData curveData)
RailSegment getSegment(int id)
Collection<RailSegment> getSegments()
List<RailSegment> getSegmentsConnectedToNode(int nodeId)
boolean removeSegment(int id)
```

Rules:

- `createSegment()` connects both nodes.
- `removeSegment()` disconnects both nodes.
- Use `getSegmentsConnectedToNode()` when selecting next segment.
- Do not rely only on `connectedNodeIds` to find a RailSegment.

### Switch State

Field:

```java
switchTargetSegmentByNodeId
```

Methods:

```java
setSwitchTargetSegment(int nodeId, int segmentId)
getSwitchTargetSegment(int nodeId)
clearSwitchTargetSegment(int nodeId)
isValidSwitchTarget(int nodeId, int segmentId)
```

Meaning:

- A node can prefer one outgoing segment.
- Vehicle transition should check switch target before fallback segment selection.

### Train Target Node

Field:

```java
trainTargetNodeByTrainId
```

Methods:

```java
setTrainTargetNode(int trainId, int nodeId)
getTrainTargetNode(int trainId)
clearTrainTargetNode(int trainId)
```

Meaning:

- A train can have a simple target node.
- Route logic can choose the next Segment that gets closer to target node.
- This is not full A* pathfinding.

### Station Nodes

Field:

```java
stationNodeIds
```

Methods:

```java
addStationNode(int nodeId)
removeStationNode(int nodeId)
isStationNode(int nodeId)
```

Meaning:

- A station is currently a node id.
- When lead car reaches a station node, it can stop for a dwell time.

### Occupied Segments

Field:

```java
occupiedSegmentIds
```

Methods:

```java
setSegmentOccupied(int segmentId, boolean occupied)
isSegmentOccupied(int segmentId)
clearOccupiedSegments()
```

Meaning:

- Simple in-memory block occupancy.
- Used to prevent trains from entering occupied segments.

Important:

```text
occupiedSegmentIds is runtime state and should not be permanently saved unless the user explicitly asks.
```

### Clear

Method:

```java
clear()
```

Expected:

- Clears nodes.
- Clears segments.
- Clears switch state.
- Clears route target state.
- Clears occupied segments.
- Clears station nodes.
- Resets next node and segment ids.

After calling `clear()` from a command:

```java
RailSystemManager.markDirty(world);
```

must be called.

### NBT Save

Methods:

```java
writeToNBT(NBTTagCompound nbt)
readFromNBT(NBTTagCompound nbt)
copyFrom(RailGraph other)
```

Saved data:

```text
NextNodeId
NextSegmentId
Nodes
Segments
Switches
TrainTargets
Stations
```

Not saved:

```text
occupiedSegmentIds
```

Do not remove existing NBT keys.

## 7. RailSystemManager Spec

File:

```text
src/game/java/net/minecraft/rail/RailSystemManager.java
```

Important methods:

```java
public static RailGraph getGraphForWorld(World world)
public static void markDirty(World world)
public static RailGraph getGlobalGraph()
public static void clearGlobalGraph()
```

Expected behavior:

- Server World should use `RailSystemSavedData`.
- Client World or failure case may use `GLOBAL_GRAPH` fallback.
- `markDirty(world)` should mark saved data dirty on server side.

Rules:

- Do not modify graph only on client side.
- Graph edits should happen server side.
- After server graph edits, call `RailSystemManager.markDirty(world)`.

Client-side limitation:

```text
Client fallback graph may be stale or incomplete.
Do not rely on client graph changes for permanent data.
```

## 8. RailSystemSavedData Spec

File:

```text
src/game/java/net/minecraft/rail/RailSystemSavedData.java
```

Purpose:

- Stores RailGraph in world save data.
- Loads RailGraph when world is loaded.

Expected saved content:

```text
RailNode data
RailSegment data
RailCurveData inside segments
Switches
TrainTargets
Stations
NextNodeId
NextSegmentId
```

Expected not saved:

```text
occupiedSegmentIds
debug marker block list
EntityRailVehicle runtime occupancy
```

`EntityRailVehicle` saves itself separately through Entity NBT.

Do not store marker block positions in `RailSystemSavedData` unless the user explicitly asks.

## 9. Common Bugs

### Bug: Segment transition does not happen

Cause:

```text
Segments do not share the same RailNode.
```

Fix:

- Use `createOrReuseNode()` when placing adjacent test lines.
- Use `findNearestNode()` with a safe distance.

### Bug: Connected nodes exist but next segment cannot be found

Cause:

```text
connectedNodeIds are node ids, not segment ids.
```

Fix:

- Use `getSegmentsConnectedToNode(nodeId)`.

### Bug: progress calculation breaks

Cause:

```text
RailSegment.length is zero or too small.
```

Fix:

- Check `segment.getLength() > 0.001D`.
- Fallback to safe progress.

### Bug: Graph changes disappear after reload

Cause:

```text
markDirty(world) was not called.
```

Fix:

```java
RailSystemManager.markDirty(world);
```

after graph changes.

### Bug: Client-only graph changes do not persist

Cause:

```text
Graph was modified on worldObj.isRemote == true side.
```

Fix:

- Make graph edits server side.

### Bug: Marker blocks are gone but RailGraph still exists

Cause:

```text
Marker blocks and RailGraph are separate.
```

Fix:

- Clear marker blocks separately.
- Clear RailGraph separately.
- Mark data dirty.

### Bug: RailGraph cleared but vehicles still exist

Cause:

```text
EntityRailVehicle is an Entity and is not automatically removed by RailGraph.clear().
```

Fix:

- `/railsys clear` should remove `EntityRailVehicle` entities separately.
- Do not remove Minecart entities.

## 10. Forbidden Changes

Do not:

- Do not rewrite `RailGraph` from scratch.
- Do not replace RailSystem with Minecart logic.
- Do not edit Minecart classes for RailSystem graph bugs.
- Do not remove existing NBT keys.
- Do not rename `/railsys`.
- Do not rename RailSystem package.
- Do not add external libraries.
- Do not edit generated build or target files.
- Do not make client-side graph edits the source of truth.

## Safe Editing Checklist

Before editing:

- Identify exact file.
- Identify exact method.
- Read nearby code.

During editing:

- Add null checks.
- Keep server/client side rules.
- Keep NBT compatibility.

After editing:

Run build:

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

