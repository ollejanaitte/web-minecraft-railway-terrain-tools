# R17_SWITCH_DESIGN — Switch / Junction (Basic)

Date: 2026-08-14 JST
Scope (from R12 roadmap): junction data, branch endpoints, route input (server
value), derived committedRoute, vehicle resolveRoute. Exit: turnout created;
route switches by input IN-SESSION (full reload restore is R23).

## 1. Position in the architecture
- R16 built the Production Rail Network (RailNode / RailConnection, explicit
  topology, traversal, crossing-without-connection).
- R17 adds the Switch / Junction on top: a junction is a RailNode with 3+
  segments where one is the main-through and the rest are branches.
- R18 adds switch animation (tongue/movable parts, smoothstep, partial-tick).
- Geometry stays on the R13/R14/R16 production pipeline (F2 RailPath,
  RailSegment, RailNode). No new geometry pipeline.

## 2. Concepts
- SwitchJunctionId: stable positive per-network id, retired-never-reused
  (same policy as NodeId/ConnectionId/RailId).
- SwitchJunction: { id, nodeId, mainIn, mainOut, branches[] }.
  - mainIn: the segment arriving at the node (its END is at the node).
  - mainOut: the through segment (its START is at the node, near-straight
    continuation of mainIn).
  - branches: each a diverging segment (its START is at the node) plus a
    route id.
- SwitchRoute: THROUGH / BRANCH / UNKNOWN. THROUGH = continue on mainOut;
  BRANCH = continue on a chosen branch.
- committedRoute: the DERIVED authoritative route (from the route input after
  validation). Route input is a server value; committedRoute is what vehicles
  resolve against.
- resolveRoute(fromSegment): given the segment a vehicle is leaving, return the
  next segment the junction commits to (mainOut or the committed branch).

## 3. Switch geometry (SwitchGeometry)
- divergence validation: the branch START forward heading vs the mainOut
  forward heading must be within the switch divergence limits
  (minSwitchAngleDeg, maxSwitchAngleDeg; e.g. 2..30 deg).
- tangent continuity: branch START tangent must continue from the node (the
  junction is exactly at the shared endpoint; position error ~0 by R16
  coalescing).
- gauge compatibility: branch.gaugeM vs main.gaugeM within GAUGE_TOLERANCE_M.
- a diverging lead-path builder (F2 fromMarkers) is provided for R18 animation
  / vehicle use; R17 validates it (finite, tangent continuity, divergence
  angle) but does not animate.

## 4. Route semantics
- setRouteInput(junctionId, SwitchRoute): server value. Validated against the
  junction's available routes; invalid -> UNKNOWN (no silent wrong route).
- committedRoute(): derived; THROUGH when input==THROUGH; the branch when
  input==BRANCH and a branch id is given; else UNKNOWN.
- resolveRoute(fromSegment): 
  - if fromSegment == mainIn  -> committed route's next segment
  - if fromSegment is a branch (its END at the node) -> mainOut (branch returns
    to through)
  - else null (not a junction input)

## 5. R17 scope boundaries
- NOT implemented: switch ANIMATION (R18), movable parts rendering (R18),
  reload restore of routes (R23 persistence), signal/crossing (R20+).
- The Zunge* parts recognised in R15 ModelPacks are bound at the metadata level
  (a junction's branch is associated with the pack's movableComponents), but
  their RENDERED animation is R18.
