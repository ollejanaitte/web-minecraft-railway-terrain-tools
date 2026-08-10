# Version Comparison (1.7.10 / 1.10.2 / 1.12.2)

Phase 0.7 PART D. Do not collapse versions.

---

## Summary table

| Topic | 1.7.10 (RTM1.7.10.x) | 1.10.2 (RTM2.2.1 era) | 1.12.2 (RTM2.4.x) | Confidence notes |
|-------|----------------------|------------------------|-------------------|------------------|
| Version line | RTM 1.7.10.* | RTM 2.2.x | RTM 2.4.x | VERIFIED mapping S01 |
| Red/blue markers | Yes | Yes (RTM2 family) | Yes | STRONGLY SUPPORTED |
| Diagonal ↑/↗ | Yes | Yes | Yes | STRONGLY SUPPORTED |
| Slope markers | Yes (2/4/8/16m) | Removed since 2.0.x | Removed | VERIFIED removal S03 |
| Empty-hand creative place | Documented | Likely | Likely | S03/S07 |
| Max length default | 64 blocks (S07) | ? | 60m (S04) vs 64 (S05) conflict | CONTRADICTION |
| Wrench shape edit | Well documented (S07) | Present | Present but UX **renewed** (S07 warning) | STRONGLY / LIKELY |
| Marker settings GUI | Stock: limited/absent; KaizPatch adds | Introduced 2.2.1 | Present | VERIFIED intro; STRONGLY on 1.12.2 |
| Cant | Stock no; KaizPatch backport | Yes | Yes | STRONGLY SUPPORTED |
| Anchor pitch (named) | Unknown on stock | Likely with GUI | Likely | LIKELY (S09) |
| Rail model air menu | Yes | Yes | Yes | STRONGLY SUPPORTED |
| Ballast craft height 0.0625 | Documented general | — | — | LIKELY shared |
| fixRTM companion | N/A (KaizPatch instead) | — | Recommended S01 | VERIFIED recommend |
| Switch / scissors | Yes | Yes | Yes | STRONGLY SUPPORTED |

---

## Placement UX narrative differences

### 1.7.10 (S07)

Highly documented: empty-hand R-click, wrench black/green, center vs edge diagrams, 64-block warning.

### 1.12.2 / RTM2

Same conceptual loop (markers → optional edit → rail confirm) but Addon Search states renewal — treat click details / GUI layout as **not byte-identical**. Prefer S03 (RTM2.2.1 updating) + S02 + S12 for 2.x flavour.

### 1.10.2

Bridge version for cant GUI (S03). Useful for RTM2-family feature dating, not as play target for this Phase.

---

## What must not be ported blindly into Railsys “1.12.2 parity”

1. Slope-marker workflow (obsolete on target).  
2. Exact 1.7.10 wrench mode list including slope placement.  
3. Assuming 60 vs 64 without config abstraction.  
4. Assuming KaizPatch-only behaviours are stock 1.12.2.
