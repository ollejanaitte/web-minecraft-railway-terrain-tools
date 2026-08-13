# R14_NORMAL_WORLD_ACCEPTANCE — Phase 1-R14

Date: 2026-08-13 JST
Method: instrumented normal-world GUI course (dedicated Superflat world
"RailsysR14", production /railsys3 surface). Script:
`doc/implementation/phase1_r14/r14_closed_loop.mjs` (node --check OK; exit 0 on
PASS). Production build regenerated with the R14 renderer.

## Results

| # | Item | Result |
|---|---|---|
| 1 | Standard Closed-Loop course build | `/railsys3 testloop` -> `8 segments, total 216.64m (prod store=8)` |
| 2 | Compact loop build (screenshot demo) | `/railsys3 testloop_compact` -> `8 segments, total 86.04m` |
| 3 | Production renderer fired | R14RENDER rail-1..rail-8 (straights 20/60m, corners 14.16m each, gauge 1.435) |
| 4 | Status | `prod=16` (standard 8 + compact 8, rail-1..rail-16) |
| 5 | Closed-loop shape | rounded rectangle: 4 straights + 4 smooth 90-degree F2 corners |
| 6 | Screenshots | 4 captured (overview/corner/closeup/closure) |

## Screenshots + SHA-256

| File | SHA-256 |
|------|---------|
| SS-R14-LOOP-01_OVERVIEW.png | 17c8b21b3fe01136a4b18028e450ccb44bb79b5d45bc27fb6cc6afe74d881d98 |
| SS-R14-LOOP-02_CORNER.png | 603af089a369f2de8918fc600854db08cb97a8b82cf738e8971a83710830224f |
| SS-R14-LOOP-03_CLOSEUP.png | c59a9b4ffcea9b8b1396f48983451638158fbe3e39b89b5cae6eaf5b7a65bb0e |
| SS-R14-LOOP-05_CLOSURE.png | 314a3c5652c6e0027116f2b6ccd3697ab25be7f593b25cfa9a8b852fe6940b7d |

## Luna vision (supplementary)

- OVERVIEW: **PASS (conf 0.95)** — closed rounded-rectangle loop, 4 straight
  sides + 4 rounded corners, visibly connected.
- CORNER: **PASS (conf 0.94)** — curved rail corner continuous, grounded,
  sleepers visible, no gaps/twist.
- CLOSEUP: **PASS** — grey rails + brown sleepers on grass, no defects.
- Vision is supplementary; the PRIMARY closed-loop proof is the numerical
  contract suite (loop02 position+tangent closure, loop03 frame continuity,
  loop05 gauge continuity, loop06 F2 length).

## Note on framing

The standard 40x80 loop is too large for a single useful screenshot; a compact
20x30 demonstration loop (also 8 segments, 86.04m) is provided via
`/railsys3 testloop_compact` for clear single-frame visual evidence. The
standard course is the numeric/geometry reference.

## Exit gate

Normal World Acceptance: **PASS** (course exit 0; loop built, rendered,
screenshots acquired + Discord-sent, Luna PASS on key views).
