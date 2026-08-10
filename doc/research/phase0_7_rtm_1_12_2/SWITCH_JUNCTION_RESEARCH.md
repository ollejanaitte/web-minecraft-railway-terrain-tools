# Switch / Junction Research

Phase 0.7 PART C. Phase 1 will **not** implement switches; research protects Geometry Core forward-compat.

---

## 1. Marker roles

| Pattern | Markers | Action | Confidence | Sources |
|---------|---------|--------|------------|---------|
| Simple turnout (片開き) | 1× blue root + 2× red ends | R-click blue (or marker) | STRONGLY SUPPORTED | S03, S04, S07, S11 |
| Crossing | Facing blue pairs | R-click | STRONGLY SUPPORTED | S04, S07 |
| Scissors crossover | 4× blue | R-click | STRONGLY SUPPORTED | S07, S11 |
| Diamond | Documented section on S03 | — | STRONGLY SUPPORTED existence | S03 headings |

Blue diagonal vs center variants same as red family. **STRONGLY SUPPORTED** (S04, S07).

---

## 2. Runtime switching

- Redstone input near junction start changes route.  
  Sources: S03, S04, S07. Confidence: **STRONGLY SUPPORTED**.  
- Dedicated 転轍機 item + RS wiring tutorials exist (S04). Confidence: **STRONGLY SUPPORTED** for existence.

---

## 3. Geometry implications (external)

| Observation | Implication for Railsys | Confidence |
|-------------|-------------------------|------------|
| One construction action creates multiple connected branches | Junction = multi-piece or branch-capable piece | STRONGLY SUPPORTED |
| Shared root point | Shared node id / connectivity | STRONGLY SUPPORTED |
| Route state is dynamic | Separate switch-state from geometry | STRONGLY SUPPORTED |
| Exact frog/point curve math | UNKNOWN | UNKNOWN |

Phase 0.6 already reserves piece **type** + params for future switch — **compatible**.

---

## 4. Out of Phase 1 scope

Confirm: switches, signals, interlocking remain excluded from Phase 1 acceptance (PHASE1_SCOPE_AND_ACCEPTANCE). This research only ensures contracts do not paint into a corner.

---

## 5. Open questions

- Max branch count per blue marker? **UNKNOWN**  
- Can switches carry cant independently per branch? **UNKNOWN**  
- Preview colouring for multi-path? **UNKNOWN**
