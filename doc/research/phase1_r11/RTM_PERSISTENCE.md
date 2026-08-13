# R11-D: RTM Confirmed Editing / Delete / Persistence

Phase 1-R11 deliverable. Evidence: SRC-1 (RTM jar NBT/classes), SRC-9
(faithful port NBT keys), SRC-5 (wiki Q&A), SRC-6 (JSON docs).

Legend: OBSERVED / KNOWN / INFERRED / UNKNOWN / VERSION-SPECIFIC.

---

## 1. Confirmed Rail Editing

### 1.1 How RTM edits an existing rail

| Aspect | Finding | Class |
|--------|---------|-------|
| Selection | Re-open marker GUI by right-clicking a placed marker holding a marker item (RTM2.2.1+) | KNOWN |
| Endpoint editing | Marker GUI changes anchorYaw/anchorPitch/anchorLengthH/anchorLengthV/height on that endpoint | KNOWN (fields SRC-9) |
| Curve editing | Wrench shape-edit changes green handle direction + length before regeneration | STRONGLY SUPPORTED (SRC-3) |
| Gradient editing | change endpoint height (wrench height steps 0.5) or anchorPitch | LIKELY |
| Cant editing | marker GUI cant fields (C_Center/C_Edge/C_Random) | KNOWN |
| Asset/modelpack change | RailProperty.railModel switch (new model); regenerate | KNOWN |
| Connected rail editing | no evidence of edit-through-node propagation; each rail is independent tile | UNKNOWN/INFERRED |
| Switch editing | re-open switch markers / rebuild | UNKNOWN |
| Preview | wrench preview before regeneration | STRONGLY SUPPORTED |
| Cancel/revert | abort shape edit without confirm | UNKNOWN (exact control) |
| Edit transaction | no formal transaction; regenerate from marker NBT | INFERRED |
| Effect on connections | canConnect re-validated; geometry rebuild may break/disconnect neighbours | INFERRED |

### 1.2 Railsys R10F vs RTM

Railsys R10F edits are PRE-CONFIRM only (transient markers + edits rebuild
preview). Confirmed rails are promoted and NOT re-editable in R10F. RTM edits
POST-CONFIRM via markers. Gap: confirmed-rail editing (select existing rail,
edit endpoint/curve/gradient/cant/asset, regenerate) is a Railsys Phase 1
requirement.

## 2. Delete / Replace / Maintenance

| Aspect | Finding | Class |
|--------|---------|-------|
| Delete | Break the rail block(s); RailMap.breakRail clears blocks; TileEntity removed | KNOWN (SRC-1 breakRail) |
| Disconnect | no explicit disconnect UI; breaking a shared endpoint orphans the neighbour | INFERRED |
| Reconnect | re-place marker / re-lay | INFERRED |
| Replace | place new model over / change RailProperty.railModel | KNOWN (field) |
| Asset-only replace | change model without moving geometry | INFERRED (railModel swap) |
| Junction/switch deletion | break switch core; branches broken | INFERRED |
| Safe recovery | no undo/redo documented | UNKNOWN |
| Dependent infrastructure | signals/crossings reference rails by position; breaking rail may orphan them | INFERRED |

## 3. Persistence / Save / Reload (very important)

### 3.1 Data model (KNOWN — NBT keys from SRC-1/SRC-9)

Per placed rail endpoint, `RailPosition.writeToNBT/readFromNBT`:

| NBT key | Meaning |
|---------|---------|
| `BlockPos` / `X`/`Y`/`Z` | block coords |
| `Direction` | direction byte (8-way) |
| `SwitchType` | marker/switch type byte |
| `Height` | height byte (1/16-block) |
| `A_Direction` | anchor yaw |
| `A_Pitch` | anchor pitch |
| `A_Length` | anchor length horizontal |
| `A_LenV` | anchor length vertical |
| `C_Center` / `C_Edge` / `C_Random` | cant center/edge/random |

Per rail tile (`TileEntityLargeRailCore.writeRailData`): `RP0..RPn` (endpoint
positions), property (railModel/block/metadata/height via RailProperty NBT).
Switch core adds `Size` + `RP0..RPn` + `fixRTMRailMapVersion`.
Marker tile stores `RP` (RailPosition) + `MarkerState` + core-marker
`StartX/Y/Z`.

### 3.2 Reload behaviour (KNOWN/INFERRED)

