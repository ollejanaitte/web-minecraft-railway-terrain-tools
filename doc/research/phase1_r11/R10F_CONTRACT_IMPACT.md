# R10F CONTRACT IMPACT REVIEW — Phase 1-R11

Date: 2026-08-13 JST
Purpose: review every frozen R10F Foundation Contract against the R11 RTM
findings. Principle: a difference from RTM does NOT justify a contract change.
Only record Contract Change CANDIDATES for R12 GO/NOGO; do NOT change anything
in R11.

---

## F1 Coordinate / Support-Surface Anchor Contract

| Check | Result |
|-------|--------|
| RTM rail reference height | RailPosition height (1/16-block) + support surface + Anchor_Correction_Value; ballast 0.0625. |
| Alignment | RTM stores block origin + precise double position; Railsys F1 stores precise support-surface coordinate. Both define a support/rail-bed datum. |
| Contract Change Candidate | NONE. F1 is compatible; persistence may additionally record block origin (additive). |
| Note | Railsys input-boundary +1 (UP face) has no RTM analogue needed; RTM markers are placed directly on a block. |

## F2 Production Geometry / RailPath Contract

| Check | Result |
|-------|--------|
| RTM direction semantics | arrows face each other; start/end tangent contract consistent with F2. |
| RTM curve math | UNKNOWN class; Railsys Hermite->Bezier is a clean-room choice matching the handle UX. |
| RTM sampling | 0.5 m; Railsys spacing configurable (1.0 default). No conflict. |
| RTM continuity | C0/C1 rules UNKNOWN; Railsys F2 defines pos<=1e-4, angle<=0.5. No evidence Railsys is wrong. |
| Contract Change Candidate | NONE. F2 is the geometry SSoT and matches observable RTM behaviour. |
| Note | Switch geometry (frog/point curves) will need NEW geometry types, not changes to existing straight/curve (F2 EXTENSIBLE). |

## F3 Editing Semantics Contract

| Check | Result |
|-------|--------|
| RTM edit fields | anchorYaw/Pitch/LenH/LenV/height/cant (Center/Edge/Random). |
| Alignment | Railsys edits rot1/rot2/handle/pitch/cant keep anchor position (F3 invariance) — consistent with RTM per-endpoint edits. |
| Cant profile | RTM has 3 cant fields; Railsys constant cant. EXTENSIBLE (F3/classification B). |
| Contract Change Candidate | NONE for ranges/invariance. POSSIBLE (EXTENSIBLE, not frozen change): add cantCenter/Edge/Random to the cant model in R12 — classified EXTENSIBLE, not a FROZEN item. |

## F4 Rail Asset / Geometry Isolation Contract

| Check | Result |
|-------|--------|
| RTM asset = look | RailProperty/ModelSetRail = appearance; RailMap = geometry. |
| Alignment | Railsys F4 (asset never alters path) matches RTM separation exactly. |
| Renderer scripts | RTM per-asset JS; Railsys declarative. F4 keeps geometry/look boundary; scripts are REPLACEABLE (C3/C4). |
| Contract Change Candidate | NONE. F4 confirmed by RTM. |

## F5 Placement Lifecycle Contract

| Check | Result |
|-------|--------|
| RTM flow | 2 facing markers -> preview/edit -> confirm; cancel/clear semantics not precisely analogous. |
| Alignment | Railsys POS1->POS2->preview->confirm matches; confirm-without-preview error is Railsys-specific (fine). |
| Delete | RTM breaks rail blocks; Railsys F5 keeps delete separate (aligned). |
| Contract Change Candidate | NONE. F5 confirmed. |
| Note | RTM "consume markers on confirm" unclear; Railsys clears transient session (behavioural parity OK). |

## F6 Client / Server Authority Contract

| Check | Result |
|-------|--------|
| RTM authority | server-authoritative world/tiles; client renders; server model packs to clients. |
| Alignment | Railsys F6 (server-authoritative inventory/world; client-local placement UI) aligns with RTM's server-world model in the Web Worker topology. |
| Contract Change Candidate | NONE. F6 confirmed; multiplayer rail sync needs R12 design (server-authoritative world data + client render). |

## Contract Change Candidates (recorded, R12 GO/NOGO)

| ID | Contract | Candidate | Reason | Impact |
|----|----------|-----------|--------|--------|
| CC-1 | F3 (EXTENSIBLE) | add cantCenter/Edge/Random (CantProfile) | RTM exposes 3 cant fields; Railsys constant only | EXTENSIBLE, not a frozen change |
| CC-2 | F2 (EXTENSIBLE) | new geometry types for switch (frog/point) | switch branches need dedicated geometry | EXTENSIBLE new types |
| CC-3 | F5 (frozen) | none proposed | RTM flow compatible | — |
| CC-4 | F1 (frozen) | record block origin in persistence (additive) | RTM stores block+double; Railsys may want block for chunk tie | additive, not a semantic change |

No frozen FROZEN item needs changing based on R11 evidence. All R11 findings
are additive / EXTENSIBLE / new-feature scope.

## R10F classification review

- FROZEN items (A1-A9) all confirmed compatible with RTM behaviour.
- EXTENSIBLE items (B1-B6) are where RTM-driven additions land (cant profile,
  edit ops, ModelPack fields, HUD, aliases, validation).
- REPLACEABLE items (C1-C6) unchanged: renderer, mesh, loader, cache are
  swappable within the frozen boundary.
