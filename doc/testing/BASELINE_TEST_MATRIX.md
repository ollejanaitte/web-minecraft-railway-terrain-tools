# v1 /railsys Baseline Test Matrix

Phase 0. Records CURRENT v1 behavior (NOT an endorsement of correctness).
Known bugs are recorded as KNOWN FAILURE and are NOT fixed in Phase 0.

Status legend: PASS / PASS WITH WARNING / KNOWN FAILURE / NOT TESTABLE YET /
OUT OF SCOPE.
Future owner = Phase that resolves the item.

## Commands (CommandRailSystem, /railsys)

| ID | Scenario | Current v1 behavior | Status | Future owner |
|----|----------|--------------------|--------|--------------|
| BL-CMD-001 | `help` (no args) | prints subcommand list | PASS | - |
| BL-CMD-002 | `clear` | clears graph + occupied + markers + rail vehicles | PASS (note: kills ALL vehicles) | Phase 3 |
| BL-CMD-003 | `testline [length]` | creates straight segment (default 30, 5..100) + markers | PASS | Phase 3 |
| BL-CMD-004 | `testcurve` | creates cubic Bezier (testcurve shape) + markers | PASS | Phase 3 |
| BL-CMD-005 | `testloop` | creates 40x40 loop (4 straight + 4 curve), uncommitted dev | PASS | Phase 2 (course) |
| BL-CMD-006 | `vehicle [progress]` | debug marker at progress on newest segment | PASS | - |
| BL-CMD-007 | `spawnvehicle` | spawns single vehicle at newest segment | PASS | Phase 2 |
| BL-CMD-008 | `spawntrain [count] [spacing]` | spawns N cars (default 3, spacing 3.0m, 0.05..20) | PASS | Phase 2 |
| BL-CMD-009 | `start` | sets targetSpeed 0.005 | PASS | Phase 4 |
| BL-CMD-010 | `stop` | sets targetSpeed 0 | PASS | Phase 4 |
| BL-CMD-011 | `speed <v>` | sets targetSpeed (0..0.05) | PASS | Phase 4 |
| BL-CMD-012 | `addcar` | appends car to nearest train | PASS | Phase 2 |
| BL-CMD-013 | `removecar` | removes tail car | PASS | Phase 2 |
| BL-CMD-014 | `unlink` | splits train into singles | PASS | Phase 2 |
| BL-CMD-015 | `route <nodeId>` | greedy target-node routing | PASS (greedy only) | Phase 5 |
| BL-CMD-016 | `station` | marks nearest node as station (dwell 60 ticks) | PASS | Phase 6 |
| BL-CMD-017 | `switch [segmentId\|clear]` | node->segment switch target | PASS | Phase 3 |
| BL-CMD-018 | tab completion | all 18 subcommand names | PASS | - |

## Rail

| ID | Scenario | Current v1 behavior | Status | Future owner |
|----|----------|--------------------|--------|--------------|
| BL-RAIL-001 | straight segment | lerp, chord length | PASS | Phase 1 |
| BL-RAIL-002 | curve segment | raw-t cubic Bezier, 32-sample length | PASS WITH WARNING (raw t) | Phase 1 |
| BL-RAIL-003 | loop | 8-segment closed loop | PASS | Phase 2 |
| BL-RAIL-004 | node adjacency | connectedNodeIds | PASS | Phase 1 |
| BL-RAIL-005 | route map | trainTargetNodeByTrainId | PASS (greedy) | Phase 5 |
| BL-RAIL-006 | switch map | switchTargetSegmentByNodeId | PASS | Phase 3 |
| BL-RAIL-007 | station set | stationNodeIds | PASS | Phase 6 |
| BL-RAIL-008 | occupancy | per-segment boolean (lead only) | KNOWN FAILURE (coarse) | Phase 5 |
| BL-RAIL-009 | persistence | WorldSavedData "rail_system" graph NBT | PASS | Phase 1 |
| BL-RAIL-010 | curve length accuracy | 32-sample polyline approximation | PASS WITH WARNING | Phase 1 |
| BL-RAIL-011 | SLOPE / SWITCH enum | defined, unused | OUT OF SCOPE | Phase 1/3 |
| BL-RAIL-012 | debug markers | gold/diamond/emerald/redstone/stone/rail/lapis blocks | PASS | Phase 3 (replaced) |

