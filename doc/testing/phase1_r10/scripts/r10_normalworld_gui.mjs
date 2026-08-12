#!/usr/bin/env node
/**
 * r10_normalworld_gui.mjs — Phase 1-R10 normal-world GUI proof (instrumented).
 *
 * NOTE: The "START" launcher smoke (R10 start-button smoke) is a SEPARATE
 * verification and is NOT claimed by this instrumented normal-world workflow.
 *
 * Creates a dedicated Superflat NORMAL world (name "RailsysR10") and drives the marker
 * placement UX through the production /railsys3 command plus real camera
 * repositioning plus real mouse/keyboard input:
 *   /railsys3 wand                          -> obtain marker wand + pressHotbar1
 *   /railsys3 camera + rightClick(640,360)  -> POS1 then POS2 (+ auto preview)
 *   /railsys3 handle/rot1/pitch/cant        -> edit the auto preview (preview-ready after each)
 *   /railsys3 asset railsys.prototype_narrow_1000 -> switch to narrow-gauge preview asset
 *   /railsys3 camera + shiftRightClickBlock -> confirm -> production rail
 *   re-confirm with no preview              -> must fail with no-preview error
 *   /railsys3 cancel/clear/status           -> confirmed rail must survive
 * NO /pos1 /pos2 command fallback: if the camera aim coordinates miss, the
 * script MUST fail (POS1/POS2/preview/confirmed waits all have to hit).
 */
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, statSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const ROOT = '/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools';
const PHASE = resolve(ROOT, 'doc/testing/phase1_r10');
const S = resolve(PHASE, 'screenshots');
const LOGD = resolve(PHASE, 'logs');
const HTML_RAW = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const INSTR = resolve(PHASE, 'scripts/instrumented.html');
const PORT = process.env.CDP_PORT || '9510';
const RUN_ID = process.env.RUN_ID || 'r10gui';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

mkdirSync(S, { recursive: true });
mkdirSync(LOGD, { recursive: true });

function log(m) {
  const line = `[${new Date().toISOString()}][${RUN_ID}] ${m}`;
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
  constructor(u) { this.u = u; this.id = 0; this.p = new Map(); this.lines = []; }
  async connect() {
    this.ws = new WebSocket(this.u);
    await new Promise((a, b) => { this.ws.onopen = a; this.ws.onerror = b; });
    this.ws.onmessage = (ev) => {
      const m = JSON.parse(ev.data);
      if (m.method === 'Runtime.consoleAPICalled') {
        const t = (m.params.args || []).map((a) => a.value ?? a.description ?? '').join(' ');
        this.lines.push(t);
        if (m.params.type === 'error') log('JS_CONSOLE_ERROR ' + t.slice(0, 500));
        if (/railsys:|MARKERARROW|RAILSYSTEM|CONTRAIL|CURVEGRAD/i.test(t)) log('CHAT ' + t.slice(0, 300));
      }
      if (m.method === 'Runtime.exceptionThrown') {
        const d = m.params.exceptionDetails || {};
        const ex = d.exception || {};
        const detail = [d.text, ex.description, ex.value].filter((v) => v !== undefined && v !== null && v !== '').join(' | ');
        log('JS_EXCEPTION ' + detail.slice(0, 700) + ' url=' + (d.url || '') + ' line=' + (d.lineNumber ?? ''));
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
    // In pointer-lock mode mouseMoved changes the camera look before the click.
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
    await sleep(40);
    await this.rightClick(640, 360, 8);
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'ShiftLeft', code: 'ShiftLeft', windowsVirtualKeyCode: 16, modifiers: 8 });
    await sleep(100);
  }
  async key(k, modifiers = 0) {
    const vk = typeof k === 'number' ? k : k.codePointAt(0);
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: typeof k === 'number' ? String.fromCharCode(k) : k, code: 'Key' + (typeof k === 'number' ? String.fromCharCode(k) : k), windowsVirtualKeyCode: vk, modifiers });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: typeof k === 'number' ? String.fromCharCode(k) : k, code: 'Key' + (typeof k === 'number' ? String.fromCharCode(k) : k), windowsVirtualKeyCode: vk, modifiers });
  }
  async typeChar(ch) {
    // Use correct DOM codes + numeric VK per char. '/' opens command chat.
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
  try {
    await c.shot(resolve(S, 'DEBUG_TITLE_TIMEOUT.png'));
    const diagnostic = await c.eval(`({
      readyState: document.readyState,
      title: document.title,
      href: location.href,
      bodyText: (document.body && document.body.innerText || '').slice(0, 500),
      eagScreenType: typeof window.__eagScreen
    })`);
    log('title-timeout diagnostic=' + JSON.stringify(diagnostic));
  } catch (e) {
    log('title-timeout diagnostic failed=' + e.message);
  }
  throw new Error('title timeout, last=' + (await c.screen()));
}

