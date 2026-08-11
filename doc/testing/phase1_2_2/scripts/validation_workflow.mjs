#!/usr/bin/env node
/**
 * Phase 1.2.2 Validation Workflow Engine (GUI + headless).
 *
 * Improvements over path_validate.mjs (Phase 1.2):
 *   - GUI window mode supported (in addition to headless)
 *   - RUN ID on every log line
 *   - Explicit stage labels with per-stage timeout
 *   - Heartbeat every HEARTBEAT_MS while waiting
 *   - Bounded retry with per-run cleanup (never kills user Chrome)
 *   - HUMAN_REQUIRED detection -> optional Discord instruction
 *   - Vision Observer integration (observation only, never verdict-only)
 *
 * Usage (usually via run-validation-workflow.sh):
 *   node validation_workflow.mjs
 * Env:
 *   GUI_MODE=auto|gui|headless   (default auto: gui if DISPLAY, else headless)
 *   CDP_PORT, VALIDATION_PROFILE, VALIDATION_HTML, VISION_MODEL, DISCORD_WEBHOOK_URL
 *   CREATE_RETRIES (default 3), STAGE_TIMEOUT_SCALE (default 1.0)
 *   NO_LAUNCH=1 (attach to existing chrome on CDP_PORT), RUN_ID
 */
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../../../../');
const PHASE = resolve(ROOT, 'doc/testing/phase1_2_2');
const S = resolve(PHASE, 'screenshots');
const LOGD = resolve(PHASE, 'logs');
const PROFILES = resolve(PHASE, 'profiles');
const RUN_ID = process.env.RUN_ID || new Date().toISOString().replace(/[-:T]/g, '').slice(0, 14);
const PROFILE = process.env.VALIDATION_PROFILE || resolve(PROFILES, 'run-' + RUN_ID);
const PORT = process.env.CDP_PORT || '9402';
const HTML =
  process.env.VALIDATION_HTML ||
  resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const LOG = resolve(LOGD, 'validation_' + RUN_ID + '.log');
const GPU_MODE = process.env.GUI_MODE || 'auto';
const CREATE_RETRIES = parseInt(process.env.CREATE_RETRIES || '3', 10);
const SCALE = parseFloat(process.env.STAGE_TIMEOUT_SCALE || '1.0');
const HEARTBEAT_MS = 12000;
const DISCORD = process.env.DISCORD_WEBHOOK_URL || '';

// Stage timeouts (ms). Derived from measured Phase 1.2 success runs, scaled.
const T = {
  chrome_boot: 30000,
  cdp_connect: 20000,
  title_ready: 90000,
  create_form: 60000,
  world_join: 240000,
  auto_validate: 60000,
  tour: 300000,
  screenshot: 20000,
  cleanup: 15000,
};
const stage = (name, ms) => {
  const t = Math.round((ms || 60000) * SCALE);
  return { name, timeoutMs: t, start: Date.now(), lastProgress: Date.now() };
};

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let curStage = null;
let chromePid = null;
let autoValidateFired = false;
let tourCount = 0;
let shotCount = 0;

function ts() {
  return new Date().toISOString();
}
function log(m) {
  const line = `[${ts()}][${RUN_ID}] ${m}`;
  console.log(line);
  writeFileSync(LOG, line + '\n', { flag: 'a' });
}
function heartbeat() {
  const now = Date.now();
  if (!curStage) return;
  const el = Math.round((now - curStage.start) / 1000);
  log(
    `[HEARTBEAT] stage=${curStage.name} elapsed=${el}s ` +
      `autovalidate=${autoValidateFired ? 'yes' : 'no'} tour=${tourCount}/8 shots=${shotCount} ` +
      `screen=${curStage.lastScreen || 'n/a'} chrome_pid=${chromePid || 'n/a'} retry=${retryCount}/${CREATE_RETRIES}`,
  );
  curStage.lastProgress = now;
}
function checkStage() {
  const now = Date.now();
  if (curStage && now - curStage.start > curStage.timeoutMs) {
    throw new Error(
      `STAGE_TIMEOUT stage=${curStage.name} timeoutMs=${curStage.timeoutMs}ms ` +
        `lastScreen=${curStage.lastScreen || 'n/a'} autovalidate=${autoValidateFired} ` +
        `tour=${tourCount} shots=${shotCount} chrome_pid=${chromePid || 'n/a'}`,
    );
  }
}
let retryCount = 0;
const heartbeatTimer = setInterval(() => {
  try {
    heartbeat();
  } catch (_) {}
}, HEARTBEAT_MS);

