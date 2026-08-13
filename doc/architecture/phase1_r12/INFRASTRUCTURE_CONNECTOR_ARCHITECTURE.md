# R12-H: Infrastructure Connector Architecture

Phase 1-R12 design freeze. Inputs: REQ-P1-06 (rail signal/occupancy +
connection hooks), R11-E (RTM Connection/MachineType/signal/crossing),
R12-A (stable ids), R12-G (delete/repair reference safety).

## 1. Goal

- Standard attachment contract so Signal / Crossing / Switch Controller /
  Station Equipment / Future Sensor can connect to the Rail Network safely.
- Phase 1 delivers the rail-side CONNECTION capability + stable contract.
  Full signal/crossing LOGIC is Phase 2.

## 2. InfrastructureConnector (frozen)

```text
InfrastructureConnector {
  connectorId                       (stable, issued at attach)
  type: SIGNAL | CROSSING | SWITCH_CONTROLLER | STATION_EQUIPMENT | SENSOR
  targetRailId                      (stable rail reference)
  nodeId?                           (optional: attach at a node)
  distanceS: double                 (s[m] along rail; -1 = at node)
  side: LEFT | RIGHT | CENTER
  travelDir: byte                   (direction relative to rail)
  state: int                        (equipment state; data only in Phase 1)
  routeRef: routeId?                (optional: which branch, for switch)
  eventHooks: enum[]                (which rail events it subscribes to)
  active: bool
}
```

## 3. Rail-side hooks (R-P1, data only)

- `signalState: int` and `occupied: bool` on RailSegment (R12-A §4.1) — the
  rail exposes them; WHO sets them (detector logic, trains) is Phase 2.
- `railEvent(type, s, payload)`: a hook list per connector (occupancy change,
  route change, delete). Phase 1 defines the event NAMES; dispatch of real
  events comes with Phase 2 systems.

## 4. Attachment semantics

- Attach by railId + distance s (or nodeId). Position is DERIVED from F2
  RailPath.resolve(s) (frames give pos/dir/side).
- Connector persists (R12-F) and is validated against orphan (R12-G repair).
- Multiple connectors per rail allowed; one connector per signal/crossing
  target.
- Delete of a rail invalidates its connectors (R12-G).

## 5. Signal

- Phase 1: connector holds `protectedRouteRef` (which segment/route) + target
  direction + occupancy hook reference. Signal MODEL rendering (R12-E/D) and
  LOGIC (detector, block section, state machine) are Phase 2.
- Rail side is ready: railId, distanceS, direction, signalState/occupied,
  routeRef.

## 6. Crossing

- Phase 1: connector holds `crossingPoint` (railId + s + side), `approachZone`
  (s range), affected tracks list, occupancy hook, activation event name.
  Crossing automation (barrier animation, sound) is Phase 2 (REQ-P2-05).
- The rail side exposes occupancy/route; the crossing logic (Phase 2) drives
  its animation.

## 7. Phase boundary (frozen)

Phase 1 ships:
- InfrastructureConnector data model + persistence + validation.
- Rail signal/occupancy data hooks + event name catalogue.
- Attachment UX (place connector on rail).

Phase 2 ships:
- train detector logic, block-section system, signal state machines,
  ATS/ATC, timetable, crossing gate automation, connector models
  (Relay/DSS). (REQ-P2-03..06,08)

## 8. Requirement trace

- REQ-P1-06: this doc (rail hooks + connection data).
- REQ-P0-01 (connectorId), R12-G (delete invalidates connectors).
- R11-E: RTM Connection analogue (clean-room).

## 9. Open questions

- Event catalogue completeness — R13 with Phase 2 systems.
- RouteRef semantics for signal protected route — R13.
- Whether connector is a world block (attachment entity) or pure data —
  R13 design decision (RTM uses blocks + Connection; Railsys may use pure data
  + optional block).
