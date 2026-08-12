#!/usr/bin/env node
/**
 * r10f_screenshots.mjs — R10F screenshot pass. Re-enters the R10F normal world
 * (profile run-r10f, world RailsysR10F, which now contains CONFIRMED rails at
 * x 0..16 and x 60..72 from the acceptance course) and captures close-framed
 * screenshots for the Foundation acceptance evidence. Uses the PRODUCTION
 * /railsys3 camera for framing and a status snapshot for context.
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
const RUN_ID = process.env.RUN_ID || 'r10fshot';
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
  constructor(u) { this.u = u; this.id = 0; this.p = new Map(); this.lines = []; }
  async connect() {
    this.ws = new WebSocket(this.u);
    await new Promise((a, b) => { this.ws.onopen = a; this.ws.onerror = b; });
    this.ws.onmessage = (ev) => {
      const m = JSON.parse(ev.data);
      if (m.method === 'Runtime.consoleAPICalled') {
        const t = (m.params.args || []).map((a) => a.value ?? a.description ?? '').join(' ');
        this.lines.push(t);
        if (/railsys:|R9RENDER/i.test(t)) log('CHAT ' + t.slice(0, 260));
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
  async key(k) {
    const vk = typeof k === 'number' ? k : k.codePointAt(0);
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: typeof k === 'number' ? String.fromCharCode(k) : k, code: 'Key' + k, windowsVirtualKeyCode: vk });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: typeof k === 'number' ? String.fromCharCode(k) : k, code: 'Key' + k, windowsVirtualKeyCode: vk });
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

async function enterExistingWorld(c) {
  await c.click(640, 258);
  await sleep(8000);
  let selection = String(await c.screen());
  if (!selection.includes('GuiSelectWorld') && !selection.includes('GuiScreenCreateWorldSelection')) {
    throw new Error('existing-world selection timeout, last=' + selection);
  }
  await c.click(500, 100);
  await sleep(1000);
  const loadStart = c.lines.length;
  await c.click(445, 422);
  const deadline = Date.now() + 300000;
  while (Date.now() < deadline) {
    if ((await c.screen()) === null) { log('world loaded: screen=null'); return; }
    for (const line of c.lines.slice(loadStart)) {
      if (/\[CONTRAIL\] render name=MpServer gate=false/.test(line)) { log('world loaded: CONTRAIL gate=false'); return; }
    }
    await sleep(1500);
  }
  throw new Error('existing-world load timeout, last=' + (await c.screen()));
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
  const opened = await tryOpenChat(c);
  if (!opened.opened) throw new Error('GuiChat could not be opened');
  await sleep(300); await sleep(1200);
  await c.typeText(opened.prefilled ? text : '/' + text);
  await sleep(500);
  await c.enter();
  await sleep(1200);
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

async function main() {
  ensureInstrumented();
  const guiMode = process.env.GUI_MODE || (process.env.DISPLAY ? 'gui' : 'headless');
  const profile = resolve(PHASE, 'profiles/run-r10f');
  launchChrome(guiMode, profile);
  await sleep(8000);
  const c = new CDP(await getPage());
  await c.connect();
  await c.send('Runtime.enable');
  await c.send('Page.enable');
  await c.send('Emulation.setDeviceMetricsOverride', { width: 1208, height: 505, deviceScaleFactor: 0, mobile: false });

  try {
    await toTitle(c);
    await enterExistingWorld(c);
    await sleep(12000);
    await c.click(640, 360);
    await sleep(1500);
    // Confirm state: two confirmed rails should be restored from WorldRailData.
    const stIdx = c.lines.length;
    await chat(c, 'railsys3 status');
    await waitConsoleAfter(c, /railsys: A=/, stIdx, 30000);

    // Close-framed shots over the confirmed rails.
    const frames = [
      ['SS-R10F-01_FOUNDATION_DATUM.png', 'camera 4 6 6 180 30'],
      ['SS-R10F-02_CURVE_GRADIENT_CANT.png', 'camera 8 7 8 180 26'],
      ['SS-R10F-03_EDITING_ASSET_ISOLATION.png', 'camera 6 5 -2 180 18'],
      ['SS-R10F-04_ACCEPTANCE_FINAL.png', 'camera 66 7 8 180 24'],
      ['SS-R10F-05_PREVIEW_CONFIRM.png', 'camera 30 8 20 200 22'],
    ];
    for (const [name, cam] of frames) {
      const idx = c.lines.length;
      await chat(c, cam);
      await waitConsoleAfter(c, /railsys: camera set/, idx, 30000);
      await sleep(2500);
      await c.shot(resolve(S, name));
    }
    log('=== R10F SCREENSHOT PASS COMPLETE ===');
    return 0;
  } catch (e) {
    log('FATAL: ' + e.message);
    return 1;
  } finally {
    killChrome(profile);
  }
}

main().then((code) => { process.exitCode = code; }).catch((e) => { log('FATAL ' + e.message); process.exitCode = 1; });
