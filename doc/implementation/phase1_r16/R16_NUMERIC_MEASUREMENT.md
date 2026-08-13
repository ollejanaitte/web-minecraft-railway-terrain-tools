# R16_NUMERIC_MEASUREMENT — Phase 1-R16

Date: 2026-08-13 JST
Command: `./gradlew r16Measure`
Course: 40 x 80 outer, corner r=10, gauge 1.435 (R14 Standard Closed Loop corrected).

## Corrected corner (handle = 3*k*r, k = 4/3*(sqrt(2)-1) = 0.55228475)
| Quantity | Measured | True quarter circle |
|----------|----------|---------------------|
| corner[0..3] length | 15.7102 m | 15.7080 m (err 0.0022) |
| sagitta | 2.9289 m | 2.9289 m (err 0.0000) |
| max radius error (10..90%) | 0.0782 m | 0 |

## Continuity / closure
| Quantity | Value |
|----------|-------|
| max position error across 8 boundaries | 0.000000 m |
| max tangent angle error across 8 boundaries | 0.000000 deg |
| closure position error | 0.000000 m |
| closure tangent error | 0.000000 deg |
| total loop length | 222.8406 m |

## Before (R14 octagonal) vs After (R16 rounded rectangle)
| Quantity | R14 (handle=1.0) | R16 (handle=3kr) |
|----------|------------------|------------------|
| corner length | 14.159 m | 15.710 m |
| sagitta | 0.177 m | 2.929 m |
| mid radius | 306.8 m | ~10 m |
| loop total | 216.64 m | 222.84 m |

## Network topology (r16 normal world, /railsys16 build)
- 8 segments registered (rail-1..rail-8), 8 nodes, 8 connections
- verify: topology VERIFIED (no dangling endpoint / orphan node / duplicate)
- forward: rail-1 -> rail-2 -> ... -> rail-8 -> rail-1 (9 steps, closed=true)
- reverse: rail-1 -> rail-8 -> ... -> rail-2 -> rail-1 (9 steps, closed=true)
