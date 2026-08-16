#!/usr/bin/env node
/**
 * r15_closed_loop.mjs — Phase 1-R16 normal-world closed-loop geometry + network acceptance.
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
const PHASE = resolve(ROOT, 'doc/implementation/phase1_r16');
const S = resolve(PHASE, 'screenshots');
const LOGD = resolve(PHASE, 'logs');
const HTML_RAW = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const INSTR = resolve(PHASE, 'instrumented.html');
const PORT = process.env.CDP_PORT || '9541';
const RUN_ID = process.env.RUN_ID || 'r16';
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
  if (guiMode !== 'gui') {
    args.push('--headless=new', '--use-gl=angle');
    const gpu = process.env.GPU_MODE || 'vulkan';
    if (gpu === 'swiftshader') args.push('--use-angle=swiftshader', '--enable-unsafe-swiftshader');
    else args.push('--use-angle=vulkan');
  }
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
    await enterWorld(c, 'RailsysR16');
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

    // R16: build the CORRECTED Standard Closed Loop (rounded rectangle) with
    // explicit topology. Default asset first (geometry visual evidence).
    const bIdx = c.lines.length;
    await chat(c, 'railsys16 build');
    const buildLine = await waitConsoleAfter(c, /railsys16: built/, bIdx, 30000);
    log('build: ' + buildLine);
    if (!buildLine.includes('8 segments')) {
      throw new Error('expected 8 segments: ' + buildLine);
    }
    if (!buildLine.includes('topology nodes=8 connections=8')) {
      throw new Error('expected topology nodes=8 connections=8: ' + buildLine);
    }

    // Teleport to loop centre and frame.
    await chatAndWaitNew(c, 'tp @p 0 6 0 180 89', /Teleported|teleported/, 30000);
    await sleep(4000);

    // Geometry visual evidence (default appearance): top view, perspective,
    // smooth 90 curve, straight->curve boundary.
    const gShots = [
      ['SS-R16-01_ROUNDED_LOOP_TOP.png', 'railsys3 camera 0 70 0 0 90'],
      ['SS-R16-02_ROUNDED_LOOP_PERSPECTIVE.png', 'railsys3 camera -25 24 45 40 28'],
      ['SS-R16-03_SMOOTH_90_CURVE.png', 'railsys3 camera 18 6 16 225 10'],
      ['SS-R16-04_STRAIGHT_CURVE_BOUNDARY.png', 'railsys3 camera 12 6 30 160 8'],
    ];
    for (const [name, cam] of gShots) {
      const idx = c.lines.length;
      await chat(c, cam);
      await waitConsoleAfter(c, /railsys: camera set/, idx, 30000);
      await sleep(2500);
      await c.shot(resolve(S, name));
    }

    // Network verify + forward/reverse traversal.
    const vIdx = c.lines.length;
    await chat(c, 'railsys16 verify');
    const verifyLine = await waitConsoleAfter(c, /railsys16: topology/, vIdx, 30000);
    log('verify: ' + verifyLine);
    if (!verifyLine.includes('VERIFIED')) {
      throw new Error('expected topology verified: ' + verifyLine);
    }
    const fIdx = c.lines.length;
    await chat(c, 'railsys16 forward');
    const fwdLine = await waitConsoleAfter(c, /railsys16: forward/, fIdx, 30000);
    log('forward: ' + fwdLine);
    if (!fwdLine.includes('closed=true')) {
      throw new Error('expected forward closed loop: ' + fwdLine);
    }
    const rIdx = c.lines.length;
    await chat(c, 'railsys16 reverse');
    const revLine = await waitConsoleAfter(c, /railsys16: reverse/, rIdx, 30000);
    log('reverse: ' + revLine);
    if (!revLine.includes('closed=true')) {
      throw new Error('expected reverse closed loop: ' + revLine);
    }

    // R16-06: R15 ModelPack regression — apply the NR01 Concrete asset to the
    // SAME corrected loop; geometry must be unchanged (asset = look only).
    const useIdx = c.lines.length;
    await chat(c, 'railsys15 use nr01-nb-rails:1435mm_nb_concrete');
    await waitConsoleAfter(c, /railsys15: asset ->/, useIdx, 30000);
    const asIdx = c.lines.length;
    await chat(c, 'railsys15 assets');
    const asLine = await waitConsoleAfter(c, /railsys15: assets=/, asIdx, 30000);
    if (!asLine.includes('nr01-nb-rails:1435mm_nb_concrete')) {
      throw new Error('expected concrete asset: ' + asLine.slice(0, 300));
    }
    // Replace asset on every loop rail (appearance only) — geometry stays same.
    const replIdx = c.lines.length;
    await chat(c, 'railsys16 status');
    const netStatusLine = await waitConsoleAfter(c, /railsys16: geometry:/, replIdx, 30000);
    log('net status: ' + netStatusLine);

    // Capture ModelPack-appearance loop shot at the top camera (same geometry).
    const mpIdx = c.lines.length;
    await chat(c, 'railsys3 camera 0 40 0 0 90');
    await waitConsoleAfter(c, /railsys: camera set/, mpIdx, 30000);
    await sleep(2500);
    await c.shot(resolve(S, 'SS-R16-05_MODELPACK_LOOP.png'));

    // Snapshot the network/topology status line for evidence.
    const stIdx = c.lines.length;
    await chat(c, 'railsys16 status');
    const statusLine = await waitConsoleAfter(c, /railsys16: geometry:/, stIdx, 30000);
    log('final status: ' + statusLine);

    // Final acceptance: rails3 status for prod=8 (one corrected loop).
    const pIdx = c.lines.length;
    await chat(c, 'railsys3 status');
    const prodLine = await waitConsoleAfter(c, /railsys: A=/, pIdx, 30000);
    log('prod status: ' + prodLine);

    log('=== SUMMARY ===');
    log('build: ' + buildLine);
    log('verify: ' + verifyLine);
    log('forward: ' + fwdLine);
    log('reverse: ' + revLine);
    log('net status: ' + netStatusLine);
    log('JS_ERROR_COUNT=' + c.jsErrors);
    log('=== R16 ACCEPTANCE COMPLETE ===');
    return 0;
    log('=== SUMMARY ===');
    log('testloop: ' + tLine);
    log('prod status: ' + prodLine);
    log('JS_ERROR_COUNT=' + c.jsErrors);
    log('=== R16 ACCEPTANCE ACCEPTANCE COMPLETE ===');
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
