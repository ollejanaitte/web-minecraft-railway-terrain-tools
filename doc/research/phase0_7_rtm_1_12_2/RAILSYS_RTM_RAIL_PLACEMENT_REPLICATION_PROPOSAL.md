# Railsys RTM-style Rail Placement — Clean-room Replication Proposal

Phase 0.7 PART E.  
**Not** a Phase 1.1 implementation plan kickoff — design proposal only.

---

## 1. Goal

Reproduce, on Railsys / Eaglercraft Web Minecraft, the **player-facing rail laying experience** of RTM **1.12.2 (RTM2.4.x)**:

- Marker-based endpoints  
- Facing / pairing rules  
- Preview + handle editing  
- Optional pitch / cant parameters  
- Model / roadbed separation  
- Confirm → persistent RailPiece  

…using **original** Railsys geometry, rendering, and persistence (Phase 0.6 contracts), **without** copying RTM code or assets.

## 2. Non-goals

- Byte-identical GUI or textures  
- Decompiled math identity  
- Phase 1 full RTM editor  
- Switches / scissors in Phase 1  
- Claiming Bezier ≡ RTM internals  
- Shipping RTM packs / models  

## 3. Clean-room policy

| Allowed | Forbidden |
|---------|-----------|
| Public UX observations (this Phase’s docs) | RTM jar source / assets |
| Original algorithms matching external feel | Porting decompiled classes |
| Railsys-named parameters inspired by observed handles | Copying proprietary meshes/sounds |
| Configurable limits | Committing secrets / webhook URLs |

## 4. Three-layer imitation model

### Layer 1 — UX / Workflow

Match player steps and feedback (markers, preview colours of our choosing, confirm/cancel).

### Layer 2 — Railway Domain Model

Markers, Anchors, RailPieces, CantProfiles, AppearanceRefs — Railsys-owned schema.

### Layer 3 — Internal Mathematics

Phase 0.6 geometry (Straight, HorizontalBezier, Vertical profile, ArcLengthTable) or future extensions. Chosen for controllability + performance, not RTM identity.

RTM math may stay **UNKNOWN** forever; Layers 1–2 still ship.

---

## 5. RTM observed workflow (summary)

See `RTM_1_12_2_RAIL_PLACEMENT_WORKFLOW.md`.

Essence: **place two facing markers → optional shape/param edit → confirm with rail/empty hand → rail exists.**

## 6. Railsys target workflow

```
IDLE
 → place Marker A (yaw from player facing; type Center|Edge)
 → place Marker B (must face A within tolerances)
 → auto-pair (or explicit link tool)
 → show centerline PREVIEW (valid/invalid colour)
 → EDIT_SHAPE: drag start/end tangent handles (yaw + length H);
               optional pitch handles / numeric pitch
 → EDIT_CANT (optional / later phase): CantProfile fields
 → SELECT_APPEARANCE: rail style + roadbed block id (data only)
 → CONFIRM → create RailPiece(s) + clear or keep markers policy
 → CANCEL → discard preview; markers remain or wipe (configurable)
```

### Feedback

- Length display (metres)  
- Invalid: too long / bad facing / degenerate geometry / NaN  
- Optional curvature/radius readout (Railsys-computed, not RTM claim)

---

## 7. Marker design (Railsys)

```
RailMarker {
  id, worldX/Y/Z,
  yawDeg, pitchDeg,
  kind: NORMAL_CENTER | NORMAL_EDGE | JUNCTION_CENTER | JUNCTION_EDGE,
  linkGroupId?,
  state: IDLE | PAIRED | EDITING | CONSUMED
}
```

| RTM analogue | Railsys | Phase |
|--------------|---------|-------|
| Red ↑ | NORMAL_CENTER | 1.4 MVP |
| Red ↗ | NORMAL_EDGE | 1.4 or soon after |
| Blue ↑/↗ | JUNCTION_* | Phase 3+ |
| Slope markers | **Omit** (RTM2 removed) | never as RTM-parity |

