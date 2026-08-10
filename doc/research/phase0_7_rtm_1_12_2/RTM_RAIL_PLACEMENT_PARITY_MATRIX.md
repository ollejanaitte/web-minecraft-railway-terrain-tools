# RTM Rail Placement Parity Matrix

Phase 0.7 PART E. Living tracker for clean-room parity (not binary compatibility).

Parity target meanings:

- **None** — not planned  
- **Conceptual** — same idea, different UX  
- **Behavioural** — same player outcomes  
- **Visual** — similar on-screen affordances  
- **Deferred** — later phase  

---

| RTM Feature | Evidence confidence | RTM observed behaviour | Railsys current | Proposed Railsys behaviour | Phase | Parity target | Unknown / Risk |
|-------------|---------------------|------------------------|-----------------|----------------------------|-------|---------------|----------------|
| Red marker pair place | STRONGLY SUPPORTED | Two facing reds → rail | Command / wand v1; v2 command validate | Marker A/B + confirm | 1.4 | Behavioural | Pair search algo |
| Diagonal / edge marker | STRONGLY SUPPORTED | Edge-aligned starts | None | NORMAL_EDGE offset rule | 1.4+ | Behavioural | Exact edge math |
| Empty-hand creative confirm | STRONGLY SUPPORTED | R-click marker | N/A | Allow in creative-like debug | 1.4 | Conceptual | Game-mode mapping |
| Rail item model select | STRONGLY SUPPORTED | Air R-click menu | None | AppearanceRef picker | 2.x | Conceptual | Pack system |
| Ballast / roadbed | LIKELY | Craft grid + height 0.0625 | Flat bed debug | appearance.ballast* | 2.x | Conceptual | |
| Max length limit | STRONGLY SUPPORTED | ~60–64 | None | config maxLengthM | 1.4 | Behavioural | 60 vs 64 |
| Wrench shape edit | STRONGLY SUPPORTED | Black preview + green handles | None | Handle edit + preview | 1.4 | Visual/Behavioural | 1.12.2 renewal details |
| Anchor yaw (handle dir) | STRONGLY SUPPORTED behaviour | Green dir | None | AnchorDefinition.yawDeg | 1.4 | Behavioural | GUI label |
| Anchor length H | STRONGLY SUPPORTED behaviour | Green length | None | lengthH_m | 1.4 | Behavioural | Label |
| Anchor pitch | LIKELY | Named; orange guide | pitch in samples | pitchDeg + viz | 1.4–2 | Behavioural | Units |
| Anchor length V | UNKNOWN label | Possibly height handle | Vertical profile | lengthV_m optional | 2.x | Conceptual | Existence |
| Cant numeric | STRONGLY SUPPORTED | Marker GUI; neg flips | rollDeg=0 Phase1 | CantProfile | 2.x (model 1.x) | Behavioural | Units |
| Cant Center | VERIFIED mention / UNKNOWN meaning | Confusing field | — | optional rollMid | 2.x | Conceptual | Semantics |
| Cant Edge / Random | UNKNOWN | Unconfirmed | — | omit until evidence | — | None | |
| Cant transition auto | UNKNOWN | Undocumented | — | LINEAR/SMOOTH Railsys choice | 2.x | Conceptual | |
| Slope markers | VERIFIED removed on RTM2 | N/A on 1.12.2 | N/A | Do not implement | — | None | |
| Gradient via ΔY | STRONGLY SUPPORTED | Unequal marker Y | Straight graded | Keep + pitch | 1.1–1.4 | Behavioural | Max grade |
| Vertical curve UI | UNKNOWN | Not documented | VerticalBezier model | Engineering later | 2.x | Conceptual | |
| Curve math class | UNKNOWN | Smooth path from handles | HorizontalBezier | Bezier via Hermite map | 1.1 | Conceptual | Not identity |
| Clothoid | UNKNOWN / no evidence | — | None | Not required for parity | optional adv | None | |
| Blue turnout | STRONGLY SUPPORTED | 1 blue + 2 red | Type reserved | Junction markers | 3.x | Behavioural | Frog geom |
| Scissors / diamond | STRONGLY SUPPORTED | 4 blue patterns | Reserved | Helpers | 3.x | Behavioural | |
| RS point switching | STRONGLY SUPPORTED | RS at root | None | Switch state | 3.x | Behavioural | |
| Obstacle black frame | LIKELY | Manual clear | N/A | Optional collision check | 2.x | Conceptual | |
| 10 m place guides | LIKELY | Marks while placing | None | Optional ruler | 1.4+ | Visual | |
| Local frame / roll to vehicle | LIKELY/UNKNOWN | Expected lean | roll field only | RailLocalFrame | 2.x | Behavioural | |

---

## Aggregate

| Bucket | Count (approx) |
|--------|----------------|
| Behavioural targets in Phase 1.x | Markers, preview, handles, grade, Bezier mapping |
| Deferred to 2.x+ | Cant UI, appearance packs, engineering mode |
| Deferred to 3.x | Switches |
| Explicit non-goals | Slope markers, clothoid-as-parity, asset copy |

**Parity matrix result:** Phase 1 can achieve **core behavioural RTM-like placement** without resolving UNKNOWN math/cant units.
