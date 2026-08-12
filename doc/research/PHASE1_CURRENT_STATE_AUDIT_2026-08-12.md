# Railsys Phase 1 Current State Audit

- Audit time: 2026-08-12 17:08 JST
- Audit HEAD: `45ea1c41df463f7fe96328a8bbf931c7e4477390`
- Branch: `main`
- Repository: `ollejanaitte/web-minecraft-railway-terrain-tools`
- Verdict: **GATE 0 GO, with documented remaining integration debt**
- Scope: read-only product-code audit; no product implementation in this roadmap task

## 1. Safety and Git state

At audit time, local `main`, `origin/main`, `git ls-remote origin main`, and the
GitHub API all resolved to `45ea1c41df463f7fe96328a8bbf931c7e4477390`.
There is one worktree and no stash entry. The worktree is dirty, but the dirty
set predates this task and can be isolated by staging only named roadmap files.

The two protected files remain byte-for-byte unchanged from the prior recovery:

| Protected file | SHA-256 |
|---|---|
| `src/game/java/net/minecraft/command/CommandRailSystem.java` | `1037f467c400d6d313f575250e27e1d41a58c197ee9058bf52855ab06365e832` |
| `src/game/java/net/minecraft/entity/item/EntityRailVehicle.java` | `b2fb0ea33a997ee4266619eefe695df80306481c4f62d8cd61dd9daf8420acff` |

The protected files and all unrelated dirty/untracked files must remain
unstaged and unmodified. No user world or runtime profile is a target of this
design task.

## 2. R1-R9 implementation audit

| Stage | Current evidence | Audit status |
|---|---|---|
| R1 Clean 3D Primitive | `SingleBoxProofRenderer`, `SingleBoxProofValidation` | Implemented proof retained |
| R2 Repeated Segment | `RepeatedBoxProofRenderer`, `RepeatedSegmentProofTest` | Implemented proof retained |
| R3 Straight Rail | `StraightRailProofRenderer`, `StraightRailSegmentProofTest` | Implemented proof retained |
| R4 Curve / Gradient | `HorizontalBezierGeometry`, vertical profiles, `CurveGradientSegmentProofTest` | Implemented proof retained |
| R5 Continuous Rail | `ContinuousRailProofRenderer`, production span emission through shared `PathSample` endpoints | GO evidence retained |
| R6 Marker Direction + Cant | `RailPath.fromMarkers`, `RailLocalFrame`, cant profiles, `MarkerDirectionContractTest`, `CantProofTest` | GO evidence retained |
| R7 Actual Placement | `ItemRailsysMarkerWand`, `RailsysMarkerSelection`, `RailsysPlacementController`, normal-world screenshots | GO implementation retained; acquisition UX remains R10 work |
| R8 Anchor Editing | client commands `rot1`, `rot2`, `handle`, `pitch`, `cant`; rebuild through the same production `RailPath` | GO implementation retained; final integrated UX remains R10 work |
| R9 Asset / ModelPack Prototype | `RailAssetProfile`, parser/loader/registry/fallback, two prototype assets, `RailsysProductionRenderer`, `RailModelPackTest` | GO prototype retained; production 3D asset contract remains R11 work |

The R9 recovery report and commits `16506bbd`, `b5f283e0`, `f260d22e`, and
`45ea1c41` support the R7/R8/R9 PASS. The root `FINAL_REPORT.txt` did not include
the completed recovery after its earlier stop record; this is a running-log
consistency defect, not a product-code rollback, and is corrected by this task.

## 3. Current normal-world UX

`START_WEB_MINECRAFT.sh` is the normal launcher. It uses
`runtime/profiles/game`, detects stale/missing offline output, invokes
`makeMainOfflineDownload` when necessary, and keeps validation profiles
separate.

The marker wand currently provides:

1. non-sneak right click: POS1, then POS2;
2. POS2: automatic preview through
   `AnchorDefinition -> RailPath.fromMarkers -> RailPath`;
3. sneak + right click: confirm if a preview exists, otherwise clear;
4. confirm: promote the same `RailPath` object, then render it as production.

