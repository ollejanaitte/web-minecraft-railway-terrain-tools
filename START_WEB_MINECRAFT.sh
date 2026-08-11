#!/usr/bin/env bash
# ============================================================================
# START_WEB_MINECRAFT.sh - Web Minecraft 起動ランチャー (通常プレイ用)
#
# 【これが唯一の起動入口です】
# Ubuntuデスクトップでこのファイルをダブルクリックするか、ターミナルで
#     ./START_WEB_MINECRAFT.sh
# と実行してください。
#
# 動作:
#   * スクリプトの場所からrepository rootを自動判定（どの場所からでも可）
#   * クライアント成果物(offline HTML)が無ければ自動ビルド
#   * ソースが成果物より新しい場合も再ビルド（stale検知）
#   * 通常プレイ専用プロファイル(runtime/profiles/game)でChromeを起動
#     ※検証用(validation)プロファイルとは完全に分離しています
#   * 通常のChromeをkillしません（専用profileだけを管理）
#   * 既に起動中なら衝突せずに通知
#   * 失敗時は画面を閉じず原因を表示
#   * ローカルHTTPサーバーは不要（file:// offline HTMLを使用）
#
# オプション:
#   --force-build   起動前に必ず再ビルド
#   --skip-build    ビルドせず起動（成果物が無い場合は失敗）
#   stop            起動中のゲームを終了
# ============================================================================
set -uo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT" || exit 2

CHROME=${CHROME:-/opt/google/chrome/chrome}
if [ ! -x "$CHROME" ]; then
  CHROME="$(command -v google-chrome-stable 2>/dev/null || command -v google-chrome 2>/dev/null || command -v chromium 2>/dev/null || true)"
fi
if [ -z "$CHROME" ] || [ ! -x "$CHROME" ]; then
  echo "[START] ERROR: Chromeが見つかりません。CHROME環境変数でChromeのパスを指定してください。" >&2
  echo "[START] 例:  CHROME=/opt/google/chrome/chrome ./START_WEB_MINECRAFT.sh" >&2
  exit 2
fi

HTML="$ROOT/target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html"
PROFILE="${GAME_PROFILE:-$ROOT/runtime/profiles/game}"
LOGDIR="$ROOT/runtime/logs"
LOG="$LOGDIR/game_$(date +%Y%m%d_%H%M%S).log"
GPU_MODE="${GPU_MODE:-auto}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"

mkdir -p "$(dirname "$PROFILE")" "$LOGDIR"

say() { echo "[START] $*"; }

# ---------------------------------------------------------------------------
# stop
# ---------------------------------------------------------------------------
if [ "${1:-}" = "stop" ]; then
  pids=$(pgrep -f "user-data-dir=$PROFILE" 2>/dev/null | grep -v $$)
  if [ -z "$pids" ]; then
    say "起動中のゲームはありません。"
    exit 0
  fi
  say "ゲームを停止します: $pids"
  kill -TERM $pids 2>/dev/null
  sleep 3
  kill -9 $pids 2>/dev/null || true
  say "停止しました。"
  exit 0
fi

# ---------------------------------------------------------------------------
# 二重起動チェック（専用profileの既存インスタンスと衝突しない）
# ---------------------------------------------------------------------------
if pgrep -f "user-data-dir=$PROFILE" >/dev/null 2>&1; then
  say "既にゲームが起動しています (profile: $PROFILE)。"
  say "停止するには: ./START_WEB_MINECRAFT.sh stop"
  exit 2
fi

# ---------------------------------------------------------------------------
# ビルド
# ---------------------------------------------------------------------------
FORCE_BUILD=0
SKIP_BUILD=0
for arg in "$@"; do
  case "$arg" in
    --force-build) FORCE_BUILD=1 ;;
    --skip-build)  SKIP_BUILD=1 ;;
  esac
done

is_stale() {
  [ -n "$(find "$ROOT/src" "$ROOT/build.gradle.kts" "$ROOT/settings.gradle.kts" \
          -type f \( -name '*.java' -o -name '*.mjs' -o -name '*.kts' \) \
          -newer "$HTML" 2>/dev/null | head -n 1)" ]
}

NEED_BUILD=0
if [ ! -f "$HTML" ]; then
  NEED_BUILD=1
elif [ "$FORCE_BUILD" = "1" ]; then
  NEED_BUILD=1
elif is_stale; then
  NEED_BUILD=1