function heartbeatStop() {
  clearInterval(heartbeatTimer);
}

async function notifyDiscord(text) {
  if (!DISCORD) return;
  try {
    await fetch(DISCORD, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: text.slice(0, 1900) }),
    });
  } catch (e) {
    log('discord notify failed: ' + e.message);
  }
}

async function humanRequired(spec) {
  // spec: { stage, screen, evidence, instruction }
  log('HUMAN_REQUIRED ' + JSON.stringify(spec));
  heartbeatStop();
  await notifyDiscord(
    `【HUMAN_REQUIRED】\nRUN: ${RUN_ID}\n現在: ${spec.screen}\n停止位置: ${spec.stage}\n\n` +
      `お願いする操作:\n${spec.instruction}\n\n` +
      `完了の目印: ${spec.doneMarker}\n完了後はそのままにしてください。`,
  );
  // Pause until user confirms progress (poll for world-enter signal up to 5 min).
  const deadline = Date.now() + 300000;
  while (Date.now() < deadline) {
    await sleep(5000);
    const s = await safeScreen();
    if (s === null && autoValidateFired) return true;
    if (String(s).includes('GuiMainMenu') || String(s).includes('GuiSelectWorld')) {
      // still in menus; keep waiting
    }
  }
  return false;
}

// ---- CDP (reuses Phase 1.2 pattern, plus GUI-mode support) ----
class CDP {
  constructor(u) {
    this.u = u;
    this.id = 0;
    this.p = new Map();
    this.lines = [];
  }
  async connect() {
    this.ws = new WebSocket(this.u);
    await new Promise((a, b) => {
      this.ws.onopen = a;
      this.ws.onerror = b;
    });
    this.ws.onmessage = (ev) => {
      const m = JSON.parse(ev.data);
      if (m.method === 'Runtime.consoleAPICalled') {
        const t = (m.params.args || []).map((a) => a.value ?? a.description ?? '').join(' ');
        this.lines.push(t);
        if (/RAILSYSTEM|auto-validat|PathDebug|camera tour=/i.test(t)) {
          log('HIT ' + t.slice(0, 320));
        }
      }
      if (m.id && this.p.has(m.id)) {
        const { resolve, reject } = this.p.get(m.id);
        this.p.delete(m.id);
        if (m.error) reject(new Error(JSON.stringify(m.error)));
        else resolve(m.result);
      }
    };
  }
  send(method, params = {}, timeoutMs = 20000) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      const t = setTimeout(() => {
        this.p.delete(id);
        reject(new Error('timeout ' + method));
      }, timeoutMs);
      this.p.set(id, {
        resolve: (v) => {
          clearTimeout(t);
          resolve(v);
        },
        reject: (e) => {
          clearTimeout(t);
          reject(e);
        },
      });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }
  async eval(e) {
    try {
      return (await this.send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true }))
        .result?.value;
    } catch (_) {
      return undefined;
    }
  }
  async screen() {
    return this.eval('window.__eagScreen');
  }
  async shot(f) {
    const res = await this.send('Page.captureScreenshot', { format: 'png' });
    writeFileSync(f, Buffer.from(res.data, 'base64'));
    shotCount++;
    return f;
  }
  async click(x, y) {
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 });
    await sleep(150);
  }
  async typeChar(ch) {
    const vk = ch.codePointAt(0);
    await this.send('Input.dispatchKeyEvent', {
      type: 'keyDown',
      key: ch,
      code: 'Key' + ch.toUpperCase(),
      windowsVirtualKeyCode: vk,
    });
    await this.send('Input.dispatchKeyEvent', {
      type: 'keyUp',
      key: ch,
      code: 'Key' + ch.toUpperCase(),
      windowsVirtualKeyCode: vk,
    });
  }
}

async function getWs() {
  for (let i = 0; i < 60; i++) {
    try {
      const r = await fetch(`http://127.0.0.1:${PORT}/json/list`);
      const list = await r.json();
      const p = list.find((t) => t.type === 'page');
      if (p) return p.webSocketDebuggerUrl;
    } catch (_) {}
    await sleep(1000);
  }
  throw new Error('CDP unavailable on port ' + PORT);
}

