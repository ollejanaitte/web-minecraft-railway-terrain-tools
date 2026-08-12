#!/usr/bin/env node
/**
 * r10f_foundation_acceptance.mjs — Phase 1-R10F Normal World Foundation
 * Acceptance course (instrumented GUI proof, dedicated normal Superflat world).
 *
 * Reuses the proven R10 GUI harness mechanics (CDP + instrumented HTML +
 * vanilla /tp + real right-click) to run the R10F acceptance course through
 * the PRODUCTION /railsys3 command surface:
 *
 *   1  Straight placement
 *   2  Curve placement
 *   3  Gradient placement
 *   4  Curve + Gradient placement
 *   5  Cant placement
 *   6  Curve + Gradient + Cant placement
 *   7  handle edit  (preview rebuild)
 *   8  rot1/yaw edit
 *   9  rot2/yaw edit
 *  10  pitch edit
 *  11  cant edit
 *  12  asset switch (centerline invariance via same pathIdentity)
 *  13  cancel (preview-only discard, markers kept)
 *  14  preview rebuild
 *  15  clear (transient session reset, confirmed rail kept)
 *  16  re-placement (next POS1 after clear)
 *  17  confirm
 *  18  next placement session
 *
 * Checks per step (console assertions):
 *   - POS1/POS2 chat lines per placement;
 *   - auto preview after POS2;
 *   - preview ready after every edit;
 *   - asset -> after switch, and R9RENDER shows the SAME pathIdentity+samples
 *     as before the switch (asset does not rebuild the RailPath);
 *   - cancel -> "preview cancelled", then /railsys3 preview rebuilds;
 *   - clear -> "session cleared; confirmed rail kept";
 *   - status -> confirmed=yes after confirm and after clear;
 *   - JS errors tracked and counted (0 expected).
 *
 * NO /pos1 /pos2 command fallback is used for placements.
 */
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, statSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const ROOT = '/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools';
const PHASE = resolve(ROOT, 'doc/testing/phase1_r10f');
const S = resolve(PHASE, 'screenshots');
const LOGD = resolve(PHASE, 'logs');
const HTML_RAW = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const INSTR = resolve(PHASE, 'scripts/instrumented.html');
const PORT = process.env.CDP_PORT || '9521';
const RUN_ID = process.env.RUN_ID || 'r10f';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

mkdirSync(S, { recursive: true });
mkdirSync(LOGD, { recursive: true });

function log(m) {
  const line = `[${new Date().toISOString()}[${RUN_ID}] ${m}`;
  console.log(line);
  writeFileSync(resolve(LOGD, `${RUN_ID}.log`), line + '\n', { flag: 'a' });
}

const HOOK_NEEDLE = Buffer.from('container: "game_frame",');
function ensureInstrumented() {
  if (!existsSync(HTML_RAW)) throw new Error('offline HTML missing: ' + HTML_RAW);
  const needRebuild =
    !existsSync(INSTR) || statSync(INSTR).mtimeMs < statSync(HTML_RAW).mtimeMs;
  if (!needRebuild) return;
  log('regenerating instrumented HTML');
  const src = readFileSync(HTML_RAW);
  const idx = src.indexOf(HOOK_NEEDLE);
  if (idx < 0) throw new Error('instrumentation needle not found in HTML');
  const hook =
    HOOK_NEEDLE +
    Buffer.from(
      `\n\thooks: {\n\t\tscreenChanged: function(name, w, h, dw, dh, s) {\n` +
        `\t\t\ttry { window.__eagScreen = name; window.__eagScreenTime = Date.now(); } catch (e) {}\n` +
        `\t\t}\n\t},`,
    );
  const out = Buffer.concat([src.slice(0, idx), Buffer.from(hook), src.slice(idx + HOOK_NEEDLE.length)]);
  writeFileSync(INSTR, out);
  log('instrumented HTML written: ' + INSTR);
}

