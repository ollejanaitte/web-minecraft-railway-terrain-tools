# Connection & Boundary Rules (Phase 1.2)

Status: IMPLEMENTED. All tolerances frozen from Phase 0.6 acceptance.

## Endpoint positions
- Endpoint position is derived from the owning geometry at s=0 (START) or
  s=length (END). There is no separately stored coordinate to drift.

## Endpoint tangents
- Native tangent points along geometry start→end. Connection validation treats
  parallel (0 deg) and anti-parallel (180 deg) as aligned, so both a natural
  continuation and a reverse junction join physically.
- A PATH additionally requires same-direction heading continuity at each
  junction (native tangent angle <= 0.5 deg), because a continuous traversal
  must not reverse heading between consecutive entries.

## Tolerances (frozen)
| Check | Tolerance |
|---|---|
| join position error | <= 1e-4 m |
| tangent angle error | <= 0.5 deg |
| yaw / pitch / roll continuity at junction | <= 0.5 deg |
| boundary jump | <= 1e-4 m |

## Exact boundary ownership
- Internal boundary `B` (end of piece i, start of piece i+1) is owned by
  **piece i (the earlier piece)**: `resolve(B)` returns piece i at full local
  length.
- Final boundary (end of path) is owned by the last piece.
- Binary search: smallest entry i with `starts[i+1] >= s`; at `s == starts[i]`
  exactly (i > 0) the earlier piece is selected.
- The boundary-epsilon tests (P10) use `B±δ` only as TEST INPUTS to prove the
  neighbourhood; the resolver itself uses no magic epsilon.

## Reverse direction
- Reverse entry: `local = globalEnd - globalM`; travel tangent = -native;
  travel yaw = native + 180 (wrapped); travel pitch = -native.
- Forward/reverse consistency is tested across whole paths (P15/P16).

## Join validation (RailConnection / RailNetwork)
- `RailConnection.validate(a,b)`: null endpoints, self-connection, position
  error > 1e-4, tangent misalignment > 0.5 deg → invalid with reason + errors.
- `RailNetwork.connect(...)`: additionally rejects duplicate connections,
  unknown (unregistered) pieces, and reports endpoint state.
- Multiple connections per endpoint are allowed (future switch compatibility).

## Invalid connection handling
- Invalid connections are NEVER silently created: `connect()` returns an
  invalid `RailValidationResult` without adding a connection.
- A path with a disconnected junction is rejected at construction
  (IllegalArgumentException) — silent gaps are forbidden.

## No epsilon hacks
- Boundary handling uses the documented ownership rule + binary search, not
  `+0.001`/`-0.001` nudges.
