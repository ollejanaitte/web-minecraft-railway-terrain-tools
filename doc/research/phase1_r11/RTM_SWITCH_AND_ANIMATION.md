# R11-B: RTM Rail Network / Connection / Switch / Switch Animation

Phase 1-R11 deliverable. Builds on SRC-1 (RTM 1.7.10.46 jar javap), SRC-2
(NR01 pack renderer scripts), SRC-3 (prior phase 0.7 switch research), SRC-9
(faithful port source facts: field names / NBT keys / constants).

Legend: OBSERVED / KNOWN / INFERRED / UNKNOWN / VERSION-SPECIFIC.

---

## 1. Rail network concepts (RTM)

### 1.1 Endpoint / node model

RTM does NOT maintain a global graph. Instead:

- `RailPosition` = one endpoint of a rail (block coords + double pos + direction
  byte + height byte + anchor fields + switchType byte). It is the persistable
  unit (NBT read/write).
- `RailMap` = the geometry BETWEEN two RailPositions (startRP/endRP), built by
  `RailMaker` from a list of RailPositions; stores a `rails` block list
  (`List<int[]>`), `length`, and sample APIs (getRailPos/getRailHeight/
  getRailRotation/getRailPitch).
- Connection is implicit: adjacent rails share a RailPosition/block; `RailMap
  .canConnect(RailMap)` checks compatibility. No explicit node id registry.

Evidence: SRC-1 (javap RailPosition, RailMap, RailMaker), SRC-9. Class: KNOWN.

### 1.2 Segment concept

- A placed rail = a `TileEntityLargeRailCore` (+ base/slope/switch variants)
  holding `RailPosition[] railPositions` + the rebuilt `RailMap`.
- `RailProperty` = appearance (railModel string, block, blockMetadata,
  blockHeight float) — persisted in NBT. Gauge is NOT in RailProperty; it is
  baked into model + RailMap geometry.

Evidence: SRC-1 (TileEntityLargeRailCore, RailProperty). Class: KNOWN.

### 1.3 Direction / gradient / cant continuity

- RailMap stores start/end RailPositions; `createLine()` builds the curve;
  `canConnect` validates compatibility. Exact C0/C1/C2 continuity rules at
  joins: UNKNOWN (Railsys F2 defines its own: pos <= 1e-4, angle <= 0.5 deg).
- Slope: `RailMapSlope` overrides pos/height/rotation/pitch with a `slopeType`
  byte. Cant: per-RailPosition fields (C_Center/C_Edge/C_Random) — continuity
  across a join NOT explicitly documented.

Evidence: SRC-1, SRC-9. Class: KNOWN (fields), continuity INFERRED.

### 1.4 Gauge compatibility

- No explicit gauge field in RTM rail data/pack JSON. Gauge is fixed by the
  model + HALF_GAUGE in renderer scripts (0.7175 = 1435mm half). Mixed-gauge
  networks rely on matching models; mismatches are a content/authoring concern,
  not a system check.

Evidence: SRC-2 (HALF_GAUGE=0.7175), SRC-9 (no gauge key). Class: KNOWN.

## 2. Switch (turnout) model

### 2.1 Creation

- Place a BLUE branch marker at the branch point + RED markers at endpoints;
  right-click the blue marker -> switch rail generated with multiple connected
  branches sharing the root RailPosition.
- `RailMaker.getSwitch()` returns a `SwitchType` built from the marker list.

Evidence: SRC-3 (SWITCH_JUNCTION_RESEARCH), SRC-5, SRC-1 (RailMaker). Class:
KNOWN.

### 2.2 SwitchType taxonomy (RTM 1.7.10.46)

| id | Class | Japanese/common name | Behaviour |
|----|-------|----------------------|-----------|
| 0 | SwitchBasic | Simple / 片開き (Y turnout) | 1 root -> through + branch; RS toggles route |
| 1 | SwitchSingleCross | Crossover / 渡り線 | middle crossover rail opened by RS |
| 2 | SwitchScissorsCross | Scissors Crossing / シーサスクロッシング | middle rails open only when powered |
| 3 | SwitchDiamondCross | Diamond Crossing / ダイヤモンドクロス | both routes always open, no switching |

Evidence: SRC-1 (javap SwitchType + 4 subclasses), SRC-9, SRC-5 (gamerch
names). Class: KNOWN.

### 2.3 Point / branch structure

`Point` holds: `rpRoot` (shared root RailPosition), `rmMain` (through
RailMapSwitch), `rmBranch` (diverging RailMapSwitch), `branchDir`
(RailDir LEFT/RIGHT/NONE), `mainDirIsPositive`, `branchDirIsPositive`.

`RailMapSwitch` adds `startDir`/`endDir` (RailDir), `MAX_COUNT=80` (branch
sample count), `setState(boolean)`, `getStartMovement()`, `getEndMovement()`,
`shouldRenderRSide()/shouldRenderLSide()`.

Evidence: SRC-1 (javap Point, RailMapSwitch, RailDir). Class: KNOWN.

## 3. Runtime route switching

- Route selection: `RailPosition.checkRSInput(world)` = redstone
  `bestNeighborSignal(blockX,blockY,blockZ) > 0` -> branch, else main.
  Re-evaluated EVERY TICK.
- Active rail map for an entity: `SwitchType.getRailMap(Entity)`; nearest
  Point picks the active branch (`Point.getActiveRailMap(world)`).

Evidence: SRC-1 (checkRSInput, SwitchType.getRailMap), SRC-9, SRC-5 (RS input
at origin side). Class: KNOWN.

## 4. Switch Animation (deep research)

### 4.1 Timing

