# R11-E: RTM Signal / Crossing / Connector

Phase 1-R11 deliverable. Evidence: SRC-1 (RTM jar electric + rail classes),
SRC-5 (gamerch 信号/信号変換器/各装置), SRC-9 (RTMConfig, electric wiring),
SRC-2 (NR01 connector JSON).

Legend: OBSERVED / KNOWN / INFERRED / UNKNOWN.

---

## 1. Signal (信号機)

### 1.1 Placement & appearance

- Signals are MODEL BLOCKS placed on catenary poles (架線柱) or any block
  (1.7.10.20+). Model switched by shift+right-click or air-left-click (bulk).
- Signal pack JSON keys: signalName, signalModel, signalTexture, lightTexture,
  modelPartsFixture (rotating mount), modelPartsBody (rotating body), lights
  (S strength / I blink tick / P object name), rotateBody.

Evidence: SRC-5 (信号の使い方 677412), SRC-6 (akikawaken signal JSON).
Class: KNOWN.

### 1.2 Detection — signals do NOT detect trains themselves

- A separate 列車検知器 (train detector) detects trains.
- Block-section (閉塞) logic is built by wiring: signal + detector +
  insulators (碍子) + +1 input connectors; consecutive signals daisy-chained
  so each shows 停止/警戒 based on occupancy ahead.
- Multiple inputs: last input wins (1.7.10.15+).

Evidence: SRC-5, SRC-9 (electric wiring). Class: KNOWN.

### 1.3 Signal network = separate wiring system

- Converters bridge to vanilla redstone: RS->Signal (GUI maps RS_ON/RS_OFF
  values), Signal->RS, +1, -1, input/output connectors, wires, insulators.
- Rail core has a signal field: `TileEntityLargeRailCore.getSignal()/
  setSignal(int)` and `isCollidedTrain` — the rail itself carries signal/
  occupancy state usable by detectors/logic.

Evidence: SRC-1 (TileEntityLargeRailCore), SRC-5 (信号変換器 677490),
SRC-9 (SignalConverterType, SignalLevel). Class: KNOWN.

### 1.4 Position / direction / target segment

- Signals mount to poles; the rail "target" is implicit by position/proximity
  (no explicit target-segment field found). Protected route/block section is
  built from the wiring graph, not a rail API.

Evidence: SRC-5, SRC-1. Class: INFERRED (no explicit rail-target id).

### 1.5 Save/reload

- Signal state + wiring connections persist via tile NBT (Connection list,
  TileEntityElectricalWiring). Signal model/id via machine/model config.

Evidence: SRC-1 (TileEntityElectricalWiring NBT, Connection.writeToNBT).
Class: KNOWN.

## 2. Crossing (踏切)

### 2.1 Type

- Crossing gate (遮断機) is a SAFETY-DEVICE MODEL under 保安装置; it is a
  `MachineType.Gate` machine block (`TileEntityCrossingGate` with
  `barMoveCount`, `lightCount`, `onActivate`).
- Config: `crossingGateSoundType` (byte 0/1), `crossingGateSoundRange`
  (default 32 blocks).

Evidence: SRC-1 (TileEntityCrossingGate, MachineType.Gate), SRC-9
(RTMConfig). Class: KNOWN.

### 2.2 Rail connection / activation

- Community use: gates placed across a rail close + sound when a train
  occupies the crossing; drive by redstone also plays sounds. Exact approach
  detection mechanics: INFERRED (standard behaviour; internals UNKNOWN).
- No explicit crossing-rail link field found in the jar surface; detection is
  via occupancy/signal wiring (INFERRED).

Evidence: SRC-5 (各装置 677450), SRC-1. Class: INFERRED for internals.

## 3. Connector

### 3.1 Electric Connection (KNOWN)

- `Connection` = { isRoot, x/y/z, ConnectionType, wireName } with NBT
  read/write. It links a wiring tile to a target block (e.g. detector, signal,
  machine) with a named wire. `TileEntityElectricalWiring` holds a connection
  list; `onGetElectricity`, `sendElectricity`, `setConnectionTo`.

### 3.2 RailPack connector models (KNOWN, SRC-2)

- `ModelConnector_*.json` (e.g. NB_Deckenstromschiene_Ende) = connectorType
  "Relay", model + wirePos + smoothing/doCulling/accuracy/tags. These are
  infrastructure connector models (DSS = overhead conductor rail end).

### 3.3 Implications for Railsys

Railsys future connector concepts (class/API NOT fixed in R11):

| Candidate | Purpose | RTM analogue |
|-----------|---------|--------------|
| RailConnector | connect rail endpoints/segments | shared RailPosition |
| InfrastructureConnector | link signal/crossing/switch controller to rail | Connection (x/y/z + type + wireName) |
| RailAttachment | attach models (signals) to rail at offset | pole/block mount |
| RouteReference | switch route target | RailMapSwitch active |
| Occupancy Hook | detection feed | isCollidedTrain + signal field |
| Event Hook | activation events | onActivate / onGetElectricity |

What the rail side must expose (requirement):

- rail position + direction + frame at a point (Railsys F2 already provides);
- per-rail signal/occupancy state (RTM has getSignal/setSignal/isCollidedTrain);
- connection points (x/y/z + type + wire id) persisted (Connection-like);
- stable identity for referencing from infrastructure (position-based in RTM;
  Railsys may add railId).

## 4. Gap Analysis (R11-E)

| RTM | Railsys R10F | Verdict |
|-----|--------------|---------|
| Signal model blocks + pole mount | none | NOT IMPLEMENTED -> R-P1/DEFER (logic Phase 2) |
| Train detector + block-section wiring | none | DEFER Phase 2 |
| Signal converter network (RS<->Signal, +1/-1) | none | DEFER Phase 2 |
| Rail signal/occupancy state (getSignal/isCollidedTrain) | none | R-P1 (add signalState/occupied to rail data) |
| Crossing gate (Gate machine) | none | DEFER Phase 2 (needs train detection) |
| Electric Connection (x/y/z/type/wireName) | none | R-P1 (connection data contract) |
| Connector models (Relay, DSS) | none | DEFER Phase 2 |
| Position-based infrastructure reference | none | R-P1 (document reference-by-position/railId) |

Phase 1 conclusion: full signal/ATS/block-section logic and crossing
automation are Phase 2. Railsys Phase 1 must (a) provide the rail-side data
hooks (signal/occupancy, connection points, stable identity) so infrastructure
can attach later, and (b) decide reference semantics (position vs railId).

## 5. Requirement Candidates

- R-P1: Rail data carries `signalState` + `occupied` (RTM getSignal/
  isCollidedTrain analogue) in the persistence contract.
- R-P1: Connection data model (Railsys-native: targetPos/type/wireId) for
  future infrastructure.
- R-P1: stable rail identity (id) alongside position for infrastructure
  references (R12 decision).
- DEFER: signal model rendering, detector/block-section logic, converters,
  crossing automation, connector models.

## 6. Open Questions

- Exact train-detection mechanism (occupancy of rail map vs entity proximity).
- Whether crossing gate references a specific rail or any nearby occupancy.
- Railsys reference semantics (position vs railId) — R12.
- Eaglercraft redstone/wiring parity for signal converters.