Edge markers offset endpoint to block edge along yaw for seamless joins (clean-room rule; exact RTM offset **INFERENCE** — define Railsys rule explicitly in 1.4).

---

## 8. Anchor design

Clean-room names (may differ from unconfirmed RTM strings):

```
AnchorDefinition {
  yawDeg,          // horizontal tangent
  pitchDeg,        // vertical tangent
  lengthH_m,       // horizontal handle magnitude
  lengthV_m,       // vertical handle magnitude (optional MVP=0)
}
```

Per rail draft: `startAnchor`, `endAnchor`.

**UI on Eaglercraft:**

1. World handles (lines/arrows) click-drag for yaw + lengthH.  
2. Sneak-drag or secondary handle for pitch / lengthV.  
3. Optional numeric panel (chat command or GUI) for precision.  

This mirrors observed green-handle UX without copying meshes.

---

## 9. Preview design

| Element | Proposal |
|---------|----------|
| Centerline | Polyline sampled from draft geometry |
| Valid | Green (or theme token) |
| Invalid | Red |
| Handles | Distinct accent (RTM used green; we may choose differently) |
| Endpoint arrows | Show marker facing |
| Cant viz | Optional tick marks / rolled sleeper ghosts (post-1.4) |
| Slope viz | Pitch ticks / grade % text |

Colour parity with RTM is **not** required.

---

## 10. Curve generation options

| Option | RTM-like UX | Controllability | Realism | Stability | Cant fit | Switches later | Complexity | Eagler perf |
|--------|-------------|-----------------|---------|-----------|----------|----------------|------------|-------------|
| A Cubic Bezier | High (≈ handles) | High | Medium | High | Good | OK | Low | Good |
| B Hermite | Highest (tangent length) | High | Medium | High | Good | OK | Low | Good |
| C Arc + transition | Medium | Medium | Higher | Medium | Good | Harder | Med | Good |
| D Clothoid + arc | Low UX match to handles | Med | Highest | Med | Excellent | Hard | High | Med |
| E Hybrid | Configurable | High | High | Med | Excellent | Med | High | Med |

### Recommendation

- **Phase 1:** Keep Phase 0.6 **HorizontalBezier** (+ vertical profile). Map anchors → Bezier control points via standard Hermite↔Bezier conversion (`C0=P0`, `C1=P0+T0/3`, etc.).  
- **Optional later:** “Engineering mode” exposing radius + transition length (Option C/D) **alongside** RTM-compatible mode — do **not** expand Phase 1 scope.

**Why not Clothoid for parity:** no RTM public evidence (CURVE research).

---

## 11. Exact RTM Feel vs Engineering Mode

| Mode | Audience | Inputs |
|------|----------|--------|
| RTM-compatible | Builders from RTM muscle memory | Markers + anchors + preview |
| Advanced engineering | Layout purists | Radius, transition L, cant mm, grade ‰ |

Value: **high for long-term**, **out of Phase 1**. Phase 1.4 = RTM-compatible MVP only.

---

## 12. Gradient model

- Endpoint Y difference + `pitchDeg` on anchors.  
- Straight: constant pitch (Phase 0.6).  
- VerticalBezier / profile for smooth grade changes (Phase 0.6 data model).  
- No slope-marker items.

---

## 13. Cant model

```
CantProfile {
  rollStartDeg, rollEndDeg,
  rollMidDeg?,          // optional “center” analogue — Railsys-defined
  interp: LINEAR | SMOOTH_STEP
}
```

Propagation:

```
sample(s) → RailLocalFrame(pos, tangent, normalHoriz, up, roll)
  → leftRail = pos - 0.5*gauge*normal_rolled
  → rightRail = pos + 0.5*gauge*normal_rolled
  → sleeper basis from frame
  → (Phase 2+) vehicle roll from frame.roll
```

Phase 1: store profile / roll field; may render roll=0 until renderer ready (per Phase 0.6).

