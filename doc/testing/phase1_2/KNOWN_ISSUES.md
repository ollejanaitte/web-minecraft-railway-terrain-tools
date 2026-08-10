# Known Issues (Phase 1.2)

## Defects
No Phase 1.2 requirement is unmet. All acceptance conditions PASS.

## Deferred (planned later phases — NOT defects)
- Switch geometry / dynamic routing (Phase 3; endpoint already supports
  multiple outgoing connections).
- A* route search (deferred per Phase 0.6).
- Persistence (Phase 1.5): piece/endpoint identity is value-based
  (int pieceId, long endpoint id) so a future store can persist it.
- Final rail renderer (Phase 1.3): left/right rails, sleepers, ballast.
- Marker / anchor placement UI (Phase 1.4).
- Train Core / formation / bogie (Phase 2): RailPath.sampleByDistance + travel
  heading is the intended distance-based API for it.
- Cant physics / RTM cant UI.

## FYI — RailLocalFrame "up" sign convention (pre-existing Phase 1.1)
During Phase 1.2 continuity work we re-derived that
`RailLocalFrame.fromTangent` computes `up = forward × right`, which for a
horizontal eastward track yields `up = (0,-1,0)` (down) rather than the
doc-comment's stated `right × forward`. The frame is orthonormal and
right-handed, and Phase 1.2 continuity tests pass because they compare
frames consistently. This is a Phase 1.1 code/doc discrepancy, NOT changed in
Phase 1.2 (would break the frozen Phase 1.1 core); flagged for a Phase 1.3+
review before the renderer consumes `up` for rail top placement.

## FYI — validation automation race
The world-create → join transition on the Flat Validation World remains the
known Phase 0.5/1.1 menu-recovery race (occasional
`GuiScreenIntegratedServerCrashed` when AutoValidate fixtures throw, or a
bounce-to-menu). Phase 1.2 run script retries with MAX_RUNS and the crash cause
(this phase) was a disconnected evidence fixture that has been fixed. The
pipeline is bounded and self-cleaning.

## Operational notes
- Chrome runtime profiles are gitignored; only evidence (SS-R1_2-*),
  console/log captures and scripts are committed.
- Instrumented HTML is generated at runtime (gitignored).
