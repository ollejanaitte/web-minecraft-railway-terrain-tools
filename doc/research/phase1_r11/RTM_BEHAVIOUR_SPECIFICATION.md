# RTM BEHAVIOUR SPECIFICATION — Phase 1-R11

Integrated behavioural specification distilled from R11-A..F, prior phase 0.7
research, RTM 1.7.10.46 jar public API, NR01 v3.0 ModelPack, and public
documentation. Evidence and inference are never mixed: every entry carries an
Information Class and Confidence.

Primary version baseline: RTM 1.7.10.46 (on-hand jar) + RTM2-family
(1.10.2/1.12.2) where noted. VERSION-SPECIFIC items are flagged.

Sources: SRC-1..SRC-12 (see `SOURCE_INVENTORY.md`).

---

## B.1 Placement & markers

| ID | Feature | User operation | Observable behaviour | Data implied | Evidence | Class | Confidence |
|----|---------|----------------|----------------------|--------------|----------|-------|-----------|
| B1-1 | Marker items | Place red marker (straight ↑ or diagonal ↗) | Red marker placed; diagonal = edge-aligned start | marker type (center/edge), position, facing | SRC-3, SRC-5 | KNOWN | High |
| B1-2 | Blue branch marker | Place blue marker | Marks switch/branch endpoint | switchType byte | SRC-3, SRC-5, SRC-1 | KNOWN | High |
| B1-3 | Facing rule | Place two markers arrows toward each other | Rail builds along chord; same-direction fails/loops | direction per marker | SRC-3, SRC-5 | KNOWN | High |
| B1-4 | Confirm | Right-click a marker (empty hand creative / rail item survival) | Rail generated along preview | rail endpoints + railProperty | SRC-3, SRC-5 | KNOWN | High |
| B1-5 | Slope markers | (historical) | removed in RTM2.0.x; gradients via height | — | SRC-3, SRC-9 | VERSION-SPECIFIC | High |
| B1-6 | Max length | place long rail | rejected / generation distance limit | railGeneratingDistance default 64 max 256 | SRC-1, SRC-9 | KNOWN | High |

## B.2 Geometry

| ID | Feature | Behaviour | Data | Evidence | Class | Confidence |
|----|---------|-----------|------|----------|-------|-----------|
| B2-1 | Curve | smooth path from handle geometry; math class UNKNOWN | anchorDirection + anchorLength (H/V) | SRC-3, SRC-9 | KNOWN behaviour / UNKNOWN math | High/— |
| B2-2 | Gradient | endpoint height difference creates slope (RailMapSlope) | height (1/16-block), slopeType byte | SRC-1, SRC-9 | KNOWN | High |
| B2-3 | Cant | numeric in marker GUI; negative reverses | cantCenter/Edge/Random | SRC-5, SRC-9 | KNOWN fields | High |
| B2-4 | Sample | 0.5 m spacing short-model placement | RailMap getRailPos/Height/Rotation/Pitch | SRC-2 | KNOWN | High |
| B2-5 | Rail reference height | support surface; ballast 0.0625 | RailPosition height + blockHeight | SRC-1, SRC-2 | KNOWN | Medium-High |

## B.3 Network / Switch

| ID | Feature | Behaviour | Data | Evidence | Class | Confidence |
|----|---------|-----------|------|----------|-------|-----------|
| B3-1 | Endpoint | RailPosition (block + double + height + direction + anchors + switchType), NBT-persisted | A_Direction/A_Pitch/A_Length/A_LenV/C_* | SRC-1, SRC-9 | KNOWN | High |
| B3-2 | Segment | TileEntityLargeRailCore + RailMap from RailPositions | RP0..RPn + RailProperty | SRC-1 | KNOWN | High |
| B3-3 | Connection | shared position + canConnect | — | SRC-1 | KNOWN | Medium |
| B3-4 | Switch create | blue root + red endpoints -> switch | SwitchType + Points | SRC-3, SRC-5 | KNOWN | High |
| B3-5 | Switch types | Basic(0) SingleCross(1) ScissorsCross(2) DiamondCross(3) | id | SRC-1, SRC-9 | KNOWN | High |
| B3-6 | Route switch | redstone at origin: powered -> branch, else main; per tick | checkRSInput | SRC-1, SRC-9 | KNOWN | High |
| B3-7 | Route persistence | NOT stored; recomputed from power on load | — | SRC-1, SRC-9 | KNOWN | High |
| B3-8 | Switch animation | ~80 ticks (~4s), partial-tick getMovement(), tongue parts + sigmoid spread | point movement 0..1 | SRC-1, SRC-2, SRC-9 | KNOWN | High |

