# Marker and Anchor UI Research

Phase 0.7 PART B. External UX only. No RTM code.

---

## 1. Marker taxonomy

| ID | Common name | Colour / glyph | Role | Confidence | Sources |
|----|-------------|----------------|------|------------|---------|
| M-RED-C | Marker (center / ↑) | Red, straight arrow | Ordinary endpoint; start at **block center** | STRONGLY SUPPORTED | S04, S07 |
| M-RED-E | Marker (diagonal / ↗) | Red, diagonal | Ordinary endpoint; start at **block edge/end** for seamless joins / diagonals | STRONGLY SUPPORTED | S04, S07 |
| M-BLUE-C | Switch marker (↑) | Blue | Junction / turnout root or crossing endpoint | STRONGLY SUPPORTED | S03, S04, S07, S11 |
| M-BLUE-E | Switch marker (↗) | Blue diagonal | Same family, edge-aligned | STRONGLY SUPPORTED | S04, S07 |
| M-SLOPE-* | Slope markers 2/4/8/16 m | Yellowish (historical) | Fixed grade helpers | VERIFIED removed in RTM **2.0.x+** | S03, S04 footnote, S10 |

For **MC 1.12.2 / RTM2.4**: treat slope markers as **absent**. Gradients use red markers (+ height tools / pitch).

---

## 2. Placement rules (observed)

| Rule | Detail | Confidence |
|------|--------|------------|
| Facing | Arrows toward each other along intended rail | STRONGLY SUPPORTED |
| Pair count (plain rail) | Two red markers | STRONGLY SUPPORTED |
| Neighbor search | Clicking one marker finds compatible partner(s) automatically | STRONGLY SUPPORTED (behavioural); algorithm **UNKNOWN** |
| Orientation constraint | Opposite headings required; same heading bad | LIKELY (S12) |
| Length constraint | Soft/hard max (~60–64 m/blocks) | STRONGLY SUPPORTED existence; exact default UNKNOWN |
| Chunk constraint | Not documented in fetched text | UNKNOWN |
| Center vs edge | Needed for flush connections between segments | STRONGLY SUPPORTED (S07 diagram description) |
| Valid/invalid visual | Preview lines; obstacle black frame (wrench display) | LIKELY |

---

## 3. Connection patterns

### 3.1 Two-marker rail

Standard. Click either marker to build. **STRONGLY SUPPORTED**.

### 3.2 Multiple markers / automatic pairing

Public docs emphasize **two** markers for a simple rail. Automatic multi-marker graphs beyond turnout layouts are **UNKNOWN**.

### 3.3 Turnout (片開き)

- Blue at branch root; two reds as destinations (through + diverge). Click blue (or any marker per some CN texts).  
  Sources: S03, S04, S07, S11. Confidence: **STRONGLY SUPPORTED**.

### 3.4 Crossing / scissors

- Two pairs of facing blues (diamond / scissors layouts in ASCII tutorials).  
  Sources: S04, S07, S11. Confidence: **STRONGLY SUPPORTED** for existence of patterns; exact validity rules **LIKELY**.

### 3.5 Point switching after build

- Redstone into junction start side.  
  Sources: S03, S04, S07. Confidence: **STRONGLY SUPPORTED**.

---

## 4. Shape-edit UI (wrench) — observational model

### 4.1 Modes (historical wrench cycling)

Air R-click cycles modes: place red / place blue / (legacy slope) / **shape change**.  
Sources: S08, S07. Confidence: **STRONGLY SUPPORTED** for mode cycling; slope modes N/A on 1.12.2.

### 4.2 Visual elements

| Visual | Role (observed) | Confidence |
|--------|-----------------|------------|
| Black / dark preview path | Proposed rail centerline / shape | STRONGLY SUPPORTED (S07) |
| Green handle(s) | Editable tangent / control from active marker | STRONGLY SUPPORTED |
| Thin vs thick green (comment) | Active vs result / both ends | LIKELY (S03 comment) |
| Orange guide line | Related to **anchor pitch** (blog) | LIKELY (S09) |
| Black obstacle frame | Blocks blocking generation | LIKELY (S05) |

### 4.3 Manipulation