Editing and asset selection are currently client-chat commands under
`/railsysplace`. `/railsys3` does not exist. `RailsysClientCommands` advertises
`give`, but the implementation has a duplicated `"arrows"` branch where the
wand-give branch should be, so `/railsysplace give` is not a reliable acquisition
path in the current source. These are R10 inputs, not grounds to invalidate the
already-proved R7 placement pipeline.

The present `confirmOrClear` operation is unsafe as a final UX contract because
one gesture has two meanings. `RailsysPlacementState.cancel()` also clears the
confirmed path. R10 must separate confirm, cancel, placement-session clear, and
future confirmed-rail deletion.

## 4. Single source of truth and render flow

The retained production flow is:

```text
Marker input
  -> AnchorDefinition
  -> RailGeometry / vertical profile / CantProfile
  -> RailPiece
  -> RailPath (distance s[m])
  -> PathSample + RailLocalFrame
  -> RailsysProductionRenderer
       + RailAssetDefinition (appearance only)
  -> continuous rail spans + s[m]-placed sleepers
```

`RailPath`, `PathSample`, and `RailLocalFrame` remain the line-shape source of
truth. Asset data may define local appearance such as gauge, cross-section,
sleeper mesh, material, and texture, but must not own the world-space line or
alter path sampling. Generated mesh is derived and disposable.

## 5. Persistence audit

`RailsysWorldRailData` is a single-rail schema-v2 prototype. It stores two
anchors and an asset ID, then reconstructs a path. It does **not** yet provide
the Phase 1 persistence contract:

- only one confirmed rail is represented;
- cant is not stored;
- asset version is not stored;
- topology/network metadata is not stored;
- restore uses a separate straight/Bezier decision rather than the complete
  authoring pipeline;
- the saved-data object is server-side, while the active placement/render state
  is client-static in Eaglercraft's client/server Worker split;
- the harness `PersistenceBaselineTest` is explicitly test-side semantic
  scaffolding, not a production save/restart gate.

Therefore persistence is **prototype/scaffold only** and R12 remains open.

## 6. Network and switch audit

`RailNetwork`, `RailConnection`, `RailEndpoint`, and `RailPiece` provide a useful
pure geometry-core registry, endpoint validation, adjacency, and tests. The
class contract explicitly excludes persistence, routing, and switch branch
logic. There is no normal-world placement, rendering, save/load, or editing
integration for a connected network. R13 remains open.

Switch support is only a fixture/contract placeholder (for example
`FIXTURE_SWITCH_BASIC_CONTRACT`) plus reserved asset terminology. There is no
production turnout geometry, route state, renderer, placement, persistence, or
GUI. R14 remains open.

## 7. Validation and vision audit

R1-R9 validation worlds are gated and the launcher maintains a separate normal
profile. Existing R7-R9 screenshots and the R9 Luna comparison remain current
evidence for HEAD. A responsibility leak remains: normal placement commands
and rendering reference `MarkerArrowRenderer` and `MarkerPlaceClientHook` under
the `validation` package. R10 must move reusable marker/arrow responsibilities
to normal production ownership while preserving validation gates.

## 8. Classification

### Completed in Phase 1 to date

- R1-R9 mathematical, rendering, placement, editing, and ModelPack prototype
  gates;
- distance-based RailPath sampling, direction contract, cant, continuous spans;
- two visibly distinct prototype assets, registry, loader, and fallback;
- normal launcher and isolated validation workflow.

### Remaining for Phase 1

- R10 final normal-world UX and `/railsys3` integration;
- R11 production Real 3D Hybrid rail assets and contract freeze;
- R12 multi-rail save/restart/restore across the Worker boundary;
- R13 connected normal-world rail network;
- R14 one production turnout;
- R15 compatibility, messages, recovery, and launcher UX cleanup;
- R16 feature freeze and complete acceptance journey.

### Phase 2 or later

- train production implementation and traversal;
- signals, stations, and catenary;
- advanced turnouts (diamond/slip/crossing), dispatching, and route finding;
- external pack distribution/download and advanced LOD.

## 9. Gate 0 conclusion

**GO.** Current Git state is understood, protected files are unchanged, R1-R9
artifacts are present in current code, and the remaining work can be designed
without touching product code or user data. The discrepancies above are
bounded R10-R14 inputs and do not require rebuilding R1-R9.
