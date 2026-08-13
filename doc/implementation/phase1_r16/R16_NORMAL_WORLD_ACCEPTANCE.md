# R16_NORMAL_WORLD_ACCEPTANCE — Phase 1-R16

Date: 2026-08-13 JST
Driver: `doc/implementation/phase1_r16/r16_acceptance.mjs` (headless Chrome/CDP,
normal Superflat world "RailsysR16"). PASS when the script completes without
FATAL and console-verified lines match.

## Verified flow
1. `/railsys16 build` -> `built 8 segments, total 222.84m,
   topology nodes=8 connections=8` (corrected rounded-rectangle loop).
2. Geometry screenshots (default appearance): TOP, PERSPECTIVE, SMOOTH_90_CURVE,
   STRAIGHT_CURVE_BOUNDARY.
3. `/railsys16 verify` -> `topology VERIFIED: closed loop, no dangling/
   orphan/duplicate`.
4. `/railsys16 forward` -> `(9 steps, closed=true):
   rail-1 -> rail-2 -> rail-3 -> rail-4 -> rail-5 -> rail-6 -> rail-7 -> rail-8 -> rail-1`.
5. `/railsys16 reverse` -> `(9 steps, closed=true):
   rail-1 -> rail-8 -> rail-7 -> rail-6 -> rail-5 -> rail-4 -> rail-3 -> rail-2 -> rail-1`.
6. `/railsys16 status` -> `geometry: rails=8 | topology: nodes=8 conns=8 |
   issues=none`.
7. R15 ModelPack regression: `/railsys15 use nr01-nb-rails:1435mm_nb_concrete`
   on the SAME loop; `SS-R16-05_MODELPACK_LOOP.png` captured; geometry unchanged
   (k01 contract test + same loop length 222.84m).
8. `/railsys3 status` -> `prod=8 (rail-1..rail-8)`.

## Screenshots (SHA-256 recorded after final run)
| File | SHA-256 |
|------|---------|
| SS-R16-01_ROUNDED_LOOP_TOP.png | 8a79628d...c5d7ced |
| SS-R16-02_ROUNDED_LOOP_PERSPECTIVE.png | 963e8923...c5a6a7 |
| SS-R16-03_SMOOTH_90_CURVE.png | 8a587187...5ccbd4 |
| SS-R16-04_STRAIGHT_CURVE_BOUNDARY.png | c76bb6b9...df3d932 |
| SS-R16-05_MODELPACK_LOOP.png | e24d21a5...f2a93890 |

(Full SHA-256 list stored in git history for the screenshots directory.)

## Luna vision result (GPT-5.6 Luna)
- TOP view: ROUNDED RECTANGLE — 4 straight sides + smooth curved 90° corners;
  NOT octagonal; no major gaps/twists/overlaps. VERDICT: PASS.

## Network / Topology evidence (console)
- geometry closed: closure position 0.000000 m, closure tangent 0.000000 deg
- topology closed: forward 9 steps returns to rail-1; reverse 9 steps returns
- no dangling endpoint / orphan node / duplicate connection / accidental branch
