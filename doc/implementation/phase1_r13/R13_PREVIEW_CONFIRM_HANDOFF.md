# R13_PREVIEW_CONFIRM_HANDOFF — Phase 1-R13

Date: 2026-08-13 JST
The Preview → Confirm exact handoff (R12 §3.1/§5.1, R10F F2.4, REQ-P0-01).

## Flow

```
POS1 → POS2 → Preview RailPath (F2 fromMarkers)
  → editing → Final Preview
  → CONFIRM:
      server validates (R13: RailSegmentValidator)
      if valid AND acceptFingerprint == previewFingerprint:
          issue railId
          create RailSegment (promotedPreview = EXACT preview RailPath)
          register in RailWorldData
      else: reject with reason (no promotion of a different line)
```

## Fingerprint (RailFingerprint)

Deterministic identity covering:
- both endpoint anchors (x,y,z,yaw,pitch,lengthH,lengthV)
- asset id + version
- cant
- path length, start/end sample positions + tangents, sample count

Exact promotion holds iff `acceptFingerprint == previewFingerprint`. This
guards against any alternate geometry pipeline producing a different line.

## Exact promotion semantics

- `RailSegment.confirm(...)` takes the FINAL PREVIEW `RailPath` and stores it as
  `promotedPreview` (the exact object). No rebuild happens at confirm.
- `derivedPath()` re-derives from endpoints via F2 for later phases (edit/load)
  — numerically identical (measured diff 0.000e+00).
- The client placement state promotion (`confirmedPath = previewPath`, R10F F5)
  is preserved by the game layer (R10F source-guarded t11).

## Test coverage

R13 contract suite covers: previewFingerprint_stable,
confirmFingerprint_equal, previewLength_equalConfirmed,
previewEndpoints_equalConfirmed, previewCant_equalConfirmed,
promotedPreviewIsExactObject, noSecondGeometryPipeline.
