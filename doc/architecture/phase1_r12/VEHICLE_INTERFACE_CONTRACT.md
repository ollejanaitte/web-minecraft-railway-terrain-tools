# R12-I: Vehicle Interface Contract

Phase 1-R12 design freeze. Inputs: REQ-P2-07 (trains DEFER Phase 2),
R11-F (RTM vehicle rail usage), R10F F2 (geometry capabilities). Phase 1 ships
the INTERFACE, not the vehicle.

## 1. Goal

- Freeze the rail-side contract Phase 2 vehicles will consume. No vehicle
  implementation in Phase 1.

## 2. Rail Traversal API (frozen contract)

```text
RailNetwork.resolve(railId, s) -> PathSample        (F2 resolve, clamped)
RailNetwork.frameAt(railId, s) -> RailLocalFrame
RailNetwork.resolveAt(nodeId, travelDir, routeRef?) -> next segment ref
RailNetwork.traverse(fromNode, direction, route?) -> ordered segment refs
RailNetwork.endpointAt(railId, side) -> RailEndpoint (AnchorDefinition)
RailNetwork.gaugeAt(railId) -> double
RailNetwork.cantAt(railId, s) -> double (rollDeg)
RailNetwork.gradientAt(railId, s) -> double (pitchDeg)
RailNetwork.segmentAt(pos, withinTolerance) -> railId?   (spatial)
RailNetwork.routeAt(junctionRailId) -> committedRoute
RailNetwork.occupancy(railId) -> bool          (R-P1 hook)
RailNetwork.signalState(railId) -> int         (R-P1 hook)
```

- PathSample carries position/tangent/frame/travel direction (F2).
- Forward/reverse: PathSample.travelDirection; reverse traversal via network
  reverse query.
- End-of-track: `resolveAt` returns null beyond the last connected segment
  (vehicle stops — RTM-aligned).
- Gauge/cant/gradient are read from segment data + frames (F2/F4).

## 3. What Phase 1 must provide

- The traversal API above over the network + data model (R12-A/B).
- occupancy/signal hooks (data fields; writers are Phase 2).
- Switch route resolution (R12-C) for branch-aware traversal.
- Persisted network (R12-F) so traversal works after reload.

## 4. What is Phase 2

- Trains, bogies, formations, motion, coupling, physics, rolling/lean,
  ATS/ATC, timetable (REQ-P2-07/08).

## 5. Requirement trace

- REQ-P2-07: DEFER vehicle, but interface defined here.
- R10F F2: all geometry access via PathSample/frames (no new math).
- R11-F: RTM nearest-RailMap behaviour -> Railsys resolve by railId/position.

## 6. Open questions

- Position-based nearest-rail lookup semantics (segmentAt tolerance) — R13.
- Whether vehicle queries by railId (attached) or by world position (nearest)
  — Phase 2; contract supports both.
- Route traversal with mixed direction — R13.
