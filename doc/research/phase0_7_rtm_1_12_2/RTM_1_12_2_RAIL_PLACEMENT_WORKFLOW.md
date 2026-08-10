# RTM 1.12.2 Rail Placement Workflow Reconstruction

Phase 0.7 PART B. Clean-room external behaviour only.

Primary target: **MC 1.12.2 / RTM 2.4.x**.  
Where evidence is RTM2-family (1.10.2) or comparison (1.7.10), confidence is labelled.

---

## 1. Goal of this reconstruction

Describe what a player does to go from empty ground to **one placed rail segment**, including optional shape edit, model/roadbed choice, and confirmation.

## 2. Prerequisites (VERIFIED / STRONGLY SUPPORTED)

| Need | Evidence | Confidence |
|------|----------|------------|
| Forge + NGTLib + RTM jars for 1.12.2 | S01, S14, S16 | VERIFIED |
| Optional fixRTM for 1.12.2 | S01 | VERIFIED (recommended, not required for UX description) |
| Creative tab / inventory access to Marker and Rail items | S02, S04, S07 | STRONGLY SUPPORTED |

## 3. Baseline “empty ground → one rail” flow (RTM2-family)

Reconstructed composite flow. Steps marked with version notes.

```
[1] Obtain red Marker (↑ and/or ↗) and Rail item
[2] Choose start block; place Marker with arrow toward track direction
[3] Choose end block within length limit; place Marker with arrow facing start
[4] Optional: wrench → shape-edit mode → adjust green handles → confirm preview
[5] Optional: open marker settings GUI (hold marker, R-click placed marker) → cant / pitch params
[6] Optional: select rail model / ballast (air R-click on rail item, or crafted rail with ballast)
[7] Confirm: R-click a marker (empty hand in creative; rail item in survival / for model)
[8] Rail entity/geometry generates along preview path; markers typically consumed/replaced by rail
```

### Step notes

#### [1] Items

- Red marker = ordinary rail marker; blue = switch/junction marker (later).  
  Sources: S03, S04. Confidence: **STRONGLY SUPPORTED**.
- Diagonal (↗) vs straight (↑): start position at block **edge** vs **center**.  
  Sources: S04, S07. Confidence: **STRONGLY SUPPORTED** (concept); exact block-edge math **INFERENCE**.

#### [2][3] Facing rule

- Arrows must **face each other** (opposite directions along the intended chord).  
  Sources: S02, S03, S04, S12. Confidence: **STRONGLY SUPPORTED**.
- Same-direction placement: fail to place or produce a loop/360° bend.  
  Source: S12. Confidence: **LIKELY** (1.12.2-titled mirror howto).

#### Distance guide while placing

- ~10 m interval guide marks while placing markers.  
  Source: S04. Confidence: **LIKELY**.

#### Length limit

- S04 tutorial: **60 m** once.  
- S05 / S07: **64 blocks** default, config-raisable.  
- Confidence for existence of a max length: **STRONGLY SUPPORTED**.  
- Exact default on stock RTM2.4.24: **UNKNOWN** (contradiction → CONTRADICTIONS.md).

#### [4] Shape edit (wrench)

Documented most clearly on **1.7.10** (S07); comments on Gamerch (S03) and draft howto (S05) treat it as general. Addon Search warns **1.12.2 renewed** — treat detailed click semantics as **LIKELY** for 1.12.2, not VERIFIED.

Observed UX (S07, S03 comment):

1. Hold wrench; air R-click → cycle to “レールの形状変更”.
2. R-click a marker → show **black** (or dark) path preview + **green** control handles.
3. Manipulate active green handle (direction + length).
4. R-click again to confirm shape before laying.

Confidence:

- Existence of wrench shape-edit with preview + green handles: **STRONGLY SUPPORTED**.
- Exact 1.12.2 keybinds / which line is black vs dark-green: **LIKELY**.
- Internal curve math (Bezier etc.): **UNKNOWN** (not asserted).

#### [5] Marker settings GUI

- Available **RTM2.2.1 (MC1.10.2) onward**: hold marker item, R-click placed marker → settings including **cant**.  
  Source: S03. Confidence for RTM2-family: **VERIFIED** (wiki statement). For 1.12.2 specifically: **STRONGLY SUPPORTED** (RTM2.4 inherits; KaizPatch lists cant as backport from 1.10.2/1.12.2 — S10).
- Named **anchor pitch** appears in secondary blog (S09). Confidence: **LIKELY**.
- Full field list: see MARKER_AND_ANCHOR_UI.md.

#### [6] Appearance selection

- Rail item air R-click → model / gauge style picker (packs).  
  Sources: S07, S12. Confidence: **STRONGLY SUPPORTED**.
- Crafting table: recipe + ballast block row + ballast height (default 0.0625).  
  Source: S06. Confidence: **LIKELY**.
- Hold selected rail when confirming to apply appearance (survival / model). Creative may empty-hand.  
  Sources: S03, S07. Confidence: **STRONGLY SUPPORTED**.

#### [7] Confirm / generate

- R-click either endpoint marker to generate.  
  Sources: S02–S04, S07, S12. Confidence: **STRONGLY SUPPORTED**.
- Creative: may not need rail in hand (S03). Survival: rail item (S03/S07).

#### [8] Failures

| Message / symptom | Mitigations (public) | Confidence |
|-------------------|----------------------|------------|
| Too long | Shorten; raise config max | STRONGLY SUPPORTED |
| Obstacles | Remove blocks in black frame (wrench display mode); creative auto-clear from 1.7.10.24 noted on S03 | LIKELY / versioned |
| Height difference too large | Flatten or use gradient tools | S02 FAQ — LIKELY |

## 4. Minimal happy path (no edit)

Confidence: **STRONGLY SUPPORTED** on RTM2-family:

1. Place two facing red markers within length limit.  
2. Hold rail (or empty hand in creative).  
3. R-click a marker.  
4. Straight-ish rail appears connecting markers.

## 5. What is NOT reconstructed as fact

- Exact sampling density of generated mesh.  
- Exact continuity rules at joins beyond “face markers / edge vs center markers”.  
- Whether confirm destroys markers or converts them.  
- Clothoid / Bezier / Hermite identity.  

All **UNKNOWN** or deferred to geometry research doc.

## 6. 1.12.2-specific caution

S07 explicitly: RTM 2.x (1.12.2) rail placement **renewed** vs 1.7.10 guide.  
Therefore this document prefers:

1. Shared RTM2-family statements (S03 cant GUI, slope removal, S01 version).  
2. Cross-checked placement basics (S02, S04, S12).  
3. 1.7.10 wrench detail as **LIKELY** analogue pending video confirmation.

## 7. State machine (player-facing)

```
IDLE
  → PLACE_MARKER_A
  → PLACE_MARKER_B (facing constraint)
  → (optional) SHAPE_EDIT
  → (optional) PARAM_GUI (cant / anchor pitch / …)
  → (optional) SELECT_MODEL_BALLAST
  → CONFIRM_CLICK
  → RAIL_CREATED | ERROR_FEEDBACK
```

Cancel paths: break markers; abort shape edit without confirm (exact cancel control **UNKNOWN**).