async function enterExistingWorld(c) {
  await c.click(640, 258);
  await sleep(8000);
  const selection = String(await c.screen());
  if (!selection.includes('GuiSelectWorld') && !selection.includes('GuiScreenCreateWorldSelection')) {
    throw new Error('existing-world selection timeout, last=' + selection);
  }
  log('existing-world entry click before screen=' + (await c.screen()));
  await c.click(500, 100);
  log('existing-world entry click after screen=' + (await c.screen()));
  await sleep(1000);
  log('play-selected click before screen=' + (await c.screen()));
  const loadStart = c.lines.length;
  await c.click(445, 422);
  log('play-selected click after screen=' + (await c.screen()));
  const deadline = Date.now() + 300000;
  while (Date.now() < deadline) {
    if ((await c.screen()) === null) {
      log('existing RailsysR10 world loaded: screen=null');
      return;
    }
    for (const line of c.lines.slice(loadStart)) {
      if (/curvegradient worldName=.*validation=false/.test(line)) {
        log('existing RailsysR10 world loaded: curvegradient validation=false');
        return;
      }
      if (/\[CONTRAIL\] render name=MpServer gate=false/.test(line)) {
        log('existing RailsysR10 world loaded: CONTRAIL gate=false');
        return;
      }
    }
    await sleep(1500);
  }
  throw new Error('existing-world load timeout, last=' + (await c.screen()));
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
  await c.send('Input.dispatchKeyEvent', {
    type: 'keyDown', key: 'Control', code: 'ControlLeft', windowsVirtualKeyCode: 17, modifiers: 2,
  });
  await c.send('Input.dispatchKeyEvent', {
    type: 'keyDown', key: 'a', code: 'KeyA', windowsVirtualKeyCode: 65, modifiers: 2, text: 'a', unmodifiedText: 'a',
  });
  await c.send('Input.dispatchKeyEvent', {
    type: 'keyUp', key: 'a', code: 'KeyA', windowsVirtualKeyCode: 65, modifiers: 2,
  });
  await c.send('Input.dispatchKeyEvent', {
    type: 'keyUp', key: 'Control', code: 'ControlLeft', windowsVirtualKeyCode: 17,
  });
  await c.send('Input.dispatchKeyEvent', {
    type: 'keyDown', key: 'Backspace', code: 'Backspace', windowsVirtualKeyCode: 8,
  });
  await c.send('Input.dispatchKeyEvent', {
    type: 'keyUp', key: 'Backspace', code: 'Backspace', windowsVirtualKeyCode: 8,
  });
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

async function waitConsole(c, re, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const hit = c.lines.find((t) => re.test(t));
    if (hit) return hit;
    await sleep(500);
  }
  throw new Error('console wait timeout for ' + re);
}

/**
 * Wait for a console line matching re among entries at index >= startIndex.
 * The caller snapshots c.lines.length BEFORE a command and passes it in, so
 * repeated messages such as "preview ready" are matched fresh instead of old
 * entries (waitConsole alone would find the earlier ones).
 */
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
  let reason = 'timeout';
  while (Date.now() < deadline) {
    if ((await c.screen()) === null) { reason = 'screen cleared'; break; }
    for (let i = startIndex; i < c.lines.length; i++) {
      if (/\[CONTRAIL\] render name=MpServer gate=false/.test(c.lines[i])) {
        reason = 'render resumed'; break;
      }
    }
    if (reason !== 'timeout') break;
    await sleep(500);
  }
  await sleep(3000);
  log('respawned from GuiGameOver: ' + reason);
}

/** Try to open command chat, with up to N attempts. */
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

