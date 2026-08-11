# Phase 1 Reopen — KEEP / ISOLATE 分類 (CP-02)

日時: 2026-08-11 (JST)
目的: 旧Phase 1を「最終完成」扱いせず、再利用基盤と旧検証物を切り分ける。

方針:
- 旧Phase 1のvisual acceptanceは再評価対象。Human Visual Acceptance なしの PASS は禁止。
- 既存コードを一括削除しない。今回の Clean Validation Scene へ「出さない」ことを優先する。
- Phase 2 Entry Gate: CLOSED (今回のPhase 1 Rebuild STEP 0〜2 はPhase 1領域)。

## KEEP (再利用候補 / Production基盤)

| 分類 | 対象 |
|---|---|
| Geometry Core | src/geometry-core/java/net/minecraft/railsys/geometry/** (Straight, HorizontalBezier, ArcLengthTable, Vertical*, RailLocalFrame, AnchorDefinition, Cant*, RailMath) |
| Path Core | src/geometry-core/java/net/minecraft/railsys/path/** (RailPath, RailPiece, RailEndpoint, RailConnection, RailNetwork, PathSample, RailPathEntry, RailValidationResult) |
| Renderer hook | EntityRenderer.renderWorldPass → renderRailSystemProduction / renderRailSystemDebug (pass==0 内) |
| Render基盤 | RailsysRenderManager, RailRenderer, RailAsset, RailAssetDefinition, RailAssetRegistry (今回は「使い込まない」だけで壊さない) |
| Placement | RailsysPlacementState, CommandRailsysPlace (壊さない) |
| Persistence | RailsysWorldRailData (壊さない) |
| GUI Validation基盤 | validation_workflow.mjs / validation_instrumented.html (screenChanged hook), CDP toolkit |
| Vision Observer | vision_observer.mjs (kimi → codex-luna auto-fallback) |
| Human Assist基盤 | run-validation-workflow.sh (GUI/headless, HUMAN_REQUIRED検出, Discord指示) |
| Flat World hook | GuiCreateWorld "eaglerflat" → FLAT強制 (STEP1/2へ新marker追加で再利用) |
| ランチャー | START_WEB_MINECRAFT.sh (通常プレイ専用profile: runtime/profiles/game) |

## ISOLATE (今回のClean Sceneへ出さない / 旧検証物)

| 分類 | 対象 | 隔離方法 |
|---|---|---|
| 旧AutoValidate | RailV2AutoValidate (course/4両車両/camera tour/stone pad/iron-block line) | world-name gate (eaglerflat / eaglervalidate)。Clean Sceneは別marker名を使い発火させない |
| 旧検証車両 | EntityRailV2Car, RenderRailV2Car (id 202) | 単一BOX検証ではspawnしない |
| 旧カメラツアー | RailV2AutoValidate の tourTag 列 (formation_side..path_overview) | 上記同様 gate で除外 |
| 旧検証線路/マーカー | RailV2Course.placeRails, placeCameraPads (stone pad), RailsysGeomDebugEvidence, RailsysPathDebugEvidence (iron_block/glass lines, gold markers) | AutoValidate非発火で配置されない |
| 旧検証コマンド | CommandRailV2Validate (/railsysv2) | Clean Scene中は実行しない |
| Phase 1.1/1.2 debug evidence | doc/testing/phase1_1, phase1_2 の screenshots/logs | 参照のみ・現行Sceneと混ぜない |
| 旧marker block / test formation | 上記fixture群 | 同上 |

## 削除方針

- DELETE は行わない。今回の Clean Scene から除外するだけでよい。
- EntityRenderer の既存フック (renderRailSystemProduction) は維持し、単一BOXは独立フックとして別途追加。

## 記録

- 本資料は commit 対象 (doc/testing/phase1_rebuild_02/**)。
- 保全対象 (CommandRailSystem.java / EntityRailVehicle.java) は不変。
