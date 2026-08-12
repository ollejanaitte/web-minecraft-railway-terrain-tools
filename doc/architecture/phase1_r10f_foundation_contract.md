# Phase 1-R10F Foundation Contract (FROZEN)

Date: 2026-08-13 JST
Status: **FROZEN** by Phase 1-R10F Foundation Contract Freeze & Hardening.
Related: `doc/testing/phase1_r10f/CONTRACT_INVENTORY.md`,
`doc/testing/phase1_r10f/FOUNDATION_TEST_MATRIX.md`,
`doc/testing/phase1_r10f/golden/`,
`doc/architecture/phase1_r10f_foundation_classification.md`,
`doc/architecture/phase1_r10f_contract_change_policy.md`.

This is the authoritative Railsys Foundation Contract. It freezes what
R1-R10 established and MUST NOT be silently changed. Any change requires a
Contract Change Proposal (see policy doc) and re-proof via the Foundation
Contract Suite, Golden Data, and Normal World Acceptance.

Notation: "SSoT" = single source of truth. "production" = normal-world path
(the `net.minecraft.railsys.*` placement/geometry/render pipeline).

---

# F1 — Coordinate / Support-Surface Anchor Contract

## F1.1 AnchorDefinition is the SSoT

`AnchorDefinition` (src/geometry-core/.../railsys/geometry/AnchorDefinition.java)
is the single source of truth for an anchor position + direction + handle.

Anchor fields: `x, y, z, yawDeg, pitchDeg, lengthH_m, lengthV_m`.

## F1.2 Anchor Y = support / rail-bed surface datum

- Anchor `y` is the **support surface** coordinate (the top face of the block
  the rail rests on).
- `RailPath.fromMarkers` lays the path on that datum. Geometry, RailPath, Asset,
  and the Production Renderer MUST NOT add +1 or re-derive the surface.

## F1.3 Minecraft clicked BlockPos Y = clicked block BOTTOM

Minecraft reports the clicked block coordinate = block BOTTOM corner plus the
hit face. A top-face click on a block occupying `[Y, Y+1)` reports `Y` with face
`UP`; the support surface is `Y+1`.

## F1.4 Input-boundary conversion ONLY

The conversion from clicked-block coordinate to support-surface coordinate
happens EXACTLY ONCE, at the input boundary:

| Entry | Input | Anchor |
|---|---|---|
| `RailsysMarkerSelection.select(player, pos)` | canonical support-surface coordinate, as-is | `x+0.5, y, z+0.5` |
| `RailsysMarkerSelection.selectFromMcLook(player, pos, yaw, pitch)` | canonical support-surface coordinate, as-is | `x+0.5, y, z+0.5` |
| `RailsysMarkerSelection.selectOnFace(player, pos, face)` | actual clicked block + face (wand) | UP: `x+0.5, y+1, z+0.5`; non-UP: REJECTED, no mutation |

- `selectOnFace` non-UP: reject with chat message BEFORE any marker/preview
  state mutation. Phase 1 = horizontal rail; a non-top click has no defined
  support surface.
- Geometry, RailPath, Asset, Production Renderer: **no +1, no datum
  compensation** anywhere downstream of the anchor.

## F1.5 Shared datum

POS1 Arrow, POS2 Arrow, Preview, and Confirmed Rail share the same anchor
support datum. The marker arrow's `+ARROW_UP` (~0.06 m) is a documented
**local visual offset** to avoid z-fighting with the rail surface — it is NOT a
datum compensation workaround and must not be conflated with one.

## F1.6 Frozen

- Anchor Y semantics (F1.2, F1.3, F1.4) — FROZEN.
- Same-datum for arrows/preview/confirmed (F1.5) — FROZEN.
- Visual-offset vs datum-workaround distinction — FROZEN.

---

# F2 — Production Geometry / RailPath Contract

## F2.1 Classes

