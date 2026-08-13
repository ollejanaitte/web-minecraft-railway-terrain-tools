# R17_NUMERIC_MEASUREMENT — Phase 1-R17

Date: 2026-08-14 JST
Command: `./gradlew r17Measure`
Base course: Standard Closed Loop 40x80, r=10 (corrected quarter-circle).

## Switch divergence
| Angle | registered | divergence | valid |
|-------|-----------|------------|-------|
| 5 deg | true | 5.00 deg | true |
| 10 deg | true | 10.00 deg | true |
| 20 deg | true | 20.00 deg | true |

## Diverging lead path (F2)
| Angle | finite | len | startTang | endTang |
|-------|--------|-----|-----------|---------|
| 5 deg | true | 0.64 m | 90.00 | 95.00 |
| 10 deg | true | 0.64 m | 90.00 | 100.00 |
| 20 deg | true | 0.66 m | 90.00 | 110.00 |

(Lead path start/end tangents are continuous with the main and branch
headings respectively; the path is used by R18 animation/vehicles.)

## Route evidence (normal world, /railsys17)
- spur junction sw-1: mainIn=rail-8, mainOut=rail-1, branches=1
- THROUGH: resolve rail-8 -> rail-1 (stays on loop)
- BRANCH: resolve rail-8 -> rail-9 (diverts to spur)
- prod store: 9 rails (8 loop + 1 spur); loop geometry unchanged (222.84m)