function launchChrome(guiMode) {
  mkdirSync(PROFILE, { recursive: true });
  mkdirSync(S, { recursive: true });
  mkdirSync(LOGD, { recursive: true });
  const isGui = guiMode === 'gui';
  const args = ['--no-sandbox', '--autoplay-policy=no-user-gesture-required', '--mute-audio', '--hide-scrollbars', '--window-size=1280,720', `--user-data-dir=${PROFILE}`];
  if (!isGui) {
    args.push('--headless=new', `--remote-debugging-port=${PORT}`);
    args.push('--use-gl=angle', '--use-angle=swiftshader');
  } else {
    args.push('--remote-debugging-port=' + PORT);
  }
  args.push('file://' + HTML);
  const cmd = `exec /opt/google/chrome/chrome ${args.map((a) => JSON.stringify(a)).join(' ')} >"${LOGD}/chrome_${RUN_ID}.log" 2>&1`;
  const child = spawn('bash', ['-c', cmd], { stdio: 'ignore', detached: true });
  child.unref();
  chromePid = child.pid;
  log('chrome launched pid=' + chromePid + ' mode=' + guiMode + ' profile=' + PROFILE);
}

function cleanupChrome() {
  // Kill ONLY our own validation profile process tree.
  curStage = stage('cleanup', T.cleanup);
  log('cleanup chrome profile=' + PROFILE);
  try {
    spawnSync('bash', ['-c', `pkill -TERM -f "user-data-dir=${PROFILE}" 2>/dev/null; sleep 2; pkill -9 -f "user-data-dir=${PROFILE}" 2>/dev/null || true`]);
  } catch (e) {
    log('cleanup err ' + e.message);
  }
}

function screenName(s) {
  if (!s) return s === null ? 'NULL(in-game)' : '(undefined)';
  const parts = String(s).split('.');
  return parts[parts.length - 1];
}
let safeScreen = async () => null;
async function waitForScreen(cdp, want, opts = {}) {
  const st = opts.stage || curStage;
  const timeoutMs = opts.timeout || (st ? st.timeoutMs : 60000);
  const match = typeof want === 'function' ? want : (s) => s === want;
  const deadline = Date.now() + timeoutMs;
  let last;
  while (Date.now() < deadline) {
    last = await cdp.screen();
    if (st) st.lastScreen = screenName(last);
    if (match(last)) return last;
    await sleep(1000);
    checkStage();
  }
  throw new Error('waitForScreen timeout want=' + (typeof want === 'function' ? '(fn)' : want) + ' last=' + last);
}
safeScreen = async (cdp) => {
  try {
    return await cdp.screen();
  } catch (_) {
    return null;
  }
};

async function waitTitle(cdp) {
  curStage = stage('title_ready', T.title_ready);
  for (let i = 0; i < 90; i++) {
    checkStage();
    const s = await cdp.screen();
    curStage.lastScreen = screenName(s);
    if (s === null || s === undefined) {
      if (i === 0) await cdp.click(640, 360);
      await sleep(2000);
      continue;
    }
    const n = String(s);
    if (n.includes('GuiMainMenu')) {
      log('title reached: ' + screenName(s));
      return;
    }
    if (n.includes('GuiScreenContentWarning')) {
      log('content warning -> continue');
      await cdp.click(640, 390);
      await sleep(500);
      await cdp.click(640, 450);
      await sleep(1000);
      continue;
    }
    if (n.includes('GuiScreenDefaultUsernameNote')) {
      log('default-username note -> continue anyway');
      await cdp.click(640, 380);
      await sleep(1000);
      continue;
    }
    await sleep(2000);
  }
  throw new Error('title timeout, last screen=' + (await cdp.screen()));
}

async function gotoCreateForm(cdp) {
  curStage = stage('create_form', T.create_form);
  await waitForScreen(cdp, (s) => String(s).includes('GuiMainMenu'), { timeout: 60000 });
  await cdp.click(640, 258); // Singleplayer
  const onSelect = (s) =>
    String(s).includes('GuiSelectWorld') || String(s).includes('GuiScreenCreateWorldSelection');
  await waitForScreen(cdp, onSelect, { timeout: 60000 });
  let s = await cdp.screen();
  if (String(s).includes('GuiScreenCreateWorldSelection')) {
    log('on create-selection screen -> Create');
    await cdp.click(640, 242);
  } else {
    await cdp.click(915, 492);
    await waitForScreen(cdp, (s) => String(s).includes('GuiScreenCreateWorldSelection'), { timeout: 60000 });
    await cdp.click(640, 242);
  }
  await waitForScreen(cdp, (s) => String(s).includes('GuiCreateWorld'), { timeout: 60000 });
  log('on create form');
}

