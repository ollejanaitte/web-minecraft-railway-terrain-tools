# Railway v2 Regression Matrix

Each Phase 1+ change must pass the relevant rows. Columns: ID, Category,
Purpose, Preconditions, Procedure, Expected, Evidence, Auto/Manual/Visual,
PASS/FAIL criteria, Owner phase, Severity.

Severity: **BLOCKER** (stops Phase 1), **CRITICAL** (major feature broken),
**MAJOR** (noticeable regression), **MINOR** (cosmetic).

## Runtime / Environment
| ID | Category | Purpose | Procedure | Expected | Evidence | Auto | Severity |
|----|----------|---------|-----------|----------|----------|------|----------|
| R01 | Runtime | Game boots | run-validation.sh boot phase | title/menu appears | `_boot.png`, console | Auto | BLOCKER |
| R02 | Runtime | World join | create "EaglerValidate" + join | in-world (sky+hotbar+terrain) | SS-01b, join log | Auto | BLOCKER |
| R03 | Runtime | Validation mode | world name contains `eaglerValidate` | AutoValidate fires | `auto-validated` console | Auto | BLOCKER |
| R19 | Isolation | Normal mode AutoValidate OFF | play a normal-named world | NO course/train/camera moves; `validation=false` | console gate line | Auto | CRITICAL |
| R20 | Isolation | Validation mode AutoValidate ON | validation world | `validation=true` + auto-validated | console | Auto | CRITICAL |
| R21 | Cleanup | chrome/server terminated | after run, pgrep | 0 stale validation chrome | pgrep output | Auto | CRITICAL |
| R22 | Cleanup | no stale process | pre-run check | stale validation chrome count 0 | pgrep output | Auto | CRITICAL |
| R23 | Build | harness green | `./gradlew harnessTest` | PASSED=30 FAILED=0 SKIPPED=3 | gradle output | Auto | BLOCKER |
| R24 | Build | production build | `./gradlew makeMainOfflineDownload` | BUILD SUCCESSFUL | gradle output | Auto | BLOCKER |

## Rail geometry / rendering
| ID | Category | Purpose | Procedure | Expected | Evidence | Auto | Severity |
|----|----------|---------|-----------|----------|----------|------|----------|
| R04 | Rail | straight rail visible | camera tour straight frame | rail bed visible, not buried | SS-02 | Visual | BLOCKER |
| R05 | Rail | curve rail visible | curve frame | curve rails visible | SS-03 | Visual | CRITICAL |
| R06 | Rail | straight→curve continuity | continuity frames | no gap/jump at transition | SS-03/05 | Visual | CRITICAL |
| R07 | Train | full-scale train visible | train frames | 20m-scale cars on course | SS-04 | Visual | BLOCKER |
| R08 | Train | scale reasonable | train frames | car ≈20m, not tiny/huge | SS-04/05 | Visual | CRITICAL |
| R09 | Train | front bogie | formation frame | front bogie present | SS-04/06 | Visual | MAJOR |
| R10 | Train | rear bogie | formation frame | rear bogie present | SS-04/06 | Visual | MAJOR |
| R11 | Train | curve pose | curve frames | cars follow curve yaw | SS-05 | Visual | MAJOR |
| R12 | Train | 2+ car formation | early frames | ≥2 cars visible | SS-04 | Visual | CRITICAL |
| R13 | Train | 4-car formation | later frames | 4 cars visible | SS-06 | Visual | MAJOR |
| R14 | Train | car spacing | formation frame | even spacing (≈22m) | SS-06 | Visual | MAJOR |
| R15 | Boundary | rail-piece boundary | boundary frame | piece boundary visible | SS-07 | Visual | CRITICAL |
| R16 | Follow | no disappearance | continuous frames | all followers always present | tour frames | Visual | BLOCKER |
| R17 | Follow | no teleport | continuous frames | followers move smoothly | tour frames | Visual | BLOCKER |
| R18 | Follow | no wrap | boundary crossing frames | no wrap-around | SS-07 | Visual | CRITICAL |

## Automation / evidence
- Machine verdicts: R01-R03, R19-R24 automated (exit codes / console markers /
  screenshot count).
- Visual review required (R04-R18): documented in
  `VALIDATION_EVIDENCE_RULES.md`.

## Owner phases
- R01-R03, R19-R24: every Phase 1+ change.
- R04-R18: rail/train/formation-affecting changes.
