# Cant Research (RTM 1.12.2 / RTM2 family)

Phase 0.7 PART C — priority topic. Clean-room external only.

---

## 1. Version availability

| Build | Cant | Evidence | Confidence |
|-------|------|----------|------------|
| RTM2.2.1 (MC1.10.2)+ marker GUI | Cant settings present | S03 | **VERIFIED** for introduction |
| RTM2.4 / MC1.12.2 | Cant expected present (RTM2 line) | S03 + S10 (cant listed as feature of 1.10.2/1.12.2 backported to KaizPatch) | **STRONGLY SUPPORTED** |
| Stock RTM 1.7.10 | Generally **no** cant; community: use KaizPatch | S03 comments | **STRONGLY SUPPORTED** |

---

## 2. How players set cant

1. Place markers (and optionally shape-edit).  
2. Hold marker item; R-click placed marker → settings GUI.  
3. Enter cant-related numeric fields.  
4. Negative value reverses bank direction.  
Source: S03. Confidence: **STRONGLY SUPPORTED**.

Wiki includes **images** for cant procedure (not downloadable as RTM assets into this repo; described only). Caption/text: “Cant Centerの説明は無視してくれ” → community treats Cant Center as poorly explained / ignorable. Confidence: **VERIFIED** that field exists and confuses users; semantics **UNKNOWN**.

---

## 3. Parameter sheet

| Name | Observed | Unit | Sign | Interpolation | Confidence |
|------|----------|------|------|---------------|------------|
| Cant (primary) | Numeric bank amount | Unknown (degrees? height diff? arbitrary UI units?) | Negative reverses | Unknown function along rail | STRONGLY SUPPORTED existence; unit **UNKNOWN** |
| Cant Center | Field exists; ignore per wiki joke | UNKNOWN | UNKNOWN | UNKNOWN | Mention VERIFIED; meaning UNKNOWN |
| Cant Edge | Research target name | — | — | — | **UNKNOWN** (not confirmed in fetched text) |
| Cant Random | Research target name | — | — | — | **UNKNOWN** |

---

## 4. Cant transition behaviour

Desired civil pattern:

```
straight (cant≈0) → transition (increasing) → curve body (constant?) → exit transition → straight
```

| Question | Public evidence | Confidence |
|----------|-----------------|------------|
| Does RTM auto-build entry/exit cant transitions? | Not described in manuals fetched | **UNKNOWN** |
| Is cant constant per rail piece? | Possible given per-marker GUI | **LIKELY** |
| Linear vs smooth interpolation along distance | Not stated | **UNKNOWN** |
| Transition length parameter | Not found | **UNKNOWN** |
| Linkage to horizontal curvature | Not stated | **UNKNOWN** |

Do **not** claim RTM implements civil cant transitions.

---

## 5. Visual / geometric propagation (external)

What players likely see (community expectation + mod nature):

| Effect | Confidence |
|--------|------------|
| Track structure rolls / left-right rail height differs visually | LIKELY (purpose of cant feature) |
| Sleepers / roadbed follow roll | LIKELY |
| Vehicles lean with cant | LIKELY / UNKNOWN split — not verified in text sources here |
| Only centerline tilt with upright rails | Possible alternative; **UNKNOWN** |

Separate **appearance** from **simulation**.

---

## 6. Relation to Phase 0.6 contract

Phase 0.6: `RailSample.rollDeg` present; cant **data model only** in Phase 1; sign = positive when right rail lower (lean into curve).

RTM research:

- Supports keeping roll in sample.  
- Does **not** force Phase 1 physics.  
- Suggests clarifying CantProfile (start/center/end) as **future** data — optional clarification, not hard rewrite.

---

## 7. Railsys clean-room CantProfile (proposal preview)

Independent of unknown RTM units:

```
CantProfile {
  rollAtStartDeg
  rollAtEndDeg
  optional rollAtMidDeg   // analogue to “center” if desired
  transitionMode: NONE | LINEAR | SMOOTH  // Railsys choice
}
```

Apply via RailLocalFrame.roll → left/right rail offsets + sleeper basis + optional vehicle roll later.

Parity target: **behavioural** banked track UX, not binary-identical GUI.