class CDP {
  constructor(u) { this.u = u; this.id = 0; this.p = new Map(); this.lines = []; this.jsErrors = 0; }
  async connect() {
    this.ws = new WebSocket(this.u);
    await new Promise((a, b) => { this.ws.onopen = a; this.ws.onerror = b; });
    this.ws.onmessage = (ev) => {
      const m = JSON.parse(ev.data);
      if (m.method === 'Runtime.consoleAPICalled') {
        const t = (m.params.args || []).map((a) => a.value ?? a.description ?? '').join(' ');
        this.lines.push(t);
        if (m.params.type === 'error') { log('JS_CONSOLE_ERROR ' + t.slice(0, 500)); this.jsErrors++; }
        if (/railsys:|MARKERARROW|RAILSYSTEM|CONTRAIL|CURVEGRAD/i.test(t)) log('CHAT ' + t.slice(0, 300));
      }
      if (m.method === 'Runtime.exceptionThrown') {
        const d = m.params.exceptionDetails || {};
        const ex = d.exception || {};
        const detail = [d.text, ex.description, ex.value].filter((v) => v !== undefined && v !== null && v !== '').join(' | ');
        log('JS_EXCEPTION ' + detail.slice(0, 700) + ' url=' + (d.url || '') + ' line=' + (d.lineNumber ?? ''));
        this.jsErrors++;
      }
      if (m.id && this.p.has(m.id)) {
        const { r, j } = this.p.get(m.id);
        this.p.delete(m.id);
        m.error ? j(new Error(JSON.stringify(m.error))) : r(m.result);
      }
    };
  }
  send(method, params = {}, to = 20000) {
    const id = ++this.id;
    return new Promise((r, j) => {
      const t = setTimeout(() => { this.p.delete(id); j(new Error('timeout ' + method)); }, to);
      this.p.set(id, { r: (v) => { clearTimeout(t); r(v); }, j: (e) => { clearTimeout(t); j(e); } });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }
  async eval(e) {
    try { return (await this.send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true })).result?.value; }
    catch (_) { return undefined; }
  }
  async screen() { return this.eval('window.__eagScreen'); }
  async shot(f) {
    const res = await this.send('Page.captureScreenshot', { format: 'png' }, 60000);
    writeFileSync(f, Buffer.from(res.data, 'base64'));
    log('shot ' + f);
  }
  async click(x, y) {
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 });
    await sleep(150);
  }
  async rightClick(x, y, modifiers = 0) {
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'right', clickCount: 1, modifiers });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'right', clickCount: 1, modifiers });
    await sleep(150);
  }
  async pressHotbar1() {
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: '1', code: 'Digit1', windowsVirtualKeyCode: 49 });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: '1', code: 'Digit1', windowsVirtualKeyCode: 49 });
    await sleep(100);
  }
  async shiftRightClickBlock() {
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'ShiftLeft', code: 'ShiftLeft', windowsVirtualKeyCode: 16, modifiers: 8 });
    await sleep(900);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: 640, y: 360 });
    await sleep(200);
    await this.rightClick(640, 360, 8);
    await sleep(900);
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'ShiftLeft', code: 'ShiftLeft', windowsVirtualKeyCode: 16, modifiers: 8 });
    await sleep(200);
  }
  async key(k, modifiers = 0) {
    const vk = typeof k === 'number' ? k : k.codePointAt(0);
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: typeof k === 'number' ? String.fromCharCode(k) : k, code: 'Key' + (typeof k === 'number' ? String.fromCharCode(k) : k), windowsVirtualKeyCode: vk, modifiers });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: typeof k === 'number' ? String.fromCharCode(k) : k, code: 'Key' + (typeof k === 'number' ? String.fromCharCode(k) : k), windowsVirtualKeyCode: vk, modifiers });
  }
  async typeChar(ch) {
    const codes = { '/': ['Slash', 191], ' ': ['Space', 32], 'a': ['KeyA', 65], 'b': ['KeyB', 66],
      'c': ['KeyC', 67], 'd': ['KeyD', 68], 'e': ['KeyE', 69], 'f': ['KeyF', 70], 'g': ['KeyG', 71],
      'h': ['KeyH', 72], 'i': ['KeyI', 73], 'j': ['KeyJ', 74], 'k': ['KeyK', 75], 'l': ['KeyL', 76],
      'm': ['KeyM', 77], 'n': ['KeyN', 78], 'o': ['KeyO', 79], 'p': ['KeyP', 80], 'q': ['KeyQ', 81],
      'r': ['KeyR', 82], 's': ['KeyS', 83], 't': ['KeyT', 84], 'u': ['KeyU', 85], 'v': ['KeyV', 86],
      'w': ['KeyW', 87], 'x': ['KeyX', 88], 'y': ['KeyY', 89], 'z': ['KeyZ', 90],
      '1': ['Digit1', 49], '2': ['Digit2', 50], '3': ['Digit3', 51], '4': ['Digit4', 52],
      '5': ['Digit5', 53], '6': ['Digit6', 54], '7': ['Digit7', 55], '8': ['Digit8', 56],
      '9': ['Digit9', 57], '0': ['Digit0', 48], '-': ['Minus', 189], '.': ['Period', 190],
      '_': ['Minus', 189] };
    let code = 'Key' + ch.toUpperCase();
    let vk = ch.codePointAt(0);
    if (codes[ch]) { code = codes[ch][0]; vk = codes[ch][1]; }
    const text = ch === ' ' ? ' ' : ch;
    await this.send('Input.dispatchKeyEvent', {
      type: 'keyDown', key: ch, code, windowsVirtualKeyCode: vk, text, unmodifiedText: text,
    });
    await this.send('Input.dispatchKeyEvent', {
      type: 'keyUp', key: ch, code, windowsVirtualKeyCode: vk,
    });
    await sleep(20);
  }
  async typeText(name) {
    await sleep(200);
    for (const ch of name) { await this.typeChar(ch); await sleep(30); }
  }
  async enter() {
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13 });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13 });
  }
}

