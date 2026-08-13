# R13_VALIDATION_LIMITS — Phase 1-R13

Date: 2026-08-13 JST
The production numeric limits frozen at R13 (REQ-P0-11, R12-J §2.1). Values
justified in `R13_LIMIT_MEASUREMENT_RESULTS.md`.

## Frozen limits (RailLimits.java)

| Constant | Value | Notes |
|----------|-------|-------|
| GEOMETRY_EPS | 1e-6 | F2, unchanged |
| MIN_RAIL_LENGTH_M | 0.25 | below this -> INVALID(too short) |
| MAX_RAIL_LENGTH_M | 256.0 | above this -> INVALID(too long) |
| MAX_GRADIENT_DEG | 45.0 | endpoint pitch beyond -> INVALID |
| MAX_CANT_DEG | 45.0 | cant beyond -> INVALID |
| MIN_GAUGE_M / MAX_GAUGE_M | 0.6 / 1.8 | gauge snapshot range -> INVALID |

## Rail-level validation (R13 scope, RailSegmentValidator)

Checks: null segment/id, retired lifecycle, duplicate/retired id (vs world
store), missing endpoint, non-finite anchor, geometry build failure, zero/non-
finite length, too short, too long, gradient > 45, cant > 45, gauge out of
[0.6,1.8], missing asset id.

Result: VALID / INVALID(reason) — DEGRADED reserved for future missing-asset
rendering fallback (REQ-P1-05, R13 keeps VALID/INVALID only).

## Deferred validation (owner phases)

- connection topology / snap tolerance → R16
- junction topology / switch route → R17
- switch animation state → R18
- connector target consistency → R19
- persistence cross-reference → R23
