# Phase 0.1 Visual Validation (INTERMEDIATE)

Status: IN PROGRESS / AGENT HANDOFF.

## IMPORTANT LIMITATION
This agent model cannot ingest images. Screenshots were verified by
OBJECTIVE PIXEL ANALYSIS (Python/PIL): color histograms, region brightness,
sky/hotbar/crosshair detection, coarse ASCII maps. Human visual confirmation
of the final screenshots is REQUIRED by the phase rules before PASS.

## Verified so far (pixel analysis)
- Game boot / title: EaglercraftX logo red (RGB ~223,36,47) on dirt
  background (RGB ~30,21,15) with menu buttons (light gray ~224).
- In-world: sky-blue pixels in the top third + dark hotbar rows at the
  bottom + crosshair pixels near center.
- World creation/load transitions observed (dark "Building Terrain" / load
  screens, then in-world).

## Required screenshot checklist (SS-01..SS-07) - NOT yet verified
| ID | Item | Status |
|----|------|--------|
| SS-01 | GAME_BOOT (title) | CAPTURED (pixel-verified title) |
| SS-02 | RAIL_STRAIGHT | NOT CAPTURED |
| SS-03 | RAIL_CURVE | NOT CAPTURED |
| SS-04 | FULL_SCALE_TRAIN (with scale reference) | NOT CAPTURED |
| SS-05 | TRAIN_ON_CURVE (bogie + yaw) | NOT CAPTURED |
| SS-06 | FORMATION (2+ cars) | NOT CAPTURED |
| SS-07 | PIECE_BOUNDARY (follower stays behind) | NOT CAPTURED |

## Per-item validation questions (to fill once captured)
Rail visible / straight / curve / not buried / continuous.
Train visible / ~18-20m scale / on rail / not floating / not buried.
Bogie front/rear alignment / natural curve yaw.
Formation 2+ cars / follower exists / no teleport / spacing reasonable.
Rendering: no severe flicker / z-fighting / broken rotation.

## Conclusion
Visual validation INCOMPLETE. Do not mark PASS until SS-02..SS-07 are
captured, pixel-verified, and human-confirmed.