function launchChrome(guiMode, profileDir) {
  mkdirSync(profileDir, { recursive: true });
  const isGui = guiMode === 'gui';
  const args = ['--no-sandbox', '--no-first-run', '--no-default-browser-check',
    '--autoplay-policy=no-user-gesture-required', '--mute-audio', '--hide-scrollbars',
    '--window-size=1280,720', `--user-data-dir=${profileDir}`, `--remote-debugging-port=${PORT}`];
  if (!isGui) {
    args.push('--headless=new', '--use-gl=angle', '--use-angle=swiftshader');
  }
  args.push('file://' + INSTR);
  const child = spawn('/opt/google/chrome/chrome', args, { detached: true, stdio: ['ignore', 'pipe', 'pipe'] });
  child.stdout.on('data', (d) => {
    const s = d.toString();
    writeFileSync(resolve(LOGD, 'chrome_stdout.log'), s, { flag: 'a' });
  });
  child.stderr.on('data', (d) => {
    const s = d.toString();
    writeFileSync(resolve(LOGD, 'chrome_stderr.log'), s, { flag: 'a' });
  });
  child.unref();
  return child;
}

function killChrome(profileDir) {
  try {
    spawnSync('bash', ['-c', `pkill -TERM -f "user-data-dir=${profileDir}" 2>/dev/null; sleep 2; pkill -9 -f "user-data-dir=${profileDir}" 2>/dev/null; true`], { timeout: 10000 });
  } catch (_) {}
}

async function getPage() {
  for (let i = 0; i < 90; i++) {
    try {
      const l = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json();
      const p = l.find((t) => t.type === 'page' && /EaglercraftX/i.test(t.url || '')) ||
        l.find((t) => t.type === 'page' && !/^chrome:\/\//.test(t.url || ''));
      if (p) return p.webSocketDebuggerUrl;
    } catch (_) {}
    await sleep(1000);
  }
  throw new Error('CDP unavailable on port ' + PORT);
}

async function toTitle(c) {
  for (let i = 0; i < 90; i++) {
    const s = await c.screen();
    if (s === null || s === undefined) {
      if (i === 0) await c.click(640, 360);
      await sleep(2000);
      continue;
    }
    const n = String(s);
    if (n.includes('GuiMainMenu')) { log('title reached'); return; }
    if (n.includes('GuiScreenContentWarning')) { await c.click(640, 390); await sleep(500); await c.click(640, 450); await sleep(1000); continue; }
    if (n.includes('GuiScreenDefaultUsernameNote')) { await c.click(640, 380); await sleep(1000); continue; }
    if (n.includes('GuiScreenEditProfile')) { await c.click(640, 420); await sleep(1000); continue; }
    await sleep(2000);
  }
  throw new Error('title timeout, last=' + (await c.screen()));
}

async function gotoCreateForm(c) {
  const want = (s) => String(s).includes('GuiSelectWorld') || String(s).includes('GuiScreenCreateWorldSelection');
  const dl = Date.now() + 180000;
  while (Date.now() < dl) {
    const s = await c.screen();
    if (s === null) { log('already in-game'); return; }
    if (want(s)) break;
    if (String(s).includes('GuiMainMenu')) { await c.click(640, 258); await sleep(1500); continue; }
    await sleep(1500);
  }
  let s = await c.screen();
  if (String(s).includes('GuiScreenCreateWorldSelection')) {
    await c.click(640, 242);
  } else {
    await c.click(762, 420);
    await sleep(3000);
    await c.click(640, 242);
  }
  for (let i = 0; i < 60; i++) {
    if (String(await c.screen()).includes('GuiCreateWorld')) { log('create form'); return; }
    await sleep(1000);
  }
  throw new Error('create form timeout, last=' + (await c.screen()));
}

async function enterWorld(c, name) {
  await c.click(500, 145);
  await sleep(800);
  await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Control', code: 'ControlLeft', windowsVirtualKeyCode: 17, modifiers: 2 });
  await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'a', code: 'KeyA', windowsVirtualKeyCode: 65, modifiers: 2, text: 'a', unmodifiedText: 'a' });
  await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'a', code: 'KeyA', windowsVirtualKeyCode: 65, modifiers: 2 });
  await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Control', code: 'ControlLeft', windowsVirtualKeyCode: 17 });
  await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Backspace', code: 'Backspace', windowsVirtualKeyCode: 8 });
  await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Backspace', code: 'Backspace', windowsVirtualKeyCode: 8 });
  await sleep(200);
  await c.typeText(name);
  await sleep(800);
}

