# Phase 0 Test Results

Date       : 2026-08-09
Environment: Ubuntu, Java 17.0.19, Gradle 8.14, headless JVM
Commands   :
  - `./gradlew makeMainOfflineDownload`   (production build gate)
  - `./gradlew harnessTest`               (test harness)
Results recorded below; re-run with the exact commands to reproduce.

## Build
- makeMainOfflineDownload: BUILD SUCCESSFUL (exit 0). Artifact:
  target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html (24M).
- No production-source change; only test-only build config added.
- Build BEFORE harness changes: BUILD SUCCESSFUL (baseline). Build AFTER:
  BUILD SUCCESSFUL. No regression.

## Test runner
- runner: railv2test.Runner via Gradle task `harnessTest` (dependency-free).
- Deterministic, repeatable: two consecutive runs identical.

## Tests discovered / passed / failed / skipped
- discovered: 33 (30 enabled + 3 @Disabled future gates)
- passed:      30
- failed:      0
- skipped:     3  (Phase 1/2 future gates, intentionally disabled)

### Passed (by class)
- StraightMathTest 4, BezierMathTest 5, ArcLengthTest 3,
  ContinuityScaffoldTest 5, FormationScaffoldTest 7,
  V1ReferenceRegressionTest 3, PersistenceBaselineTest 2,
  KnownFailureDocumentationTest 2.

### Skipped (future gates)
- ArcLengthTest.productionDistanceRoundTripScaffold        -> Phase 1
- ContinuityScaffoldTest.productionCantRollContinuity       -> Phase 1
- FormationScaffoldTest.productionSolverNoTeleportAtBoundary-> Phase 2

### Expected failures
- None executed as failing; known failures are pinned as documentation
  (KnownFailureDocumentationTest asserts the v1 buggy wrap behavior).

## Manual baseline
- Not executed this session (no browser launch in headless env).
  See MANUAL_BASELINE_CHECKLIST.md; commands/rail/vehicle baseline recorded
  from source in BASELINE_TEST_MATRIX.md.

## Known bugs (recorded, not fixed)
See KNOWN_FAILURES.md: BL-TRAIN-001..004, BL-NET-001, BL-ROUTE-001,
BL-OCC-001, BL-STATIC-001, BL-TRACK-001, BL-CMD-002.

## Persistence baseline
- rail_system WorldSavedData: graph nodes/segments + curveData + switch/
  route/station maps (v1 NBT). Semantic round-trip covered by test-side
  PersistenceBaselineTest (straight + curve). Byte-for-byte not required.

## Environment
- os: Linux; java: OpenJDK 17.0.19; gradle 8.14; JAVA_HOME=/usr/lib/jvm/
  java-17-openjdk-amd64; GRADLE_USER_HOME default (.gradle-local present).

## Timestamp / commit candidate
- Runs completed 2026-08-09. Commit candidate: this Phase 0 changeset
  (build.gradle.kts harness + src/harness + doc/testing + FINAL_REPORT.txt).
