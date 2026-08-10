# Rail Geometry Contract (Phase 0.6 - PART C, frozen)

Status: FROZEN for Phase 1. Based on discovery + architecture options.
No product code changed.

## 1. Units / coordinates / conventions
- Distance unit: **metre (1 block = 1 m)**. P1. All lengths/samples in metres.
- Coordinates: world block coordinates (double), +X east, +Z south, +Y up.
- Yaw: Minecraft convention, degrees, 0 = +Z, clockwise positive from above,
  wrap (-180, 180]. sample.yaw = world heading `atan2(tx, tz)` (NOT negated).
  (Resolution of the v2 spike sign inconsistency: the SAMPLE carries the raw
  heading; entity/renderer apply MC entity conventions separately.)
- Pitch: degrees, positive = nose up (+Y). `atan2(dy, hypot(dx,dz))`.
- Roll (cant): degrees, positive = right rail lower (lean into curve). Phase
  1: field present, value 0 for straight/horizontal; cant profile data model
  only (no physics).
- Precision: doubles for geometry; EPS = 1e-6 m for direction guards;
  sampling tolerance 1e-4 m. NaN/Infinity: any sample/geometry producing
  NaN/Inf must be treated as invalid piece; count must be 0 in all
  acceptance tests (throw/guard policy below).

## 2. RailGeometry contract
```
interface RailGeometry {
  double lengthM();                       // total 3D arc length, metres, exact for straight
  int pieceId();
  RailSample sampleByDistance(double distanceM);  // local distance 0..lengthM
  // Phase 1 required helpers:
  RailSample sampleByProgress(double progress);   // convenience via table
  double lengthAt(double progress);               // arc length at normalized progress
  ArcLengthTable table();                         // cached, lazy, invalidated on edit
}
```
Behavior:
- distanceM clamped to [0, lengthM]; past-end returns end sample, negative
  returns start sample (clamping policy, deterministic).
- sample fields: distanceM (the requested distance), x/y/z, yawDeg,
  pitchDeg, rollDeg (0 in Phase 1), pieceId.
- Deterministic: same geometry + distance -> identical sample (no RNG).
- NaN/Inf: if inputs or computed outputs are non-finite, throw
  IllegalStateException with pieceId; acceptance asserts 0 occurrences.

## 3. Straight
- `Straight(sx,sy,sz, ex,ey,ez, pieceId)`; length = sqrt(dx^2+dy^2+dz^2)
  exact. Graded allowed (ey != sy): constant pitch. sample: linear lerp;
  yaw = atan2(dx,dz); pitch = atan2(dy,hypot(dx,dz)); roll 0.

## 4. HorizontalBezier (curve)
- Cubic Bezier over X/Z with two endpoints + two control points; Y from
  endpoint linear interpolation (gradient) in Phase 1 (or a paired vertical
  piece). length via adaptive polyline; ArcLengthTable reparameterizes
  metres->t.
- sample: t = table.distanceToParam(distanceM); point = Bezier(t);
  tangent = analytic derivative; yaw = atan2(tx,tz); pitch =
  atan2(dy,hypot(dx,dz)).

## 5. VerticalBezier (gradient transition, data model in Phase 1)
- Defines Y as a function of horizontal distance to smooth gradient changes.
  Phase 1.1: implement class + exact length for a pure vertical profile;
  used by test course gradient piece. pitch continuous.

## 6. ArcLengthTable
- Fields: split, cumulative metres[], paramAt[] (distance->parameter and
  parameter->distance).
- Adaptive split: base = clamp(round(lengthM*32), 8, 384); refine where
  curvature high (within memory budget).
- distance->parameter: binary search on cumulative, linear interp between
  entries (as v2 spike tForDistance).
- Built lazily once per piece; invalidated on edit. Cached.
- Long curves: split cap keeps memory bounded; accuracy verified by
  numerical acceptance (arc-length error tolerance).

## 7. RailSample (fundamental output)
`distanceM, x, y, z, yawDeg, pitchDeg, rollDeg, pieceId`.
`progress` is a derived lookup key only, NOT spacing authority (P6).

## 8. RailPiece contract
- Piece ID (int, unique per world store).
- Owns: geometry reference, endpoints (world x/y/z), length (from geometry),
  connectivity (start/end node ids).
- Invalid piece (missing geometry, non-finite, zero/negative length):
  rejected at creation; load drops piece with a warning (no silent crash).
- Future switch: piece may later expose branches; Phase 1 data model keeps a
  type field + params so a switch piece can be added without schema change.

## 9. RailPath contract (Phase 1.2)
- Ordered traversal of pieces; accumulated physical distance (global metres).
- `resolve(globalM) -> (pieceId, localM, sample)`:
  - binary search over piece start offsets;
  - piece boundary owned by the earlier piece (exit sample at its end);
    final boundary owned by last piece (as v2 Course.resolve).
  - clamp globalM to [0, totalLength].
- Direction/reverse: path can be traversed both ways (dir +1/-1); sample
  yaw carried as-is; a reverse traversal flips yaw by 180 for pose use
  (Phase 2 detail, API reserved).
- Train-facing boundary (Phase 2): distance -> sample; bogie anchors at
  distance ± offset; body pose from bogie chord.
- Future branching: RailNetwork adjacency + switch state select the next
  piece at a node; path records history (Phase 2/3). Phase 1 provides the
  resolve API + next-piece selection at nodes (first connected / explicit).

## 10. Continuity contract
- Within a piece: smooth by construction.
- Across pieces: at a shared node, exit position of piece A must match entry
  position of piece B within EPS_POS = 1e-4 m; heading (yaw/pitch) continuity
  carried by the resolver (sample at the boundary uses the shared node);
  roll continuity trivially 0 in Phase 1.
- Intentional discontinuity: a deliberate break (e.g., unconnected pieces)
  is allowed only at distinct (non-connected) nodes; a connected junction
  must be continuous.
- Acceptance: assert continuity at every junction in the test course.

## 11. Gradient / vertical profile
- Phase 1 scope: gradient allowed on Straights (constant pitch) and a
  VerticalBezier for transitions (data model + class). Maximum test grade:
  10% (5.71 deg) for the test course. Grade defined as dy/ds.

## 12. Cant / roll
- Phase 1 decision: **data model only.** RailSample.rollDeg present, value 0
  for straight/horizontal in Phase 1. CantProfile class (per-distance roll)
  may be added as data model without physics. Roll sign: positive = right
  rail lower (lean into curve). Degrees. No body lean in Phase 1.

## 13. Invalid state / error policy
- Non-finite input/output -> IllegalStateException (counted, must be 0).
- Zero/negative length piece -> rejected at creation.
- Degenerate Bezier (all points identical) -> treated as zero-length,
  rejected unless length >= EPS.
- Clamping is explicit and deterministic; never throws for out-of-range
  distance (clamps), except NaN/Inf.
