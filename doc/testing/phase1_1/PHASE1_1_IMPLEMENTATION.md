# Phase 1.1 Implementation Log

Status: IN_PROGRESS → numerical core complete (visual pending in later checkpoints)

## Baseline (Checkpoint 0)

| Item | Value |
|------|-------|
| Start SHA | `ceeac6ca` |
| Phase 0.6 | PASS / DESIGN GATE OPEN |
| Phase 0.7 | PASS / Research Gate READY / Impact B Clarification |
| dirty v1 | PRESERVED (md5 match) |
| Spike | `net.minecraft.railv2.*` retained (no wholesale delete) |
| Package | `net.minecraft.railsys.geometry` under `src/geometry-core/java` (shared main+harness) |

## Clarifications applied (from Phase 0.7)

- `AnchorDefinition` + `HorizontalBezierGeometry.fromAnchors` (Hermite→Bezier C1=P0+T0/3)
- `RailLocalFrame` orthonormal basis + roll hook
- `CantProfile` / `ZeroCantProfile` / `LinearCantProfile` (no physics, no RTM UI)
- No Clothoid

## Checkpoints

| CP | Content | Status |
|----|---------|--------|
| 0 | Audit / baseline | DONE |
| 1 | Contract + Straight | DONE |
| 2 | HorizontalBezier + adaptive ArcLengthTable | DONE |
| 3 | Vertical profile + LocalFrame + Cant hook | DONE |
| 4 | Numerical regression T01–T15 | DONE (harness) |
| 5 | Flat World visual proof | PENDING |
| 6 | Docs cleanup / Final | PENDING |

## Production classes

- RailGeometry, RailSample, RailMath
- StraightGeometry
- HorizontalBezierGeometry
- ArcLengthTable (adaptive)
- VerticalProfile, Flat/Linear/VerticalBezierProfile
- VerticalBezierGeometry
- RailLocalFrame
- CantProfile, ZeroCantProfile, LinearCantProfile
- AnchorDefinition

## Harness (after CP4)

`PASSED=49 FAILED=0 SKIPPED=3` (was 30/0/3; +19 production tests)
