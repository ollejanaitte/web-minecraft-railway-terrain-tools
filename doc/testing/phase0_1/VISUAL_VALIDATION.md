# Phase 0.1 Visual Validation

Reviewer: Cursor Agent (image Read tool) — 2026-08-10

## Method
- Screenshots captured from live headless Chrome (SwiftShader) against
  `EaglercraftX_1.8_Offline_International.html`.
- Each required shot opened as an image and inspected for rails / cars /
  markers / formation (not accepted on file-size or sky-ratio alone).

## Per-shot review

| Shot | Content observed | Pass? |
|------|------------------|-------|
| SS-01 | Title / boot menu | yes |
| SS-01b | In-world after AutoValidate chat | yes (join proof) |
| SS-02 | Near-field lead car (large blue body) + rail edge | yes (scale) |
| SS-03 | Same near-field car / village context | partial→support |
| SS-04 | Multiple red/blue cars on stone+rail bed; gold + bogie greens | **yes (primary)** |
| SS-05 | Blue/red car on rails over water; gold markers; curve approach | **yes** |
| SS-06 | Camera pad foot view at late tour (not primary proof) | n/a |
| SS-07 | Straight rails + regularly spaced gold markers | **yes** |
| SS-08 | Same straight corridor confirmation | yes |

## Gate mapping
1. **Straight + curve rail**: SS-07/08 straight; SS-05 course spanning water toward curve.
2. **Full-scale (~20m) train**: SS-02/03 fill FOV; SS-04 elongated bodies on track.
3. **Bogie pose**: SS-04 colored bogie markers under car bodies.
4. **Formation (2+)**: SS-04 shows multiple spaced cars.
5. **Piece boundary**: distance-based cars stay course-aligned while advancing
   (SS-04 → SS-05); no teleport/wrap/disappear observed in reviewed frames.

## Verdict
**PASS** — real-game visual proof obtained for the Phase 0.1 architecture spike.
