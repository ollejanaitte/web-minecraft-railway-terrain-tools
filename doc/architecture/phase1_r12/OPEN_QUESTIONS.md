# OPEN_QUESTIONS — Phase 1-R12

Date: 2026-08-13 JST
Design unknowns that remain after R12. None blocks P0 design (all P0
architecture is frozen with the numeric/implementation details below owned by
R13+). Format: ID / Question / Priority / Why / Evidence / Design assumption /
Risk / Blocking? / Owner phase.

## P0-adjacent (numeric/implementation; do NOT block design freeze)

| ID | Question | Pri | Why | Evidence | Assumption | Risk | Blocking? | Owner |
|----|----------|-----|-----|----------|-----------|------|-----------|-------|
| OQ-01 | maxRailLengthM / maxHeightDeltaM defaults | P0 | validation limits | RTM 64/256, 8/256 | use RTM defaults; measure | LOW | no | R13 |
| OQ-02 | minRailLengthM default | P0 | guard | F2 EPS | F2 guard + config | LOW | no | R13 |
| OQ-03 | mesh spacing default (0.5 vs 1.0) | P0 | 3D rail | RTM 0.5; Railsys 1.0 | measure | LOW | no | R13 |
| OQ-04 | snap tolerance | P0 | network | none | ~0.1m recommend | MED | no | R13 |
| OQ-05 | Eaglercraft powered-input representation for switch route | P0 | switch | RTM redstone | server value | MED | no | R13 |
| OQ-06 | serialization format (JSON/NBT/custom) | P0 | persistence | Railsys JSON precedent | JSON first | LOW | no | R13 |
| OQ-07 | railId counter/format | P0 | identity | none | monotonic int | LOW | no | R13 |
| OQ-08 | junction family scope Phase 1 (Basic only?) | P0 | switch | RTM 4 families | Basic first | MED | no | R13 |
| OQ-09 | animation duration/tongue timing | P0 | animation | RTM ~80 ticks | ~0.5-1s target | LOW | no | R13 |

## P1 (design exists; details R13/R21+)

| ID | Question | Pri | Why | Assumption | Blocking? | Owner |
|----|----------|-----|-----|-----------|-----------|-------|
| OQ-10 | centre/edge placement math | P1 | marker families | block-centre today; edge = offset | no | R13 |
| OQ-11 | cant profile interpolation | P1 | cant | constant + EXTENSIBLE profile | no | R21 |
| OQ-12 | culling/LOD distances + chunk size | P1 | perf | measure | no | R24 |
| OQ-13 | event-name catalogue | P1 | connectors | names fixed; dispatch Phase 2 | no | R19 |
| OQ-14 | connector = world block vs pure data | P1 | connectors | pure data + optional block | no | R19 |

## Phase 2 / DEFERRED (recorded, not R12-blocking)

| ID | Question | Pri | Why | Assumption | Blocking? | Owner |
|----|----------|-----|-----|-----------|-----------|-------|
| OQ-15 | train detection mechanics | P2 | signal | occupancy hook writers | no | Phase 2 |
| OQ-16 | crossing rail-link semantics | P2 | crossing | connector approach zone | no | Phase 2 |
| OQ-17 | vehicle nearest-rail vs railId query | P2 | vehicles | contract supports both | no | Phase 2 |
| OQ-18 | mqo fidelity + ModelConfig.scale | P2 | compat | converter iterates | no | R15 |
| OQ-19 | renderer-script behaviour catalogue | P2 | compat | adapter maps | no | R15 |

## Blocker assessment

- P0-design-blocking OPEN: **0**. All P0 requirements have a frozen
  architecture; the open items above are numeric/implementation details owned
  by named R13+ phases.
- Therefore R12 PASS condition ("P0-blocking UNKNOWN none") is satisfied.

## Count

- P0-adjacent: 9 (OQ-01..09, all R13 owner, no blocker)
- P1: 5 (OQ-10..14)
- Phase 2: 5 (OQ-15..19)