---

## 14. RailLocalFrame

**Recommended as shared basis** (clarification to Phase 0.6, not a rewrite):

`frameAt(distanceM) -> {p, t, n, u, roll}`

Unlocks left/right rails, sleepers, cant, bogies without duplicate math.

---

## 15. Renderer interaction

Phase 0.6 hybrid visible rails: sample frames along piece → mesh or debug lines.  
AppearanceRef (railStyleId, ballastBlockId, ballastHeight) **separated** from geometry — matches RTM craft/model split (S06, S13).

---

## 16. Placement state machine

```
IDLE
 → MARKER_A_SET
 → MARKER_B_SET
 → PREVIEW
 → EDIT_SHAPE
 → EDIT_CANT        # may skip / defer
 → CONFIRM
 → RAIL_CREATED
CANCEL from PREVIEW/EDIT_* → MARKERS_ONLY or IDLE
```

Invalid geometry blocks CONFIRM with message (length, facing, NaN).

---

## 17. Persistence representation

Extend WorldRailData v2 ideas:

```
RailPiece {
  id, geometryType, geometryParams,
  startAnchor, endAnchor,
  cantProfile?,
  appearanceRef?,
  connectivity
}
RailMarker (optional live entities) separate from baked pieces
```

Migration: ignore unknown fields forward-compatibly.

---

## 18. Invalid geometry handling

Align with Phase 0.6 error policy: reject non-finite / zero-length; clamp sample distances; user-facing reason codes for placement.

---

## 19. RTM-compatible Placement MVP (Phase 1.4)

Minimum:

1. Two markers (center sufficient).  
2. Start/end orientation from placement yaw.  
3. Preview centerline.  
4. Curve edit via two tangent handles → Bezier.  
5. Confirm / cancel.  
6. RailPiece creation + persistence hook.

**Defer from 1.4 MVP:** full cant GUI, edge markers, junctions, modelpack browser, clothoid, engineering mode.

Cant: keep data model; optional command-set roll for tests.

---

## 20. Future parity roadmap (proposal only)

| Feature | Suggested phase |
|---------|-----------------|
| Edge markers | 1.4+ / 1.5 |
| Cant editor UI | 2.x |
| Junction markers / switches | 3.x |
| Scissors / diamond helpers | 3.x |
| Gauge / rail model / roadbed picker | 2.x content |
| Numeric engineering editor | 2.x+ |
| Transition curve engineering | optional advanced |

---

## 21. Phase mapping vs Phase 0.6 sequence

| Phase 0.6 sequence | RTM UX relevance |
|--------------------|------------------|
| 1.1 Geometry Core | Enables handle→curve |
| 1.2 Piece/Path | Network joins |
| 1.3 Renderer | Preview + visible rails |
| 1.4 Placement | **MVP RTM-like flow** |
| 1.5 Persistence/Regression | Save drafts/pieces |

---

## 22. Implementation risks

| Risk | Mitigation |
|------|------------|
| Chasing unknown RTM math | Lock to Bezier/Hermite UX tests |
| Scope creep to full RTM UI in 1.4 | Hard MVP list above |
| Cant unit confusion | Railsys degrees only; document |
| Edge-marker join bugs | Explicit edge offset tests |
| Perf on long previews | Cap sample count; maxLength config |

---

## 23. Open questions (Railsys decisions — not USER_DECISION blockers)

1. Keep or destroy markers on confirm? (Default: destroy / consume.)  
2. Preview colours theme.  
3. Whether 1.4 includes Edge markers.  
4. Default maxLengthM = 64.

These can be decided at Phase 1.4 design without blocking 1.1.

---

## 24. Verdict of this proposal

Railsys **can** deliver RTM-like placement on top of Phase 0.6 geometry with **clarifications** (local frame, anchor mapping, CantProfile shape) and **without** mandatory clothoid/circular-arc contract revision before Phase 1.1.
