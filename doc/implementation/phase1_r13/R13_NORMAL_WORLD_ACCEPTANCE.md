# R13_NORMAL_WORLD_ACCEPTANCE — Phase 1-R13

Date: 2026-08-13 JST
Method: instrumented normal-world GUI course (dedicated Superflat NORMAL world
"RailsysR13", production /railsys3 surface, vanilla /tp + real right-click +
sneak+right-click). Script:
`doc/implementation/phase1_r13/r13_production_data.mjs` (node --check OK; exit
0 on PASS).

## Results

| # | Item | Result (console evidence) |
|---|---|---|
| 1 | wand give | `/railsys3 wand` -> "marker wand added" |
| 2 | POS1 / POS2 / preview | RAIL1 at (0)-(20): POS1/POS2 chat + "preview ready" |
| 3 | edit | `/railsys3 pitch 0` -> preview rebuilt |
| 4 | Confirm -> STABLE ID | `railsys: confirmed (rail-1 length 20.00m)` |
| 5 | status reports prod id | `prod=1(rail-1)` |
| 6 | 2nd confirm -> different ID | `railsys: confirmed (rail-2 length 20.00m)`; `rail-2 != rail-1` |
| 7 | over-length reject | 260m confirm -> `confirmed (length 260.00m)` with NO rail id (registration rejected) |
| 8 | prod store excludes over-length | status after oversize `prod=2(rail-1,rail-2)` |
| 9 | valid placement returns | RECOVERY (100)-(110) -> `confirmed (rail-4 length 10.00m)` |
| 10 | next placement works | recovery confirm succeeded after oversize |

## Evidence details

- Stable ID lifecycle in-world: preview has no id; confirm issues `rail-1`,
  second confirm `rail-2` (unique); over-length confirm does NOT issue a
  committed id (the 260m segment was rejected by max-length validation at
  registration); a valid placement after that issues a NEW id (`rail-4`).
- The issuer counter advanced past the rejected id (rail-3 unused), confirming
  the "retired/unused ids never reused" design — `rail-4` is issued, not
  rail-3.
- Production build was regenerated (`makeMainOfflineDownload`) before the run
  so the game layer includes the R13 wiring.

## Screenshot

- SS-R13-PRODUCTION_DATA.png (final overview of confirmed rails)
  SHA-256 9f9ea49bef275fcabaad698f0dbaad0bad2c9ec66c64c306720b3394008c1d8c
- JS error count 1 = the known headless "Pointer Lock user gesture" browser
  warning (present in R10/R10F/R12 evidence; not a Railsys code error).

## Exit gate

Normal World Acceptance: **PASS** (course exit 0; stable id lifecycle,
over-length rejection, and recovery all verified in-world).