- Geometry core (pure, TeaVM-safe): `AnchorDefinition`, `RailGeometry`,
  `StraightGeometry`, `HorizontalBezierGeometry`, `VerticalBezierGeometry`,
  `CantProfile` (+ Constant/Zero/Linear), `ArcLengthTable`, `RailSample`,
  `RailLocalFrame`, `RailMath`.
- Path: `RailPath`, `RailPiece`, `RailPathEntry`, `PathSample`,
  `RailEndpoint`, `RailConnection`, `RailValidationResult`.

## F2.2 Fixed quantities

For a path built by `RailPath.fromMarkers(a, b, cantDeg, pieceId)`:

- start position = anchor A position exactly;
- end position = anchor B position exactly;
- start tangent = unit POS1 player forward (dot == +1);
- end tangent = unit -POS2 player forward (dot == -1) via `b.reversed()`;
- centerline = the produced geometry (straight or Hermite->Bezier);
- path length = total 3D arc length, `totalLength()` in metres [m];
- sampling: `resolve(globalM)` clamps `s` to `[0, total]`, non-finite s throws;
- sample position / tangents / yaw / pitch / roll come from the geometry table;
- `RailLocalFrame`: orthonormal {forward, right, up}, right-handed, roll about
  forward; positive roll lowers the +right side (cant);
- gradient = pitch from tangent (positive up);
- cant = roll applied to the frame only, never the centerline;
- orientation continuity: internal boundaries owned by the EARLIER piece
  (`earlier-piece-owns-internal-boundary`); join position <= 1e-4 m,
  heading continuity <= 0.5 deg (documented frozen tolerances).

## F2.3 Direction contract

```
start tangent ~= POS1 player forward   (dot == +1)
end tangent   ~= -POS2 player forward  (dot == -1)
```

This is frozen. It is proven numerically for straight, curve, pitched, and
canted marker paths.

## F2.4 Preview / Confirmed identity

- Preview and Confirmed are the SAME production semantics: identical endpoints,
  centerline, path length, samples, tangents, local frames, cant.
- The controller confirm performs **exact object promotion**
  (`RailsysPlacementState.confirm`: `confirmedPath = previewPath`), never a
  rebuild. The deterministic-pipeline equivalence is also proven numerically.
- Renderer-only Fake Geometry is FORBIDDEN: the preview is rendered from the
  real `RailPath`, not a copied/drawn-by-hand line.

## F2.5 Frozen

- Direction contract (F2.3) — FROZEN.
- Preview/Confirm numerical identity (F2.4) — FROZEN.
- `RailPath.fromMarkers` anchor -> path semantics (F2.2) — FROZEN.
- Boundary ownership + continuity tolerances — FROZEN.

---

# F3 — Editing Semantics Contract

## F3.1 Edit operations (current production surface)

`/railsys3` (and wand-adjacent flow) exposes:

- `rot1 <deg>` / `rot2 <deg>` — rotate Marker A / Marker B yaw;
- `handle <m>` — set both anchor handle `lengthH_m` in [0.1, 20.0];
- `pitch <deg>` — set both anchor pitch in [-45, 45];
- `cant <deg>` — set transient cant in [-45, 45], positive = right rail lower;
- `asset <id>` — select active asset (look only);
- `preview` — rebuild preview from markers + cant;
- `confirm` / `cancel` / `clear` — lifecycle (see F5).

## F3.2 Influence Matrix (frozen; audited against source, not assumed)

| Edit | Anchor pos | Centerline | LocalFrame | Asset | Preview rebuild |
|---|---|---|---|---|---|
| handle | FIXED | CHANGE (curve strength / arc length) | CHANGE | FIXED | yes |
| rot1 (POS1 yaw) | FIXED | CHANGE (start tangent) | CHANGE | FIXED | yes |
| rot2 (POS2 yaw) | FIXED | CHANGE (end tangent) | CHANGE | FIXED | yes |
| pitch | FIXED (y kept) | CHANGE (gradient) | CHANGE | FIXED | yes |
| cant | FIXED | FIXED (centerline unchanged) | CHANGE (roll) | FIXED | yes |
| asset | FIXED | FIXED | FIXED | CHANGE (look) | look re-applied, path unchanged |

