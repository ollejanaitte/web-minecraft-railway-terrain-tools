# R12-A: Production Rail Data Model

Phase 1-R12 design freeze. Inputs: REQ-P0-01 (Production Rail Data Model),
REQ-P0-02 (Network), REQ-P1-06 (hooks), R10F F1/F2, R11
PERSISTENCE_DATA_MATRIX.

## 1. Goals

- A confirmed rail / junction / connection is real, persisted, referenced data.
- Stable identity decoupled from coordinates.
- Preview and Confirmed share the F1/F2 anchor/geometry semantics with a clear
  data boundary.
- Infrastructure (signal/crossing) and vehicles can reference the network by
  stable id + distance.

## 2. Authoritative vs derived vs cache

| Data | Kind | Notes |
|------|------|-------|
| railId (issued @confirm), nodeId (@first connection), connectorId (@attach) | AUTHORITATIVE | see §3 lifecycle |
| Segment endpoints (AnchorDefinition) | AUTHORITATIVE | F1/F2 semantics; the SSoT of position/direction |
| Connection membership | AUTHORITATIVE | explicit (see R12-B) |
| Junction route input (powered source) | AUTHORITATIVE | persisted reference to input |
| Asset reference (assetId + assetVersion) | AUTHORITATIVE | persisted |
| Cant profile | AUTHORITATIVE | EXTENSIBLE (CC-1) |
| signalState / occupied | AUTHORITATIVE (data), schema-reserved | R-P1 hooks; logic is Phase 2; NOT persisted until a Phase 2 writer exists |
| gaugeM (denormalized) | DERIVED SNAPSHOT | refreshed from asset at save/render; asset is authoritative for gauge |
| validation result | DERIVED, PERSISTED metadata | recomputed; stored for load-time reporting |
| RailPath geometry | DERIVED | rebuilt from endpoints via F2 fromMarkers |
| Local frames / samples | DERIVED | from RailPath |
| Mesh / display lists | CACHE | rebuildable (R12-D) |
| Spatial index / network lookup | CACHE | rebuildable (R12-B/J) |

Operational-field ownership (frozen):
- `occupied` / `signalState`: schema-reserved data fields. In Phase 1 there are
  no writers, so they are NOT persisted and default to false/0 on load. The
  R12-F schema reserves the keys for forward compatibility; Phase 2 systems
  become the writers and add them to the persisted set via a schema version
  bump. No stale-occupancy-after-restart is possible because Phase 1 never
  writes them.
- `gaugeM`: a derived snapshot for validation only. On asset change the
  snapshot is refreshed from the asset (asset = authoritative, R10F F4). A
  saved gauge never overrides the asset.

## 3. Stable identity semantics (frozen)

| ID | Issued at | Owner | Notes |
|----|-----------|-------|-------|
| `railId` | CONFIRM (R10F F5 promotion) | RailSegment / Junction | unique per world |
| `nodeId` | FIRST connection (snap/connect merge) | RailNode | a free endpoint has no nodeId; issued when a node is created |
| `connectorId` | INFRASTRUCTURE ATTACH | InfrastructureConnector | issued at attachment time |

- Deletion: id is retired (not reused). A replacement rail gets a NEW id.
- ID source: world-monotonic counter (persisted) + optional salt. Format is a
  serialization detail (REPLACEABLE).

### 3.1 Confirm authority handoff (frozen, F6-aligned)

- Placement markers/preview/edits live in the client-local placement state
  (R10F F6 CLIENT-LOCAL) and have NO stable id.
- CONFIRM is a two-phase handoff:
  1. Client sends a `confirmRequest` (endpoints, asset, cant, geometry) to the
     server (R10F F6: server authoritative world).
  2. Server VALIDATES (R12-J) WITHOUT silent canonicalization: it either
     ACCEPTS the exact proposed endpoint values, or REJECTS with a reason
     (over-length, continuity, overlap...). It issues `railId` (and `nodeId`
     on snap) and writes the authoritative RailWorldData using the ACCEPTED
     values. If the server would need to change the geometry (e.g. snap the
     node), it returns the corrected record and the client re-derives — the
     preview is NOT promoted in that case.
  3. Client promotes the exact preview `RailPath` as the initial derived
     geometry/cache ONLY when the server returns `acceptFingerprint ==
     previewFingerprint` (a deterministic fingerprint of endpoints + asset +
     cant + geometry params).
     Any mismatch path: client re-derives from the returned record
     (F2.4 exact promotion preserved; no divergence).
- EDIT (R12-G) follows the same handoff: client proposes, server validates +
  commits, client applies.
- This is the frozen ownership boundary: the server owns authoritative rail
  data; the client owns placement UI state. Specified via CC-6; F6
  classification unchanged.

## 4. Core types

### 4.1 RailSegment

```text
RailSegment {
  railId
  kind: NORMAL | SLOPE | CURVE        (from F2 fromMarkers decision)
  endpointA, endpointB: RailEndpoint
  assetId, assetVersion
  gaugeM                              (denormalized from asset for validation)
  cant: CantProfile                   (constant today; EXTENSIBLE center/edge)
  geometry: { handleA, handleB }      (F2/F3 handle semantics)
  signalState: int                    (R-P1; default 0)
  occupied: bool                      (R-P1; default false)
  metadata: Map<string,string>        (EXTENSIBLE)
}
```

