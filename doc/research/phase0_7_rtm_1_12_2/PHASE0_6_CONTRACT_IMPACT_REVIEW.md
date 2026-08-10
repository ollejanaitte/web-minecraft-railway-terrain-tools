# Phase 0.6 Contract Impact Review

Phase 0.7 PART F.  
Baseline: `PHASE 1 DESIGN GATE = OPEN` (not discarded).

---

## 1. Review method

Compare RTM 1.12.2 external research against frozen docs in `doc/testing/phase0_6/`:

- RAIL_GEOMETRY_CONTRACT.md  
- RAIL_RENDERING_VISUAL_CONTRACT.md  
- PHASE1_SCOPE_AND_ACCEPTANCE.md  
- PHASE0_6_RAIL_CORE_DESIGN_FREEZE.md  

---

## 2. Topic-by-topic

| Topic | Phase 0.6 freeze | RTM research finding | Impact |
|-------|------------------|----------------------|--------|
| StraightGeometry | Exact 3D straight | Default aligned markers ≈ straight | **None** |
| HorizontalBezier | Cubic X/Z + controls | Handle UX ≈ end tangents; math UNKNOWN | **Clarification**: document Anchor→Bezier mapping; do not claim RTM identity |
| VerticalProfile / VerticalBezier | Graded straight + vertical bezier model | RTM2 uses ΔY / pitch; no slope markers; no public vertical-curve UI | **None** (aligned) |
| ArcLengthTable | Adaptive metres↔param | RTM sampling UNKNOWN | **None** |
| RailSample | dist,x,y,z,yaw,pitch,roll | Matches needed outputs | **None** |
| roll / cant | Field present; data model only; sign defined | Cant exists on RTM2; units/interp UNKNOWN | **Clarification**: sketch CantProfile shape; keep Phase 1 physics-off |
| Piece / Path | Connectivity; switch type reserved | Junctions need multi-branch later | **None** (already reserved) |
| Rendering | Left/right + sleepers from geometry | RTM separates model/ballast | **Clarification**: AppearanceRef separation called out |
| Placement | Command prototype + preview in Phase 1 | RTM is marker+wrench UX | **Clarification**: 1.4 MVP = RTM-like markers/handles; commands remain valid prototype path for 1.1–1.3 |
| Persistence | WorldRailData v2 | Need anchors/cant/appearance optional fields | **Clarification** in future schema notes; not blocking 1.1 geometry classes |

---

## 3. Additional contract candidates

| Candidate | Required by evidence? | Recommendation |
|-----------|----------------------|----------------|
| CircularArcGeometry | No | **Do not add** for Phase 1.1 |
| ClothoidGeometry / TransitionCurveGeometry | No positive RTM evidence | **Do not add** as parity requirement |
| Curvature helper API | Nice-to-have | Optional later |
| VerticalCurve (civil) | No RTM UI evidence | Keep VerticalBezier only |
| CantProfile / CantTransitionProfile | Supported as future data | **Clarify** as data-model appendix; implement class optional in 1.x |
| RailLocalFrame | Strongly useful for cant/rails/sleepers | **Clarification recommended** |
| left/right rail offset | Implied by rendering contract | Clarify via LocalFrame |
| AnchorDefinition / RailPlacementDefinition | Needed for 1.4 UX | **Clarification** / small additive doc before or during 1.4 — not a 1.1 blocker |

---

## 4. Impact verdict

### Chosen: **B — CLARIFICATION RECOMMENDED**

Not **A**, because RTM research surfaces concrete additive concepts (anchors, local frame, cant profile shape, appearance separation, 1.4 marker MVP) that should be written down so Phase 1.1–1.4 implementers do not guess.

Not **C**, because:

- No evidence forces replacing HorizontalBezier with clothoid/arc.  
- No evidence invalidates distance-centred sampling, piece/path, or Phase 1 numerical gates.  
- Cant remains data-model-only for Phase 1 as already frozen.  
- Switches remain deferred as already frozen.

### Recommended clarifications (documentation only; before or in parallel with 1.1 — **not** a redesign gate close)

1. Add short appendix (or Phase 0.7 pointer) : AnchorDefinition ↔ Bezier control mapping.  
2. Define RailLocalFrame as the sampling basis for render/cant.  
3. Outline CantProfile fields + unknown RTM units disclaimer.  
4. State Phase 1.4 RTM-like placement MVP vs command prototype.  
5. Explicitly reject clothoid-as-RTM-parity myth.

**Product code:** still **no changes** in Phase 0.7.  
**Phase 1.1:** may proceed on frozen geometry contracts with the above clarifications tracked.

---

## 5. Research Gate

**READY** (with clarification backlog) — see Final Report.

Equivalent gate label: READY, not REVISION FIRST, not BLOCKED.
