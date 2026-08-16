#!/usr/bin/env node
/**
 * unicode_font_test.mjs — Minimal Japanese/Unicode font display check.
 *
 * Boots Eaglercraft (HW Vulkan by default), creates a world, types a Japanese
 * message in chat and captures screenshots of:
 *   - the chat input field with Japanese text
 *   - the chat log after sending (Japanese + ASCII mix)
 *
 * This is the lightweight "confirm Japanese rendering" gate. It does NOT build
 * rails or run the full acceptance tour.
 *
 * Usage (usually via run-full-gate.sh):
 *   ./run-full-gate.sh doc/testing/phase1_r17_5/unicode_font_test.mjs
 * Env: GPU_MODE=vulkan|swiftshader, CDP_PORT, RUN_ID, GUI_MODE
 */
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, statSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const ROOT = '/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools';
const PHASE = resolve(ROOT, 'doc/testing/phase1_r17_5');
const S = resolve(PHASE, 'screenshots');
const LOGD = resolve(PHASE, 'logs');
const HTML_RAW = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const INSTR = resolve(PHASE, 'instrumented.html');
const PORT = process.env.CDP_PORT || '9581';
const RUN_ID = process.env.RUN_ID || 'fonttest';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

mkdirSync(S, { recursive: true });
mkdirSync(LOGD, { recursive: true });

const JAPANESE = '日本語テスト 鉄道 車両 接続 分岐器';

function log(m) {
  const line = `[${new Date().toISOString()}][${RUN_ID}] ${m}`;
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
        if (/railsys:|RAILSYSTEM|\[CHAT\]/i.test(t)) log('CHAT ' + t.slice(0, 260));
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
    const codes = { '/': ['Slash', 191], ' ': ['Space', 32], '-': ['Minus', 189], '.': ['Period', 190] };
    let code = 'Key' + ch.toUpperCase();
    let vk = ch.codePointAt(0);
    if (codes[ch]) { code = codes[ch][0]; vk = codes[ch][1]; }
    const text = ch === ' ' ? ' ' : ch;
    // Japanese chars are delivered via the `key`/`text` fields (IME-like insert).
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: ch, code, windowsVirtualKeyCode: vk, text, unmodifiedText: text });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: ch, code, windowsVirtualKeyCode: vk });
    await sleep(30);
  }
  async typeText(name) { await sleep(200); for (const ch of name) { await this.typeChar(ch); await sleep(20); } }
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
  for (let i = 0; i < 120; i++) {
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
  for (let i = 0; i < 120; i++) {
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

async function tryOpenChat(c) {
  for (let i = 0; i < 5; i++) {
    await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: '/', code: 'Slash', windowsVirtualKeyCode: 191, text: '/', unmodifiedText: '/' });
    await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: '/', code: 'Slash', windowsVirtualKeyCode: 191 });
    await sleep(900);
    if (String(await c.screen()).includes('GuiChat')) return { opened: true, prefilled: true };
  }
  await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 't', code: 'KeyT', windowsVirtualKeyCode: 84 });
  await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 't', code: 'KeyT', windowsVirtualKeyCode: 84 });
  await sleep(900);
  if (String(await c.screen()).includes('GuiChat')) return { opened: true, prefilled: false };
  return { opened: false, prefilled: false };
}

async function openChatRobust(c) {
  for (let attempt = 0; attempt < 3; attempt++) {
    const r = await tryOpenChat(c);
    if (r.opened) return r;
    await c.pressEscape();
    await sleep(800);
  }
  throw new Error('GuiChat could not be opened');
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
    await enterWorld(c, 'UnicodeFontTest');
    await createWorld(c);
    await sleep(8000);
    await c.click(640, 360);
    await sleep(1500);

    // 1) Open chat and type Japanese; capture the chat input field BEFORE sending.
    const opened = await openChatRobust(c);
    await sleep(1200);
    const inputText = opened.prefilled ? JAPANESE : '/' + JAPANESE;
    await c.typeText(inputText);
    await sleep(600);
    await c.shot(resolve(S, RUN_ID + '_CHAT_INPUT_JAPANESE.png'));

    // 2) Send; the chat closes. Re-open and send a PLAIN Japanese chat message.
    await c.enter();
    await sleep(1800);
    await c.shot(resolve(S, RUN_ID + '_CHAT_SENT_JAPANESE.png'));

    // plain message: open chat, backspace the prefilled '/', type Japanese, send
    const op2 = await openChatRobust(c);
    if (op2.prefilled) {
      await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Backspace', code: 'Backspace', windowsVirtualKeyCode: 8 });
      await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Backspace', code: 'Backspace', windowsVirtualKeyCode: 8 });
      await sleep(200);
    }
    await c.typeText(JAPANESE + ' メッセージ');
    await sleep(600);
    await c.shot(resolve(S, RUN_ID + '_CHAT_PLAIN_INPUT_JAPANESE.png'));
    await c.enter();
    await sleep(1800);
    await c.shot(resolve(S, RUN_ID + '_CHAT_PLAIN_SENT_JAPANESE.png'));

    // 3) ASCII baseline for regression: type and send an ASCII-only message.
    const op3 = await openChatRobust(c);
    if (op3.prefilled) {
      await c.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Backspace', code: 'Backspace', windowsVirtualKeyCode: 8 });
      await c.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Backspace', code: 'Backspace', windowsVirtualKeyCode: 8 });
      await sleep(200);
    }
    await c.typeText('ascii baseline 123 ABC XYZ');
    await sleep(600);
    await c.shot(resolve(S, RUN_ID + '_CHAT_ASCII_INPUT.png'));
    await c.enter();
    await sleep(1800);
    await c.shot(resolve(S, RUN_ID + '_CHAT_ASCII_SENT.png'));

    log('=== SUMMARY ===');
    log('japanese="' + JAPANESE + '"');
    log('js_errors=' + c.jsErrors);
    log('shot dir=' + S);
    log('=== UNICODE FONT TEST COMPLETE ===');
    return 0;
  } catch (e) {
    log('FATAL: ' + e.message);
    return 1;
  } finally {
    killChrome(profile);
  }
}

main().then((code) => { process.exitCode = code; }).catch((e) => { log('FATAL ' + e.message); process.exitCode = 1; });