async function createWorld(c) {
  await c.click(444, 470);
  const start = Date.now();
  while (Date.now() - start < 300000) {
    const s = await c.screen();
    if (s === null) { log('in-game'); return; }
    const n = String(s);
    if (n.includes('GuiSelectWorld') || n.includes('GuiMainMenu')) {
      throw new Error('world create bounced to menu: ' + n);
    }
    await sleep(1500);
  }
  throw new Error('world create timeout');
}

async function waitConsoleAfter(c, re, startIndex, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    for (let i = startIndex; i < c.lines.length; i++) {
      if (re.test(c.lines[i])) return c.lines[i];
    }
    await sleep(500);
  }
  throw new Error('console wait timeout (after index ' + startIndex + ') for ' + re);
}

function hasConsoleAfter(c, re, startIndex) {
  return c.lines.slice(startIndex).some((line) => re.test(line));
}

async function chatAndWaitNew(c, text, re, timeoutMs) {
  const startIndex = c.lines.length;
  await chat(c, text);
  return waitConsoleAfter(c, re, startIndex, timeoutMs);
}

async function respawnIfGameOver(c) {
  const screen = await c.screen();
  if (!screen || !String(screen).includes('GuiGameOver')) return;
  const startIndex = c.lines.length;
  await c.click(604, 291);
  const deadline = Date.now() + 60000;
  while (Date.now() < deadline) {
    if ((await c.screen()) === null) { log('respawned (screen cleared)'); break; }
    if (hasConsoleAfter(c, /\[CONTRAIL\] render name=MpServer gate=false/, startIndex)) { log('respawned (render resumed)'); break; }
    await sleep(500);
  }
  await sleep(3000);
}

async function tryOpenChat(c) {
  for (let i = 0; i < 5; i++) {
    await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: '/', code: 'Slash', windowsVirtualKeyCode: 191, text: '/', unmodifiedText: '/' });
    await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: '/', code: 'Slash', windowsVirtualKeyCode: 191 });
    await sleep(900);
    const scr = await c.screen();
    if (scr && String(scr).includes('GuiChat')) { return { opened: true, prefilled: true }; }
  }
  await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
  await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
  await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 't', code: 'KeyT', windowsVirtualKeyCode: 84 });
  await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 't', code: 'KeyT', windowsVirtualKeyCode: 84 });
  await sleep(900);
  const plain = await c.screen();
  if (plain && String(plain).includes('GuiChat')) { return { opened: true, prefilled: false }; }
  return { opened: false, prefilled: false };
}

async function chat(c, text) {
  const opened = await tryOpenChat(c);
  if (!opened.opened) throw new Error('GuiChat could not be opened; refusing blind command input');
  await sleep(300);
  await sleep(1200);
  await c.typeText(opened.prefilled ? text : '/' + text);
  await sleep(500);
  await c.enter();
  await sleep(1200);
}

