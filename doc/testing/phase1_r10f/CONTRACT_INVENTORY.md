# Phase 1-R10F Contract Inventory

Date: 2026-08-13 JST
Scope: freeze-and-harden the Railsys Foundation that R1-R10 established.

This document records the Existing Contract Inventory step (Baseline + inventory
of DOCUMENTED / IMPLEMENTED / TESTED / RUNTIME-PROVEN / AMBIGUOUS / MISSING) for
each Railsys Foundation area. It is the evidence base for the F1-F6 contracts
frozen in `doc/architecture/phase1_r10f_foundation_contract.md`.

## Classification legend

| Class | Meaning |
|---|---|
| DOCUMENTED | described in a `doc/` spec/architecture/decision document |
| IMPLEMENTED | present in production source (src/game or src/geometry-core) |
| TESTED | covered by a harness/contract test that currently PASSES |
| RUNTIME-PROVEN | demonstrated in a normal-world run with screenshots/logs (R10 evidence) |
| AMBIGUOUS | source and doc disagree or semantics not pinned |
| MISSING | not implemented / not documented / not tested |

A contract marked FROZEN is final only when the F1-F6 doc says so AND the
Foundation Contract Suite + Golden Data + Normal World Acceptance all hold.

## Baseline

- HEAD: `9142383f13bc5d3890103c0a8cb85e361f366f72` (docs: record R10 final
  completion sync); local main == origin/main == GitHub main (4-way match).
- Existing harness baseline: PASSED=177 FAILED=0 SKIPPED=3 SUCCESS.
- Protected files (verbatim, byte-identical to R10 record):
  - `CommandRailSystem.java` sha256 `1037f467c400d6d313f575250e27e1d41a58c197ee9058bf52855ab06365e832`
  - `EntityRailVehicle.java` sha256 `b2fb0ea33a997ee4266619eefe695df80306481c4f62d8cd61dd9daf8420acff`
- Existing dirty/untracked are preserved verbatim and never staged.

## R10 runtime evidence (from R10 Final Report + run logs)

- Normal world (dedicated `run-r10final2`): POS1 (0,4,0) -> POS2 (12,4,0) ->
  auto preview (12.05 m) -> edits handle 10 / rot1 20 / pitch 4 / cant 6 ->
  preview 12.08 m -> asset `railsys.prototype_narrow_1000` -> Shift+right-click
  Confirm -> confirmed 12.08 m, 13 samples, SAME path identity (promoted).
- `RailsysProductionRenderer` log: `asset=railsys.prototype_narrow_1000
  gauge=1.0 ... samples=13 len=12.08`.
- `RailsysPlacementState.status`: after confirm + clear, `A=none B=none
  preview=no confirmed=yes asset=..._narrow_1000 cant=0.0deg` (confirmed rail
  preserved by clear; transient session reset).
- These numeric facts are the representative regression case for Golden Data:
  **12.08 m / 13 samples / path identity preserved**.

## Per-area inventory

### 1. Coordinate datum (F1)

- AnchorDefinition (src/geometry-core): x/y/z = canonical support-surface
  coordinate; yaw/pitch + lengthH_m handle semantics documented.
  DOCUMENTED, IMPLEMENTED, TESTED (MarkerDirectionContractTest), RUNTIME-PROVEN.
- Anchor y = support/rail-bed surface datum. DOCUMENTED
  (`phase1_r10_clicked_surface_anchor_contract.md`), IMPLEMENTED, RUNTIME-PROVEN.
- Clicked BlockPos Y = block bottom. IMPLEMENTED (MC convention), RUNTIME-PROVEN.
- UP-face input-boundary conversion only
  (`RailsysMarkerSelection.selectOnFace`): anchor y = pos.getY()+1; non-UP
  rejected before mutation. DOCUMENTED, IMPLEMENTED, TESTED (R10 t22-t25),
  RUNTIME-PROVEN.
- `select` / `selectFromMcLook` pass canonical coordinate as-is.
  DOCUMENTED, IMPLEMENTED, TESTED.
- No +1 in Geometry / RailPath / Asset / Production Renderer. IMPLEMENTED
  (source audit), TESTED (R10 t26, t27). See F1 test additions.
- Marker arrow datum = anchor y + ARROW_UP (visual offset, not workaround).
  DOCUMENTED, IMPLEMENTED, TESTED, RUNTIME-PROVEN.

### 2. Geometry / RailPath (F2)

- RailPath.fromMarkers: straight vs Hermite->Bezier; end anchor reversed.
  DOCUMENTED (`RAIL_GEOMETRY_DESIGN.md`, R6 report), IMPLEMENTED, TESTED.
- start tangent ~= POS1 forward; end tangent ~= -POS2 forward.
  DOCUMENTED, IMPLEMENTED, TESTED (MarkerDirectionContractTest),
  RUNTIME-PROVEN.
- Path length = arc length (s [m]); sample = resolve(global s).
  DOCUMENTED, IMPLEMENTED, TESTED (RailPathTest, ArcLengthTest).
- Boundary ownership: earlier-piece-owns-internal-boundary. DOCUMENTED,
  IMPLEMENTED, TESTED (RailPathTest p09/p10).
- RailLocalFrame orthonormal {forward,right,up}; roll = cant.
  DOCUMENTED, IMPLEMENTED, TESTED (CantProofTest, ProductionGeometryTest).
