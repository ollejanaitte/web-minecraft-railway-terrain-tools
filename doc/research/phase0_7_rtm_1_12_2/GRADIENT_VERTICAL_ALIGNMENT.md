# Gradient / Vertical Alignment Research

Phase 0.7 PART C.

---

## 1. Version context

| Era | Gradient mechanism | Confidence |
|-----|--------------------|------------|
| RTM 1.7.10 stock | Slope markers 2/4/8/16 m (62.5–500‰) + later red-marker slopes | VERIFIED (S03) |
| RTM **2.0.x+** / **1.12.2** | Slope markers **removed**; red markers create grades | VERIFIED removal (S03); STRONGLY SUPPORTED red-marker grades |

KaizPatch on 1.7.10 also removes slope rails when installing (S10) while back-porting newer features.

---

## 2. Observed vertical behaviours

| Behaviour | Detail | Confidence | Sources |
|-----------|--------|------------|---------|
| Marker ΔY | Different marker elevations produce inclined rail | STRONGLY SUPPORTED | S02 FAQ, S03, S07 height tools |
| Wrench height edit | Adjust rail height before confirm; comments cite ~0.5 block steps | LIKELY | S05, S03 comments |
| Anchor Pitch | Named parameter; orange guide; set 0 to clear | LIKELY | S09 |
| Dedicated “vertical curve” UI (crest/sag parabolic) | Not found in public manuals | UNKNOWN |
| Grade limits | Soft: “height difference too large” FAQ; historical slope markers imply discrete per-mille steps | LIKELY existence of limits; exact RTM2.4 max **UNKNOWN** |
| Pitch continuous along rail | Visual expectation for smooth models | INFERENCE |

Historical slope marker grades (S03) — **1.7.10 reference only**:

| Marker | Grade |
|--------|-------|
| 16 m | 62.5‰ |
| 8 m | 125‰ |
| 4 m | 250‰ |
| 2 m | 500‰ |

These are **not** available as items on RTM2 / 1.12.2.

---

## 3. Mapping to civil terms

| Civil concept | RTM public analogue | Confidence |
|---------------|---------------------|------------|
| 縦断勾配 (grade) | Marker height difference / height tool / pitch | STRONGLY SUPPORTED |
| 縦曲線 (vertical curve) | Not explicitly exposed | UNKNOWN |
| Constant grade segment | Straight inclined rail between equal-pitch ends | LIKELY |
| Vertical transition | May emerge from handle Length V / pitch params if they exist | UNKNOWN |

---

## 4. Interaction with horizontal curve

Players combine curves + grades (tutorials mention both). Exact coupling (3D spline vs horizontal+vertical separation) **UNKNOWN**.

Phase 0.6 chose horizontal Bezier + vertical profile separation — compatible with unknown RTM internals so long as UX (endpoints + pitch) is matched.

---

## 5. Railsys takeaway

- Support graded straights and graded curves via endpoint Y + pitch continuity.  
- Optional VerticalBezier / vertical profile already in Phase 0.6 — **aligned**.  
- Do not require clothoid-like vertical transitions for RTM parity.  
- Expose pitch handles / numeric pitch for RTM-like feel.
