# Phase 1 Rebuild STEP 0〜2 — 実装計画

日時: 2026-08-11 (JST)

## 到達目標

クリーンなMinecraftワールド内に、Minecraft Block でも旧RailV2Car でもない
独立した3D直方体モデルを1個だけ、正しい位置・正しいサイズで描画し、
ユーザー本人に画面確認してもらう。ユーザー承認前に次STEPへ進まない。

## Scope 制限 (厳守)

- 線路は完成させない / RailPathへ複数BOXを並べない
- Curve / Gradient / Placement / Persistence へ進まない
- STEP 3 (BOXをStraight RailPathへ連続配置) には着手しない

## STEP 1 — Clean Validation Scene

- 新flat marker を GuiCreateWorld へ追加:
  - ワールド名に "cleanflat" → FLAT 強制, 検証hook無し (Clean Scene)
- 検証hook (SingleBoxProofValidation) は "singlebox" ワールドのみ発火。
  "cleanflat" / 通常ワールドでは何も起きない。
- Clean Sceneワールド名: `EaglerCleanFlat` (旧AutoValidate marker "eaglerflat" を含まない → 旧fixture非発火)
- 画面は「空・平坦な地面・HUD」のみ → SS-01取得
- FAIL条件: 旧巨大青赤車両・旧Rail/Markerが写っていたらSTEP 1 FAIL

## STEP 2 — Single 3D Box Proof

- 新規 `SingleBoxProofRenderer` (validation-only, 完全に小さい):
  - ワールド座標 static (300.0, 4.1, 300.0) ※flat地面top y=4 の上に載せる
  - 寸法: L=0.50m (X) / W=0.30m (Z) / H=0.20m (Y) — 1ブロックより十分小さい
  - 色: 明るい赤橙 (255,60,40) — 緑地面と明確に区別
  - Tessellator/WorldRenderer で独立geometry描画 (setBlockしない・Entity不要)
  - camera-relative translate を正しく扱う
  - world-name gate ("singlebox") で通常ワールド非漏洩
- EntityRenderer に1行: `SingleBoxProofRenderer.render(entity, partialTicks, entity.worldObj)`
  (RenderGlobal.java は編集しない → 未コミットdebug printlnと混ぜない)
- 新規 `SingleBoxProofValidation` (server tick):
  - "singlebox" ワールドのみ: プレイヤーをcreative flight + カメラ固定
  - ステージ: front (SS-02) → diagonal (SS-03) → far (SS-04) → 解放
  - 各ステージを chat tag `[RAILSYSTEM] singlebox stage=<tag>` で自動化に通知
  - ツアー終了後はカメラ解放 → ユーザーが自由移動して
    「カメラ移動してもワールド位置がズレない」を確認可能
- カメラ位置 (BOX中心 300,4.1,300):
  - SS-02 front: (300.0, 5.6, 301.6) yaw180 pitch12
  - SS-03 diagonal: (301.6, 5.6, 301.6) yaw135 pitch12
  - SS-04 far: (300.0, 7.5, 305.0) yaw180 pitch8

## 検証パイプライン

- 新規スクリプト doc/testing/phase1_rebuild_02/scripts/singlebox_proof.mjs
  - GUI (DISPLAYあり) / headless 両対応。検証専用profile (runtime/profiles/game 不使用)
  - RUN A: EaglerCleanFlat 作成 → SS-01
  - RUN B: EaglerSingleBox 作成 → SS-02..04 (stage tag 待ち)
- Vision Observer 補助 (vision_observer.mjs 再利用)
- 確認項目: BOX 1個のみ / 巨大でない / 想定位置 / 地面との関係自然 /
  duplicateなし / block gridへ丸められていない / 旧RailV2Carではない / 旧検証物と混在していない

## Checkpoints

- CP-01 Initial Audit (commit済み目安)
- CP-02 Phase 1 Reopen記録
- CP-03 Clean Validation Scene (STEP 1)
- CP-04 Single Box Renderer最小実装 (STEP 2コード)
- CP-05 GUI Screenshot Evidence (SS-01..04)
- CP-06 Human Acceptance待ち (USER_DECISION_REQUIRED)
- CP-07 Final Report

## 禁止事項 (再掲)

BOX複数配置 / RailPath配置 / Straight Rail / head・web・foot / sleeper /
Curve / Gradient / Piece Boundary / Rail Asset本実装 / Placement /
Persistence / Train / Switch / Phase 2
