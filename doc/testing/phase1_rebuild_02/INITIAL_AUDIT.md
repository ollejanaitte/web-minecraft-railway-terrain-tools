# Phase 1 Rebuild — STEP 0 初期監査 (CP-01)

日時: 2026-08-11 18:5x JST (start)
エージェント: OpenCode (DeepSeek系)

## Git 状態 (監査時点の実測)

- Branch        : main
- Local HEAD    : b531d6f5dab21be768bae234b9149dd7672b6ddc
- origin/main   : b531d6f5dab21be768bae234b9149dd7672b6ddc
- GitHub main   : b531d6f5dab21be768bae234b9149dd7672b6ddc (git ls-remote 確認)
- Sync          : LOCAL == origin/main == GitHub main (SYNC OK)
- Worktree      : main のみ (1)
- Local branch  : main のみ
- Stash         : なし
- origin/master : 削除済み (Phase 1.2.1) / 存在しない

## 未コミット変更 (tracked)

| ファイル | numstat | 扱い |
|---|---|---|
| doc/testing/phase0_1/screenshots/_create.png | Bin | 旧Phase 0.1成果物・変更しない |
| doc/testing/phase0_1/screenshots/_named.png  | Bin | 旧Phase 0.1成果物・変更しない |
| src/game/java/net/minecraft/client/renderer/RenderGlobal.java | +2 / -0 | Phase 1.4デモ用 debug println・変更しない |
| src/game/java/net/minecraft/command/CommandRailSystem.java | +111 / -6 | 保全対象 (md5 9afd512d414a3b67a15235c9290936cf) |
| src/game/java/net/minecraft/command/CommandRailsysPlace.java | +5 / -0 | Phase 1.4デモ用 debug println・変更しない |
| src/game/java/net/minecraft/entity/item/EntityRailVehicle.java | +13 / -3 | 保全対象 (md5 77ce0d66d8fbe4e4c6e2594579d10c0c) |

保全対象 md5 は過去 Phase (0.1〜1.5) の記録と一致。PRESERVED。

## 未追跡 (untracked, 52件)

- FINAL_REPORT.md, RTM JAR, NR01 ZIP, Phase 0.2/0.5/1.2.2/1.2.3 の screenshots/logs/scripts。
  いずれも commit 対象外。RTM JAR/NR01 は RTM プロプラ資産のため repo に保存しない方針は不変。

## ベースライン (監査時点で実測)

- makeMainOfflineDownload : BUILD SUCCESSFUL
- harnessTest             : PASSED=84 FAILED=0 SKIPPED=3 (RESULT: SUCCESS)
- offline HTML            : target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html
- ユーザー通常Chrome       : 稼働中 (PID 1747070, ~/.config/google-chrome) — 不干渉

## 対象ファイル確認 (Phase 1 基盤)

- src/geometry-core/java/net/minecraft/railsys/geometry/** : KEEP
- src/geometry-core/java/net/minecraft/railsys/path/**      : KEEP
- src/game/java/net/minecraft/railsys/render/*              : KEEP (RailRenderer / RailAsset / RailAssetDefinition / RailAssetRegistry / RailsysRenderManager)
- src/game/java/net/minecraft/railsys/placement/*           : KEEP
- src/game/java/net/minecraft/railsys/persist/*             : KEEP
- src/game/java/net/minecraft/railsys/validation/*          : RailsysGeomDebugEvidence / RailsysPathDebugEvidence は旧検証fixture (ISOLATE候補)
- src/game/java/net/minecraft/railv2/**                     : 旧検証spike (ISOLATE候補)
- EntityRailV2Car / RenderRailV2Car / CommandRailV2Validate : 旧検証物 (ISOLATE候補)
- MinecraftServer.tick → RailV2AutoValidate.onServerTick   : 旧AutoValidate (ISOLATE候補)
- GuiCreateWorld "eaglerflat" → FLAT hook                   : 再利用 (STEP1/2の新markerへ拡張)

## 環境

- DISPLAY=:0 (GUI検証可能)
- Chrome: /opt/google/chrome/chrome v151.0.7922.71
- Node: v22.23.2
- JAVA_HOME: /usr/lib/jvm/java-17-openjdk-amd64

## リスクメモ

- RenderGlobal.java に未コミットの per-frame debug println が残る。
  今回は RenderGlobal を編集せず、EntityRenderer へ1行のみ追記し、
  単一BOX描画を新規 `SingleBoxProofRenderer` に分離する方針。
  (RenderGlobal の未コミット変更と混ぜないため)