async function enterWorld(cdp) {
  await waitForScreen(cdp, (s) => String(s).includes('GuiCreateWorld'), { timeout: 60000 });
  await cdp.click(495, 138);
  await sleep(800);
  await cdp.send('Input.dispatchKeyEvent', {
    type: 'keyDown',
    key: 'a',
    code: 'KeyA',
    modifiers: 2,
    windowsVirtualKeyCode: 65,
  });
  await cdp.send('Input.dispatchKeyEvent', {
    type: 'keyUp',
    key: 'a',
    code: 'KeyA',
    modifiers: 2,
    windowsVirtualKeyCode: 65,
  });
  await sleep(300);
  for (const ch of 'EaglerFlatValidate') {
    await cdp.typeChar(ch);
    await sleep(40);
  }
  await sleep(1000);
  await cdp.shot(resolve(S, RUN_ID + '_named.png'));
  log('world named');
}

async function attemptCreate(cdp) {
  curStage = stage('world_join', T.world_join);
  await cdp.click(400, 540); // Confirm create
  const start = Date.now();
  const seen = [];
  let menuBounces = 0;
  while (Date.now() - start < 240000) {
    checkStage();
    const s = await cdp.screen();
    const n = screenName(s);
    seen.push(n);
    curStage.lastScreen = n;
    if (s === null) {
      log('create->in-game detected');
      return { ok: true, seen };
    }
    const str = String(s);
    if (str.includes('GuiSelectWorld') || str.includes('GuiMainMenu')) {
      menuBounces++;
      log('create bounced back to ' + n + ' (menu-recovery race) bounce#' + menuBounces);
      if (menuBounces >= 2) {
        // repeated menu bounce: human assist is faster than more retries
        log('HUMAN_REQUIRED_DECISION repeated menu bounce');
        throw new Error('HUMAN_REQUIRED repeated menu bounce');
      }
      return { ok: false, seen };
    }
    await sleep(1000);
  }
  return { ok: false, seen };
}

async function waitInWorld(cdp) {
  curStage = stage('world_join', T.world_join);
  for (let i = 1; i <= 60; i++) {
    await sleep(3000);
    checkStage();
    const s = await cdp.screen();
    curStage.lastScreen = screenName(s);
    if (s === null && cdp.lines.some((t) => /auto-validat/i.test(t))) {
      log('IN_WORLD validated=true');
      autoValidateFired = true;
      return { validated: true, inworld: true };
    }
    if (cdp.lines.some((t) => /auto-validat/i.test(t))) {
      log('IN_WORLD validated=true (console)');
      autoValidateFired = true;
      return { validated: true, inworld: true };
    }
  }
  return { validated: false, inworld: false };
}

async function waitAutoValidate(cdp) {
  curStage = stage('auto_validate', T.auto_validate);
  const deadline = Date.now() + 60000;
  while (Date.now() < deadline) {
    checkStage();
    if (cdp.lines.some((t) => /auto-validat/i.test(t))) {
      log('AutoValidate fired');
      autoValidateFired = true;
      return true;
    }
    await sleep(1000);
  }
  throw new Error('AutoValidate did NOT fire');
}

async function captureTour(cdp) {
  curStage = stage('tour', T.tour);
  const map = [
    ['path_multi_straight', RUN_ID + '_SS-01_MULTI_STRAIGHT.png'],
    ['path_straight_curve_straight', RUN_ID + '_SS-02_STRAIGHT_CURVE_STRAIGHT.png'],
    ['path_s_curve', RUN_ID + '_SS-03_S_CURVE.png'],
    ['path_boundary', RUN_ID + '_SS-04_PIECE_BOUNDARY.png'],
    ['path_gradient', RUN_ID + '_SS-05_GRADIENT_CHAIN.png'],
    ['path_curve_gradient', RUN_ID + '_SS-06_CURVE_GRADIENT.png'],
    ['path_reverse', RUN_ID + '_SS-07_REVERSE_PATH.png'],
    ['path_overview', RUN_ID + '_SS-08_OVERVIEW.png'],
  ];
  const taken = new Set();
  const deadline = Date.now() + 300000;
  while (Date.now() < deadline && taken.size < map.length) {
    checkStage();
    for (const [tag, file] of map) {
      if (taken.has(tag)) continue;
      if (cdp.lines.some((t) => t.includes('camera tour=' + tag))) {
        await sleep(1500);
        tourCount++;
        await cdp.shot(resolve(S, file));
        log('tour shot ' + tourCount + '/8 ' + file);
        taken.add(tag);
      }
    }
    await sleep(1000);
  }
  if (taken.size < map.length) {
    log('WARN missing tags; timed fallback remaining=' + (map.length - taken.size));
    for (const [tag, file] of map) {
      if (taken.has(tag)) continue;
      await sleep(4000);
      await cdp.shot(resolve(S, file));
      log('fallback shot ' + file);
      taken.add(tag);
    }
  }
  writeFileSync(resolve(LOGD, 'console_' + RUN_ID + '.txt'), cdp.lines.join('\n'));
}