Notes (from source evidence):

- Anchor POSITION is never changed by any edit: edits only mutate
  `yawDeg`, `pitchDeg`, `lengthH_m` of anchors or the transient cant.
- `cant` does NOT move the centerline: `MarkerPlacementEditingTest.t06`
  proves the centreline fingerprint is identical with cant 0 vs 8, while
  `frame.rollDeg` changes.
- `asset` does NOT rebuild the path: switching assets only changes
  gauge/colours in the renderer; the same `RailPath` object is consumed
  (R9RENDER log shows same pathIdentity, same samples/len).

## F3.3 Input units / ranges / invalid behaviour

| Edit | Unit | Valid range | Invalid behaviour |
|---|---|---|---|
| rot1/rot2 | degrees | unbounded (wrapped) | arity-guarded; missing arg -> usage msg |
| handle | metres | [0.1, 20.0] | out-of-range -> chat error, no mutation |
| pitch | degrees | [-45, 45] | out-of-range -> chat error, no mutation |
| cant | degrees | [-45, 45] | out-of-range -> chat error, no mutation |
| asset | id | registered ids (or fallback) | unknown id -> fallback asset, logged |

## F3.4 Frozen

- Anchor invariance under edits (position never changed) — FROZEN.
- cant does not alter centerline; asset does not alter path — FROZEN.
- The Influence Matrix above — FROZEN (values, not the example).
- Ranges/units of the six edits — FROZEN.

---

# F4 — Rail Asset / Geometry Isolation Contract

## F4.1 Responsibility boundary

- **RailPath** = where the rail goes (geometry: centerline, gradient, cant).
- **Rail Asset** = how that path is drawn (gauge, rail profile, sleepers,
  colours, mesh).

## F4.2 Minimum conditions (frozen)

1. Active asset change does NOT rebuild the RailPath.
2. Asset change does NOT change the centerline (positions/tangents/length).
3. Curve / Gradient / Cant geometry is never altered by an asset.
4. Identical RailPath can be rendered with multiple assets.
5. Asset / ModelPack loader internals are REPLACEABLE (this is the R9
   prototype loader; a real pack loader may replace it without contract
   change).
6. The Geometry/Look boundary is maintained: the renderer reads the RailPath
   frames and applies the asset as a "look".

## F4.3 REPLACEABLE implementation

The R9 `RailModelPackLoader` / `RailAssetDefinition.fromProfile` /
procedural segment drawer are an ORIGINAL prototype implementation, NOT frozen.
The frozen part is the semantic boundary in F4.2. Production ModelPack / 3D
mesh work (R11+) may replace internals.

## F4.4 Frozen

- Asset does not alter RailPath / centerline / curve / gradient / cant — FROZEN.
- RailPath is geometry; Asset is look — FROZEN.

---

# F5 — Placement Lifecycle Contract

## F5.1 Official user flow

```
./START_WEB_MINECRAFT.sh
 -> enter a normal World
 -> /railsys3 wand            (server-authoritative give)
 -> POS1 ordinary right-click
 -> POS2 ordinary right-click
 -> Auto Preview (production RailPath)
 -> optional edits (rot1/rot2/handle/pitch/cant/asset)
 -> Shift + right click Confirm (or /railsys3 confirm)
 -> Confirmed Production Rail (preview-identical)
 -> next placement session
```

## F5.2 Frozen lifecycle semantics

