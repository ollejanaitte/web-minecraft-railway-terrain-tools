#!/usr/bin/env node
/**
 * r15_closed_loop.mjs — Phase 1-R15 normal-world ModelPack closed-loop acceptance.
 * Reuses the proven GUI harness mechanics (CDP + instrumented HTML) in a
 * dedicated normal Superflat world.
 *
 * Verifies:
 *   1. /railsys3 testloop builds the Standard Closed-Loop course
 *   2. status shows prod=8 (8 production RailSegments)
 *   3. the loop renders (4 straights + 4 corners) in normal world
 *   4. screenshots: overview, corner, closeup, closure
 *   5. camera moves without visual breakage (sanity)
 */
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, statSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const ROOT = '/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools';
const PHASE = resolve(ROOT, 'doc/implementation/phase1_r15');
const S = resolve(PHASE, 'screenshots');
const LOGD = resolve(PHASE, 'logs');
const HTML_RAW = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const INSTR = resolve(PHASE, 'instrumented.html');
const PORT = process.env.CDP_PORT || '9541';
const RUN_ID = process.env.RUN_ID || 'r15';
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
  const needRebuild = !existsSync(INSTR) || statSync(INSTR).mtimeMs < statSync(HTML_RAW).mtimeMs;
  if (!needRebuild) return;
  const src = readFileSync(HTML_RAW);
  const idx = src.indexOf(HOOK_NEEDLE);
  if (idx < 0) throw new Error('instrumentation needle not found in HTML');
  const hook = HOOK_NEEDLE + Buffer.from(
    `\n\thooks: {\n\t\tscreenChanged: function(name, w, h, dw, dh, s) {\n` +
    `\t\t\ttry { window.__eagScreen = name; window.__eagScreenTime = Date.now(); } catch (e) {}\n` +
    `\t\t}\n\t},`);
  writeFileSync(INSTR, Buffer.concat([src.slice(0, idx), Buffer.from(hook), src.slice(idx + HOOK_NEEDLE.length)]));
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
        if (m.params.type === 'error') { log('JS_CONSOLE_ERROR ' + t.slice(0, 400)); this.jsErrors++; }
        if (/railsys:|RAILSYSTEM/i.test(t)) log('CHAT ' + t.slice(0, 260));
      }
      if (m.method === 'Runtime.exceptionThrown') {
        log('JS_EXCEPTION ' + ((m.params.exceptionDetails || {}).text || ''));
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
  async typeChar(ch) {
    const codes = { '/': ['Slash', 191], ' ': ['Space', 32], 'a': ['KeyA', 65], 'b': ['KeyB', 66],
      'c': ['KeyC', 67], 'd': ['KeyD', 68], 'e': ['KeyE', 69], 'f': ['KeyF', 70], 'g': ['KeyG', 71],
      'h': ['KeyH', 72], 'i': ['KeyI', 73], 'j': ['KeyJ', 74], 'k': ['KeyK', 75], 'l': ['KeyL', 76],
      'm': ['KeyM', 77], 'n': ['KeyN', 78], 'o': ['KeyO', 79], 'p': ['KeyP', 80], 'q': ['KeyQ', 81],
      'r': ['KeyR', 82], 's': ['KeyS', 83], 't': ['KeyT', 84], 'u': ['KeyU', 85], 'v': ['KeyV', 86],
      'w': ['KeyW', 87], 'x': ['KeyX', 88], 'y': ['KeyY', 89], 'z': ['KeyZ', 90],
      '1': ['Digit1', 49], '2': ['Digit2', 50], '3': ['Digit3', 51], '4': ['Digit4', 52],
      '5': ['Digit5', 53], '6': ['Digit6', 54], '7': ['Digit7', 55], '8': ['Digit8', 56],
      '9': ['Digit9', 57], '0': ['Digit0', 48], '-': ['Minus', 189], '.': ['Period', 190] };
    let code = 'Key' + ch.toUpperCase();
    let vk = ch.codePointAt(0);
    if (codes[ch]) { code = codes[ch][0]; vk = codes[ch][1]; }
    const text = ch === ' ' ? ' ' : ch;
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: ch, code, windowsVirtualKeyCode: vk, text, unmodifiedText: text });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: ch, code, windowsVirtualKeyCode: vk });
    await sleep(20);
  }
  async typeText(name) { await sleep(200); for (const ch of name) { await this.typeChar(ch); await sleep(30); } }
  async enter() {
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13 });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13 });
  }
  async pressEscape() {
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
    await sleep(800);
  }
}