- CantProfile: rollDegAt(distance, length); positive = right rail lower.
  DOCUMENTED, IMPLEMENTED, TESTED.
- Continuity (position <= 1e-4, angle <= 0.5 deg). DOCUMENTED, IMPLEMENTED,
  TESTED (RailPathTest P-series).
- Preview and Confirmed share the same RailPath (exact object promotion in
  `RailsysPlacementState.confirm`). DOCUMENTED, IMPLEMENTED, TESTED (R10 t09/t11),
  RUNTIME-PROVEN (12.08 m / 13 samples, pathIdentity preserved).

### 3. Editing semantics (F3)

- rot1/rot2: rotate Marker A/B yaw by delta. IMPLEMENTED, TESTED
  (MarkerPlacementEditingTest t02/t03), RUNTIME-PROVEN.
- handle: set both anchors lengthH_m in [0.1,20]. IMPLEMENTED, TESTED (t04),
  RUNTIME-PROVEN.
- pitch: set both anchors pitch in [-45,45]. IMPLEMENTED, TESTED (t05),
  RUNTIME-PROVEN.
- cant: set transient cant in [-45,45], positive = right rail lower. IMPLEMENTED,
  TESTED (t06, CantProofTest), RUNTIME-PROVEN.
- asset: switch active asset; rebuilds preview look only. IMPLEMENTED, TESTED
  (RailModelPackTest, R10 asset flow), RUNTIME-PROVEN.
- Influence matrix: to be frozen in the F3 section of the foundation contract.

### 4. Rail Asset / Geometry isolation (F4)

- RailPath = where the rail goes; RailAsset = how it looks. DOCUMENTED
  (`CONTENT_MODELPACK_DESIGN.md`, `r9_visual_gate_root_cause_recovery.md`),
  IMPLEMENTED, TESTED, RUNTIME-PROVEN.
- Asset switch must NOT rebuild RailPath / change centerline. DOCUMENTED,
  IMPLEMENTED (RailModelPackLoader has no geometry knowledge), TESTED
  (t07 profiles carry no geometry), RUNTIME-PROVEN.
- identical RailPath + multiple assets: supported. IMPLEMENTED, RUNTIME-PROVEN
  (SS-R9 asset A/B).
- ModelPack loader internals REPLACEABLE; semantic boundary frozen.

### 5. Placement lifecycle (F5)

- Flow: wand -> POS1 -> POS2 -> auto preview -> optional edit -> Shift+RMB
  confirm -> next session. DOCUMENTED (`PHASE1_FINAL_UX_CONTRACT.md`),
  IMPLEMENTED, RUNTIME-PROVEN.
- POS2 -> auto preview (no immediate confirm). DOCUMENTED, IMPLEMENTED,
  RUNTIME-PROVEN.
- Confirm without preview = error + no mutation. DOCUMENTED, IMPLEMENTED,
  TESTED (R10 t06), RUNTIME-PROVEN.
- Confirm promotes preview (does not rebuild a different line). DOCUMENTED,
  IMPLEMENTED, TESTED (R10 t11).
- Third ordinary click = no silent replace of POS1/POS2. DOCUMENTED,
  IMPLEMENTED, TESTED (R10 t08).
- cancel = discard preview only; clear = reset transient session (markers/
  preview/transient values), confirmed rail + active asset preserved.
  DOCUMENTED, IMPLEMENTED, TESTED (R10 t06/t07), RUNTIME-PROVEN.
- Confirmed Rail Delete is a separate operation, not part of confirm/cancel/
  clear. DOCUMENTED, IMPLEMENTED (no delete API in R10), TESTED (no delete path).
- Command fallback and wand gesture drive the same controller operations.
  DOCUMENTED, IMPLEMENTED, TESTED (R10 t02, t05).

### 6. Client/Server Authority (F6)

- Inventory: server authority; `/railsys3 wand` -> client forwards
  `/railsysplace wand` -> server CommandRailsysPlace does authoritative give +
  full-inventory drop. DOCUMENTED (`phase1_r10_server_authoritative_wand.md`),
  IMPLEMENTED, TESTED (R10 t12), RUNTIME-PROVEN.
- Placement state (markers/preview/confirmed/asset/cant): client-static state
  (Web Worker separation); validated, NOT server-authoritative for rendering
  because the server cannot reach client render statics. This nuance must be
  documented precisely (see F6).
- Rendering: client representation of placement state. IMPLEMENTED,
  RUNTIME-PROVEN.

## Cross-cutting

- Renderer-only fake geometry is forbidden (Preview/Confirmed must be the SAME
  production RailPath pipeline). DOCUMENTED, IMPLEMENTED, TESTED.
- Golden Data policy: `doc/architecture/phase1_r10f_contract_change_policy.md`.
- Clean-room: no RTM proprietary source/assets copied. Verified.

## AMBIGUOUS / MISSING items (R10F resolves)

- AMBIGUOUS: "server-authoritative" applied loosely to client-static placement
  state; R10F F6 pins exact authority classes.
- AMBIGUOUS: cant influence on Anchor / Centerline (audit in F3).
- MISSING (by design, out of R10F scope): persisted confirmed-rail IDs / delete
  API (R11+), switch/turnout geometry (Phase 2/3), production 3D mesh assets
  (REPLACEABLE), real ModelPack file loading (prototype embedded).
