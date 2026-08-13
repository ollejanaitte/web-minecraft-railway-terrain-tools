#!/usr/bin/env node
/**
 * r13_production_data.mjs — Phase 1-R13 normal-world production data
 * acceptance. Reuses the proven GUI harness mechanics (CDP + instrumented
 * HTML + vanilla /tp + real right-click) in a dedicated normal Superflat world.
 *
 * Verifies:
 *   1. wand -> POS1 -> POS2 -> preview -> edit -> confirm
 *   2. confirm surfaces a STABLE rail id (rail-N) in the chat message
 *   3. second confirm -> a DIFFERENT stable id
 *   4. /railsys3 status reports production ids
 *   5. invalid placement (over-long) rejected / valid placement recovers
 *   6. next placement works after confirm
 *
 * NO /pos1 /pos2 fallback for placements.
 */
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, statSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const ROOT = '/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools';
const PHASE = resolve(ROOT, 'doc/implementation/phase1_r13');
const S = resolve(PHASE, 'screenshots');
const LOGD = resolve(PHASE, 'logs');
const HTML_RAW = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const INSTR = resolve(PHASE, 'instrumented.html');
const PORT = process.env.CDP_PORT || '9531';
const RUN_ID = process.env.RUN_ID || 'r13';
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
    if ((await c.screen()) === null) { log('respawned'); break; }
    if (hasConsoleAfter(c, /\[CONTRAIL\] render name=MpServer gate=false/, startIndex)) { log('respawned (render)'); break; }
    await sleep(500);
  }
  await sleep(3000);
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

async function placePair(c, label, x1, x2) {
  await chatAndWaitNew(c, `tp @p ${x1} 6 0 270 89`, /Teleported|teleported/, 30000);
  await sleep(2500);
  await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
  const p1 = c.lines.length;
  await c.rightClick(640, 360);
  await waitConsoleAfter(c, /railsys: POS1 at/, p1, 30000);
  await chatAndWaitNew(c, `tp @p ${x2} 6 0 90 89`, /Teleported|teleported/, 30000);
  await sleep(2500);
  await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
  const p2 = c.lines.length;
  await c.rightClick(640, 360);
  await sleep(3000);
  if (!hasConsoleAfter(c, /railsys: POS2 at/, p2)) { await c.rightClick(640, 360); }
  await waitConsoleAfter(c, /railsys: POS2 at/, p2, 30000);
  await waitConsoleAfter(c, /railsys: preview ready/, p2, 30000);
  log(`${label} POS1(${x1}) POS2(${x2}) preview ready`);
}

