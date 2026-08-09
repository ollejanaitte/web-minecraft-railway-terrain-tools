# Test Harness Design

Phase 0. Railway System v2.

## 1. Test goals
- Pin the current v1 baseline (including known bugs) so future product
  changes are objectively measurable (regression detection).
- Provide a fast, deterministic, JVM-only harness for rail math and
  formation simulation, independent of the Minecraft/Eaglercraft runtime.
- Provide reference implementations ("oracles") that Phase 1/2 production
  code must reproduce.
- Define manual (Tier 3) baseline checklists.

## 2. Test layers
- Tier 1 Math / Pure Unit: geometry math (straight, Bezier, arc length,
  continuity), path arithmetic, spacing arithmetic. Runs on plain JVM.
- Tier 2 Headless Railway Simulation: multi-piece paths, loops, reverse,
  formation spacing, boundary scenarios, switch scaffold. Uses the test
  oracle (RefPathWalker). Runs on plain JVM.
- Tier 3 Integration / Manual: real game or build artifact. Command/entity/
  render/save/load/multiplayer/UI. Manual checklist + future automation.

## 3. Source layout
```
src/harness/java/railv2test/
  Runner.java            dependency-free test runner (main)
  harness/               Test, Disabled annotations; Assert helpers
  ref/                   Reference implementations (oracles)
  fixtures/              FIXTURE_* data definitions
  tests/                 Test classes (static @Test methods)
src/harness/resources/   (empty; reserved)
```
Build: Gradle custom source set `harness` (test-only), task `harnessTest`
(JavaExec), wired into `check`. No external dependencies (no JUnit).

## 4. Runner
- `./gradlew harnessTest` compiles and runs `railv2test.Runner`.
- Runner reflects over registered test classes; runs each static @Test
  method; methods annotated @Disabled are reported SKIPPED (never fail).
- Exit code != 0 if any enabled test fails. Output shows PASS/FAIL/SKIP and
  a summary.
- Deterministic: no randomness, fixed tolerances, fixed fixtures.

## 5. Naming
- Test methods: `camelCaseDescribingAssertion`.
- Fixtures: `FIXTURE_<SHAPE>_<DIMENSION>`.
- Known-failure IDs: `BL-<AREA>-<NNN>` (see BASELINE_TEST_MATRIX.md).

## 6. Fixtures
Defined in `fixtures/RailFixtures.java` and TEST_FIXTURES.md:
straight, curve 90deg, S-curve, gradient, vertical curve, cant, multi-piece,
simple loop, switch contract, 2/8/16-car formation sizes.

## 7. Deterministic random policy
Phase 0 uses no randomness. Future soak tests (Phase 2+) may use a seeded
RNG recorded with results.

## 8. Tolerance policy
| Quantity | Tolerance | Note |
|----------|-----------|------|
| integer IDs | exact | - |
| world coordinate | 1e-4 m (reference) | production must match |
| distance | 1e-3 m (reference) / 0.25 m spacing acceptance (Phase 2) | formal gate |
| angle (yaw/pitch/roll) | 1e-3 deg (reference) / 0.5 deg continuity | formal gate |
| spacing (long-run) | < 0.25 m sustained | Phase 2 soak acceptance |
| arc length | 0.1% of length | formal gate |

Phase 0 tolerances are provisional budgets; the formal Acceptance Gate is
doc/testing/ACCEPTANCE_TEST_STRATEGY.md.

## 9. Expected failure policy
- Known bugs are NOT enabled as failing tests. They are:
  (a) documented in KNOWN_FAILURES.md + BASELINE_TEST_MATRIX.md;
  (b) pinned by a documentation test that asserts the buggy behavior
      (KnownFailureDocumentationTest) so the bug is detectable;
  (c) future fixes are represented by @Disabled tests (owning phase) that
      must be enabled when the production solver lands (Phase 2 gate).
- CI stays green: no always-red tests.

## 10. Regression policy
- v1 math formulas are mirrored as reference tests (V1ReferenceRegressionTest)
  so Phase 1/2 changes that alter them are caught.
- Every Phase MUST re-run harnessTest + production build.
- Manual baseline checklist re-run on Phase 3/4+ changes.

## 11. CI future plan
- Add a CI job running `./gradlew harnessTest makeMainOfflineDownload`
  after repository CI is established (not present today).
- Phase 2+: soak test (100+ laps) as a separate long job.

## 12. Phase ownership
- Phase 0: harness, reference oracles, fixtures, baseline matrix, known
  failures, entry gate.
- Phase 1: production RailGeometry/ArcLengthTable validated against refs;
  enable ArcLength/continuity future tests.
- Phase 2: production formation solver validated against RefPathWalker;
  enable productionSolverNoTeleportAtBoundary; add soak.
- Phase 3+: editor/track tests; Phase 5 signal tests; Phase 10 multiplayer.

## 13. Rules
- Test code never depends on game classes (src/game, TeaVM).
- Test-only build config; production bundle unaffected.
- No JUnit dependency in Phase 0 (dependency-free runner); may add later
  if the user approves.
