# UNKNOWN_AND_OPEN_QUESTIONS — Phase 1-R11

Date: 2026-08-13 JST
Honest record of what R11 could not resolve, with relevance to Phase 1 design
(P0/P1/P2) and whether it blocks R12.

Legend: Blocker = would prevent a P0 design freeze.

---

## Geometry

| # | Question | Why important | Current evidence | What was checked | Why unresolved | Risk | Relevance | Follow-up |
|---|----------|---------------|------------------|------------------|----------------|------|-----------|-----------|
| U1 | RTM exact curve class (Bezier/Hermite/clothoid/custom) | Railys parity claim | handle UX documented; math unnamed | wiki/text, jar API surface | RTM repo private; not in public text | LOW (Railsys chooses clean-room) | P2 | none |
| U2 | Cant units + along-rail interpolation / transition length | Railsys CantProfile design | C_Center/Edge/Random fields; behaviour only | text, fields | not documented | LOW | P1 | R12 design decision (Railsys units) |
| U3 | RTM zero/min-length + sharp-curve limits | validation limits | no public value | text | not documented | LOW | P2 | Railsys defines own |
| U4 | RTM obstacle/height validation details | placement limits parity | black frame + generating limits | text/config | partial | LOW | P1 | Railsys own semantics |

## Switch

| # | Question | Why important | Evidence | Checked | Unresolved | Risk | Relevance | Follow-up |
|---|----------|---------------|----------|---------|------------|------|-----------|-----------|
| U5 | Exact frog/point curve math | switch geometry | SwitchType/Point structure; no math | jar API | internal math | MEDIUM (design) | P0 | R12 clean-room geometry |
| U6 | Max branch count per junction | switch data limits | none | text | undocumented | LOW | P2 | Railsys own limit |
| U7 | Per-branch cant independence | switch+cant | none | — | undocumented | LOW | P2 | Railsys choice |
| U8 | Multiplayer switch animation consistency | network | route shared; animation client-interpolated | inference | not directly observed | LOW | P1 | R12 design |

## ModelPack / compatibility

| # | Question | Why important | Evidence | Checked | Unresolved | Risk | Relevance | Follow-up |
|---|----------|---------------|----------|---------|------------|------|-----------|-----------|
| U9 | mqo->native mesh fidelity (UV/normals/materials) across packs | adapter/converter | mqo format observed | sample packs | tooling needed | MEDIUM | P1 | R12 converter |
| U10 | ModelConfig.scale default value | mesh placement | scale field exists | jar | default value not extracted | LOW | P1 | R12 test import |
| U11 | Renderer script behaviour catalogue completeness | adapter mapping | NR01 scripts + RTM scripts | sample | many packs | MEDIUM | P1 | R12 survey |
| U12 | Mandatory vs optional rail JSON keys per version | schema v2 | sample JSON | 99 NR01 files | cross-version drift | LOW-MED | P1 | R12 schema audit |
| U13 | Per-pack license surfacing UX | import legality | RTM LICENSE + pack terms | read | UX design | LOW | P1 | R12 |

## Persistence

| # | Question | Why important | Evidence | Checked | Unresolved | Risk | Relevance | Follow-up |
|---|----------|---------------|----------|---------|------------|------|-----------|-----------|
| U14 | Rail block layout (which blocks belong to a rail) | chunk-based persistence | TileEntity per block | jar | mapping detail | MEDIUM | P0/P1 | R12 data model |
| U15 | Post-edit regeneration impact on neighbours | confirmed editing | canConnect | inference | undocumented | MEDIUM | P0 | R12 design |
| U16 | Confirmed-edit identity (id vs position) | infra references | position-based in RTM | jar | Railsys choice | MEDIUM | P0 | R12 decision |
| U17 | Chunk-boundary placement rules | validation | none | — | undocumented | LOW | P2 | Railsys own |

## Signal / Crossing / Connector / Vehicle

| # | Question | Why important | Evidence | Checked | Unresolved | Risk | Relevance | Follow-up |
|---|----------|---------------|----------|---------|------------|------|-----------|-----------|
| U18 | Train detection mechanism (occupancy vs proximity) | signal hooks | signal field + detector | jar/wiki | internals | MEDIUM | P1/P2 | R12 (Phase 2) |
| U19 | Crossing rail link semantics | crossing | Gate machine | jar | internals INFERRED | LOW | P2 | Phase 2 |
| U20 | End-of-track vehicle behaviour | vehicle | run out of map | inference | undocumented | LOW | P2 | Phase 2 |
| U21 | Eaglercraft redstone/wiring parity | switch/signal input | RTM redstone-based | — | web mapping | MEDIUM | P0 | R12 design |

## RTM version specifics

| # | Question | Evidence | Unresolved | Relevance |
|---|----------|----------|------------|-----------|
| U22 | Exact 1.12.2 placement "renewal" differences vs 1.7.10 | S07 warning | not itemized | P2 |
| U23 | RTM2.4.24 default values (GeneratingDistance etc) vs 1.7.10 | 64/256 (source) | exact build variance | P1 |

---

## Blocker assessment

No P0-design-blocking UNKNOWN remains: every P0 requirement has enough
evidence (RTM structure + observable behaviour) for R12 to freeze a Railsys
design. The medium items (U5, U9, U11, U14, U15, U16, U21) are R12 design
decisions, not research blockers.

## Count

- Total open items: 23
- P0-relevant unknowns: 0 blockers (U5/U14/U15/U16/U21 are design decisions)
- P1-relevant: 14
- P2: 9
