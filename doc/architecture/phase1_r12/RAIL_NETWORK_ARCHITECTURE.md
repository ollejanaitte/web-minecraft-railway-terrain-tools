# R12-B: Rail Network Architecture

Phase 1-R12 design freeze. Inputs: REQ-P0-02 (Rail Network), R10F F2
continuity, R11-B (RTM RailMap/connection), current `RailNetwork` source.

## 1. Goals

- Treat multiple RailSegments as a traversable rail network.
- Separate "coordinates are near" (geometry) from "connected on the network"
  (topology). Connection is EXPLICIT production data (frozen decision).
- Provide stable node references for route traversal, editing, deletion, and
  infrastructure.

## 2. Concepts

| Term | Meaning |
|------|---------|
| RailNode | a connection point; nodeId is authoritative; node POSITION is DERIVED from members (frozen rule §4); 1..n endpoint members |
| RailEndpoint | a segment's end (A or B), referencing a nodeId or free; ANCHOR is the authoritative position |
| RailConnection | the explicit membership of two endpoints in one node (R12-A) |
| Node position | derived from the FIRST confirmed endpoint; equal endpoints within snap tolerance become one node |
| Snap | connect-on-placement: when a new endpoint lies within snapTolerance of an existing free endpoint, merge into one node (REQ-P0-02) |
| Free end | endpoint with no node (open track end) |

## 3. Explicit connection (decision)

- `RailConnection { nodeId, endpointRefA, endpointRefB, createdVia }` records
  pairwise membership; `RailNode` is the canonical membership source of truth
  (R12-A §4.3). Both are persisted.
- "Nearby but not merged" = crossing without connection: allowed only if
  `allowCrossing` policy permits (REQ-P1-08); otherwise flagged as
  INVALID(overlap) during validation (R12-J).
- Disconnected segments are valid data (no forced auto-connect).

## 4. Node / membership

- A node is an EXPLICIT persisted record (`RailNode` in R12-A §4.3): nodeId +
  member endpoint refs + kind (JOIN/JUNCTION/TERMINUS). Position is DERIVED.
- Position rule (frozen): node position = the FIRST member's endpoint anchor
  position. Membership is canonical (sorted by railId+side) so 3+ member
  junctions are unambiguous.
- Editing a member endpoint that would move the node: only allowed if all
  members move together (reposition) OR continuity is re-validated and the
  other members stay authoritative (their anchors unchanged). If continuity
  breaks, the edit is rejected or the connection is flagged for manual
  disconnect (R12-G). No silent movement of other member anchors.
- Membership: 1..n endpoints. A junction node has >=3 (root + branches).
- nodeId is stable; editing never re-issues it.

## 5. Continuity guarantees (frozen, F2-aligned)

At a connection, the merged endpoints MUST satisfy F2 continuity:
- position within `POSITION_TOLERANCE_M` (1e-4);
- heading continuity within `ANGLE_TOLERANCE_DEG` (0.5 deg);
- gradient/cant continuity: same rules (F2); cant continuity at joins is
  ensured by shared CantProfile policy (R12-C/J).
- gauge compatibility: matching asset gauge or explicit mixed-gauge policy
  (REQ-P2-01 warning; R12-J validation).

Snap merges ONLY endpoints that satisfy these; otherwise the connection is
rejected (validation error) or flagged for manual review.

## 6. Route traversal

- `RailNetwork` exposes:
  - `resolveNext(nodeId, incomingEndpointRef) -> outgoing segments`
  - `traverse(nodeId, routeRef?) -> ordered segments`
  - at a junction node, the committed route (R12-C) selects the branch.
- Route is a derived query over node membership + junction committed route; no
  separate persisted route table (matches RTM derived-route behaviour,
  REQ-P0-03).
- Reverse traversal supported (F2 PathSample travel direction).

## 7. Lookup & index

- `RailNetwork` owns `nodeIndex: nodeId -> node`, `segmentIndex: railId ->
  segment`, `endpointIndex: position -> endpoint` (spatial, REPLACEABLE
  implementation — e.g. simple hash grid, R12-J).
- World data is the authoritative store; the network is the in-memory query
  view (CACHE, rebuilt on load / edit).

## 8. Invalid topology

| Case | Handling |
|------|----------|
| Three+ endpoints at one node | valid if junction (>=3) or flagged for review |
| Duplicate connection (same pair twice) | rejected (validation) |
| Self-connection | rejected |
| Overlapping parallel rails | allowed only with allowCrossing (REQ-P1-08) |
| Disconnected free ends | valid |
| Orphan node (no segment) | pruned on repair (R12-G) |
| Node with single member | pruned or kept as terminus marker (design: prune on edit) |

## 9. Relationship to R10F F2

- F2 `RailPath` = geometry of ONE segment (frozen). Network adds the TOPOLOGY
  layer above it. A multi-rail route = ordered list of segment RailPaths.
- F2 `RailConnection.validate` reused for endpoint continuity at merge time.

## 10. Requirement trace

- REQ-P0-02 (network/snap/continuous): this doc.
- REQ-P0-03 (junction route): junction node + committed route (R12-C).
- REQ-P1-08 (overlap/crossing): node/overlap validation (R12-J).
- REQ-P1-04 (switch persistence): junction node persisted (R12-C/F).

## 11. Open questions

- Snap tolerance default (measure in R13; recommend ~0.1m, NOT frozen numeric).
- Auto-connect during continuous placement vs explicit confirm merge.
- Node count limits (junction branches) — R12-C.
