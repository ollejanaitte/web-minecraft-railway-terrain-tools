# Curve and Geometry Research (RTM 1.12.2 external)

Phase 0.7 PART C. **No Bezier/Clothoid assertion without evidence.**

---

## 1. Observed facts (what players see)

| Observation | Confidence | Sources |
|-------------|------------|---------|
| Two endpoints with facing directions define a rail | STRONGLY SUPPORTED | S02–S04, S07 |
| Default connection is “simple” join (often visually straight if aligned) | STRONGLY SUPPORTED | S07 |
| Wrench shape edit shows continuous preview path between markers | STRONGLY SUPPORTED | S07 |
| Green handles at ends change **outgoing direction** and **magnitude** | STRONGLY SUPPORTED | S07, S03 comment |
| Changing handles yields shallow diagonals / curves without moving markers | STRONGLY SUPPORTED | S07 |
| Curve tutorial section on Gamerch unfinished | — | S04 |
| No public manual names “Bezier”, “Clothoid”, “Hermite”, or “circular arc” for track math | — | all fetched text |
| Pack JSON documents rail **appearance**, not marker curve equations | STRONGLY SUPPORTED | S13 |

---

## 2. Endpoint / tangent model (observational)

```
Marker A: position P0, facing F0 (player-placed yaw; pitch via height / anchor pitch?)
Marker B: position P1, facing F1
Handles:  H0 length/dir from A; H1 from B
Preview:  continuous path C(u) from P0 to P1 respecting handles
```

Confidence for this **external** model: **STRONGLY SUPPORTED**.  
Confidence that F0/F1 equal handle directions after edit: **LIKELY**.

---

## 3. Mathematical model candidates

| Candidate | Why it could fit | Why not verified | Confidence as “RTM uses X” |
|-----------|------------------|------------------|----------------------------|
| A. Cubic Bezier | Two endpoints + two control points ↔ handles | Never named; many interpolants look similar | **INFERENCE only** — not claim of fact |
| B. Cubic Hermite | Endpoints + end tangents (dir×length) map cleanly to green handles | Same | INFERENCE |
| C. Circular arc | Simple constant-radius curves | Handle length control suggests more DOF than single radius | Weak for general case |
| D. Clothoid / Euler spiral | Railway-realistic curvature ramp | No UI for transition length / A-parameter found | **No evidence** |
| E. Composite / proprietary | Author-built blend | Likely for a large mod, but unprovable from outside | UNKNOWN |
| F. Piecewise linear preview only | Unlikely given smooth visuals described | Tutorials imply smooth rails | Low |

**Verdict for Phase 0.7:** Internal RTM curve class = **UNKNOWN**.  
For Railsys, choose a clean-room model that **matches the handle UX** (recommend Hermite or Cubic Bezier — see replication proposal), without claiming RTM identity.

---

## 4. Transition curve / clothoid explicit search

Searched JP/EN for クロソイド / clothoid / transition curve / 緩和曲線 in RTM rail context.

| Question | Result | Confidence |
|----------|--------|------------|
| Does public RTM UX expose clothoid parameters? | **Not found** | UNKNOWN (absence ≠ proof of absence, but no positive evidence) |
| Is curvature ramp UI documented? | **Not found** | UNKNOWN |
| Is cant transition tied to geometry transition in docs? | Cant set numerically; linkage undocumented | UNKNOWN |

Record: **do not implement ClothoidGeometry as RTM-parity requirement** unless new evidence appears. Optional advanced engineering mode later is separate.

---

## 5. Sampling / visual smoothness

- Generated rails appear as continuous 3D models (entity/TE rails), not vanilla block rails.  
  Confidence: **STRONGLY SUPPORTED** (mod identity / screenshots in community).  
- Sampling density / LOD: **UNKNOWN**.  
- Performance warning if max length raised (S07): **STRONGLY SUPPORTED** for 1.7.10 guide; LIKELY still relevant on 1.12.2.

---

## 6. Radius

No public “set radius = R” field found for ordinary marker rails. Radius is an **emergent** property of handle geometry if any. Confidence: **LIKELY**.

---

## 7. Implications

1. Phase 0.6 HorizontalBezier already matches candidate A (clean-room).  
2. Hermite is UX-isomorphic to Bezier via control-point conversion.  
3. Do **not** revise contract to Clothoid based on this research.  
4. Leave math identity UNKNOWN in parity matrix.