fi

if [ "$NEED_BUILD" = "1" ] && [ "$SKIP_BUILD" = "1" ]; then
  say "ERROR: --skip-build指定ですがクライアント成果物が無い/古いため起動できません。" >&2
  exit 2
fi

if [ "$NEED_BUILD" = "1" ]; then
  say "クライアントをビルドします (makeMainOfflineDownload) ..."
  if ! ./gradlew makeMainOfflineDownload >"$LOG" 2>&1; then
    say "ERROR: ビルドに失敗しました。ログ: $LOG" >&2
    say "最後の30行:" >&2
    tail -n 30 "$LOG" >&2
    exit 2
  fi
  say "ビルド完了。"
fi

if [ ! -f "$HTML" ]; then
  say "ERROR: offline HTMLが見つかりません: $HTML" >&2
  exit 2
fi

# ---------------------------------------------------------------------------
# 起動モード判定
# ---------------------------------------------------------------------------
# GUI(ウィンドウ)モード: ChromeのデフォルトGPU設定を使用する。明示的な
#   --use-gl/--use-angle 指定はGUI環境でEGL初期化に失敗しウィンドウが
#   正しく表示されないケースがあるため(Phase 1.2.1 CP-04で実測)、付けない。
#   GPU_MODE=swiftshader を明示指定した場合のみフォールバックを適用。
# HEADLESSモード: 検証パイプラインと同じGPUフラグで起動し、CDPで確認可能に。
PORT="${PORT:-9222}"
HEADLESS=0
if [ "${HEADLESS:-0}" = "1" ] || [ -z "${DISPLAY:-}" ]; then
  HEADLESS=1
fi

FLAGS=(--no-sandbox --autoplay-policy=no-user-gesture-required --mute-audio
       --hide-scrollbars --window-size=1280,720 --user-data-dir="$PROFILE")

if [ "$HEADLESS" = "1" ]; then
  # 検証用と同じGPU選択 (auto -> vulkan / 明示swiftshader)
  if [ "$GPU_MODE" = "auto" ]; then
    if ls /dev/dri/renderD* >/dev/null 2>&1 && \
       ls /usr/share/vulkan/icd.d/nvidia_icd.json >/dev/null 2>&1; then
      GPU_MODE=vulkan
    else
      GPU_MODE=swiftshader
    fi
  fi
  if [ "$GPU_MODE" = "swiftshader" ]; then
    FLAGS+=(--disable-gpu-sandbox --enable-unsafe-swiftshader --use-gl=angle --use-angle=swiftshader)
  else
    FLAGS+=(--use-gl=angle --use-angle=vulkan)
  fi
  if ss -tln 2>/dev/null | grep -q ":$PORT "; then
    say "ERROR: CDPポート $PORT が使用中です。既存の検証プロセスを停止するか PORT=xxxx で変更してください。" >&2
    say "確認: ss -tlnp | grep $PORT" >&2
    exit 2
  fi
  FLAGS+=(--headless=new "--remote-debugging-port=$PORT")
  say "HEADLESSモード (DISPLAYなし) - CDP: $PORT"
else
  if [ "$GPU_MODE" = "swiftshader" ]; then
    FLAGS+=(--disable-gpu-sandbox --enable-unsafe-swiftshader --use-gl=angle --use-angle=swiftshader)
  fi
  say "ウィンドウ表示モード (DISPLAY=$DISPLAY)"
fi

say "Chromeを起動します (gpu=$GPU_MODE, profile=$PROFILE)"
say "ログ: $LOG"
# shellcheck disable=SC2086
setsid "$CHROME" "${FLAGS[@]}" "file://$HTML" >"$LOG" 2>&1 < /dev/null &
PID=$!
disown
say "起動しました。PID=$PID"
say "ゲームが画面に表示されるまでお待ちください。"
say "終了するには: ./START_WEB_MINECRAFT.sh stop"

# 起動直後の即死を検知（Chromeが秒以内に落ちる場合）
sleep 5
if ! kill -0 "$PID" 2>/dev/null && ! pgrep -f "user-data-dir=$PROFILE" >/dev/null 2>&1; then
  say "WARNING: Chromeが起動直後に終了しました。ログの最後の30行:" >&2
  tail -n 30 "$LOG" >&2
  say "Enterキーで閉じます..." >&2
  read -r -p "" 2>/dev/null || true
  exit 1
fi

exit 0