### 4.2 RailEndpoint

```text
RailEndpoint {
  anchor: AnchorDefinition            (x,y,z,yawDeg,pitchDeg,lengthH,lengthV)
  blockPos: BlockPos?                 (CC-4 additive; derivable block origin
                                       for spatial tie-in; NOT authoritative)
  markerType: NORMAL | JUNCTION
  placement: CENTER | EDGE            (REQ-P1-01)
  nodeId: nodeId?                     (null = free end)
}
```

- Reuses F1/F2 AnchorDefinition exactly — no new coordinate math.
- `y` is the support-surface datum (F1).
- `blockPos` (CC-4) is DERIVED metadata for spatial indexing; the anchor is the
  authoritative position.

### 4.3 RailNode (authoritative membership)

```text
RailNode {
  nodeId
  memberEndpointRefs[]: { railId, side (A/B) }   (authoritative membership;
                                                1..n, canonical order)
  position: x,y,z                               (DERIVED from members;
                                                frozen rule below)
  kind: JOIN | JUNCTION | TERMINUS
}
```

- RailNode is an EXPLICIT persisted record (not just pairwise connections), so
  3+ member junctions are unambiguous.
- Position rule (frozen): node position = the FIRST member's endpoint anchor
  position; editing a member endpoint that would move the node is only allowed
  if ALL members move together (see R12-G) OR the edit is a reposition that
  re-validates continuity. When one member is edited, the other members'
  anchors stay authoritative; if continuity breaks, the edit is rejected or the
  connection is flagged for manual disconnect.
- `RailConnection` remains for pairwise membership bookkeeping; the node is the
  canonical membership source of truth.

### 4.4 Junction (R12-C detail in its doc)

```text
Junction {
  railId
  rootNodeId                          (shared origin node)
  branchRailIds[]                     (independent RailSegments, own railIds)
  points[]: JunctionPoint
  routeInput: { type: POWERED | SERVER; ref }
  committedRoute: routeId (derived, see R12-C)
  animationProgress: transient
}
```

Branch ownership (frozen): each junction branch is an INDEPENDENT RailSegment
with its own railId, rooted at the shared node. The Junction owns points,
route selection, and the input; it does not own branch geometry. This makes
editing/deletion of a single branch clean (R12-G).

### 4.5 InfrastructureConnector (R12-H)

```text
InfrastructureConnector {
  connectorId
  type: SIGNAL | CROSSING | SWITCH_CONTROLLER | STATION_EQUIPMENT | SENSOR
  targetRailId
  distanceS: double                  (s[m] along the rail)
  side: LEFT | RIGHT | CENTER
  direction: byte                    (travel direction)
  state: int
  routeRef: routeId?                 (optional)
}
```

## 5. Lifecycle

- PLACEMENT (transient): markers + edits, no stable id, preview is a transient
  RailPath (R10F F5).
- CONFIRM: two-phase handoff (client confirmRequest -> server validate +
  issue railId (and nodeId IF a connection is created) -> write world data ->
  client promotes the EXACT preview
  RailPath object as initial derived geometry/cache ONLY when the server
  accepted unchanged (fingerprint rule §3.1); otherwise re-derives from the
  returned record — F2.4 exact promotion preserved).
- EDIT (R12-G): client edit proposal -> server validate + commit -> client
  applies; railId stable.
- DELETE: validate references (connections, connectors); disconnect, remove,
  retire id.
- RESTORE: read world data, rebuild RailPath from endpoints, reconnect by
  nodeId.

## 5.1 Confirm exact-promotion (F2.4, frozen)

The initial confirmed geometry/cache IS the exact preview `RailPath` object
(not a re-derivation) — but ONLY when the server accepted the exact proposed
endpoints (`acceptFingerprint == previewFingerprint`, §3.1). If the server
corrected geometry, the client re-derives from the returned record. This
preserves R10F F2.4 "exact object promotion" without allowing client/server
divergence. Later edits/loads re-derive a fresh RailPath from endpoints.

## 6. Preview vs Confirmed boundary

- Preview = transient `RailPath` (F2 pipeline) + no id. Data held in
  RailsysPlacementState (unchanged, F5).
- Confirmed = authoritative `RailSegment` in world data + stable id. On
  confirm, the SAME F2 `RailPath` semantics are promoted (R10F F5 identity
  preserved).

## 7. Validation state

- Each segment carries a validation result derived at creation/edit:
  `VALID | INVALID(reason) | DEGRADED(missing asset)`. Persisted as metadata
  (REQ-P1-05).

## 8. R10F contract impact

- None. AnchorDefinition / RailPath / F5 promotion are reused as-is.
- ADDITIVE: stable id, explicit connection, cant profile EXTENSIBLE (CC-1).

## 9. Open questions

- Exact serialization (JSON/NBT) — REPLACEABLE, R13.
- nodeId issuance policy for manual join vs snap — R13 detail.
- Gauge denormalization refresh when asset changes — R12-G.
