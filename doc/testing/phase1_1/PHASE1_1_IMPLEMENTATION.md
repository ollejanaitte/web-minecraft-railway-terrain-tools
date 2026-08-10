# Phase 1.1 Implementation Log

Status: **COMPLETE** — numerical PASS + Flat-World visual PASS → VERDICT PASS
(resume from prior STOPPED run, 2026-08-10)

## Baseline (Checkpoint 0)

| Item | Value |
|------|-------|
| Start SHA | `ceeac6ca` |
| Phase 0.6 | PASS / DESIGN GATE OPEN |
| Phase 0.7 | PASS / Research Gate READY / Impact B Clarification |
| dirty v1 | PRESERVED (md5 match at start, stop, and final) |
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
| 5 | Flat World visual proof | DONE on resume (SS-R1_1-01..08, Hardware Vulkan) |
| 6 | Docs cleanup / Final | DONE (COMPLETE) |

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

## Resume work (2026-08-10)

- Validation automation reworked: `screenChanged`-hook driven navigation,
  chat-announced camera tour tags (`RailV2AutoValidate.tourTag`), bounded
  create retry, crash-UI reload, Chrome stability flags, MAX_RUNS=4 default.
- Flat Validation World reached; AutoValidate fired; SS-R1_1-01..08 captured
  on NVIDIA GTX 1050 (ANGLE Vulkan).
- See VISUAL_VALIDATION.md, REGRESSION_RESULTS.md.

## Harness (final)

`PASSED=49 FAILED=0 SKIPPED=3` (was 30/0/3; +19 production tests)

## Prior stop (2026-08-10, preserved for history)

CP5 visual was STOPPED by user request (menu-recovery race, 0 screenshots);
report closed with VERDICT BLOCKED. Resume (this run) completed CP5.
