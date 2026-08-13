# R16_PRODUCTION_RAIL_NETWORK — Phase 1-R16 Design

Date: 2026-08-13 JST

## 1. Purpose
Two goals:
- A) Correct the R14 Standard Closed Loop "octagonal" appearance into a
  production rounded rectangle (4 straights + 4 true 90° curves).
- B) Build the Production Rail Network foundation (RailNode, RailConnection,
  Endpoint Snap, Continuous Placement, Explicit Topology, Traversal).

## 2. Root cause of the octagonal look
`StandardClosedLoopCourse.corner(...)` created corner anchors with handle=1.0 m.
F2 `HorizontalBezierGeometry.fromAnchors` places Bezier controls at
C1=P0+T0/3, |T0|=handle, so controls sat 0.33 m from the endpoints and the
Bezier could not bend; the corner hugged the chord (14.16 m vs true quarter
circle 15.71 m), producing the octagonal appearance.

## 3. Geometry correction (R16-02)
Standard quarter-circle cubic Bezier: control distance = k*r with
k = 4/3*(sqrt(2)-1) ≈ 0.55228475. Since F2 divides the Hermite tangent by 3,
the corner anchors use handle = 3*k*r ≈ 1.656854*r. Same F2 pipeline
(`RailPath.fromMarkers` -> `HorizontalBezierGeometry`); only the handle
parameter (a production AnchorDefinition field) changed. Verified numerically
(corner 15.7102 m, sagitta 2.9289 m, radius error ≤ 0.078 m, closure 0).

## 4. Production RailNetwork (geometry-core package net.minecraft.railsys.network)
- `NodeId`, `ConnectionId`: stable positive per-network ids, retired-never-reused,
  REPLACEABLE serialization (R12-F style). Parse/isValid helpers.
- `RailNode`: explicit network connection point (nodeId, x/y/z, endpoint
  memberships, lifecycle ACTIVE/RETIRED, connectionEligible). Coalesced by
  position (NodeCoalesceTolerance = 0.5 m).
- `RailConnection`: explicit validated link between two segment endpoints at a
  node. Rejects self, duplicate, invalid/retired endpoints, incompatible gauge,
  excessive position gap, excessive tangent mismatch. Carries measured errors.
- `EndpointSnap`: candidate discovery (position + tangent tolerance, gauge),
  unique vs ambiguous. Proximity alone never auto-connects.
- `ProductionRailNetwork`: registerNode / node / nodes / addEndpoint /
  nodeForEndpoint / connect / connections / connectionsOf / removeNode /
  removeConnection / clear / validateTopology / forwardCycle / reverseCycle /
  nextSegment / previousSegment / NodeCoalesceTolerance.
- `ClosedLoopTopology`: builds 8 nodes + 8 connections for the 8-segment loop.

## 5. Topology validation (validateTopology)
Reports: dangling endpoint (active segment endpoint without a node), orphan
node (< 2 endpoints), duplicate membership, disconnected segment (BFS from the
first segment over connections). A valid closed loop has NONE of these and is
one closed cycle.

## 6. Traversal
- forward: nextSegment (connection at a segment's END) walks the network.
- reverse: previousSegment (connection at a segment's START).
- forwardCycle/reverseCycle: walk with cycle detection + step guard (maxSteps),
  returns the ordered segment list (start repeated at end when closed).

## 7. Game layer
- `RailsysNetworkStore`: world-scoped ProductionRailNetwork; rebuilt from the
  authoritative world store (attach each endpoint to a coalesced node, connect
  within each node). resetForNewWorld on world entry.
- `RailsysNetworkCommands`: /railsys16 build|topology|status|verify|forward|
  reverse|snap. Never creates/deletes rails; only topology over confirmed rails.

## 8. Scope boundaries (R16)
NOT implemented: Switch geometry (R17), Switch animation (R18), Infrastructure
connector (R19), Signal/Crossing (R20+), Confirmed geometry editing (later),
Delete/Repair (later), Persistence (later), LOD/perf (later), Vehicles (Phase 2).
The network is a production foundation for R17.

## 9. Crossing Without Connection (R16-13)
Two segments that geometrically cross do NOT connect unless an explicit
RailNode + RailConnection exists (j01). This is the contract for future diamond
crossings/switches.
