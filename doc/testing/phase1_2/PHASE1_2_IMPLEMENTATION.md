# Phase 1.2 Implementation Plan — Rail Piece / Rail Path

Status: **COMPLETE** — numerical PASS + Flat-World visual PASS → VERDICT PASS
(2026-08-11)

## Purpose

Build on the PASSED Phase 1.1 Production Geometry Core
(`net.minecraft.railsys.geometry`, `src/geometry-core/java`) to connect multiple
RailGeometry into:

- RailPiece
- RailEndpoint / RailConnection
- RailPath (ordered traversal + cumulative distance + global distance resolver)

so that a caller supplies only the **physical path distance s [m]** and receives
the correct piece, local distance, position, tangent, yaw/pitch, and continuous
RailLocalFrame even when a boundary between pieces is crossed.

## Frozen prerequisites (Phase 0.6 contract)

- Distance unit metre (1 block = 1 m); +X east, +Y up, +Z south.
- Yaw = `atan2(tx,tz)` degrees, wrap (-180,180], NOT negated.
- Pitch = `atan2(ty,hypot(tx,tz))`, positive = nose up.
- Roll: positive = right rail lower (cant data model only).
- EPS = 1e-6 m; sampling tolerance 1e-4 m.
- **Boundary ownership (RAIL_GEOMETRY_CONTRACT.md §9, frozen):**
  internal piece boundary is **owned by the earlier piece** (exit sample at its
  end); the final boundary (end of the whole path) is owned by the last piece.
- Resolver: binary search over piece start offsets; clamp globalM to [0, total].
- Join position <= 1e-4 m; yaw/pitch/roll continuity <= 0.5 deg; boundary jump
  <= 1e-4 m.
- NO normalized segment progress as movement authority (external API is metres).
- NO A* route search, NO switch routing, NO persistence rewrite (Phase 1.2 scope).

## Checkpoints

| CP | Content | Status |
|----|---------|--------|
| 0 | Baseline / repo / design audit + plan doc | DONE |
| 1 | RailPiece + RailEndpoint + connection validation | DONE (commit 068d3eff) |
| 2 | RailPath + cumulative distance + global resolve + sampleByDistance | DONE (commit 07988bac) |
| 3 | Exact boundary resolver + reverse traversal + clamp/error policy | DONE (commit 07988bac) |
| 4 | Lightweight RailNetwork (registry + adjacency + validation) | DONE (commit 07988bac) |
| 5 | Multi-piece numerical regression P01–P22 + Phase 0.6 tolerance | DONE (commit 775b52a6) |
| 6 | Flat Validation World visual proof (SS-R1_2-01..08) | DONE (commit b1eff42e) |
| 7 | Full regression / cleanup / docs / Final Report | DONE (final commits) |

## Final results

- Harness: PASSED=84 FAILED=0 SKIPPED=3 (Phase 1.1 49 + Phase 1.2 35).
- Build: `./gradlew makeMainOfflineDownload` BUILD SUCCESSFUL.
- Visual: 8/8 real Eaglercraft screenshots on Hardware Vulkan (NVIDIA GTX 1050).
- Boundary ownership: internal boundary owned by the earlier piece; exact
  binary-search rule (no magic epsilon); join pos 0.0 m; yaw/pitch/roll 0.0 deg.
- Forward/reverse world-position consistency: 8.6e-14 m.
- Final report: `Railsys_Phase_1.2_Rail Piece.txt`.

## Package / layout

- New package `net.minecraft.railsys.path` under `src/geometry-core/java`
  (compiled into game main + harness; no new source set needed).
- Tests: `src/harness/java/railv2test/tests/RailPathTest.java` (+ optional
  `RailNetworkTest.java`), registered in `Runner.TEST_CLASSES`.

## Production classes (planned)

- `RailEndpoint` (enum Side START/END; geometry-derived position + native
  tangent; stable endpoint identity derived from pieceId + side; connection state)
- `RailPiece` (stable piece id = geometry.pieceId(), geometry ownership,
  endpoints, length, validation, optional metadata hook)
- `ConnectionValidation` / `RailConnectionResult` (valid, reason,
  positionError, angleError) — no over-engineering
- `RailConnection` (two endpoints + validation result; connect/disconnect)
- `RailPathEntry` (piece + traversal direction +1/-1 + global start/end)
- `PathSample` (globalM, entry index, pieceId, localM, travelDirection,
  native RailSample, travel yaw/pitch, native RailLocalFrame)
- `RailPath` (ordered entries, prefix lengths, binary-search resolve,
  sampleByDistance, reverse(), validation status, exact boundary rule)
- `RailNetwork` (piece registry, endpoint connections, adjacency lookup,
  network validation; NO A*, NO switches)

## Exact boundary ownership rule (frozen)

Internal boundary at global distance `B = start[i]` (i > 0) resolves to piece
`i-1` (the earlier piece) with local distance = its full length. This is
implemented via an upper-bound binary search plus an exact-boundary check
(`start[idx] == s` → step back one entry). No magic epsilon is used in the
resolver; P10 uses B±δ only as *test input* sensitivity.

## Error / clamp policy (mirrors Phase 1.1 geometry)

| Case | Policy |
|------|--------|
| s < 0 | clamp to 0 |
| s > totalLength | clamp to totalLength |
| NaN / Inf s | IllegalStateException |
| empty path | reject at construction (IllegalArgumentException) |
| zero-length piece | reject at construction |
| disconnected consecutive entries | reject at construction (silent gaps forbidden) |
| unexpected exception | counted, must be 0 in acceptance |

## Numerical tests (P01–P22)

Straight→Straight; Straight→Curve→Straight; gentle chain; tight chain; S-curve;
gradient chain; curve+gradient chain; 3+ piece long path; exact internal
boundary; boundary epsilon neighbourhood; path start; path end; out-of-range
clamp; reverse single piece; reverse multi-piece path; forward/reverse
consistency (world position identical, travel tangent opposite); disconnected
rejection; zero-length rejection; NaN/Inf rejection; dense multi-piece sampling;
local-frame continuity; determinism.

## Visual evidence (Flat Validation World, Hardware Vulkan)

SS-R1_2-01_MULTI_STRAIGHT .. SS-R1_2-08_OVERVIEW (real Eaglercraft screenshots,
reusing the Phase 1.1 screen-hook automation pipeline). Debug visualization is
validation-only (world-name gated), no normal-world pollution.

## Definition of done

- Harness FAILED = 0 (Phase 1.1 regression stays green).
- `./gradlew makeMainOfflineDownload` BUILD SUCCESSFUL.
- All P01–P22 PASS; NaN/Inf = 0; unexpected exceptions = 0.
- Flat World visual evidence 8/8 on hardware renderer + visual review PASS.
- dirty v1 exact preserved; no scope creep; no secret leak.
- Checkpoint commits merged to GitHub main; Final Report
  `Railsys_Phase_1.2_Rail Piece.txt` merged + attached to Discord.
