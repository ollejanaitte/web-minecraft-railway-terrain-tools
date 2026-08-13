# R17_NORMAL_WORLD_ACCEPTANCE — Phase 1-R17

Date: 2026-08-14 JST
Driver: `doc/implementation/phase1_r17/r17_acceptance.mjs` (headless Chrome/CDP,
normal Superflat world "RailsysR17").

## Verified flow
1. `/railsys16 build` -> 8 segments, total 222.84m (corrected rounded-rectangle
   loop).
2. `/railsys17 spur 10` -> `spur junction sw-1 at node 1 branches=1
   mainIn=rail-8 mainOut=rail-1` (a switch turnout on the loop's first straight).
3. `/railsys17 route sw-1 through` -> `route -> THROUGH`.
4. `/railsys17 resolve sw-1 rail-8` -> `resolve rail-8 -> rail-1` (through keeps
   the loop closed).
5. `/railsys17 route sw-1 branch 0` -> `route -> BRANCH`.
6. `/railsys17 resolve sw-1 rail-8` -> `resolve rail-8 -> rail-9` (a vehicle is
   diverted onto the spur).
7. `/railsys17 status` -> `junctions=1 sw-1 route=BRANCH`.
8. `/railsys3 status` -> `prod=9(rail-1..rail-9)` — 8 loop + 1 spur; the loop's
   8 segments and 222.84m are unchanged (switch geometry never modifies rails).

## Screenshots (SHA-256 recorded after final run)
| File | SHA-256 |
|------|---------|
| SS-R17-01_SWITCH_OVERVIEW.png | 9d69d72e...5d992b |
| SS-R17-02_SWITCH_PERSPECTIVE.png | b4830cb6...ff2f |
| SS-R17-03_SWITCH_SPUR_NEAR.png | 4670d92c...ee055a |
| SS-R17-04_LOOP_CURVE.png | e63e1c8a...dd31 |
| SS-R17-05_MODELPACK_LOOP.png | d06f4092...7e97c0 |

(Full SHA-256 list stored in git history for the screenshots directory.)

## Luna vision result (GPT-5.6 Luna)
- ROUNDED-RECTANGLE loop clearly visible; a diverging branch/spur rail leaves
  the loop near the turnout. No missing textures, major gaps, twists, or
  catastrophic overlaps. VERDICT: SWITCH_VISIBLE. PASS.
