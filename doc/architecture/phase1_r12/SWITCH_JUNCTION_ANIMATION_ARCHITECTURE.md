# R12-C: Switch / Junction / Animation Architecture

Phase 1-R12 design freeze. Inputs: REQ-P0-03 (switch creation + route),
REQ-P0-04 (switch animation), REQ-P1-04 (switch persistence/editing), R10F F2
piece params, R11-B (RTM SwitchType/Point/animation), R11-D (persistence).

## 1. Goals

- A junction is Topology + Route + Movable Geometry + Animation, designed as
  SEPARATE state, never a single boolean.
- Vehicles resolve the active branch deterministically.
- Persistence stores geometry + input; route + animation are transient/derived.

## 2. State separation (frozen)

| State | Owner | Persisted? | Source |
|-------|-------|-----------|--------|
| TOPOLOGY | Junction record (root + branch endpoints, nodeId) | YES | stable data |
| ROUTE | derived each tick from powered input | NO | routeInput + power |
| ANIMATION | transient progress (0..1) | NO | last animation, reset on load |
| RENDER | per-frame mesh/frame state | NO (cache) | topology + animation + asset |
| VEHICLE TRAVERSAL | per-vehicle branch selection | NO | topology + committed route |

These MUST NOT be conflated. R12-C2 below defines each.

## 3. Junction data model (frozen)

```text
Junction {
  railId
  rootNodeId                       (shared origin node)
  branchRailIds[]                  (independent RailSegments, own railIds;
                                    through + diverging branches)
  points[]: JunctionPoint
  routeInput: RouteInput           (powered source ref)
  family: BASIC | SINGLE_CROSS | SCISSORS_CROSS | DIAMOND_CROSS
  desiredRoute: routeId            (input requests)
  committedRoute: routeId          (vehicles traverse this)
  animationProgress: transient
}
```

Branch ownership (frozen): branches are INDEPENDENT RailSegments with their
own railIds, rooted at the shared node (R12-A §4.4). The Junction references
them by `branchRailIds` and does NOT own branch geometry. JunctionPoint holds
the branch DIRECTION + route binding, not the geometry.

JunctionPoint (analogue of RTM Point concept, clean-room):

```text
JunctionPoint {
  branchRailId                     (which branch segment)
  branchDir: LEFT | RIGHT | NONE
  tongueIndex: double              (s position of tongue along main; asset keyframes
                                    own the tongue geometry/pose — see 3D_RAIL §8)
}
```

## 4. Route definitions

- A Route is a first-class type:
  ```text
  Route {
    routeId                        (stable, per junction; e.g. main, branchA, ...)
    junctionRailId
    orderedBranchRailIds[]         (ordered traversal through the junction)
    tieBreakIndex                  (deterministic order for 2+ routes)
  }
  ```
- Basic turnout: 2 routes (main, branch). Single/Scissors: 2+ routes with a
  moving middle. Diamond: 2 routes, both always open, no switching.
- Route SELECTION is a 3-value model (frozen — never a single boolean):

| State | Meaning | Set by |
|-------|---------|--------|
| `desiredRoute` | the route the input requests | input change (powered/server) |
| `committedRoute` | the route the junction is currently locked to for VEHICLES | set at animation COMPLETE (or lock condition) |
| `traversalRoute` | the route a specific vehicle follows | resolveRoute per vehicle = committedRoute |

- `committedRoute` starts = desiredRoute at load. On input change, a route
  change request is QUEUED; `desiredRoute` updates immediately (for input
  bookkeeping), `committedRoute` stays until animation completes; then
  committedRoute = desiredRoute.
- Vehicles traverse the COMMITTED route (not the raw input), so traversal
  NEVER disagrees with the finished switch. During animation, vehicles keep the
  previous committed route until it completes (or a lock/clearance condition is
  met — Phase 2).
- Boolean powered input selects among Basic routes (powered -> branch, else
  main). For 2+ route families, the input carries a route index (server value)
  or a deterministic tie-break.

## 5. Movable geometry

- Movable parts = tongue rails (Zunge) + (for scissors) middle rails.
- Each movable part is defined by an animation keyframe:
  `{ startOffset, endOffset, startYaw, endYaw, durationTicks }` — Railsys
  CLEAN-ROOM design, NOT RTM constant copy (R11-C boundary).
- Static parts (base, lead rails) never move.

## 6. Animation state machine (frozen)

```
IDLE (committedRoute == desiredRoute)
  -> on input change: DESIRED = new route (bookkeeping), COMMITTED unchanged
  -> ANIMATING (progress 0..1 toward DESIRED, partial-tick interpolation)
  -> COMPLETE (COMMITTED = DESIRED; renderer keeps final pose)
  -> idle
```

- progress is interpolated with a smoothstep (Railsys choice; RTM uses a
  sigmoid — reference only).
- `committedRoute` changes ONLY at COMPLETE (or a lock condition). Vehicles
  read committedRoute, so traversal and rendered switch never diverge.
- duration: Railsys default TBD by measurement (R12-J), ~0.5-1.0s target;
  NOT frozen numeric.
- Mid-animation save/reload: animation resets to idle with committedRoute =
  derived from persisted input on load (matches RTM; REQ-P0-04).
- Multiple players: route is world/input-driven (shared); animation is
  client-side interpolation of the shared route-change event; committedRoute is
  server-authoritative (F6).

## 7. Rendering

- Static parts render from the normal RailPath (F2) of each branch.
- Movable parts render from the branch RailPath with the animation offset
  applied to the tongue frames (R12-D mesh + per-frame transform).
- `RailLocalFrame` at the tongue distance gives the basis.

## 8. Vehicle route resolution

- `Junction.resolveRoute(vehicleS, travelDir) -> segmentRef`:
  - returns the COMMITTED route's ordered continuation through the node
    (never the raw input mid-animation);
  - Diamond => nearest of both committed routes (RTM behaviour);
  - before first load, committedRoute = derived from persisted input.

## 9. Persistence (R12-F detail)

- Persist: junction railId, family, points (branchRailId + branchDir +
  tongueIndex), routeInput ref, branchRailIds. Branch GEOMETRY lives in the
  independent RailSegment records (their own railIds + endpoints), persisted
  normally; the Junction does NOT own branch endpoints.
- Do NOT persist: committedRoute (derived), animation progress (transient).
- On load: rebuild branch segments from their endpoints, rebuild the Junction
  references by railId, read input, derive committedRoute, animation starts
  idle.

## 10. Editing / deletion (R12-G)

- Edit: change branch endpoints / family / input source; rebuild topology;
  route re-derived.
- Delete: validate connected rails + connectors; remove branch segments;
  root node cleanup; retire id.

## 11. Failure / recovery

- Invalid branch geometry (degenerate) -> validation error at creation, no
  partial junction.
- Missing asset for movable parts -> fallback static render + warning
  (REQ-P1-05).
- Inconsistent input source -> route defaults to main + warning.

## 12. Requirement trace

- REQ-P0-03: this doc (junction + route).
- REQ-P0-04: this doc (animation).
- REQ-P1-04: persistence + editing (this doc + R12-F/G).
- R10F F2: branch geometry uses RailPath.fromMarkers (piece params reserved).

## 13. Open questions

- Tongue timing constant (measure in R13).
- Junction family scope for Phase 1 (Basic first; scissors/diamond later —
  R13 decision).
- Max branch count per junction (R13; recommend >=3, >=4 scissors).
- Eaglercraft powered-input representation (redstone block vs server value).
