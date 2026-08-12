# Phase 1-R10F Foundation Classification (FROZEN / EXTENSIBLE / REPLACEABLE)

Date: 2026-08-13 JST
Related: `doc/architecture/phase1_r10f_foundation_contract.md`,
`doc/testing/phase1_r10f/CONTRACT_INVENTORY.md`.

Every Foundation Contract item is classified so later phases know exactly what
must never change, what may grow, and what internals may be swapped.

Legend: evidence = where the current behaviour is fixed and proven.

---

## A. FROZEN (meaning may not change)

| ID | Contract | Evidence |
|---|---|---|
| A1 | Support-surface Anchor datum: anchor y = rail-bed surface; +1 only at `selectOnFace` UP input boundary; Geometry/RailPath/Asset/Renderer never compensate | `phase1_r10_clicked_surface_anchor_contract.md`; R10 t22-t27; Foundation Suite F1 |
| A2 | POS1/POS2 direction semantics: start tangent = +POS1 forward, end tangent = -POS2 forward | R6 report; MarkerDirectionContractTest; F2 |
| A3 | Preview/Confirm identity: confirm promotes the exact preview RailPath; no renderer fake geometry | R10 t09/t11; Golden G-R10 (12.08 m/13 samples) |
| A4 | Asset does not alter RailPath: geometry is RailPath-owned; asset is look-only | `r9_visual_gate_root_cause_recovery.md`; F4 |
| A5 | Cancel/Clear non-destructive semantics: cancel discards preview only; clear resets transient session; confirmed rail preserved | R10 t06/t07; lifecycle runtime proof |
| A6 | No +1 datum workaround in production renderer; visual offsets are documented | F1; R10 t26 |
| A7 | Editing anchor invariance: edits never change anchor POSITION; cant never changes centerline | MarkerPlacementEditingTest.t06; F3 |
| A8 | Authority: inventory give server-authoritative; client never self-grants wand | `phase1_r10_server_authoritative_wand.md`; R10 t12; F6 |
| A9 | Command fallback == wand gesture via the same controller operations | `PHASE1_FINAL_UX_CONTRACT.md`; R10 t02/t05 |

## B. EXTENSIBLE (may add without breaking existing meaning)

| ID | Area | Allowed additions |
|---|---|---|
| B1 | Editing operations | more edits (e.g. lengthV handle, vertical curve control) as long as anchor-position invariance holds |
| B2 | Rail Asset fields | new look knobs (ballast colour, extra parts) that never touch the RailPath |
| B3 | ModelPack fields | new pack/profile keys in the Railsys JSON format (schema versioning) |
| B4 | Placement HUD/UI | HUD buttons, key bindings calling the same controller operations |
| B5 | Command alias | new `/railsys3` subcommands / aliases routing to the same controller |
| B6 | Validation tooling | new contract tests, additional assertions, new golden fixtures |

## C. REPLACEABLE IMPLEMENTATION (internals may be swapped; FROZEN contract must hold)

| ID | Area | Freeze boundary |
|---|---|---|
| C1 | RailPath algorithm internals | F2 semantics (arc length, sampling, continuity) must hold; implementation may change |
| C2 | Renderer | F2.4 (no fake geometry) and F4 (asset=look) must hold |
| C3 | Mesh generator / procedural drawer | F4.2 semantics; the R9 procedural segments may be replaced by production meshes |
| C4 | ModelPack loader / parser | F4.2 (look-only, never geometry); file I/O vs embedded JSON may change |
| C5 | Cache | any caching of frames/tables; must preserve deterministic sampling |
| C6 | Future persistence backend | `RailsysWorldRailData` format/backend; confirmed rails restored identically |

---

## Decision rules

1. A change that touches an A-item = Contract Change Proposal (policy doc), not
   a refactor.
2. A change that only adds B-items = allowed without proposal, but must keep all
   existing tests green and add its own tests.
3. A change that replaces a C-item = allowed, but the corresponding A-item
   invariants must be re-proven (Foundation Suite + Golden + Normal World).
4. "RTM does it that way" alone is never a reason to change an A-item.
