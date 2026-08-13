# R11-A: RTM Basic Rail Behaviour

Phase 1-R11 deliverable. Format per item: RTM Behaviour / Evidence / Information
Class / Version / Railsys R10F Current State / Gap / Requirement Candidate /
Open Question.

Evidence references: SRC-N ids from `SOURCE_INVENTORY.md`.

Legend: OBSERVED / KNOWN / INFERRED / UNKNOWN / CONFLICTING / VERSION-SPECIFIC.

---

## A1. Marker items and types

| Field | Value |
|-------|-------|
| RTM Behaviour | Red markers for ordinary rail endpoints; blue markers for switch/branch endpoints; straight (↑, center) vs diagonal (↗, edge-aligned) variants. Slope markers removed in RTM2.0.x. |
| Evidence | SRC-3 (MARKER_AND_ANCHOR_UI), SRC-5 (gamerch マーカー), SRC-9 (marker fields). |
| Class | KNOWN (concept); exact edge math INFERRED. |
| Version | all (slope removal VERSION-SPECIFIC RTM2.0+) |
| Railsys R10F | Two markers POS1/POS2 (RailsysMarkerSelection); no center/edge distinction; no blue/branch markers. |
| Gap | Railsys has only a single marker family and block-center placement; RTM distinguishes center vs edge markers (needed for flush joins) and red vs blue (branch). |
| Requirement Candidate | R-P0/R-P1: marker families NORMAL vs JUNCTION; placement mode CENTER vs EDGE. |
| Open Question | Exact block-edge offset semantics (INFERRED); edge marker direction convention. |

## A2. Placement flow

| Field | Value |
|-------|-------|
| RTM Behaviour | Place two facing markers (arrows toward each other) within length limit; then confirm by right-clicking a marker (empty hand in creative; rail item for model/ballast). Straight-ish rail generated along preview path. |
| Evidence | SRC-3 (WORKFLOW), SRC-5 (S02/S03/S04/S07), SRC-6 (pack). |
| Class | KNOWN (STRONGLY SUPPORTED). |
| Version | RTM2-family; 1.12.2 placement "renewed" (S07 warning) |
| Railsys R10F | POS1 click -> POS2 click -> auto preview -> confirm (Shift+RMB / /railsys3 confirm). Same two-endpoint concept; confirmation is sneak+right-click instead of R-click marker. |
| Gap | Conceptual match. Railsys auto-preview is immediate; RTM shows preview with wrench/black line and confirms on marker click. Rail item required to apply appearance in survival. |
| Requirement Candidate | R-P0: keep two-endpoint flow (foundation F5). R-P1: appearance applied at confirm (hold asset), matching RTM rail-item workflow. |
| Open Question | Does RTM consume markers on confirm? (UNKNOWN; railsys clears transient session — behavioural parity fine). |

## A3. Direction / facing contract

| Field | Value |
|-------|-------|
| RTM Behaviour | Arrows must face each other (opposite headings along the intended chord). Same-direction placement fails or makes a loop/360° bend. |
| Evidence | SRC-3 (workflow), SRC-5 (S02/S03/S04/S12). |
| Class | KNOWN (STRONGLY SUPPORTED). |
| Version | all |
| Railsys R10F | start tangent == +POS1 forward; end tangent == -POS2 forward (F2 frozen). RTM "faces each other" is the same intent. |
| Gap | None conceptually. Railsys is stricter (exact dot ±1 at endpoints, F2); RTM facing is per-marker arrow direction. |
| Requirement Candidate | KEEP RAILSYS (F2). No change. |
| Open Question | RTM tolerance for near-miss facing? (UNKNOWN) |

## A4. Coordinate datum / rail reference height

| Field | Value |
|-------|-------|
| RTM Behaviour | RailPosition has `blockX/Y/Z` + `posX/Y/Z` (double) + `height` (byte, 1/16-block units). Anchor correction constant exists (`Anchor_Correction_Value`). Rail laid on support surface; ballast height default 0.0625 (1/16). |
| Evidence | SRC-1 (javap RailPosition), SRC-2 (NR01 defaultBallast height 0.0625), SRC-5 (レール). |
| Class | KNOWN (fields); exact rail reference height INFERRED. |
| Version | all (ballast height consistent) |
| Railsys R10F | AnchorDefinition x/y/z; anchor y = support surface (F1). Clicked block bottom +1 at input boundary only. |
| Gap | RTM stores both block origin and precise double position; Railsys stores precise double position only (block origin derivable). RTM `height` byte (1/16) parallels Railsys F1 surface datum. |
| Requirement Candidate | KEEP RAILSYS F1. Optionally record block origin in persistence (R-P0 persistence). |
| Open Question | Exact `Anchor_Correction_Value` meaning (INFERRED as center/edge offset correction). |

## A5. Preview / edit