## B.4 ModelPack / Appearance

| ID | Feature | Behaviour | Data | Evidence | Class | Confidence |
|----|---------|-----------|------|----------|-------|-----------|
| B4-1 | Pack layout | pack.json + mods/RTM/ModelRail_*.json + assets(mqo,scripts,textures) | railName/model/textures/rendererPath | SRC-2, SRC-6 | KNOWN | High |
| B4-2 | Rail JSON | railName, model, buttonTexture, defaultBallast, ballastWidth, allowCrossing, polygonType, accuracy, tags | RailConfig fields | SRC-2, SRC-6, SRC-1 | KNOWN | High |
| B4-3 | Renderer script | per-asset RenderRail*.js draws parts + switch animation | rendererPath | SRC-2 | KNOWN | High |
| B4-4 | Gauge | NOT in pack JSON; implicit (model + HALF_GAUGE 0.7175) | — | SRC-2, SRC-9 | KNOWN | High |
| B4-5 | Missing pack | dummy config / black rails | RailConfig.getDummy | SRC-1, SRC-5 | KNOWN/reported | Medium-High |
| B4-6 | Server packs | ModelPack_ prefix downloaded to clients | useServerModelPack | SRC-1 | KNOWN | Medium |

## B.5 Persistence

| ID | Feature | Behaviour | Data | Evidence | Class | Confidence |
|----|---------|-----------|------|----------|-------|-----------|
| B5-1 | Rail save | per-tile NBT; survives reload | RP0..RPn + RailProperty + endpoint NBT keys | SRC-1, SRC-9 | KNOWN | High |
| B5-2 | Switch save | Size + RP0..RPn + fixRTMRailMapVersion; route derived | — | SRC-1 | KNOWN | High |
| B5-3 | Formations | WorldSavedData | FormationData | SRC-1 | KNOWN | Medium |
| B5-4 | Train couplings | break on re-login (by design) | — | SRC-5 | KNOWN | High |
| B5-5 | Migration | fixRTMRailMapVersion | version field | SRC-1 | KNOWN | Medium |

## B.6 Signal / Crossing / Connector

| ID | Feature | Behaviour | Data | Evidence | Class | Confidence |
|----|---------|-----------|------|----------|-------|-----------|
| B6-1 | Signal model | model block on pole; no self-detection | signal JSON + model | SRC-5, SRC-6 | KNOWN | High |
| B6-2 | Detection | separate train detector; block-section wiring (last-input-wins) | wiring graph | SRC-5, SRC-9 | KNOWN | High |
| B6-3 | Rail signal/occupancy | rail core getSignal/setSignal(int) + isCollidedTrain | signal int + flag | SRC-1 | KNOWN | High |
| B6-4 | Converters | RS<->Signal, +1, -1 | SignalConverterType | SRC-5, SRC-9 | KNOWN | Medium |
| B6-5 | Crossing gate | Gate machine; barrier + light counts; sound 0/1 + range 32 | barMoveCount/lightCount | SRC-1, SRC-9 | KNOWN fields | Medium |
| B6-6 | Connector | Connection {isRoot,x,y,z,type,wireName} NBT | connections list | SRC-1 | KNOWN | High |
| B6-7 | Connector models | ModelConnector JSON (Relay, DSS, wirePos) | connectorType/wirePos | SRC-2 | KNOWN | High |

## B.7 Vehicle

| ID | Feature | Behaviour | Data | Evidence | Class | Confidence |
|----|---------|-----------|------|----------|-------|-----------|
| B7-1 | Rail following | nearest RailMap each tick; switch branch by proximity | RailMap samples | SRC-1 | KNOWN | High |
| B7-2 | Formation | cars linked via FormationEntry; FormationData persisted | formation id + entries | SRC-1, SRC-6 | KNOWN | Medium |
| B7-3 | End-of-track | runs out of rail map and stops (no derail found) | — | SRC-9/INFERRED | INFERRED | Low-Medium |

---

## Cross-cutting unknowns (not asserted as RTM fact)

- Exact curve math class (Bezier/Hermite/clothoid): UNKNOWN.
- Cant units and interpolation: UNKNOWN (Railsys uses degrees by decision).
- Obstacle/height validation details and exact failure feedback: partial.
- Chunk-boundary placement rules: UNKNOWN.
- Crossing-rail link internals: UNKNOWN (inferred occupancy-based).
- Multiplayer switch animation consistency: INFERRED (shared world state).