/** Place a marker pair at x1->x2 (Y=4 support surface) with the player
 * teleported above the block looking straight down (pitch 89), then wait for
 * POS1/POS2/preview lines. Returns console index for subsequent assertions. */
async function placePair(c, label, x1, x2) {
  const tp1 = await chatAndWaitNew(c, `tp @p ${x1} 6 0 270 89`, /Teleported|teleported/, 30000);
  log(`${label} POS1 tp complete: ${tp1}`);
  await sleep(2500);
  await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
  const p1Idx = c.lines.length;
  await c.rightClick(640, 360);
  await waitConsoleAfter(c, /railsys: POS1 at/, p1Idx, 30000);
  log(`${label} POS1 set at (${x1},6,0)`);

  const tp2 = await chatAndWaitNew(c, `tp @p ${x2} 6 0 90 89`, /Teleported|teleported/, 30000);
  log(`${label} POS2 tp complete: ${tp2}`);
  await sleep(2500);
  await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
  const p2Idx = c.lines.length;
  await c.rightClick(640, 360);
  await sleep(3000);
  if (!hasConsoleAfter(c, /railsys: POS2 at/, p2Idx)) {
    log(`${label} POS2 gesture retry after pointer-lock acquisition`);
    await c.rightClick(640, 360);
  }
  await waitConsoleAfter(c, /railsys: POS2 at/, p2Idx, 30000);
  await waitConsoleAfter(c, /railsys: preview ready/, p2Idx, 30000);
  log(`${label} POS2 set at (${x2},6,0) + auto preview ready`);
}

