# R13_STABLE_ID_CONTRACT — Phase 1-R13

Date: 2026-08-13 JST
The production Stable Rail ID contract (R12-A §3, REQ-P0-01). Implemented by
`net.minecraft.railsys.data.RailId` + `RailIdIssuer` + `RailWorldData`.

## Contract

| Rule | Implementation |
|------|----------------|
| Preview has NO stable id | placement state holds only AnchorDefinitions + transient RailPath (no RailId) |
| Confirm ASSIGNS the id | `RailWorldData.nextRailId()` called at the confirm boundary |
| Confirmed id is fixed | immutable `RailId` value; segment holds it for its lifetime |
| Edit keeps the same id | R12-G edit mutates the SAME RailSegment record (R21); id unchanged |
| Delete retires the id | `RailWorldData.delete(id)` marks retired via issuer; id not reused |
| Retired id not reused | issuer `next()` is monotonic; `isRetired` guards re-registration |
| Uniqueness | world store rejects duplicate ids (`duplicate RailId rejected`) |
| Invalid id rejected | `RailId` constructor + `parse` reject <=0 / malformed; validator rejects null/retired ids |
| Future persistence/network compatible | id value semantics (positive long + prefix) independent of serialization |

## Lifecycle

```
PLACEMENT (transient, no id)
  → CONFIRM (issue id via world issuer; create RailSegment)
  → ACTIVE (in world store)
  → EDIT (same id)
  → DELETE (retire id; remove from store)
  → id retired (never reused)
```

## Test coverage

R13 contract suite covers: preview_hasNoStableId, confirm_assignsStableId,
secondConfirm_assignsDifferentStableId, duplicateId_rejected,
retiredId_notReused, malformedId_rejected, deleteRetiresAndRemoves,
worldStoreKeepsActiveRails.