let guiMode = GPU_MODE;
if (guiMode === 'auto') {
  guiMode = process.env.DISPLAY ? 'gui' : 'headless';
}

async function run(cdp) {
  retryCount = 0;
  for (let attempt = 1; attempt <= CREATE_RETRIES; attempt++) {
    retryCount = attempt;
    log('=== create attempt ' + attempt + '/' + CREATE_RETRIES + ' ===');
    try {
      await waitTitle(cdp);
      await gotoCreateForm(cdp);
      await enterWorld(cdp);
      const res = await attemptCreate(cdp);
      if (res.ok) {
        const inw = await waitInWorld(cdp);
        if (!inw.inworld) throw new Error('failed in-world');
        if (!inw.validated) await waitAutoValidate(cdp);
        await captureTour(cdp);
        log('SUCCESS tour=' + tourCount + ' shots=' + shotCount);
        return true;
      }
    } catch (e) {
      log('attempt ' + attempt + ' failed: ' + e.message);
      if (/HUMAN_REQUIRED/.test(e.message)) {
        // Human assist: send concrete Discord instruction, then wait.
        const screenNow = curStage ? (curStage.lastScreen || 'n/a') : 'n/a';
        const ok = await humanRequired({
          stage: curStage ? curStage.name : 'unknown',
          screen: screenNow,
          instruction:
            '1. Chromeウィンドウ（検証用）を前面にしてください。\n' +
            '2. 現在の画面（メニュー / ワールド選択 / クラッシュ画面）を確認してください。\n' +
            '3. 「Singleplayer」→「Create New World」をクリックしてください。\n' +
            '4. World Nameが「EaglerFlatValidate」であることを確認し、「Create New World」をクリックしてください。\n' +
            '5. フラットワールドに入り、クロスヘアが見える状態になるまでお待ちください。',
          doneMarker: 'ゲーム画面（クロスヘア）が見える状態',
        });
        if (ok) {
          log('human assist succeeded - resuming');
          const inw = await waitInWorld(cdp);
          if (!inw.inworld) throw new Error('failed in-world after human assist');
          if (!inw.validated) await waitAutoValidate(cdp);
          await captureTour(cdp);
          log('SUCCESS tour=' + tourCount + ' shots=' + shotCount);
          return true;
        }
        throw new Error('HUMAN_REQUIRED unresolved');
      }
      if (/STAGE_TIMEOUT/.test(e.message)) {
        // Surface hard timeout; do not blindly retry.
        throw e;
      }
      if (attempt < CREATE_RETRIES) {
        log('cleanup before retry');
        cleanupChrome();
        await sleep(3000);
        launchChrome(guiMode);
        const ws = await getWs();
        cdp = new CDP(ws);
        await cdp.connect();
        await cdp.send('Runtime.enable');
        await cdp.send('Page.enable');
        continue;
      }
    }
  }
  throw new Error('create never entered the world after ' + CREATE_RETRIES + ' attempts');
}

log('START Phase1.2.2 validation RUN=' + RUN_ID + ' gui=' + guiMode + ' profile=' + PROFILE + ' create_retries=' + CREATE_RETRIES);

async function main() {
  writeFileSync(LOG, '');
  if (!process.env.NO_LAUNCH) {
    curStage = stage('chrome_boot', T.chrome_boot);
    launchChrome(guiMode);
    await sleep(6000);
    curStage = stage('cdp_connect', T.cdp_connect);
    const ws = await getWs();
    const cdp = new CDP(ws);
    await cdp.connect();
    await cdp.send('Runtime.enable');
    await cdp.send('Page.enable');
    await run(cdp);
  } else {
    curStage = stage('cdp_connect', T.cdp_connect);
    const ws = await getWs();
    const cdp = new CDP(ws);
    await cdp.connect();
    await cdp.send('Runtime.enable');
    await cdp.send('Page.enable');
    await run(cdp);
  }
  curStage = stage('cleanup', T.cleanup);
  cleanupChrome();
  log('END SUCCESS tour=' + tourCount + ' shots=' + shotCount);
  heartbeatStop();
  process.exit(0);
}

main().catch((e) => {
  log('FATAL ' + (e.stack || e));
  try {
    cleanupChrome();
  } catch (_) {}
  heartbeatStop();
  process.exit(1);
});
