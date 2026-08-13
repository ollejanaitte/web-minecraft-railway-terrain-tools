# ARCHITECTURE CONSISTENCY REVIEW — Phase 1-R12 (incl. Sol review resolution)

Date: 2026-08-13 JST
Self-review + independent Sol architecture review, with resolutions applied.

## Checklist

| # | Check | Result |
|---|-------|--------|
| 1 | Stable IDs consistent across subsystems | PASS (after fix) — railId@confirm, nodeId@first-connection, connectorId@attach; lifecycle table in DATA_MODEL §3. |
| 2 | Segment/Node ownership no contradiction | PASS (after fix) — RailNode is explicit persisted record; position rule frozen; node edits don't silently move member anchors. |
| 3 | Preview/Confirmed boundary matches R10F F5 | PASS — confirm = two-phase handoff; client promotes EXACT preview RailPath (F2.4) as initial derived geometry. |
| 4 | Asset/Geometry separation matches F4 | PASS — RailPath centerline source; asset=look; tongue authority resolved (asset keyframes + junction part binding). |
| 5 | Cant matches F2/F3 | PASS — frame-only roll; CC-1 additive; F3 six-edit invariance kept. |
| 6 | Switch state and Persistence consistent | PASS (after fix) — desired/committedRoute model; persist routeInput + owner state; committed derived; animation not saved. |
| 7 | ModelPack import and Asset Definition consistent | PASS (after fix) — import-boundary-only RTM format; allowlist renderer mapping; no JS. |
| 8 | Connector and Persistence consistent | PASS — connector persisted by id+targetRailId; reload restore at R23. |
| 9 | Connector and Delete/Edit consistent | PASS — delete dependency check includes connectors; repair flags orphans. |
| 10 | Vehicle Interface and Network consistent | PASS — traversal reads committedRoute; interface frozen; vehicle Phase 2. |
| 11 | Validation and Data Model consistent | PASS — pure function over data model at confirm/edit/load/repair. |
| 12 | R11 P0 all traced | PASS — 11/11 DESIGNED (REQ-P0-11 marked policy-frozen/numeric-pending). |
| 13 | Circular dependency none | PASS — layer DTO/port boundary documented; switch<->3D<->modelpack dependency resolved via asset profile DTO (no runtime cycle). |
| 14 | Phase 1 / Phase 2 boundary clear | PASS — signal/crossing logic, detectors, trains, ATS/ATC, undo/redo = Phase 2. |

## Sol review resolution

Independent review (gpt-5.6-sol) raised design blockers; each is now resolved:

| Sol finding | Resolution |
|-------------|------------|
| F3 conflict: confirmed editing moves endpoints | CC-5 reclassified as an APPROVED ADDITIVE Contract Change Proposal (R10F policy): F3 six-edit invariance unchanged; one new reposition operation added (EDIT_DELETE_REPAIR §2.3, R10F_CONTRACT_CHANGE_DECISIONS CC-5). |
| F6 authority handoff undefined | CC-6 + §3.1: two-phase handoff with acceptFingerprint==previewFingerprint promotion rule + server-corrected re-derive path. |
| RailNode not authoritative / position authority | RailNode explicit persisted record; position DERIVED from members; members authoritative; no silent member movement (DATA_MODEL §4.3, NETWORK §2/§4). |
| ID issuance inconsistent | lifecycle table (railId@confirm / nodeId@connect / connectorId@attach) normalized across DATA_MODEL §3, CLASSIFICATION D-F1, PERSISTENCE matrix. |
| Junction branch ownership undefined | branches = independent RailSegments (own railIds) via branchRailIds; Junction owns points/route/input only (DATA_MODEL §4.4, SWITCH §3). |
| operational fields owner (occupied/signalState/gauge) | occupied/signalState schema-reserved, NOT persisted in Phase 1 (no writers); gauge snapshot refreshed from asset, asset authoritative (DATA_MODEL §2, PERSISTENCE matrix). |
| route desired/committed/traversal conflated | Route type + 3-value model; vehicles traverse committedRoute; terminology normalized to committedRoute everywhere. |
| persistence wording contradictory | "save routeInput + owner state; route derived; animation not saved" (PERSISTENCE §2/§5/§8). |
| roadmap R17/R18 reload gate before R23 | R17/R18 exit gates = in-session only; reload restore deferred to R23 (ROADMAP R17/R18/R23). |
| CC-4 blockPos absent from schema | blockPos added as derived metadata on RailEndpoint + in PERSISTENCE matrix (DATA_MODEL §4.2). |
| F2 exact promotion not stated | confirm promotes EXACT preview RailPath only when server accepts unchanged (fingerprint rule); else re-derive (DATA_MODEL §5.1). |
| ModelPack "runtime never depends" wording | clarified: post-import runtime core/renderer never depends on RTM formats (MODELPACK §1). |
| tongue keyframe authority | asset owns keyframes/default pose; junction owns part->route binding by id (3D_RAIL §8). |

## Result

All 14 consistency checks PASS after applying the Sol-review resolutions.
P0 trace complete; no frozen R10F contract is changed (additions only, CC-1..6).