## Vehicle / Train (EntityRailVehicle)

| ID | Scenario | Current v1 behavior | Status | Future owner |
|----|----------|--------------------|--------|--------------|
| BL-TRAIN-001 | follower across segment boundary | teleport/wrap ahead of leader | KNOWN FAILURE | Phase 2 |
| BL-TRAIN-002 | curve spacing | raw-t spacing drift on Bezier | KNOWN FAILURE | Phase 2 |
| BL-TRAIN-003 | yaw at segment end | look-ahead clamps; one-frame yaw snap | KNOWN FAILURE | Phase 2 |
| BL-TRAIN-004 | train ID after restart | static nextTrainId; possible collision with NBT-loaded | KNOWN FAILURE (inferred) | Phase 2 |
| BL-TRAIN-005 | spawn vehicle | newest segment, progress 0 | PASS | Phase 2 |
| BL-TRAIN-006 | spawn train | N cars at 15/12/9/6m spacing | PASS | Phase 2 |
| BL-TRAIN-007 | follower update | copies leader segment+progress each tick (bug source) | KNOWN FAILURE | Phase 2 |
| BL-TRAIN-008 | start/stop/speed | targetSpeed accel/decel | PASS | Phase 4 |
| BL-TRAIN-009 | rider control | moveForward throttle | PASS | Phase 4 |
| BL-TRAIN-010 | station dwell | 60-tick stop at station node | PASS | Phase 6 |
| BL-TRAIN-011 | noClip / no push | canBeCollidedWith true, canBePushed false | PASS | Phase 3 |
| BL-TRAIN-012 | NBT save/load | 14 keys round-trip | PASS | Phase 1/2 |

## Integration

| ID | Scenario | Current v1 behavior | Status | Future owner |
|----|----------|--------------------|--------|--------------|
| BL-INT-001 | command registration | ServerCommandManager registers /railsys (perm 2) | PASS | - |
| BL-INT-002 | entity registration | EntityList "RailVehicle" id 201 | PASS | Phase 2 |
| BL-INT-003 | tracker | range 80, update freq 3, no velocity | PASS WITH WARNING (coarse) | Phase 10 |
| BL-INT-004 | client spawn | S0EPacketSpawnObject type 80 -> setRailSegment | PASS (single-player) | Phase 10 |
| BL-INT-005 | DataWatcher | 9 fields synced | PASS | Phase 10 |
| BL-INT-006 | NBT | graph + vehicle state | PASS | Phase 1 |
| BL-INT-007 | client graph | no rail-graph sync to clients; static shared in single-JVM | KNOWN FAILURE | Phase 10 |
| BL-INT-008 | renderer | RenderRailVehicle cuboids + coupler line | PASS (minimal) | Phase 9 |
| BL-INT-009 | debug render | RenderGlobal F3 overlay | PASS | Phase 9 |
| BL-INT-010 | RailWand | item 433, 4-point Bezier | PASS | Phase 3 |
| BL-STATIC-001 | static global graph | RailSystemManager static mirror (multi-world hazard) | KNOWN FAILURE (design) | Phase 1 |

## Manual (out-of-harness)

| ID | Scenario | Status |
|----|----------|--------|
| BL-MAN-001 | launch build + testloop + spawntrain + start + ride in browser | MANUAL (see MANUAL_BASELINE_CHECKLIST.md) |
| BL-MAN-002 | save/load world with rails + trains | MANUAL |
| BL-MAN-003 | 2-client multiplayer parity | OUT OF SCOPE (client graph absent) |
