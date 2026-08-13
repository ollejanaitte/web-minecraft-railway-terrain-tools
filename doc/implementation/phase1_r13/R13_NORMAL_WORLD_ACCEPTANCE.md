# R13_NORMAL_WORLD_ACCEPTANCE — Phase 1-R13

Date: 2026-08-13 JST
Method: instrumented normal-world GUI course (dedicated Superflat NORMAL world
"RailsysR13", production /railsys3 surface, vanilla /tp + real right-click +
sneak+right-click). Script:
`doc/implementation/phase1_r13/r13_production_data.mjs` (node --check OK; exit
0 on PASS). Production build regenerated with the R13 wiring + Sol-review
fixes.

## Results

| # | Item | Result (console evidence) |
|---|---|---|
| 1 | wand give | `/railsys3 wand` -> "marker wand added" |
| 2 | POS1 / POS2 / preview | RAIL1 at (0)-(20): POS1/POS2 chat + "preview ready" |
| 3 | edit | `/railsys3 pitch 0` -> preview rebuilt |
| 4 | Confirm -> STABLE ID | `railsys: confirmed (rail-1 length 20.00m)` |
| 5 | status reports prod id | `prod=1(rail-1)` |
| 6 | 2nd confirm -> different ID | `railsys: confirmed (rail-2 length 20.00m)`; `rail-2 != rail-1` |
| 7 | over-length REJECTED | 260m `/railsys3 confirm` -> `railsys: confirm rejected — rail fails production validation` (validate-before-issue: NO id) |
| 8 | prod store excludes over-length | status after oversize `prod=2(rail-1,rail-2)` |
| 9 | markers retained after rejection | status `A=set B=set preview=yes` (user can fix) |
| 10 | clear -> recovery | `/railsys3 clear` then valid 10m -> `confirmed (rail-3 length 10.00m)` |

## Evidence details

- Stable ID lifecycle in-world: preview no id; confirm issues `rail-1`, second
  confirm `rail-2` (unique); over-length confirm is REJECTED BEFORE any id is
  issued (Sol-review fix: validate-before-issue) — the store count stays 2 and
  the next valid confirm gets `rail-3` (monotonic, no gap, no reuse).
- Rejected confirm does NOT promote client-confirmed state (the controller
  returns false and leaves markers/preview for correction) — the R12 §3.1
  accept-or-reject rule.

## Screenshot

- SS-R13-PRODUCTION_DATA.png (final overview)
  SHA-256 9f9ea49bef275fcabaad698f0dbaad0bad2c9ec66c64c306720b3394008c1d8c
- JS error count 1 = the known headless "Pointer Lock user gesture" browser
  warning (present in R10/R10F/R12 evidence; not a Railsys code error).

## Exit gate

Normal World Acceptance: **PASS** (course exit 0; stable id lifecycle,
validate-before-issue over-length rejection, retention + clear recovery all
verified in-world).
