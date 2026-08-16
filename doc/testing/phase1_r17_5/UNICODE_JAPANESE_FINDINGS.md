# 日本語Unicode表示 調査結果 (2026-08-17)

対象: Web Minecraft / Eaglercraft (MC 1.8系) + Railway Mod & Terrain Editing Tools
作業: AI-RIM Job 20260816-235445

## 1. 結論

**現在のビルドでは日本語文字（漢字・ひらがな・カタカナ）は正常に描画される。**

実機（headless Chrome + Eaglercraft、HW Vulkan）でワールドに入り、チャットへ
日本語を入力・送信してスクリーンショットを取得し、フォントソースデータ
（EPK内の unicode_page_XX.png + glyph_sizes.bin）とピクセル単位で照合した。

- チャット入力欄: 全15グリフで IoU = 1.000（完全一致）
- チャットログ（送信メッセージ）: 15/16グリフで IoU = 1.000
- 検証文字列: 日本語テスト / 鉄道 / 車両 / 接続 / 分岐器 をすべて含む文字列で確認

「日本語が表示されない」という症状は現行コードでは再現しない。原因として考えられるのは:
- 過去のビルド/状態でフォント資産が未梱包だった、または
- デスクトップのIME(日本語入力)の composition イベントに未対応で「入力できない・
  出ない」ように見えた（表示自体は正常）、または
- 誤認

## 2. 調査手順と確認内容

### 2.1 フォント資産の実体

ビルド済みオフラインHTMLに含まれる `assets.epk` を自作デコンパイラで解析。

| 資産 | 有無 | 内容 |
|---|---|---|
| `assets/minecraft/font/glyph_sizes.bin` | あり (65536B) | 日本語グリフ幅データ (U+3042 あ=0x0F 等) |
| `assets/minecraft/textures/font/unicode_page_*.png` | あり | 日本語ページ含む実グリフ画素あり |
| `assets/minecraft/textures/font/ascii.png` | あり | 既定英数字フォント |
| `ja_JP.lang` (lang.tmp.epk) | あり (140KB) | 日本語言語リソース |

- 日本語ページ (0x30-0x5f 等) は 256x256 PNG で実グリフ（alpha 255）を保持。
- EPK ソースは `desktopRuntime/resources`（build.gradle.kts の epkSources）。
- これらの資産は最初期コミット 784a776d "add files" から追跡されており、
  欠落・除外は無い。

### 2.2 描画経路

- FontRenderer (バニラ 1.8): `func_181559_a()` で日本語文字は
  `renderUnicodeChar()` へ。glyph_sizes.bin の幅データ + unicode_page_XX.png を描画。
- EaglerFontRenderer (Eaglercraft): ASCIIのみを最適化描画。日本語を含む文字列は
  `decodeASCIICodepointsAndValidate()` が false を返しバニラ経路へフォールバック。
  正しく動作。
- DefaultResourcePack: `/assets/minecraft/...` を EPK から解決。正常。

### 2.3 実機確認（軽量テスト）

`unicode_font_test.mjs`（run-full-gate.sh 経由、約70秒）で:

1. ワールド作成 → チャット入力欄に「/日本語テスト 鉄道 車両 接続 分岐器」を入力
   → スクリーンショット → グリフIoU=1.000
2. 平文メッセージ「日本語テスト 鉄道 車両 接続 分岐器 メッセージ」を送信
   → チャットログに `<PlayerName> 日本語テスト...` が表示 → グリフIoU=1.000
3. 対照として ASCII メッセージ送信 → 正常描画（回帰なし）

## 3. 残課題（入力側・既知の制限）

- デスクトップの日本語IMEの composition（変換中・確定）イベントには未対応。
  直接のキーボード入力が一部環境で確定文字が届かない可能性がある。
  - 代替: モバイル/タッチキーボードは composition 対応済み。
  - 代替: Ctrl+V の貼り付け（CLIPBOARD_PASTE 経由）で日本語を入力可能。
- これは表示ではなく入力側の制限であり、本作業の「表示」要件には影響しない。

## 4. 成果物

- `doc/testing/phase1_r17_5/unicode_font_test.mjs` … 日本語表示の最小確認テスト
- `doc/testing/phase1_r17_5/screenshots/fonttest3_*.png` … 実機スクリーンショット
- 実行: `./run-full-gate.sh doc/testing/phase1_r17_5/unicode_font_test.mjs`
  （既定 HW Vulkan、全体タイムアウト付き、実測約69秒）