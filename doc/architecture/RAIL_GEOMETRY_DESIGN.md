# Rail Geometry Design

Design-only (Phase -1). Pseudo-interfaces; no Java implementation.

Mirrors RTM's RailMap concept [R-I]: a rail piece provides, at any distance
along its length, position + orientation. This is the single most important
gap versus v1 (which only had segment-progress and raw-t Bezier).

---

## 1. RailSample (the fundamental output)

```
RailSample {
  distanceM: double       // metres from piece start (3D, arc-length)
  x,y,z: double           // world position
  tangent: (tx,ty,tz)     // unit forward
  yaw: double             // degrees
  pitch: double           // degrees (+up)
  roll: double            // degrees (cant)
  curvature: double       // 1/metres (horizontal), optional
  gradient: double        // dy/ds (pitch tangent)
  cant: double            // roll degrees
  pieceId: int
  dir: +1 / -1            // traversal direction on this piece
  progress: double        // [0,1] derived (lookup key only, NOT spacing authority)
}
```

### Required API (pseudo)
```
interface RailGeometry {
  double length();                          // total 3D metres
  double lengthAt(double progress);         // arc-length at normalized progress
  RailSample sampleByDistance(double distanceM);       // main entry
  RailSample sampleByProgress(double progress);        // convenience (uses table)
  double nearestDistance(double x, double z);          // for spawn/attract (metres)
  Vec3  pointAtDistance(double distanceM);
  double yawAtDistance(double distanceM);
  double pitchAtDistance(double distanceM);
  double rollAtDistance(double distanceM);             // cant
  double curvatureAtDistance(double distanceM);
  RailSample[] sampleAll(int split);                  // cache/preview
  RailBranch[] branches();                            // SWITCH only
  ArcLengthTable table();
}
```
Acceptance: for any continuous path (straight+curve+vertical+switch), the
six functions x/y/z/yaw/pitch/roll are continuous (no jumps) across piece
boundaries (piece-end/next-piece-start continuity enforced at resolve time
by snapping to the shared node).

---

## 2. Line types (first-class)

### Straight (horizontal + gradient)
- start/end heading yaw; optional constant pitch (gradient).
- length = sqrt(dx^2+dy^2+dz^2) exactly.
- distance<->progress linear.

### Curve (horizontal cubic Bezier)
- Two endpoints + two control points on X/Z.
- Length via adaptive polyline; arc-length table reparameterizes so that
  each progress step is ~equal metres (RTM uses normalizedParameters [R-I]).
- yaw from tangent; pitch from endpoint heights; roll from cant profile.

### Vertical curve
- Vertical Bezier over Z/Y (or along X) to smooth gradient transitions.
- pitch continuous (no corner). Height = P0.y*...+P3.y.
- First-class so gradient changes are smooth, not abrupt.

### Switch (turnout) - real geometry
- A SWITCH piece exposes `branches[]`; each branch is itself a RailGeometry
  (e.g. straight branch + curved branch). Selecting the route selects the
  active branch. state persisted; traversal uses the active branch.
- Types to support (Phase 3): Basic turnout, Single cross, Scissors cross,
  Diamond cross [R-I taxonomy]. A switch is NOT merely a "next segment id".

### Turntable (optional, later)
- Rotating geometry; treated as a special piece (future).

---

## 3. Arc length

- Straight: exact closed form.
- Bezier (horizontal/vertical): adaptive polyline subdivision.
  - base split = clamp(round(length*32), 8, 384) (RTM-style [R-I]);
    rebuild at finer split where curvature is high (adaptive) if within
    memory budget.
- ArcLengthTable:
  ```
  split: int
  cumulative: float[]   // metres at each i (length at i/split)
  paramAt[i]: float     // raw parameter t at equal-metre i (reparameterization)
  ```
  - distance -> progress: binary search on cumulative.
  - progress -> distance: table lookup.
- Cache: table built once per piece (lazy on first sample), invalidated on
  edit. Memory: split ~ 32/metre of curve capped (e.g. 384) => tiny.
- Nearest point: sample-all then local refine (one pass over split).
- Huge-network: only pieces near active trains/edits are sampled per tick
  (ActiveRailCache, see PERFORMANCE_DESIGN.md).

---

## 4. Continuity guarantees

- Within a piece: sample functions are smooth by construction.
- Across pieces: a train resolves at a node; exit sample of piece A (dir)
  and entry sample of piece B must match position within EPS (1e-4 m) and
  heading within a small bound; the resolver snaps to the node and keeps
  yaw/pitch/roll continuous by carrying the exit values until the next
  sample crosses. Acceptance tests assert continuity at every junction
  (straight-curve, curve-curve, curve-slope, switch both branches).

---

## 5. Cant / roll

- CantProfile: per-distance roll (0 at tangent, ramps to max in curve,
  returns to 0). centerCant/edgeCant concepts (RTM RailPosition stores
  cantCenter/cantEdge/cantRandom [R-I]).
- Body roll = geometry roll at the car center; bogie roll = geometry roll
  at each bogie; body lean averaged. Used by pose and rail render.

---

## 6. Relation to RailNetwork and RailPiece

- RailGeometry = pure shape (no world side effects).
- RailPiece = geometry + world blocks + state + network refs.
- RailNetwork = registry of pieces + node adjacency + routing + route search.
- Route selection at a node: switch state > planned route (A*) > first
  connected. Path entries record the actual traversal (route history) so
  followers stay on the same physical route (fixes v1 follower teleport).

---

## 7. Migration of v1 geometry

- v1 RailSegment STRAIGHT -> Straight geometry (exact).
- v1 RailSegment CURVE + RailCurveData (6 control coords) -> horizontal
  Bezier geometry (same 4 points; recompute arc length).
- v1 SLOPE (unused) -> gradient straight or vertical curve.
- v1 progress semantics replaced; old progress values only meaningful via
  lengthAt().
