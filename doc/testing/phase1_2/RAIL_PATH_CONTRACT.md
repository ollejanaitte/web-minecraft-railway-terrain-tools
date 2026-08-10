# RailPath Contract (Phase 1.2)

Package: `net.minecraft.railsys.path`. Status: IMPLEMENTED.

## Ordered entries
- A RailPath is an ordered list of `RailPathEntry` (piece + traversal direction
  +1/-1 + global start/end cumulative distance).
- Cumulative prefix: `starts[0]=0`, `starts[i] = sum of previous lengths`.
  `totalLength = starts[n]`.
- A piece may appear more than once, even with different directions; the
  geometry is never rewritten.

## Global distance API (metres, never normalized progress)
- `PathSample resolve(double globalM)` — primary resolver.
- `PathSample sampleByDistance(double globalM)` — alias of resolve.
- Returns: global distance, entry index, piece id, piece, local distance,
  traversal direction, native `RailSample`, travel yaw/pitch, native
  `RailLocalFrame`.

## Local distance
- FORWARD (+1): `local = globalM - globalStart`.
- REVERSE (-1): `local = globalEnd - globalM`; travel tangent = -native;
  travel yaw = native+180 (wrapped); travel pitch = -native.

## Boundary ownership (frozen Phase 0.6)
- Internal boundary at `B` is owned by the EARLIER piece (exit sample at its
  full local length); the final boundary is owned by the last piece.
- Implemented as upper-bound binary search `starts[i+1] >= s` (smallest i) with
  an exact-boundary step-back. NO magic epsilon in the resolver.

## Reverse
- `reverse()` builds a new path: same pieces reversed, each direction -1.
- Consistency invariant: `forward.resolve(s)` and `reverse.resolve(L-s)`
  land at the same world position with opposite travel tangents.

## Clamp / error policy
| Case | Policy |
|------|--------|
| s < 0 | clamp 0 |
| s > total | clamp total |
| NaN / ±Inf | IllegalStateException |
| empty path | rejected at construction |
| zero-length piece | rejected at construction |
| disconnected consecutive entries | rejected at construction (silent gaps forbidden) |
| unexpected exceptions | counted; acceptance requires 0 |

## Sample result
- `PathSample` exposes both native (geometry) and travel-adjusted values so
  that boundary continuity checks use native frames consistently while train
  pose uses travel values.