- `RailMapSwitch.MAX_COUNT = 80` samples; `Point.onUpdate(world)` drives the
  movement over ~80 ticks (~4 seconds) with partial-tick interpolation
  (`getMovement()` returns a 0..1 progress).
- The renderer keeps the active animation index so the tongue position is
  interpolated between frames (no snap-back).

Evidence: SRC-1 (javap), SRC-9 (Point onUpdate "転てつアニメ"), SRC-2
(renderer uses getMovement()/TONG_MOVE). Class: KNOWN.

### 4.2 Renderer mechanics (NR01 RenderRailNB.js — external behaviour)

- `TONG_MOVE = 0.35` (max tongue transverse offset in model units),
  `TONG_POS = 1/10` (tongue position at 1/10 of rail length),
  `HALF_GAUGE = 0.7175`, `YAW_RATE = 600` (tongue yaw per rail length).
- For each branch RailMapSwitch, the diverging rail is drawn sample-by-sample
  (spacing 0.5 m); the "separate rate" (0..1 along half the rail) is shaped by
  `sigmoid2(x) = d/(sqrt(1+d²)) * 0.75 + 0.25` with d = x*3.5, then scaled by
  `move * dirFixture` and applied as a lateral offset `(separateRate -
  halfGaugeMove)` plus a yaw `separateRate * YAW_RATE / railLength`.
- Tongue parts (ZungeL0/1, ZungeR0/1) render at the fixed tongue index and only
  on the diverging side; lead rails (railL/railR) render after the tongue.
- Non-branching side renders static rail; `shouldRenderObject` excludes movable
  parts for a switch rail.

Evidence: SRC-2 (renderer scripts). Class: OBSERVED-from-script (external
behaviour); these are Railsys-adjacent reference values, NOT to be copied as
Railsys constants.

### 4.3 Route state vs geometry

- Geometry (RailMapSwitch startDir/endDir + shared root) is static.
- Route state is dynamic (redstone-powered per tick); animation progress is
  transient (getMovement), NOT persisted.
- Save/reload: switch geometry persists (NBT Size+RP0..RPn); the SELECTED route
  is NOT stored — recomputed from redstone power on load (powered redstone
  persists, so effective route survives reload).
- World re-entry / chunk reload: tile-entity NBT restores geometry; animation
  restarts from current power state (no mid-animation persistence).
- Multiple players: switch state is world/redstone-based (shared), so it is
  consistent across players; animation is client-side interpolation of the
  shared movement progress.

Evidence: SRC-1 (TileEntityLargeRailSwitchCore read/write NBT), SRC-9, SRC-5.
Class: KNOWN (fields/persistence), animation-in-multiplayer INFERRED.

### 4.4 Switch types and animation specifics

- SwitchBasic: tongues move to open one branch; the other side static.
- SingleCross / ScissorsCross: middle crossover rails open/close with power
  (moving rails animated similarly).
- DiamondCross: no moving parts; both routes always open.

Evidence: SRC-1, SRC-9, SRC-5. Class: KNOWN.

## 5. Railsys R10F state

- Railsys R10F has: `RailConnection.validate` (pos <= 1e-4, angle <= 0.5),
  `RailEndpoint`, `RailNetwork` (addPiece/connect/disconnect), `RailPath`
  builder with forward/reverse entries. No switch, no branch, no route state,
  no junction persistence, no animation. Foundation contract reserves piece
  type/params for future switch (F2).

## 6. Gap Analysis (R11-B)

| RTM | Railsys R10F | Verdict |
|-----|--------------|---------|
| RailPosition endpoint persisted (block+double+height+direction) | AnchorDefinition (x/y/z/yaw/pitch) | PARTIAL -> ADAPT for persistence (R-P0) |
| RailMap per segment | RailPath/RailPiece | MATCH conceptually (F2) |
| Connection via shared RailPosition + canConnect | RailConnection.validate | MATCH (F2) |
| SwitchType (4 types) | none | NOT IMPLEMENTED -> R-P0/R-P1 |
| Point (root/main/branch + dirs) | none | NOT IMPLEMENTED -> R-P0/R-P1 |
| Route = redstone per tick | none | NOT IMPLEMENTED -> R-P0 (Eaglercraft input mapping) |
| Switch animation (~80 ticks, tongue offset+yaw, sigmoid) | none | NOT IMPLEMENTED -> R-P1 (visual); needs Railsys-specific design |
| Switch route NOT persisted (derived from power) | n/a | ADOPT BEHAVIOUR candidate (R12 design) |
| Gauge implicit in model | RailAssetDefinition.gaugeM explicit | DIFFERENT -> Railsys has explicit gauge (KEEP, R12 revisit) |

## 7. Requirement Candidates

- R-P0: Rail Network with persisted endpoints (anchor + direction + height) and
  connection/snap (extend Railsys RailNetwork).
- R-P0: Switch creation via junction marker (blue) + branch endpoints.
- R-P0: Switch route state (powered input mapping to Eaglercraft: redstone
  signal or server-controlled).
- R-P1: Switch animation (tongue movement + partial-tick interpolation) —
  Railsys-specific visual, not RTM constant copy.
- R-P1: Switch persistence (geometry + derived route on load).
- R-P2: switch editing / deletion with connected-rail repair.

## 8. Open Questions

- Exact RTM switch geometry (frog/point curves) UNKNOWN -> Railsys clean-room.
- Per-branch cant on a switch UNKNOWN.
- Max branch count per junction UNKNOWN.
- How Eaglercraft represents redstone power for switch input (server tick vs
  block update) — Railsys design decision.
- Chunk unload/reload animation continuity (RTM restarts from power state;
  Railsys can choose same).