| Action | Effect (observed language) | Confidence |
|--------|----------------------------|------------|
| R-click marker in shape mode | Enter edit; select handle set | STRONGLY SUPPORTED |
| Move / aim while editing | Change green line **direction** (yaw-like) | STRONGLY SUPPORTED |
| Adjust length | Change green line **length** (handle magnitude) | STRONGLY SUPPORTED |
| Second R-click | Confirm shape | STRONGLY SUPPORTED (S07) |
| Scroll / modifiers | Not clearly documented in text | UNKNOWN |

### 4.4 State machine

```
WRENCH_IDLE
  --air R-click--> MODE_CYCLE (... → SHAPE_EDIT_ARMED)
SHAPE_EDIT_ARMED
  --R-click marker--> EDITING_HANDLES (preview visible)
EDITING_HANDLES
  --manipulate--> EDITING_HANDLES
  --R-click confirm--> SHAPE_LOCKED (preview committed)
  --??? cancel--> WRENCH_IDLE / SHAPE_EDIT_ARMED   [UNKNOWN]
```

Must edit **before** rail generation (S05). Confidence: **STRONGLY SUPPORTED**.

---

## 5. Anchor / parameter GUI

### 5.1 How to open

Hold **marker item**, R-click **placed marker**.  
Introduced RTM2.2.1 (MC1.10.2)+. Source: S03. Confidence: **VERIFIED** for intro version; **STRONGLY SUPPORTED** on 1.12.2.

### 5.2 Parameter inventory (evidence grades)

| Parameter | Meaning from public text | Unit / sign | Confidence |
|-----------|--------------------------|-------------|------------|
| Cant (value) | Track bank / カント; negative reverses side | Numeric; sign flips bias | STRONGLY SUPPORTED (S03) |
| Cant Center | Mentioned; wiki says “ignore explanation” / community confusion | UNKNOWN semantics | Mention VERIFIED; meaning **UNKNOWN** |
| Cant Edge | Prompted by research scope; **not found** as labelled field in fetched text | — | **UNKNOWN** (name not confirmed) |
| Cant Random | Not found in fetched text | — | **UNKNOWN** |
| Anchor Pitch | Named in S09; orange guide; 0 hides | Angle-like; 0 = flat guide | **LIKELY** |
| Anchor Yaw | Not found as exact GUI label in text; green handle direction is yaw-analogue | — | Handle behaviour STRONGLY SUPPORTED; label **UNKNOWN** |
| Anchor Length H | Not found as exact GUI label; green handle length is length-analogue (horizontal emphasis in flat edits) | — | Behaviour STRONGLY SUPPORTED; label **UNKNOWN** |
| Anchor Length V | Not found as exact GUI label; may relate to vertical handle / height edit | — | **UNKNOWN** / INFERENCE only if assumed |

**Policy:** Do not invent a complete RTM GUI schema. Railsys may *name* AnchorYaw/Pitch/LengthH/LengthV as **clean-room equivalents** of observed handle semantics.

### 5.3 Defaults / ranges

Exact numeric ranges for cant / pitch: **UNKNOWN** in public text.  
Negative cant allowed (S03). Confidence: **STRONGLY SUPPORTED**.

### 5.4 Start vs end

Cant images on wiki (not transcribed here) imply per-marker or per-rail settings; whether start/end independent: **LIKELY** (two markers each openable) but exact interpolation ownership **UNKNOWN**.

---

## 6. Height editing

- Wrench “レールの高さ” before laying (S05). Confidence: **LIKELY**.  
- Comments: wrench can change height in 0.5-block steps (S03 comment). Confidence: **LIKELY**.  
- Marker elevation difference creates gradient when rail generates (general RTM behaviour; slope markers obsolete on 2.x). Confidence: **STRONGLY SUPPORTED**.

---

## 7. Implications for Railsys (preview)

Railsys should expose:

1. Marker types: NormalCenter, NormalEdge, JunctionCenter, JunctionEdge (Phase mapping later).  
2. Facing + pair validation.  
3. Handle-based tangent edit (yaw + length) with centerline preview.  
4. Optional numeric panel: pitch, cant profile — even if RTM field names stay partially UNKNOWN.

See replication proposal for design.
