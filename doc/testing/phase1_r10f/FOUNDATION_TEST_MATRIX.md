# Phase 1-R10F Foundation Test Matrix

Date: 2026-08-13 JST
Suite: `railv2test.tests.RailsysFoundationContractSuite` (registered in
`railv2test.Runner`), run via `./gradlew harnessTest`.

Gate rule: the Foundation Contract Suite MUST be 100% PASS. Any FAILED test
here is a NOGO for R11+ and for any phase touching the Foundation. SKIPPED
tests must carry a documented reason and never be used to dodge a Foundation
gate.

## Suite results (R10F)

- PASSED=207 (baseline 177 + 30 new Foundation tests)
- FAILED=0
- SKIPPED=3 (pre-existing, documented in the harness — see below)

## Contract -> test mapping

| Contract | Test method(s) | Kind |
|---|---|---|
| F1 Anchor y = support surface | `f1_anchorYIsSupportSurfaceDatum` | numerical |
| F1 no +1 in Geometry/RailPath/Asset/Renderer | `f1_geometryRailPathRendererNoPlusOne` | source |
| F1 non-UP rejected before mutation | `f1_nonUpFaceRejectedWithoutMutation` | source |
| F1 canonical select/McLook pass pos as-is | `f1_canonicalSelectAndMcLookPassPosUnchanged` | source |
| F1 arrow shares anchor datum | `f1_arrowSharesAnchorDatum` | source |
| F2 start tangent == POS1 forward | `f2_startTangentPos1Forward` | numerical |
| F2 end tangent == -POS2 forward | `f2_endTangentNegPos2Forward` | numerical |
| F2 curve direction contract | `f2_curveDirectionContract` | numerical |
| F2 length/sampling/clamp/NaN | `f2_pathLengthAndSamplingSemantics` | numerical |
| F2 preview==confirm identity | `f2_previewConfirmNumericalIdentity` | numerical |
| F2 cant rolls frame not centerline | `f2_cantRollsFrameNotCenterline` | numerical |
| F2 orientation continuity | `f2_orientationContinuity` | numerical |
| F3 edits never change anchor position | `f3_editsNeverChangeAnchorPosition` | source |
| F3 edit ranges guarded | `f3_editRangesAreGuarded` | source |
| F3 every edit rebuilds preview | `f3_everyLineShapeEditRebuildsPreview` | source |
| F4 asset switch never rebuilds path | `f4_assetSwitchNeverRebuildsPath` | source |
| F4 asset change does not alter centerline | `f4_assetChangeDoesNotAlterCenterlineNumerically` | numerical |
| F4 ModelPack loader is look-only | `f4_modelPackLoaderIsLookOnly` | source |
| F5 auto preview after POS2, no immediate confirm | `f5_autoPreviewAfterPos2NoImmediateConfirm` | source |
| F5 confirm w/o preview = error + no mutation | `f5_confirmWithoutPreviewIsErrorNoMutation` | source |
| F5 confirm promotes exact preview path | `f5_confirmPromotesExactPreviewPath` | source |
| F5 cancel discards preview only | `f5_cancelDiscardsPreviewOnly` | source |
| F5 clear non-destructive | `f5_clearNonDestructiveConfirmedRail` | source |
| F5 third click no silent replace | `f5_thirdClickDoesNotSilentlyReplace` | source |
| F5 delete separate from confirm/cancel/clear | `f5_confirmCancelClearDeleteAreDistinct` | source |
| F5 command fallback == controller | `f5_commandFallbackUsesSameController` | source |
| F6 wand give server-authoritative | `f6_wandGiveIsServerAuthoritative` | source |
| F6 validation hooks never mutate placement | `f6_validationHooksNeverMutatePlacement` | source |
| Golden R10 regression 12.08 m / 13 samples | `golden_representativeRegression_1208m_13samples` | golden/numerical |
| Golden dataset matches committed JSON | `golden_allFixturesMatchCommittedJson` | golden/numerical |

## Existing harness regression (baseline, must stay green)

- 177 baseline tests across StraightMathTest, BezierMathTest, ArcLengthTest,
  ContinuityScaffoldTest, FormationScaffoldTest, V1ReferenceRegressionTest,
  PersistenceBaselineTest, KnownFailureDocumentationTest,
  ProductionGeometryTest, RailPathTest, RepeatedSegmentProofTest,
  StraightRailSegmentProofTest, CurveGradientSegmentProofTest,
  ContinuousRailProofTest, MarkerDirectionContractTest, CantProofTest,
  MarkerPlacementEditingTest, RailModelPackTest, R10SourceContractTest.

## SKIPPED (documented)

3 pre-existing `@Disabled` tests. Reason documented in the corresponding test
files (disabled for runtime/environment reasons unrelated to the Foundation).

## Golden dataset fixtures

`doc/testing/phase1_r10f/golden/*.json`:

| Fixture | Shape | Length (m) | Samples |
|---|---|---|---|
| G01 | Straight | 12.000000 | 13 |
| G02 | Straight + Gradient | 12.649111 | 13 |
| G03 | Left Curve | 28.294550 | 29 |
| G04 | Right Curve | 28.294550 | 29 |
| G05 | Curve + Gradient | 28.573873 | 29 |
| G06 | Curve + Cant | 28.294550 | 29 |
| G07 | Curve + Gradient + Cant | 28.573873 | 29 |
| G08 | Short Segment (1 m) | 1.000000 | 2 |
| G09 | Long Segment (100 m) | 100.000000 | 101 |
| G10 | Different Endpoint Heading | 28.302076 | 29 |
| G-R10 | R10 regression (12.08 m / 13 samples) | 12.077656 | 13 |

Regeneration: `./gradlew goldenGenerate` (writes to
`doc/testing/phase1_r10f/golden/`). Values are NOT accepted blindly: the
generator validates each fixture against the frozen contract (anchor SSoT,
direction dots, exact straight length, cant presence) and the committed JSON is
verified by `golden_allFixturesMatchCommittedJson` on every harness run.

## Mutation-guard evidence

See `MUTATION_GUARD_RESULTS.md` for the deliberate-violation proof (each
violation makes the corresponding test FAIL, then the violation is reverted).
