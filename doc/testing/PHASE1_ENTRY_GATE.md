# Phase 1 Entry Gate

Conditions the Phase 1 Agent must confirm BEFORE starting Phase 1
(Rail Geometry / RailNetwork Core v2).

## Phase 0.3 status
**PHASE 1 ENTRY GATE = OPEN** (see
`doc/testing/railway_v2_regression/PHASE0_3_ENTRY_GATE.md` for the Phase 0.3
judgment: Golden Baseline, Regression Matrix, Evidence Rules, Development
Workflow, Formal Regression).

## Gate checklist
- [ ] Repository scope confirmed (local path + GitHub repo + branch main).
- [ ] Pre-existing dirty Railway files preserved:
      CommandRailSystem.java, EntityRailVehicle.java (diff unchanged,
      124 insertions / 9 deletions).
- [ ] Production build green: `./gradlew makeMainOfflineDownload` exit 0.
- [ ] Test harness green: `./gradlew harnessTest` -> PASSED, FAILED=0.
- [ ] Baseline documented: doc/testing/BASELINE_TEST_MATRIX.md.
- [ ] Known failures documented: doc/testing/KNOWN_FAILURES.md (not fixed).
- [ ] Straight math reference tests green (StraightMathTest).
- [ ] Bezier reference tests green (BezierMathTest, ArcLengthTest).
- [ ] Fixture framework available (fixtures/RailFixtures.java,
      TEST_FIXTURES.md).
- [ ] Formation boundary fixture available
      (FormationScaffoldTest.followerStaysOnPreviousPieceAtBoundary).
- [ ] Persistence semantic round-trip baseline available/documented
      (PersistenceBaselineTest + MIGRATION_PLAN.md v1 read).
- [ ] Phase -1 architecture docs intact (doc/architecture/**).
- [ ] No runtime regression: product code diff unchanged by Phase 0.

## Phase 1 allowed work (summary)
- Implement production RailGeometry (straight/Bezier/vertical/cant),
  ArcLengthTable, RailNetwork, RailPiece + WorldRailData schema v2 read
  (with v1 migration), A* route.
- Validate production geometry against the reference oracles; ENABLE the
  @Disabled future tests (ArcLength round-trip, cant roll continuity).
- Keep v1 runtime behavior unchanged (adapter comes in Phase 2).

## Failure to meet gate
If any gate item fails, STOP and report (do not start Phase 1).