| Scenario | Behaviour | Class |
|----------|-----------|-------|
| World save/quit/restart | tile-entity NBT persists; rails rebuild RailMap on load | KNOWN |
| World reload | same as restart | KNOWN |
| Geometry restore | RailMap rebuilt from RailPositions | KNOWN |
| Asset restore | RailProperty.railModel string -> ModelSetRail lookup; missing pack -> dummy/black | KNOWN (dummy getDummy) / reported black-rails (SRC-5) |
| Gauge/cant restore | gauge implicit in model; cant from NBT fields | KNOWN |
| Connection restore | endpoints shared in world; canConnect re-validated on rebuild | INFERRED |
| Switch restore | geometry from NBT; route recomputed from redstone power | KNOWN |
| Switch animation state | NOT persisted; restarts from power state | KNOWN |
| Confirmed edit identity | per-tile RailPositions ARE the identity (no separate id) | KNOWN |
| Segment/node identity | implicit by position (no global id) | KNOWN |
| Connector identity | electric Connection list (x/y/z/type/wireName) in wiring tile NBT | KNOWN |
| Signal link / crossing link | by position; rail signal field on TileEntityLargeRailCore (getSignal/setSignal int) | KNOWN |
| Chunk load/unload | tile-entity lifecycle; onChunkUnload handler exists | KNOWN |
| Missing ModelPack | dummy fallback (RTM) / black rails (reported) | KNOWN/reported |
| Changed ModelPack | model swap on load (schema may drift) | INFERRED |
| Broken ModelPack | may fail load / dummy | INFERRED |
| Migration | fixRTMRailMapVersion field suggests versioned migration | KNOWN (field) |
| Multiplayer | server model packs (ModelPack_ prefix) downloaded to clients | KNOWN (SRC-1 README) |
| Train couplings | BREAK on re-login (by design; "仕様です" gamerch Q&A) | KNOWN |

### 3.3 Railsys R10F persistence status

- `RailsysWorldRailData` (persist) + `RailsysRenderManager.ensureRestored`
  restore confirmed rails per world — a minimal prototype exists.
- No asset/gauge/cant/connection/switch persistence; no confirmed-edit
  identity; no reload of transient edits.
- Railsys confirmed rail is single-path (render manager holds one path); no
  multi-rail network persistence.

## 4. Gap Analysis (R11-D)

| RTM | Railsys R10F | Verdict |
|-----|--------------|---------|
| Post-confirm editing via markers/wrench | pre-confirm edits only | NOT IMPLEMENTED -> R-P0 confirmed editing |
| RailProperty model swap | setActiveAsset (look-only, path unchanged) | PARTIAL -> R-P0 asset persistence |
| Delete via breakRail | no delete API | NOT IMPLEMENTED -> R-P0 delete/repair |
| Undo/redo | none | not required (R-P2) |
| RailPosition NBT per endpoint | no persisted endpoint | NOT IMPLEMENTED -> R-P0 persistence contract |
| Height (1/16) + direction byte + anchors | AnchorDefinition (double yaw/pitch/handle) | PARTIAL -> R12 data-contract mapping |
| Cant C_Center/Edge/Random | constant cant | PARTIAL -> R-P1 cant profile persistence |
| Switch route derived from power | none | R-P0 (see R11-B) |
| Missing-pack dummy fallback | RailAssetRegistry fallback | MATCH concept |
| Train coupling reload quirk | n/a (no trains) | DEFER (Phase 2) |

## 5. Requirement Candidates

- R-P0: Persistence data contract (Railys native: rails as endpoint sets with
  position/direction/height/anchors/cant/asset; world-bound).
- R-P0: Save/quit/restart/world-load restore of confirmed rails + network.
- R-P0: Confirmed rail editing (select existing rail, edit endpoint/curve/
  gradient/cant/asset, regenerate).
- R-P0: Delete / replace / repair with connected-rail handling.
- R-P1: missing/changed/broken ModelPack restore policy (fallback + warning).
- R-P1: versioned data schema + migration (fixRTM-analogue).
- R-P2: undo/redo.

## 6. Open Questions

- RTM exact rail-block layout (which blocks belong to a rail) — needed for
  Railsys chunk/per-block persistence mapping.
- Post-edit regeneration impact on neighbours (disconnect/reconnect).
- Whether RTM stores a separate "confirmed edit identity" anywhere (looks like
  position-based only).
- Railsys-native persistence unit (per-endpoint vs per-rail vs per-network) —
  R12 freeze.
- Multiplayer rail sync model (server-authoritative world + client rendering;
  R10F F6 aligns).