async function confirmPair(c, label, x2) {
  await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
  await chatAndWaitNew(c, `tp @p ${x2} 6 0 90 89`, /Teleported|teleported/, 30000);
  await sleep(2500);
  await chatAndWaitNew(c, 'railsys3 camera reset', /railsys: camera reset to player/, 30000);
  await sleep(2500);
  const idx = c.lines.length;
  await c.shiftRightClickBlock();
  await sleep(3000);
  if (!hasConsoleAfter(c, /railsys: confirmed/, idx)) { await c.shiftRightClickBlock(); }
  const line = await waitConsoleAfter(c, /railsys: confirmed/, idx, 30000);
  log(`${label} confirmed: ${line}`);
  return line;
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
    await enterWorld(c, 'RailsysR13');
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

    await chatAndWaitNew(c, 'difficulty peaceful', /Difficulty.*Peaceful|difficulty.*Peaceful/i, 30000);
    const wandStart = c.lines.length;
    await chat(c, 'railsys3 wand');
    await waitConsoleAfter(c, /railsys: marker wand added/, wandStart, 30000);
    await c.pressHotbar1();
    log('wand given');

    // 1st rail: place + confirm, capture stable id.
    await placePair(c, 'RAIL1', 0, 20);
    await chat(c, 'railsys3 pitch 0');
    await sleep(800);
    const c1 = await confirmPair(c, 'RAIL1', 20);
    const id1 = c1.match(/rail-\d+/)?.[0];
    if (!id1) throw new Error('no stable id in first confirm: ' + c1);
    log('RAIL1 stable id = ' + id1);

    // status shows the production id.
    const stIdx = c.lines.length;
    await chat(c, 'railsys3 status');
    const status1 = await waitConsoleAfter(c, /railsys: A=/, stIdx, 30000);
    if (!status1.includes('prod=1(' + id1 + ')')) {
      throw new Error('status does not show prod id: ' + status1);
    }
    log('status after rail1: ' + status1);

    // 2nd rail: different stable id.
    await placePair(c, 'RAIL2', 40, 60);
    await chat(c, 'railsys3 pitch 0');
    await sleep(800);
    const c2 = await confirmPair(c, 'RAIL2', 60);
    const id2 = c2.match(/rail-\d+/)?.[0];
    if (!id2 || id2 === id1) throw new Error('second confirm must have a different stable id: ' + c2);
    log('RAIL2 stable id = ' + id2 + ' (different)');

    // Invalid placement: over-long (>=257) must be REJECTED by the controller
    // (no client confirm, no production id).
    await placePair(c, 'OVERSIZE', 80, 340);
    await chat(c, 'railsys3 pitch 0');
    await sleep(800);
    const oIdx = c.lines.length;
    await chat(c, 'railsys3 preview');
    const preLine = await waitConsoleAfter(c, /railsys: preview|railsys: preview failed/, oIdx, 30000);
    log('oversize preview line: ' + preLine);
    const oIdx2 = c.lines.length;
    await chat(c, 'railsys3 confirm');
    const rejLine = await waitConsoleAfter(c, /railsys: confirm rejected|railsys: no preview/, oIdx2, 30000);
    if (!rejLine.includes('confirm rejected') && !rejLine.includes('no preview')) {
      throw new Error('oversize confirm must be rejected: ' + rejLine);
    }
    log('OVERSIZE confirm rejected: ' + rejLine);
    // The production store must not include an over-length segment.
    const stIdx2 = c.lines.length;
    await chat(c, 'railsys3 status');
    const status2 = await waitConsoleAfter(c, /railsys: A=/, stIdx2, 30000);
    log('status after oversize: ' + status2);
    if (status2.includes('prod=3')) {
      throw new Error('over-length rail must not be registered: ' + status2);
    }

    // Recovery: after the rejected over-length confirm the markers/preview are
    // RETAINED (user can fix). Clear the session, then a short valid placement
    // works again.
    const clIdx = c.lines.length;
    await chat(c, 'railsys3 clear');
    await waitConsoleAfter(c, /railsys: session cleared/, clIdx, 30000);
    log('cleared after oversize rejection');
    await placePair(c, 'RECOVERY', 100, 110);
    await chat(c, 'railsys3 pitch 0');
    await sleep(800);
    const c4 = await confirmPair(c, 'RECOVERY', 110);
    const id4 = c4.match(/rail-\d+/)?.[0];
    if (!id4) throw new Error('recovery confirm should produce a stable id: ' + c4);
    log('RECOVERY stable id = ' + id4);

    // final overview shot
    const camIdx = c.lines.length;
    await chat(c, 'railsys3 camera 50 10 12 180 28');
    await waitConsoleAfter(c, /railsys: camera set/, camIdx, 30000);
    await sleep(2000);
    await c.shot(resolve(S, 'SS-R13-PRODUCTION_DATA.png'));

    log('=== SUMMARY ===');
    log('RAIL1 id: ' + id1);
    log('RAIL2 id: ' + id2 + ' (must differ from ' + id1 + ')');
    log('oversize confirm: REJECTED (no client confirm, no production id)');
    log('recovery id: ' + id4);
    log('status2: ' + status2);
    log('JS_ERROR_COUNT=' + c.jsErrors);
    log('=== R13 PRODUCTION DATA ACCEPTANCE COMPLETE ===');
    return 0;
  } catch (e) {
    log('FATAL: ' + e.message);
    return 1;
  } finally {
    killChrome(profile);
  }
}

main().then((code) => { process.exitCode = code; }).catch((e) => { log('FATAL ' + e.message); process.exitCode = 1; });