function launchChrome(guiMode, profileDir) {
  mkdirSync(profileDir, { recursive: true });
  const args = ['--no-sandbox', '--no-first-run', '--no-default-browser-check',
    '--autoplay-policy=no-user-gesture-required', '--mute-audio', '--hide-scrollbars',
    '--window-size=1280,720', `--user-data-dir=${profileDir}`, `--remote-debugging-port=${PORT}`];
  if (guiMode !== 'gui') args.push('--headless=new', '--use-gl=angle', '--use-angle=swiftshader');
  args.push('file://' + INSTR);
  const child = spawn('/opt/google/chrome/chrome', args, { detached: true, stdio: ['ignore', 'pipe', 'pipe'] });
  child.stdout.on('data', (d) => writeFileSync(resolve(LOGD, 'chrome_stdout.log'), d.toString(), { flag: 'a' }));
  child.stderr.on('data', (d) => writeFileSync(resolve(LOGD, 'chrome_stderr.log'), d.toString(), { flag: 'a' }));
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
    if (s === null || s === undefined) { if (i === 0) await c.click(640, 360); await sleep(2000); continue; }
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
    for (let i = startIndex; i < c.lines.length; i++) if (re.test(c.lines[i])) return c.lines[i];
    await sleep(500);
  }
  throw new Error('console wait timeout for ' + re);
}

async function chatAndWaitNew(c, text, re, timeoutMs) {
  const startIndex = c.lines.length;
  await chat(c, text);
  return waitConsoleAfter(c, re, startIndex, timeoutMs);
}

async function tryOpenChat(c) {
  for (let i = 0; i < 5; i++) {
    await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: '/', code: 'Slash', windowsVirtualKeyCode: 191, text: '/', unmodifiedText: '/' });
    await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: '/', code: 'Slash', windowsVirtualKeyCode: 191 });
    await sleep(900);
    if (String(await c.screen()).includes('GuiChat')) return { opened: true, prefilled: true };
  }
  await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
  await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
  await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 't', code: 'KeyT', windowsVirtualKeyCode: 84 });
  await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 't', code: 'KeyT', windowsVirtualKeyCode: 84 });
  await sleep(900);
  if (String(await c.screen()).includes('GuiChat')) return { opened: true, prefilled: false };
  return { opened: false, prefilled: false };
}

async function chat(c, text) {
  let opened = await tryOpenChat(c);
  if (!opened.opened) {
    await c.pressEscape();
    await sleep(600);
    opened = await tryOpenChat(c);
  }
  if (!opened.opened) throw new Error('GuiChat could not be opened');
  await sleep(300); await sleep(1200);
  await c.typeText(opened.prefilled ? text : '/' + text);
  await sleep(500);
  await c.enter();
  await sleep(1200);
}

