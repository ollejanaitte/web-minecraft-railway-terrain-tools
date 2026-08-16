# Visual Validation FAST/FULL Gate 設計 (2026-08-17)

対象: Web Minecraft / Eaglercraft + Railway Mod & Terrain Editing Tools
作業: AI-RIM Job 20260816-235445「Visual Validation高速化＋日本語Unicode対応」

## 1. 背景: Visual Validation が遅い原因（調査結果）

R16/R17 の実測ログ（`doc/implementation/phase1_r16/logs/`、
`doc/implementation/phase1_r17/logs/`）と各スクリプトの実装を確認した。

### 遅延要因（複合）

1. **毎 Phase で FULL Visual Validation を複数回実行している（運用問題）**
   - R16: r16 → r16v2 → r16v3 の 3 回、R17: r17 → r17v2 の 2 回。
   - 1 回の FULL 実行は概ね 5〜8 分（ビルド 1〜2 分 + Chrome/Eaglercraft 起動 1 分
     + ワールド作成 2〜3 分 + チャット操作・スクリーンショット 3〜4 分）。
   - Sol レビュー指摘ごとに FULL を再実行していたため、Phase あたり 15〜20 分以上。

2. **FAST な代替が存在していなかった（設計問題）**
   - `harnessTest`（純 JVM、実測 4 秒）や各数値 Measure タスクは存在するが、
     「通常開発中はこれで良い」というルールが無く、実質的に毎回ゲーム実機確認へ行っていた。
   - Phase 1.1 以降、主要な数値検証は harness で十分可能なのに利用されていなかった。

3. **R16/R17 の acceptance スクリプトが headless 時に SwiftShader を使っていた（設計/実装問題）**
   - `r16_acceptance.mjs` / `r17_acceptance.mjs` の `launchChrome`:
     `--headless=new --use-gl=angle --use-angle=swiftshader`（ソフトウェア描画）。
   - Phase 0.2 で確立した標準は HW Vulkan（`--use-angle=vulkan`、NVIDIA GTX 1050）。
     ソフトウェア描画は 5〜10 倍遅く、ワールドロード・レンダリングが大幅に低下。
   - `validation_workflow.mjs`（phase1_2_2）も同様に SwiftShader 指定。

4. **固定スリープ・ポーリングが過剰**
   - ワールド入場後 20 秒固定待ち、1 チャット当たり約 2 秒、スクリーンショット前 2.5 秒、
     1.5 秒ポーリングループ等。1 回の acceptance で数十秒の無駄待ち。

5. **シェルレベルに全体タイムアウトが無い**
   - 各ステージの待機は（概ね）上限付きだが、Chrome フリーズ・CDP ハング・
     SwiftShader 凍結時は複数の上限が連鎖して数分〜10 分以上滞留し得る。

### 結論

- **Visual Validation（実機スクリーンショット）自体は有用であり、廃止しない。**
- 主因は **運用・テストルールの問題**（FULL を毎回・複数回実行、FAST ルール欠如、
  非標準の GPU パス使用、過剰な固定待ち、全体タイムアウト無し）。

## 2. 改善方針

### FAST gate（通常開発中）

- ブラウザ / ゲーム起動なし。コンパイル + 軽量テスト + 数値測定のみ。
- 実測: ビルド(インクリメンタル)約 14 秒 + harnessTest 約 4 秒 ≈ 20 秒。
- コマンド: `./run-fast-gate.sh [measure-task]`

実行内容:
1. `./gradlew makeMainOfflineDownload`（コンパイル確認、変更が無ければ UP-TO-DATE で高速）
2. `./gradlew harnessTest`（純 JVM 数値テスト）
3. 任意: `./gradlew <measure>`（r16Measure / r17Measure 等の数値品質タスク）

### FULL gate（Phase / 重要節目）

- FAST に加え、実機 Chrome + Eaglercraft + ワールド + スクリーンショット。
- HW Vulkan を標準とし、SwiftShader はフォールバックのみ（明示指定時）。
- **シェルレベル全体タイムアウトを必須**（既定 1500 秒、環境変数 FULL_TIMEOUT で変更）。
- コマンド: `./run-full-gate.sh <acceptance.mjs> [--skip-build]`

## 3. 最終仕様

| Gate | 用途 | 実行内容 | ブラウザ/ゲーム | 想定時間 |
|---|---|---|---|---|
| FAST | 通常実装中・細かい修正 | build + harness + measure | 不要 | 約 20〜60 秒 |
| FULL | Phase 完了・実機確認が必要な場合 | FAST + acceptance.mjs + スクリーンショット | 必要（HW Vulkan） | 約 5〜10 分 |

- 既定 GPU: headless でも `--use-angle=vulkan`（HW）。`GPU_MODE=swiftshader` で明示フォールバック。
- 全体タイムアウト: `timeout` コマンドで強制。超過時はクリーンアップ後に FAIL 終了。
- 失敗時はログと exit code で原因追跡可能とする。