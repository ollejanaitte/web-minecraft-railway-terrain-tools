# R12-F: Persistence Architecture

Phase 1-R12 design freeze. Inputs: REQ-P0-10 (Persistence/Save/Reload),
REQ-P0-01 (data model), REQ-P1-05 (missing/broken pack), R11-D +
PERSISTENCE_DATA_MATRIX (RTM NBT), R11-B (switch route derived).

## 1. Goals

- World Save / Quit / Restart / Reload fully restores the Rail Network.
- Save authoritative data only; derive/cache the rest.
- Versioned schema + migration; resilient to missing/broken packs.

## 2. Save vs derive vs cache (frozen)

| Item | Persist? | Source |
|------|----------|--------|
| schemaVersion | YES | authoring |
| railId (issued @confirm) / nodeId (@first connection) / connectorId (@attach) | YES | R12-A §3 |
| Segment endpoints (AnchorDefinition + blockPos derived metadata) | YES | R12-A |
| RailNode membership (explicit) | YES | R12-A §4.3 |
| Connection membership | YES | R12-B |
| Junction record (railId, family, points, routeInput ref, branchRailIds) | YES | R12-C (branch GEOMETRY lives in their own RailSegments) |
| Junction routeInput (powered source) | YES | R12-C |
| routeInput owner's persistent state | YES (input source) | powered block / server value |
| committedRoute | NO (derived) | derived from input on load |
| animation progress | NO (transient) | reset on load |
| assetId + assetVersion | YES | R12-A |
| gaugeM (denormalized snapshot) | YES | refreshed from asset at save |
| CantProfile | YES | EXTENSIBLE |
| signalState / occupied | NO (Phase 1: schema-reserved, not written) | Phase 2 writers |
| InfrastructureConnector list | YES | R12-H |
| RailPath geometry | NO (derived) | rebuilt from endpoints via F2 |
| Frames / mesh / index | NO (cache) | rebuilt |

## 3. Storage model

- World-global store (not chunk-local) — frozen decision: `RailWorldData`
  (analogue of RTM tile-per-rail but Railsys keeps a world-level authoritative
  record + spatial index).
  - Rationale: rails are long objects spanning chunks; world-global avoids
    chunk-boundary split; Eaglercraft world data is JS-object-serializable.
- On chunk load, only the spatial index needs a region query (R12-J); the
  authoritative store is already in memory per world.

## 4. Schema & versioning

- `schemaVersion` at store root; Railsys migration hook (RTM
  fixRTMRailMapVersion analogue).
- Serialization encoding (JSON / NBT / custom) is REPLACEABLE; R13 chooses.
- `railId` generation: world-monotonic counter persisted in the store.

## 5. Reload behaviour

1. Read store; validate schemaVersion (migrate if needed).
2. Rebuild each segment's RailPath from endpoints via F2 fromMarkers — using
   the full endpoint record (anchor incl. pitch/handles, cant profile,
   geometry subtype, asset) plus junction points/family and stable piece
   identity (railId).
3. Rebuild nodes + connections by nodeId.
4. Derive junction committedRoute from routeInput owner's restored state
   (powered block power / server value); animation starts idle.
5. Animation starts idle (mid-animation state is NOT saved — frozen, matches
   RTM + REQ-P0-04).
6. Restore connectors by id + targetRailId.
7. Missing/broken asset -> fallback + warning (REQ-P1-05); gauge snapshot
   refreshed from asset.
8. occupied/signalState stay default (Phase 1 no writers; not persisted).
9. Build spatial index + render cache.

## 6. Missing / changed / broken ModelPack

- Missing asset on load: use fallback definition + log + user warning; rail
  geometry intact (geometry never depends on asset — F4).
- Changed asset (same id, new version): re-resolve; if compatible keep
  geometry; if incompatible fallback + report.
- Broken pack: skip asset, fallback, report; world data preserved.

## 7. Orphans / corruption

| Case | Handling |
|------|----------|
| Orphan connection (missing endpoint rail) | repair: drop connection + log (R12-G) |
| Orphan connector (missing target rail) | repair: keep with INVALID flag or drop (R12-G) |
| Duplicate railId | detect on load; quarantine + log |
| Corruption / parse failure | do not crash; fall back to last-good; report |
| Backward compatibility | schema migration preserves old stores |

## 8. Mid-animation switch decision (frozen)

Save the STABLE route INPUT, not the animation:
- Persist `routeInput` reference AND the input source's own persistent state
  (powered block power / server value). On load, `committedRoute` is DERIVED
  from that state. The route itself (committedRoute) is NOT stored.
- Animation progress is transient and never saved.
Rationale: RTM does the same; animation is short and client-visible only;
persistence of a 0.5-1s transient adds no value and risks inconsistency.
(REQ-P0-04 / R11-B.)

## 9. Multiplayer

- Server-authoritative world data (R10F F6): server owns RailWorldData; clients
  render the restored network. Client placement UI remains client-local
  (F6). Sync of edits is R13+ design (server command / packet).
- Server-side model packs to clients (RTM ModelPack_ analogue) — REQ-P0-07
  note.

## 10. Requirement trace

- REQ-P0-10: this doc.
- REQ-P0-01: data model (R12-A) is the persisted unit.
- REQ-P0-03/04: switch/route/animation persistence policy (§8).
- REQ-P1-05: missing/changed/broken (§6).
- REQ-P1-04: switch persistence (§2/§5).

## 11. Open questions

- Serialization format + file placement in Eaglercraft world — R13.
- RailWorldData vs chunk-interaction for very large networks — R12-J measure.
- Migration policy specifics — R13.