| Field | Value |
|-------|-------|
| RTM Behaviour | Wrench shape-edit shows a preview path (black/dark line) with green handles at endpoints; manipulating handles changes outgoing direction + magnitude (length); second click confirms shape. Orange guide line relates to anchor pitch (S09). |
| Evidence | SRC-3 (MARKER_AND_ANCHOR_UI 4.x), SRC-5 (S07, S03 comment). |
| Class | STRONGLY SUPPORTED (concept); exact 1.12.2 controls LIKELY (renewal warning). |
| Version | 1.7.10 documented; RTM2 renewed |
| Railsys R10F | Auto preview after POS2 + edits (rot1/rot2/handle/pitch/cant) rebuild preview. No green-handle visual; no wrench tool. |
| Gap | Railsys exposes numeric edits via /railsys3; RTM exposes visual handle manipulation + numeric marker GUI. Behavioural parity exists; visual affordance missing. |
| Requirement Candidate | R-P1: handle-based visual edit (green handle) as EXTENSIBLE addition; numeric edits remain. |
| Open Question | 1.12.2 exact wrench click semantics (UNKNOWN/LIKELY). |

## A6. Curve / S-curve / gradient / cant

| Field | Value |
|-------|-------|
| RTM Behaviour | Smooth curves from handle geometry; gradient via endpoint height difference (slope markers obsolete); cant set numerically in marker GUI (2.2.1+), negative reverses bank. Curve math class UNKNOWN. |
| Evidence | SRC-3 (CURVE_AND_GEOMETRY_RESEARCH, CANT_RESEARCH), SRC-5, SRC-9 (cant fields C_Center/C_Edge/C_Random, anchorPitch). |
| Class | KNOWN (behaviours); curve math UNKNOWN; cant interpolation UNKNOWN. |
| Version | cant RTM2.2.1+/1.12.2; 1.7.10 via KaizPatchX |
| Railsys R10F | HorizontalBezierGeometry (Hermite->Bezier) + StraightGeometry; gradient via anchor pitch/endpoint y; ConstantCantProfile rolls frame not centerline (F2/F3). |
| Gap | Railsys already matches the observable RTM behaviours (curve, gradient, cant). RTM cant has Center/Edge/Random fields; Railsys has constant cant (EXTENSIBLE to profile). |
| Requirement Candidate | KEEP RAILSYS geometry (F2). R-P1: cant profile (start/center/end) as EXTENSIBLE. |
| Open Question | RTM cant units (degrees vs other) UNKNOWN; Railsys uses degrees by clean-room decision. |

## A7. Geometry limits / invalid placement

| Field | Value |
|-------|-------|
| RTM Behaviour | Max rail length: config `railGeneratingDistance` default 64 blocks, max 256 (public source); `railGeneratingHeight` default 8, max 256. Tutorial "60m" is community/practical. Obstacles block placement (black frame); too-long / too-tall fail with feedback. |
| Evidence | SRC-1 (README config), SRC-9 (RTMConfig defaults), SRC-5 (S05/S07). |
| Class | KNOWN (config defaults via source); 60m UNKNOWN/CONFLICTING. |
| Version | all (defaults may vary) |
| Railsys R10F | No max-length / max-height / obstacle check. RailPath rejects degenerate/non-finite geometry only. |
| Gap | Railsys needs placement validation limits (length, height delta, obstacles) to match RTM and prevent pathological rails. |
| Requirement Candidate | R-P0: configurable max length + max height delta; R-P1: obstacle/collision check (allow/auto-clear). |
| Open Question | Railsys default limits (recommend 64m/256m analogues); obstacle semantics for Eaglercraft. |

## A8. Continuous placement

| Field | Value |
|-------|-------|
| RTM Behaviour | Placing successive segments connects at shared endpoints (edge/center markers used to join flush). No explicit "snap to existing rail" UI documented beyond shared marker positions. |
| Evidence | SRC-3 (MARKER_AND_ANCHOR_UI 2,3), SRC-5 (S07 edge markers). |
| Class | STRONGLY SUPPORTED (concept); exact snap algorithm UNKNOWN. |
| Version | all |
| Railsys R10F | No connection/snapping; each confirm replaces the render path; no persisted rail network. Continuous placement just re-selects POS1/POS2. |
| Gap | Rail Network + endpoint snapping needed (R11-B / R-P0). |
| Requirement Candidate | R-P0: Rail Network with endpoint connect/snap; flush joins via edge placement. |
| Open Question | Snap tolerance, auto-connect on placement, near-miss handling. |

---

## Summary (R11-A)

| Area | RTM | Railsys R10F | Verdict |
|------|-----|--------------|---------|
| Marker types | red/blue, center/edge | single pair | PARTIAL -> ADAPT (R-P1) |
| Placement flow | 2 facing markers + confirm | POS1/POS2 + confirm | MATCH (F5) |
| Direction | arrows face each other | start=+POS1, end=-POS2 | MATCH (F2) |
| Datum | block+pos+height(1/16) | support-surface anchor | MATCH (F1) |
| Preview/edit | wrench handles + numeric GUI | numeric edits + auto preview | PARTIAL -> EXTENSIBLE |
| Curve/gradient/cant | smooth, height-cant | Bezier/gradient/constant-cant | MATCH (F2/F3) |
| Limits | 64/256 length, 8/256 height | none | NOT IMPLEMENTED -> R-P0 |
| Continuous placement | connected segments | none | NOT IMPLEMENTED -> R-P0 (network) |
