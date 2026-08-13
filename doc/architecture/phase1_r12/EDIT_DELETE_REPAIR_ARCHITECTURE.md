# R12-G: Confirmed Editing / Delete / Repair Architecture

Phase 1-R12 design freeze. Inputs: REQ-P0-08 (Confirmed Rail Editing),
REQ-P0-09 (Delete/Replace/Repair), R10F F3 (anchor invariance) + F5 (delete
separate), R11-D (RTM edit/delete), R11-B (switch editing).

## 1. Goals

- Confirmed rails can be safely edited / deleted / replaced / repaired after
  placement.
- Every operation is a validated TRANSACTION: select -> edit -> preview ->
  validate -> commit | cancel.
- Reference safety: connections, junctions, and connectors are always
  considered.

## 2. Confirmed Rail Editing (REQ-P0-08)

### 2.1 Selection

- Select a confirmed rail by: railId (from network), click-on-rail (spatial),
  or endpoint node. Selection yields the authoritative RailSegment record.

### 2.2 Edit transaction

```
SELECT rail
  -> open transaction (copy working set: segment + affected connections)
  -> apply edit(s): endpoint / direction / handle / curve / gradient / cant /
     asset / gauge policy
  -> rebuild PREVIEW RailPath from edited endpoints (F2 fromMarkers)
  -> VALIDATE (R12-J: length/height/continuity/overlap/gauge)
  -> COMMIT (write world data, invalidate caches) | CANCEL (discard)
```

### 2.3 Edit kinds

F3 scoping (frozen): R10F F3 froze the SIX PLACEMENT-EDIT operations
(rot1/rot2/handle/pitch/cant/asset) with anchor-position invariance. Confirmed
Rail Editing ADDS one NEW operation — endpoint repositioning (with a height
sub-mode) — which is NOT part of the F3 six-edit set and therefore does not
change F3. It is approved as an additive Contract Change Proposal via CC-5
(see R10F_CONTRACT_CHANGE_DECISIONS.md). All six F3 edits keep their frozen
invariance.

| Edit | Changes | Invariant |
|------|---------|-----------|
| reposition endpoint (NEW, CC-5) | endpoint anchor x/y/z | node continuity re-validated; other members unchanged |
| reposition height (sub-mode of CC-5) | anchor y | same as above |
| direction (F3 rot) | yaw | anchor position unchanged (F3) |
| handle | lengthH/V | position unchanged (F3) |
| curve | handles/geometry params | F2 pipeline (F3) |
| gradient (F3 pitch) | pitch | position unchanged (F3); endpoint height is the CC-5 variant |
| cant | CantProfile | centerline unchanged (F2) |
| asset | assetId/version | geometry unchanged (F4) |
| gauge policy | mixed-gauge flag | validation only |

### 2.4 Impact on connections

- If an edited endpoint is in a node, the node position (derived) updates and
  F2 continuity at the node is re-validated. If continuity breaks, the edit is
  rejected or the connection is flagged for manual disconnect.
- Junction edits (R12-C §10) rebuild topology; branch edits propagate.

## 3. Delete / Replace / Repair (REQ-P0-09)

### 3.1 Delete

```
DELETE rail:
  1. dependency check (connections, junction members, connectors)
  2. optionally disconnect members (disconnect instead of delete)
  3. remove segment; retire railId
  4. prune orphan nodes / connections
  5. connectors referencing it -> INVALID flag (or user choice)
```

- Deletion is a SEPARATE operation (R10F F5) — never part of confirm/cancel/
  clear.
- Junction deletion removes branches + root; connected rails become free ends.

### 3.2 Replace

- Asset-only replace: swap assetId; geometry unchanged (F4); re-validate gauge.
- Geometry replace: change endpoints/handles (same as edit §2).
- Reconnect: after delete/edit, re-snap free ends within tolerance.

### 3.3 Repair

| Case | Repair |
|------|--------|
| Broken connection (missing member) | drop stale connection + rebuild node |
| Missing asset | fallback + warning; user re-assigns |
| Invalid topology (orphan node / 1-member node) | prune or re-terminate |
| Orphan connector | drop or flag INVALID |
| Corrupt store entry | quarantine + log (R12-F) |

## 4. Undo / Redo decision (frozen)

- Phase 1: NO persisted undo/redo (REQ-P2-02). Edits are transactional
  (commit/cancel) but no history stack. Rationale: P0 scope + R11 DEFER.
  A future undo/redo would wrap the same transaction layer (EXTENSIBLE).

## 5. Transaction data (in-memory only)

- Working copy of the segment + affected connections + connectors list.
- No partial writes: commit writes the full set; cancel discards.
- During transaction, the renderer continues showing the pre-edit rail until
  commit (preview overlay separate).

## 6. Requirement trace

- REQ-P0-08: §2.
- REQ-P0-09: §3.
- REQ-P1-04: switch edit/delete (§2.4, §3.1).
- R10F F3: anchor invariance; F5: delete separate.
- REQ-P2-02: undo/redo DEFER (§4).

## 7. Open questions

- Click-on-rail selection precision (spatial index) — R13.
- Disconnect-then-delete UX — R13.
- Multiplayer concurrent edit conflict handling — R13 (server-authoritative
  transaction, F6).
