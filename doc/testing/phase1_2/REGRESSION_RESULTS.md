# Regression Results (Phase 1.2)

Status: PASS.

## Harness (final, 2026-08-11)

```
PASSED=84 FAILED=0 SKIPPED=3
RESULT: SUCCESS
```

Phase 1.1 baseline was `PASSED=49 FAILED=0 SKIPPED=3`; Phase 1.2 added 35
tests (RailPathTest P01–P22 + connection/network/endpoint/empty-path cases).
The Phase 1.1 geometry regression (ProductionGeometryTest T01–T15 + extras,
StraightMath/Bezier/ArcLength/Continuity/V1-reference etc.) remains green — no
FAIL increase.

## Production build

`./gradlew makeMainOfflineDownload` — BUILD SUCCESSFUL (final regression).

## Numerical acceptance (worst-case, all fixtures)

| Check | Tolerance | Measured |
|---|---|---|
| join position error | 1e-4 m | 0.0 m |
| yaw continuity | 0.5 deg | 0.0 deg |
| pitch continuity | 0.5 deg | 0.0 deg |
| roll continuity | 0.5 deg | 0.0 deg |
| boundary jump | 1e-4 m | 1.0e-7 m |
| forward/reverse position | identical | 8.6e-14 m |
| NaN / Inf | 0 | 0 |
| unexpected exceptions | 0 | 0 |
| dense sample monotonicity | monotonic | 1989 samples OK |

## Flat Validation / Visual

- Flat World entered; AutoValidate fired; Hardware Vulkan (NVIDIA GTX 1050).
- SS-R1_2-01..08 captured 8/8; visual review PASS (see VISUAL_VALIDATION.md).

## Isolation

- Path debug visualization is world-name gated (AutoValidate); normal worlds
  are unaffected. No debug markers in normal play.
- Validation state does not leak into production path classes.

## Regression evidence files

- `logs/final_harness_*.log` (harness full output)
- `logs/build_*.log`, `logs/harness_*.log`
- `NUMERICAL_VALIDATION.md`, `VISUAL_VALIDATION.md`