/** Open command chat ('/'), type a command, press Enter. */
async function chat(c, text) {
  const opened = await tryOpenChat(c);
  log('chat opened: ' + JSON.stringify(opened));
  if (!opened.opened) throw new Error('GuiChat could not be opened; refusing blind command input');
  await sleep(300);
  await sleep(1200);
  await c.typeText(opened.prefilled ? text : '/' + text);
  await sleep(500);
  if (process.env.R10_CHAT_DEBUG === '1') {
    await c.shot(resolve(S, 'DEBUG_CHAT_TYPED.png'));
    log('chat typed screen=' + (await c.screen()) + ' cmd=' + text);
  }
  await c.enter();
  await sleep(1200);
  if (process.env.R10_CHAT_DEBUG === '1') {
    log('chat post-enter screen=' + await c.screen());
  }
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
    if (process.env.R10_REUSE_PROFILE === '1') {
      await enterExistingWorld(c);
    } else {
      await gotoCreateForm(c);
      if (process.env.R10_FORM_DEBUG === '1') {
        await c.shot(resolve(S, 'DEBUG_CREATE_FORM.png'));
        log('create-form debug screen=' + (await c.screen()));
        return 2;
      }
      if (process.env.R10_OPTIONS_DEBUG === '1') {
        await c.click(604, 394);
        await sleep(1000);
        await c.shot(resolve(S, 'DEBUG_WORLD_OPTIONS.png'));
        log('world-options debug screen=' + (await c.screen()));
        return 2;
      }
      await enterWorld(c, 'RailsysR10');
      await c.click(604, 394);
      await sleep(1000);
      await c.click(764, 220);
      await sleep(500);
      await c.click(445, 322);
      await sleep(500);
      await c.click(604, 394);
      await sleep(1000);
      await createWorld(c);
    }
    await sleep(20000);
    await respawnIfGameOver(c);
    // Ensure in-game focus: click center, then press Escape if a screen is open.
    await c.click(640, 360);
    await sleep(1500);
    await c.shot(resolve(S, 'DEBUG_PRE_WORKFLOW.png'));
    const preWorkflow = await c.eval(`({
      screen: window.__eagScreen,
      activeElement: document.activeElement ? document.activeElement.tagName : null,
      pointerLockElement: document.pointerLockElement ? document.pointerLockElement.tagName : null,
      hasFocus: document.hasFocus()
    })`);
    log('pre-workflow diagnostic=' + JSON.stringify(preWorkflow));
    log('in dedicated Superflat normal world (RailsysR10)');

    // === R10 instrumented normal-world workflow ===
    const difficulty = await chatAndWaitNew(c, 'difficulty peaceful', /Difficulty.*Peaceful|difficulty.*Peaceful/i, 30000);
    log('difficulty set to peaceful: ' + difficulty);
    // Give the marker wand, wait for the add message, then select hotbar slot 1.
    const start = c.lines.length;
    await chat(c, 'railsys3 wand');
    await waitConsoleAfter(c, /railsys: marker wand added/, start, 30000);
    await c.pressHotbar1();
    log('wand given + hotbar slot 1 selected');
    let idx;

    // POS1 via vanilla player teleport + actual center right-click. Player
    // teleport controls the wand ray and stored direction; render camera is
    // reserved for screenshots. There is NO /pos1 /pos2 command fallback.
    const tp1 = await chatAndWaitNew(c, 'tp @p 0 6 0 270 89', /Teleported|teleported/, 30000);
    log('POS1 player teleport complete: ' + tp1);
    await sleep(2500);
    const p1Camera = await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
    log('POS1 render view restored to teleported player: ' + p1Camera);
    const p1Idx = c.lines.length;
    await c.rightClick(640, 360);
    await waitConsoleAfter(c, /railsys: POS1 at/, p1Idx, 30000);
    log('POS1 set by player tp (0,6,0) west via real right-click');

    // POS2 via vanilla player teleport + actual center right-click -> preview.
    const tp2 = await chatAndWaitNew(c, 'tp @p 12 6 0 90 89', /Teleported|teleported/, 30000);
    log('POS2 player teleport complete: ' + tp2);
    await sleep(2500);
    const p2Camera = await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
    log('POS2 render view restored to teleported player: ' + p2Camera);
    const p2Idx = c.lines.length;
    await c.rightClick(640, 360);
    await sleep(3000);
    if (!hasConsoleAfter(c, /railsys: POS2 at/, p2Idx)) {
      log('POS2 gesture retry after pointer-lock acquisition');
      await c.rightClick(640, 360);
    }
    await c.shot(resolve(S, 'DEBUG_AFTER_POS2_CLICK.png'));
    await waitConsoleAfter(c, /railsys: POS2 at/, p2Idx, 30000);
    await waitConsoleAfter(c, /railsys: preview ready/, p2Idx, 30000);
    log('POS2 set by player tp (12,6,0) east + preview ready');

    // Overview shot of wand + POS1 + POS2.
    idx = c.lines.length;
    await chat(c, 'railsys3 camera 6 10 12 180 28');
    await waitConsoleAfter(c, /railsys: camera set/, idx, 30000);
    await sleep(2000);
    const f1 = resolve(S, 'SS-R10-01_WAND_POS1_POS2.png');
    await c.shot(f1);

    // Edit the auto preview: handle/rot1/pitch/cant, then switch asset to the
    // narrow gauge. Each edit must emit a FRESH preview-ready line, so the
    // console index is snapshotted before every command (waitConsole would
    // otherwise match the earlier preview-ready lines).
    idx = c.lines.length;
    await chat(c, 'railsys3 handle 10');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    idx = c.lines.length;
    await chat(c, 'railsys3 rot1 20');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    idx = c.lines.length;
    await chat(c, 'railsys3 pitch 4');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    idx = c.lines.length;
    await chat(c, 'railsys3 cant 6');
    await waitConsoleAfter(c, /railsys: preview ready/, idx, 30000);
    idx = c.lines.length;
    await chat(c, 'railsys3 asset railsys.prototype_narrow_1000');
    await waitConsoleAfter(c, /railsys: asset ->/, idx, 30000);
    await sleep(2000);
    const f2 = resolve(S, 'SS-R10-02_AUTO_PREVIEW_EDIT.png');
    await c.shot(f2);

    // Return the render view to the player, then teleport the player onto POS2
    // for the real sneak+right-click confirm gesture.
    await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
    const tpConfirm = await chatAndWaitNew(c, 'tp @p 12 6 0 90 89', /Teleported|teleported/, 30000);
    log('confirm player teleport complete: ' + tpConfirm);
    await sleep(2500);
    const confirmCamera = await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
    log('confirm render view restored to teleported player: ' + confirmCamera);
    const cfIdx = c.lines.length;
    await c.shiftRightClickBlock();
    await sleep(3000);
    if (!hasConsoleAfter(c, /railsys: confirmed/, cfIdx)) {
      log('Confirm gesture retry after pointer-lock acquisition');
      await c.shiftRightClickBlock();
    }
    await waitConsoleAfter(c, /railsys: confirmed/, cfIdx, 30000);
    log('confirmed via sneak+right-click');

    // Overview camera over the confirmed rail.
    idx = c.lines.length;
    await chat(c, 'railsys3 camera 6 10 12 180 28');
    await waitConsoleAfter(c, /railsys: camera set/, idx, 30000);
    await sleep(2000);
    const f3 = resolve(S, 'SS-R10-03_CONFIRMED_RAIL_FINAL.png');
    await c.shot(f3);

    // Re-confirm with no active preview: MUST fail with a no-preview error.
    idx = c.lines.length;
    await chat(c, 'railsys3 confirm');
    await waitConsoleAfter(c, /railsys: no preview to confirm/, idx, 30000);
    log('re-confirm without preview rejected as required');

    // cancel then clear then status — the confirmed rail must survive.
    idx = c.lines.length;
    await chat(c, 'railsys3 cancel');
    await waitConsoleAfter(c, /railsys: preview cancelled/, idx, 30000);
    idx = c.lines.length;
    await chat(c, 'railsys3 clear');
    await waitConsoleAfter(c, /railsys: session cleared/, idx, 30000);
    idx = c.lines.length;
    await chat(c, 'railsys3 status');
    const statusLine = await waitConsoleAfter(c, /railsys: A=/, idx, 30000);
    if (!/confirmed=yes/.test(statusLine)) {
      throw new Error('status does not report confirmed=yes: ' + statusLine);
    }
    log('status keeps confirmed rail: ' + statusLine);

    // Screenshot existence / nonzero checks.
    for (const f of [f1, f2, f3]) {
      if (!existsSync(f) || statSync(f).size === 0) {
        throw new Error('screenshot missing or empty: ' + f);
      }
    }

    log('=== SUMMARY ===');
    log('ACTUAL_RIGHT_CLICK_POS1: 640,360 (rightClick at POS1)');
    log('ACTUAL_RIGHT_CLICK_POS2: 640,360 (rightClick at POS2)');
    log('ACTUAL_SHIFT_RIGHT_CLICK_CONFIRM: 640,360 (shiftRightClickBlock)');
    log('SS-R10-01_WAND_POS1_POS2:     ' + f1);
    log('SS-R10-02_AUTO_PREVIEW_EDIT:  ' + f2);
    log('SS-R10-03_CONFIRMED_RAIL_FINAL: ' + f3);
    log('=== R10 INSTRUMENTED NORMAL-WORLD PLACEMENT PROOF COMPLETE ===');
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