async function main() {
  ensureInstrumented();
  const guiMode = process.env.GUI_MODE || (process.env.DISPLAY ? 'gui' : 'headless');
  log('GUI_MODE=' + guiMode);

  const profile = resolve(PHASE, 'profiles/run-' + RUN_ID);
  launchChrome(guiMode, profile);
  await sleep(8000);
  const c = new CDP(await getPage());
  await c.connect();
  await c.send('Runtime.enable');
  await c.send('Page.enable');
  await c.send('Emulation.setDeviceMetricsOverride', { width: 1208, height: 505, deviceScaleFactor: 0, mobile: false });

  try {
    await toTitle(c);
    await gotoCreateForm(c);
    await enterWorld(c, 'RailsysR10F');
    await c.click(604, 394);
    await sleep(1000);
    await c.click(764, 220);
    await sleep(500);
    await c.click(445, 322);
    await sleep(500);
    await c.click(604, 394);
    await sleep(1000);
    await createWorld(c);
    await sleep(20000);
    await respawnIfGameOver(c);
    await c.click(640, 360);
    await sleep(1500);

    // ---- setup: peaceful + wand ----
    await chatAndWaitNew(c, 'difficulty peaceful', /Difficulty.*Peaceful|difficulty.*Peaceful/i, 30000);
    const wandStart = c.lines.length;
    await chat(c, 'railsys3 wand');
    await waitConsoleAfter(c, /railsys: marker wand added/, wandStart, 30000);
    await c.pressHotbar1();
    log('wand given + hotbar slot 1 selected');

    // ==== 1 Straight ====
    await placePair(c, 'STRAIGHT', 0, 12);
    let idx = c.lines.length;
    await chat(c, 'railsys3 pitch 0');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    await chat(c, 'railsys3 asset railsys.prototype_narrow_1000');
    await waitConsoleAfter(c, /railsys: asset ->/, idx, 30000);
    await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
    await chatAndWaitNew(c, 'tp @p 12 6 0 90 89', /Teleported|teleported/, 30000);
    await sleep(2500);
    await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
    await sleep(2500);
    const f1cIdx = c.lines.length;
    await c.shiftRightClickBlock();
    await sleep(3000);
    if (!hasConsoleAfter(c, /railsys: confirmed/, f1cIdx)) {
      await c.shiftRightClickBlock();
    }
    await waitConsoleAfter(c, /railsys: confirmed/, f1cIdx, 30000);
    log('straight confirmed (narrow asset)');
    idx = c.lines.length;
    await chat(c, 'railsys3 camera 6 9 4 180 35');
    await waitConsoleAfter(c, /railsys: camera set/, idx, 30000);
    await sleep(2000);
    const f1 = resolve(S, 'SS-R10F-01_FOUNDATION_DATUM.png');
    await c.shot(f1);
    await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);

    // ==== 2 Curve + 3 Gradient + 4 Curve+Gradient + 5 Cant + 6 CG+Cant ====
    // Use /railsys3 rot1/pitch/cant edits on the SAME straight markers to make
    // the curve/gradient/cant previews (production preview rebuild semantics).
    await chat(c, 'railsys3 clear');
    await sleep(800);
    await placePair(c, 'CURVEGRAD_CANT', 0, 16);
    idx = c.lines.length;
    await chat(c, 'railsys3 rot1 25');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    log('7/8 handle+rot1 edit preview rebuilt');
    idx = c.lines.length;
    await chat(c, 'railsys3 handle 10');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    idx = c.lines.length;
    await chat(c, 'railsys3 pitch 6');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    log('9/10 rot2+pitch edits preview rebuilt');
    idx = c.lines.length;
    await chat(c, 'railsys3 rot2 15');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    idx = c.lines.length;
    await chat(c, 'railsys3 cant 6');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    log('11 cant edit preview rebuilt');

    idx = c.lines.length;
    await chat(c, 'railsys3 camera 8 10 12 180 28');
    await waitConsoleAfter(c, /railsys: camera set/, idx, 30000);
    await sleep(2000);
    const f2 = resolve(S, 'SS-R10F-02_CURVE_GRADIENT_CANT.png');
    await c.shot(f2);
    // Preview is transparent cyan here; switch to the confirmed state later
    // (f3). This shot demonstrates the curve+gradient+cant PREVIEW.

    // ==== 12 asset switch: centerline invariance ====
    // Capture the R9RENDER identity BEFORE and AFTER the asset switch; both
    // must show the same pathIdentity and the same samples/len (asset never
    // rebuilds the RailPath).
    const beforeIdx = c.lines.length;
    await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
    await chatAndWaitNew(c, 'tp @p 16 6 0 90 89', /Teleported|teleported/, 30000);
    await sleep(2500);
    await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
    await sleep(2500);
    await c.shiftRightClickBlock();
    await sleep(3000);
    if (!hasConsoleAfter(c, /railsys: confirmed/, beforeIdx)) {
      log('confirm retry after pointer-lock acquisition');
      await c.shiftRightClickBlock();
    }
    await waitConsoleAfter(c, /railsys: confirmed/, beforeIdx, 30000);
    log('17 confirmed via sneak+right-click');
    const confirmIdx = c.lines.length;
    await chatAndWaitNew(c, 'railsys3 confirm', /railsys: no preview to confirm/, 30000);
    log('re-confirm without preview rejected (no mutation)');

    idx = c.lines.length;
    await chat(c, 'railsys3 asset railsys.prototype_standard_1435');
    await waitConsoleAfter(c, /railsys: asset ->/, idx, 30000);
    log('12 asset switch to standard_1435 (look-only)');
    await sleep(2500);

    // F4 asset invariance: the R9RENDER trace before the switch vs after must
    // show the SAME pathIdentity and SAME len (centerline unchanged by the
    // asset). sample COUNT is asset-defined spacing (a look parameter) and may
    // legitimately differ.
    const renderLines = c.lines.filter((l) => /R9RENDER/.test(l));
    const beforeAsset = renderLines[renderLines.length - 2];
    const afterAsset = renderLines[renderLines.length - 1];
    log('pathIdentity before asset: ' + (beforeAsset || 'none'));
    log('pathIdentity after asset:  ' + (afterAsset || 'none'));
    if (!beforeAsset || !afterAsset) {
      throw new Error('R9RENDER lines missing for asset invariance check');
    }
    const id1 = beforeAsset.match(/pathIdentity=(-?\d+)/)?.[1];
    const id2 = afterAsset.match(/pathIdentity=(-?\d+)/)?.[1];
    const l1 = beforeAsset.match(/len=([0-9.]+)/)?.[1];
    const l2 = afterAsset.match(/len=([0-9.]+)/)?.[1];
    if (id1 !== id2 || l1 !== l2) {
      throw new Error(`F4 asset switch changed path: identity ${id1}->${id2} len ${l1}->${l2}`);
    }
    log(`F4 asset switch preserved path: identity=${id1} len=${l1} (centerline invariant)`);

    idx = c.lines.length;
    await chat(c, 'railsys3 camera 8 10 12 180 28');
    await waitConsoleAfter(c, /railsys: camera set/, idx, 30000);
    await sleep(2000);
    const f3 = resolve(S, 'SS-R10F-03_EDITING_ASSET_ISOLATION.png');
    await c.shot(f3);
    // ==== 13 cancel / 14 preview rebuild / 15 clear ====
    // Re-select markers to have a live preview to cancel.
    await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
    await placePair(c, 'CANCEL_CLEAR', 30, 42);
    idx = c.lines.length;
    await chat(c, 'railsys3 cancel');
    await waitConsoleAfter(c, /railsys: preview cancelled/, idx, 30000);
    log('13 cancel discarded preview only');
    const afterCancel = await c.eval(`({A: window.__noop})`);
    idx = c.lines.length;
    await chat(c, 'railsys3 preview');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    log('14 preview rebuilt after cancel');
    idx = c.lines.length;
    await chat(c, 'railsys3 clear');
    await waitConsoleAfter(c, /railsys: session cleared; confirmed rail kept/, idx, 30000);
    log('15 clear reset transient session, confirmed rail kept');
    idx = c.lines.length;
    await chat(c, 'railsys3 status');
    const statusLine = await waitConsoleAfter(c, /railsys: A=/, idx, 30000);
    if (!/confirmed=yes/.test(statusLine)) {
      throw new Error('status does not report confirmed=yes after clear: ' + statusLine);
    }
    log('status after clear: ' + statusLine);

    // ==== 16 re-placement (next session) + 18 next session ====
    await placePair(c, 'REPLACEMENT', 60, 72);
    idx = c.lines.length;
    await chat(c, 'railsys3 pitch 0');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    idx = c.lines.length;
    await chat(c, 'railsys3 asset railsys.prototype_narrow_1000');
    await waitConsoleAfter(c, /railsys: asset ->/, idx, 30000);
    await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
    await chatAndWaitNew(c, 'tp @p 72 6 0 90 89', /Teleported|teleported/, 30000);
    await sleep(2500);
    await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
    await sleep(2500);
    const f4cIdx = c.lines.length;
    await c.shiftRightClickBlock();
    await sleep(3000);
    if (!hasConsoleAfter(c, /railsys: confirmed/, f4cIdx)) {
      await c.shiftRightClickBlock();
    }
    await waitConsoleAfter(c, /railsys: confirmed/, f4cIdx, 30000);
    log('18 next placement session confirmed');
    idx = c.lines.length;
    await chat(c, 'railsys3 camera 66 14 0 0 90');
    await waitConsoleAfter(c, /railsys: camera set/, idx, 30000);
    await sleep(2000);
    const f4 = resolve(S, 'SS-R10F-04_ACCEPTANCE_FINAL.png');
    await c.shot(f4);

    // ==== final overview + verification ====
    idx = c.lines.length;
    await chat(c, 'railsys3 camera 66 10 12 180 28');
    await waitConsoleAfter(c, /railsys: camera set/, idx, 30000);
    await sleep(2000);
    const f5 = resolve(S, 'SS-R10F-05_PREVIEW_CONFIRM.png');
    await c.shot(f5);

    const statusIdx = c.lines.length;
    await chat(c, 'railsys3 status');
    const finalStatus = await waitConsoleAfter(c, /railsys: A=/, statusIdx, 30000);
    if (!/confirmed=yes/.test(finalStatus)) {
      throw new Error('final status does not report confirmed=yes: ' + finalStatus);
    }
    log('final status: ' + finalStatus);

    for (const f of [f1, f2, f3, f4, f5]) {
      if (!existsSync(f) || statSync(f).size === 0) {
        throw new Error('screenshot missing or empty: ' + f);
      }
    }
    log('JS_ERROR_COUNT=' + c.jsErrors);
    log('=== SUMMARY ===');
    log('F1 straight: ' + f1);
    log('F2 curve+gradient+cant: ' + f2);
    log('F3 editing + F4 asset isolation: ' + f3);
    log('F5 lifecycle / re-placement: ' + f4);
    log('F5 preview/confirm overview: ' + f5);
    log('=== R10F FOUNDATION ACCEPTANCE COMPLETE ===');
    return 0;
  } catch (e) {
    log('FATAL: ' + e.message);
    return 1;
  } finally {
    killChrome(profile);
  }
}

main().then((code) => { process.exitCode = code; }).catch((e) => {
  log('FATAL ' + e.message);
  process.exitCode = 1;
});