| Step | Contract |
|---|---|
| POS2 set | Auto Preview builds immediately. No immediate Confirm. |
| Confirm w/o preview | ERROR + no state mutation. |
| Confirm with preview | Promotes the EXACT preview RailPath; never rebuilds a different line. |
| Third ordinary click | Does NOT silently replace POS1/POS2; reports edit/confirm/cancel/clear. |
| cancel | Discards current preview ONLY; keeps POS1/POS2, edit values, cant, confirmed rail. |
| clear | Resets the transient session (markers/preview/transient cant); keeps confirmed rail + active asset. |
| Confirmed Rail Delete | A SEPARATE explicit operation (not yet implemented in R10); never part of confirm/cancel/clear. |

## F5.3 Command fallback identity

The command fallback (`/railsys3 confirm|cancel|clear|...`) and the wand
gesture call the SAME `RailsysPlacementController` operations. There is no
second geometry pipeline.

## F5.4 Frozen

- Auto-preview after POS2, no immediate confirm — FROZEN.
- Preview-less confirm = error + no mutation — FROZEN.
- Confirm = promotion (identity) — FROZEN.
- No silent replacement on third click — FROZEN.
- cancel/clear non-destructive semantics — FROZEN.
- Delete is separate — FROZEN.

---

# F6 — Client / Server Authority Contract

## F6.1 Authority inventory (audited fact, not aspirational)

| Domain | Class | Authority | Notes |
|---|---|---|---|
| Inventory | `EntityPlayerMP.inventory` | SERVER-AUTHORITATIVE | client inserts discarded by integrated-server sync |
| Wand give | `CommandRailsysPlace` (server) | SERVER-AUTHORITATIVE | `/railsys3 wand` forwards `/railsysplace wand`; client NEVER mutates its own inventory |
| Placement markers/preview/confirmed/asset/cant | `RailsysPlacementState` | CLIENT-LOCAL (client-static) + VALIDATION-ONLY | Web Worker split: server commands cannot reach client render statics; state is client-side SSoT for the renderer |
| Preview/confirmed render path | `RailsysRenderManager` | CLIENT-LOCAL | set from placement state (promotion); restored from `RailsysWorldRailData` (persistence) |
| Marker arrows overlay | `MarkerArrowRenderer` | CLIENT-LOCAL (production) | world-anchored, driven by placement state + `arrowsVisible` |
| Confirmed rail persistence | `RailsysWorldRailData` | SHARED/SYNCHRONIZED | world-bound data, restored into render manager once per world |
| Validation proof hooks | `*.validation.*` | VALIDATION-ONLY | world-gated, must not mutate normal placement |

Important: "server-authoritative" is used ONLY where the server really is the
authority (inventory, command routing). The placement state is CLIENT-LOCAL
in the Web Worker topology; calling it server-authoritative would be
mis-labelling. The integrated server is authoritative for the world/inventory;
the client is authoritative for its own transient placement UI state.

## F6.2 Frozen

- Inventory give is server-authoritative; client never grants itself the wand
  directly — FROZEN.
- Command routing: client forwards `/railsysplace wand`; server inserts with
  full-inventory drop semantics — FROZEN.
- The authority classification in F6.1 (as fact) — FROZEN.
- Validation hooks never mutate normal placement — FROZEN.

---

# Contract suite & gates

- The Foundation Contract Suite is a dedicated harness class
  (`railv2test.tests.RailsysFoundationContractSuite` and its helpers) that must
  be 100% PASS. Any FAILED test in the Foundation Suite is a NOGO for R11+.
- Golden Data (JSON fixtures under `doc/testing/phase1_r10f/golden/`) pin the
  production geometry numerically; representative regression case:
  **12.08 m / 13 samples / path identity preserved**.
- Normal World Acceptance must pass the R10F course (see
  `doc/testing/phase1_r10f/NORMAL_WORLD_ACCEPTANCE.md`).

## Change policy summary

A FROZEN contract may only be changed via a Contract Change Proposal
(`doc/architecture/phase1_r10f_contract_change_policy.md`). "RTM does it this
way" is not, by itself, a valid reason.