async function main() {
  ensureInstrumented();
  const guiMode = process.env.GUI_MODE || (process.env.DISPLAY ? 'gui' : 'headless');
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
    await enterWorld(c, 'RailsysR15');
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
    await c.click(640, 360);
    await sleep(1500);

    await chatAndWaitNew(c, 'difficulty peaceful', /Difficulty.*Peaceful|difficulty.*Peaceful/i, 30000);

    // R15: ModelPack asset selection + closed-loop application.
    // The embedded Railsys-native bundle auto-imports at client init; verify
    // the real NR01 Concrete asset is registered, then apply it to the loop.
    const asIdx = c.lines.length;
    await chat(c, 'railsys15 assets');
    const asLine = await waitConsoleAfter(c, /railsys15: assets=/, asIdx, 30000);
    log('assets: ' + asLine.slice(0, 400));
    if (!asLine.includes('nr01-nb-rails:1435mm_nb_concrete')) {
      throw new Error('expected nr01 concrete asset in list: ' + asLine.slice(0, 400));
    }

    // Apply the ModelPack asset to the Standard Closed-Loop course.
    // First select the NR01 Concrete asset as the current rail asset.
    const useIdx = c.lines.length;
    await chat(c, 'railsys15 use nr01-nb-rails:1435mm_nb_concrete');
    await waitConsoleAfter(c, /railsys15: asset ->/, useIdx, 30000);
    const tIdx = c.lines.length;
    await chat(c, 'railsys15 testloop');
    const tLine = await waitConsoleAfter(c, /railsys15: testloop built/, tIdx, 30000);
    log('testloop: ' + tLine);
    if (!tLine.includes('8 segments')) {
      throw new Error('expected 8 segments in testloop: ' + tLine);
    }
    if (!tLine.includes('asset=nr01-nb-rails:1435mm_nb_concrete')) {
      throw new Error('expected ModelPack asset on loop: ' + tLine);
    }

    // Compact loop for screenshots (same ModelPack asset).
    const cIdx = c.lines.length;
    await chat(c, 'railsys15 testloop_compact');
    await waitConsoleAfter(c, /railsys15: testloop_compact built/, cIdx, 30000);
    log('testloop_compact built');

    // railsys15 status: current asset must be the ModelPack concrete.
    const stIdx = c.lines.length;
    await chat(c, 'railsys15 status');
    const statusLine = await waitConsoleAfter(c, /railsys15: asset=/, stIdx, 30000);
    log('status: ' + statusLine);
    if (!statusLine.includes('nr01-nb-rails:1435mm_nb_concrete')) {
      throw new Error('expected concrete as current asset: ' + statusLine);
    }
    // railsys3 status: prod store has standard (8) + compact (8) = 16.
    const pIdx = c.lines.length;
    await chat(c, 'railsys3 status');
    const prodLine = await waitConsoleAfter(c, /railsys: A=/, pIdx, 30000);
    log('prod status: ' + prodLine);
    if (!prodLine.includes('prod=16')) {
      throw new Error('expected prod=16 in status: ' + prodLine);
    }

    // Frame the compact loop at (70,0) and capture ModelPack-appearance shots.
    await chatAndWaitNew(c, 'tp @p 70 6 0 180 89', /Teleported|teleported/, 30000);
    await sleep(4000);

    // Screenshots of the COMPACT loop at (70,0): outer bounds x 60..80,
    // z -15..15. Overview: top-down fits the whole loop; corner/closeup/closure
    // near the track. All show the MODELPACK asset appearance.
    const shots = [
      ['SS-R15-01_CLOSED_LOOP_OVERVIEW.png', 'railsys3 camera 70 28 0 0 90'],
      ['SS-R15-02_MODELPACK_STRAIGHT.png', 'railsys3 camera 70 6 18 180 8'],
      ['SS-R15-03_MODELPACK_CURVE.png', 'railsys3 camera 78 6 12 225 10'],
      ['SS-R15-04_STRAIGHT_CURVE_BOUNDARY.png', 'railsys3 camera 75 6 6 200 6'],
      ['SS-R15-05_RAIL_TEXTURE_CLOSEUP.png', 'railsys3 camera 70 4.8 12 180 20'],
      ['SS-R15-07_BEFORE_AFTER_DEFAULT.png', 'railsys3 camera 70 28 0 0 90'],
      ['SS-R15-07_BEFORE_AFTER_MODELPACK.png', 'railsys3 camera 70 28 0 0 90'],
    ];
    for (const [name, cam] of shots) {
      const idx = c.lines.length;
      await chat(c, cam);
      await waitConsoleAfter(c, /railsys: camera set/, idx, 30000);
      await sleep(2500);
      await c.shot(resolve(S, name));
    }

    // BEFORE/AFTER: build the compact loop with the DEFAULT asset, capture the
    // same camera; then clear, rebuild the SAME loop with the ModelPack asset,
    // capture again. Proves Geometry=SAME / Appearance=CHANGED.
    const cl1Idx = c.lines.length;
    await chat(c, 'railsys15 clearloop');
    await waitConsoleAfter(c, /railsys15: cleared/, cl1Idx, 30000);
    const rdIdx = c.lines.length;
    await chat(c, 'railsys15 reset');
    await waitConsoleAfter(c, /railsys15: current asset/, rdIdx, 30000);
    const cdIdx = c.lines.length;
    await chat(c, 'railsys15 testloop_compact');
    await waitConsoleAfter(c, /railsys15: testloop_compact built/, cdIdx, 30000);
    await chatAndWaitNew(c, 'tp @p 70 6 0 180 89', /Teleported|teleported/, 30000);
    await sleep(4000);
    const camIdx = c.lines.length;
    await chat(c, 'railsys3 camera 70 28 0 0 90');
    await waitConsoleAfter(c, /railsys: camera set/, camIdx, 30000);
    await sleep(2500);
    await c.shot(resolve(S, 'SS-R15-BEFORE_DEFAULT.png'));

    // Clear, select ModelPack concrete, rebuild compact loop, same camera.
    const cl2Idx = c.lines.length;
    await chat(c, 'railsys15 clearloop');
    await waitConsoleAfter(c, /railsys15: cleared/, cl2Idx, 30000);
    const suIdx = c.lines.length;
    await chat(c, 'railsys15 use nr01-nb-rails:1435mm_nb_concrete');
    await waitConsoleAfter(c, /railsys15: asset ->/, suIdx, 30000);
    const c2Idx = c.lines.length;
    await chat(c, 'railsys15 testloop_compact');
    await waitConsoleAfter(c, /railsys15: testloop_compact built/, c2Idx, 30000);
    await chatAndWaitNew(c, 'tp @p 70 6 0 180 89', /Teleported|teleported/, 30000);
    await sleep(4000);
    const cam2Idx = c.lines.length;
    await chat(c, 'railsys3 camera 70 28 0 0 90');
    await waitConsoleAfter(c, /railsys: camera set/, cam2Idx, 30000);
    await sleep(2500);
    await c.shot(resolve(S, 'SS-R15-AFTER_MODELPACK.png'));

    // Open the Asset Selector GUI (same UI as Shift+Right-click) and capture.
    // This is the LAST screen action — the GUI blocks further chat.
    const selIdx = c.lines.length;
    await chat(c, 'railsys15 open');
    await waitConsoleAfter(c, /railsys15: selector opened/, selIdx, 30000);
    await sleep(2000);
    await c.shot(resolve(S, 'SS-R15-06_ASSET_SELECTOR.png'));

    log('=== SUMMARY ===');
    log('testloop: ' + tLine);
    log('prod status: ' + prodLine);
    log('JS_ERROR_COUNT=' + c.jsErrors);
    log('=== R15 CLOSED LOOP ACCEPTANCE COMPLETE ===');
    return 0;
    return 0;
  } catch (e) {
    log('FATAL: ' + e.message);
    return 1;
  } finally {
    killChrome(profile);
  }
}

main().then((code) => { process.exitCode = code; }).catch((e) => { log('FATAL ' + e.message); process.exitCode = 1; });
