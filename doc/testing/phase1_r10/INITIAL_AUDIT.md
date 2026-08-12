# Railsys Phase 1-R10 — Initial Audit (CP-R10-01 / GATE 0)

- Audit time: 2026-08-12 18:0x JST (start)
- Agent: OpenCode (DeepSeek V4 Flash: opencode-go/deepseek-v4-flash)
- Task: R10 implementation entry audit — Final UX / /railsys3 / safe state transitions
- Verdict: **GATE 0: GO**

## 1. Git 状態 (監査時点の実測)

- Branch        : main
- Local HEAD    : 48a5dc149f8b8774546780b8b53c32cdc2593ad9
- origin/main   : 48a5dc149f8b8774546780b8b53c32cdc2593ad9
- GitHub main   : 48a5dc149f8b8774546780b8b53c32cdc2593ad9 (git ls-remote 確認)
- Sync          : LOCAL == origin/main == GitHub main (SYNC OK)
- Worktree      : main のみ (1)
- Local branch  : main のみ
- Stash         : なし

## 2. 保全対象ファイル (SHA-256)

| ファイル | SHA-256 | 判定 |
|---|---|---|
| `src/game/java/net/minecraft/command/CommandRailSystem.java` | `1037f467c400d6d313f575250e27e1d41a58c197ee9058bf52855ab06365e832` | PRESERVED (一致) |
| `src/game/java/net/minecraft/entity/item/EntityRailVehicle.java` | `b2fb0ea33a997ee4266619eefe695df80306481c4f62d8cd61dd9daf8420acff` | PRESERVED (一致) |

保全対象の SHA-256 は RR-01 / Current State Audit 記録と一致。今回も編集・stage・commit 対象外。

## 3. 既存 dirty tracked (保全・不変更)

- `doc/testing/phase0_1/screenshots/_create.png` : 旧Phase 0.1成果物・変更しない
- `doc/testing/phase0_1/screenshots/_named.png`  : 旧Phase 0.1成果物・変更しない
- `マイクラ_Phase1-R4_2026-08-12_00-34_CurveGradientRailSegmentProof.txt` : R4 session残骸・触らない

## 4. 既存 untracked (stage/commit 対象外)

- FINAL_REPORT.md (旧残骸)
- RTM1.7.10.46_Forge10.13.4.1558.jar / [unzip]NR01_v3.0.zip (RTM プロプラ資産、repo 不保存方針不変)
- Phase 0.2 / 0.5 / 1.2.2 / 1.2.3 / phase1_rebuild_02 の screenshots / logs / scripts / profiles
- マイクラ_Phase1-R7-R9_..._途中中止.txt
- いずれも commit 対象外。

## 5. 既知 5 欠陥 (R10 入力として確定)

1. **`/railsys3` コマンド欠如** — `RailsysClientCommands` に canonical `/railsys3` dispatch が無い
   (R10_IMPLEMENTATION_SCOPE slice 1)。現状は `/railsysplace` のみ。
2. **`/railsysplace give` が信頼不能** — 実装に duplicated `"arrows"` branch が入り、
   wand-give branch が無い (slice 8 修正対象)。
3. **`confirmOrClear` の意味二重化** — sneak+右クリック が confirm と clear を兼ねる unsafe UX。
   `RailsysPlacementState.cancel()` は confirmed path も消す (slice 4/5 修正対象)。
4. **RailsysPlacementState / RailsysRenderManager はクライアント static** — サーバー側
   Command / Validation とは Worker 分離のため到達しない。クライアント駆動 UX での
   command/state integration が R10 の核心作業 (slice 1-7)。
5. **`validation` パッケージへの責任漏れ** — 通常 placement の marker/arrow が
   `MarkerArrowRenderer` / `MarkerPlaceClientHook` (validation 下) を参照。
   R10 で正常運用 ownership へ移管し、validation gate は保持する (slice 9)。

## 6. ベースライン (RR-06 記録を最新として参照)

- Harness        : discovered 151 / PASS 148 / FAIL 0 / SKIP 3 / SUCCESS
- makeMainOfflineDownload : BUILD SUCCESSFUL
- Luna (Codex GPT-5.6)    : PASS, confidence 0.91 (R7-R9 evidence)
- R10 開始時の fresh harness/build 再計測は実装 Phase 冒頭で実施予定。

## 7. R10 方針 (Freeze済み spec との整合)

- 作業対象: クライアント駆動の `/railsys3` 統合 + safe state transitions のみ。
- Non-scope 不変: 新数学 / Real3D(R11) / persistence(R12) / network(R13) / switch(R14) /
  confirmed rail 削除 / train。
- Single confirmed slot 許容 (multi-rail は R12)。preview/confirmed 同一 RailPath。
- Acceptance: R10_IMPLEMENTATION_SCOPE.md の全要件。
- preserved 2 files 不変、対象文書のみ path 指定 stage、secret scan 実施。

## 8. リスクメモ

- R10 実装中は preserved 2 files / 既存 dirty / untracked に触れない。
- launcher fresh walkthrough は R10 implementation gate で実施。
- 危険・競合・同期不一致時は commit を止めて停止する。
