# R10F_CONTRACT_CHANGE_DECISIONS — Phase 1-R12

Date: 2026-08-13 JST
Formal evaluation of the R11 Contract Change Candidates (CC-1..CC-4) against
R10F Foundation. Per R10F policy, no frozen change is made without approval;
all candidates here are additive/EXTENSIBLE. None changes a frozen meaning.

## CC-1: Cant profile (center/edge/random)

- Related: F3 (Editing), F2 (geometry), classification B (EXTENSIBLE).
- Current contract: constant cant; positive = right rail lower; cant rolls
  frame not centerline (F2/F3). Controller range [-45,45].
- Proposed: EXTENSIBLE CantProfile with start/mid/end (center/edge/random
  fields), applied to the frame only (centerline unchanged).
- Reason: R11 Evidence — RTM exposes C_Center/C_Edge/C_Random fields.
- User impact: richer cant authoring.
- Geometry impact: NONE (frame roll only, F2 preserved).
- Persistence impact: additive cant fields (R12-A/F).
- Compatibility impact: none.
- Tests impact: new profile tests; existing ConstantCantProfile unchanged.
- Golden Data impact: none (constant-cant fixtures unchanged).
- Decision: **APPROVE ADDITIVE (EXTENSIBLE)** — no frozen item changes.

## CC-2: New geometry types for switch (frog/point)

- Related: F2 (EXTENSIBLE piece params reserved), classification.
- Current contract: straight + Hermite->Bezier; continuity rules.
- Proposed: new switch-branch geometry types built on the same F2 sampling/
  frame/continuity machinery (not a change to existing straight/curve).
- Reason: R11-B switch evidence.
- User impact: junction branches.
- Geometry impact: additive types; F2 semantics preserved.
- Persistence impact: endpoints + points (R12-A/C).
- Compatibility impact: none.
- Tests impact: new switch tests; existing geometry tests unchanged.
- Golden Data impact: none.
- Decision: **APPROVE ADDITIVE (EXTENSIBLE)**.

## CC-3: F5 placement flow change

- Related: F5 (frozen).
- Current contract: POS1->POS2->preview->confirm; cancel/clear non-destructive;
  delete separate.
- Proposed: NONE. R11 confirmed F5 compatibility.
- Decision: **NO CHANGE REQUIRED**.

## CC-4: Record block origin in persistence (additive)

- Related: F1.
- Current contract: anchor y = support surface; +1 only at input boundary.
- Proposed: ADDITIVE persisted `blockPos` (derivable block origin) alongside
  the canonical anchor, for chunk/spatial tie-in. Does NOT alter anchor
  semantics.
- Reason: RTM stores block+double; Railsys spatial index (R12-J) benefits.
- Geometry impact: NONE (derived data only).
- Persistence impact: additive field (R12-A/F).
- Decision: **APPROVE ADDITIVE** — no frozen meaning changes.

## CC-5: Confirmed-rail endpoint repositioning / height change

- Related: F3 (frozen: the SIX placement-edit ops keep anchor-position
  invariance; R10F classification A7 + B1).
- Current contract: F3.4/A7 freeze "edits never change anchor position".
  REQ-P0-08 (Confirmed Rail Editing) requires moving an endpoint after
  placement — this IS a position change, so it is a **frozen-contract change
  under the R10F Contract Change Policy**, not merely "outside the set".
- Proposed (Contract Change Proposal, APPROVED ADDITIVE):
  1. F3 six-edit ops (rot1/rot2/handle/pitch/cant/asset) keep their frozen
     invariance UNCHANGED (position never changes via these six).
  2. A NEW confirmed-rail operation "reposition endpoint / change endpoint
     height" is added to the edit surface. It moves an endpoint anchor and is
     validated against node continuity (R12-G §2.3). Other member anchors are
     never silently moved.
- Reason: REQ-P0-08 + R11-D evidence (RTM lets users move/reposition rails).
- User impact: confirm rails can be repositioned.
- Geometry impact: rebuild via F2 fromMarkers; continuity re-validated.
- Persistence impact: endpoint anchors persisted (already R12-A).
- Compatibility impact: none.
- Tests impact: new reposition tests; F3 six-edit tests unchanged; Golden Data
  unchanged.
- Decision: **APPROVE ADDITIVE as a Contract Change Proposal** — extends the
  frozen edit surface with one new operation; the six F3 ops and their
  invariance remain frozen.

## CC-6: Confirm/Edit authority handoff protocol

- Related: F6 (server-authoritative world; client-local placement UI).
- Current contract: F6 classifies placement UI client-local; world data
  shared/synchronized. The exact confirm/edit handoff was not specified.
- Proposed: frozen two-phase handoff (client confirmRequest/edit proposal ->
  server validate + issue ids + write -> client promotes exact preview
  RailPath). This makes F6 concrete, does not change its classification.
- Decision: **APPROVE ADDITIVE (specifies F6, no classification change)**.

## Decisions summary

| ID | Decision | Type |
|----|----------|------|
| CC-1 | APPROVE ADDITIVE (EXTENSIBLE) | cant profile |
| CC-2 | APPROVE ADDITIVE (EXTENSIBLE) | switch geometry types |
| CC-3 | NO CHANGE REQUIRED | F5 |
| CC-4 | APPROVE ADDITIVE | block origin field |
| CC-5 | APPROVE ADDITIVE | confirmed-rail endpoint reposition (outside F3 set) |
| CC-6 | APPROVE ADDITIVE | confirm/edit authority handoff (specifies F6) |

No frozen FROZEN item (R10F A1-A9) is changed in R12. All additions are
forward-compatible with the R10F Foundation and Golden Data.
