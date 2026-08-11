#!/usr/bin/env node
/**
 * Phase 1.3A GUI validation: Production 3D rail renderer on a straight path.
 *
 * Flow:
 *   1. launch GUI chrome (validation profile, instrumented HTML)
 *   2. reach Flat Validation World (EaglerFlatValidate)
 *   3. run /railrender straight 40  via chat
 *   4. move camera above the rail
 *   5. capture screenshots (SS-P1_3A-01..n)
 *   6. report stage progress / heartbeat
 *
 * Reuses Phase 1.2.2 navigation knowledge (GuiMainMenu -> create world).
 * This script is validation-only.
 */
import { spawn } from 'node:child_process';
import { writeFileSync, mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const ROOT = '/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools';
const PHASE = resolve(ROOT, 'doc/testing/phase1_2_3');
const S = resolve(PHASE, 'screenshots');
const INSTR = resolve(ROOT, 'doc/testing/phase1_2_2/scripts/validation_instrumented.html');
const PROFILE = process.env.VALIDATION_PROFILE || resolve(PHASE, 'profiles/run-13a');
const PORT = process.env.CDP_PORT || '9431';
const RUN_ID = process.env.RUN_ID || new Date().toISOString().replace(/[-:T]/g, '').slice(0, 14);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
mkdirSync(S, { recursive: true });
mkdirSync(resolve(PHASE, 'logs'), { recursive: true });
const LOG = resolve(PHASE, 'logs/validation_' + RUN_ID + '.log');
function log(m) {
  const line = `[${new Date().toISOString()}][${RUN_ID}] ${m}`;
  console.log(line);
  writeFileSync(LOG, line + '\n', { flag: 'a' });
}

function launchChrome() {
  const args = ['--no-sandbox', '--no-first-run', '--no-default-browser-check',
    '--autoplay-policy=no-user-gesture-required', '--mute-audio', '--hide-scrollbars',
    '--window-size=1280,720', `--user-data-dir=${PROFILE}`, '--remote-debugging-port=' + PORT,
    'file://' + INSTR];
  const child = spawn('/opt/google/chrome/chrome', args, { detached: true, stdio: 'ignore' });
  child.unref();
  log('chrome launched pid=' + child.pid);
}

async function getPage() {
  for (let i = 0; i < 90; i++) {
    try {
      const l = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json();
      const p = l.find((t) => t.type === 'page' && /EaglercraftX|file:\/\//i.test(t.url || ''));
      if (p) return p;
    } catch (_) {}
    await sleep(1000);
  }
  throw new Error('CDP unavailable');
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
      }
      if (m.id && this.p.has(m.id)) { const { resolve, reject } = this.p.get(m.id); this.p.delete(m.id); m.error ? reject(new Error(JSON.stringify(m.error))) : resolve(m.result); }
    };
  }
  send(method, params = {}, timeoutMs = 20000) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      const t = setTimeout(() => { this.p.delete(id); reject(new Error('timeout ' + method)); }, timeoutMs);
      this.p.set(id, { resolve: (v) => { clearTimeout(t); resolve(v); }, reject: (e) => { clearTimeout(t); reject(e); } });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }
  async eval(e) { try { return (await this.send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true })).result?.value; } catch (_) { return undefined; } }
  async screen() { return this.eval('window.__eagScreen'); }
  async shot(tag) {
    const res = await this.send('Page.captureScreenshot', { format: 'png' }, 60000);
    writeFileSync(resolve(S, tag), Buffer.from(res.data, 'base64'));
    return tag;
  }
  async click(x, y) {
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y }); await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 }); await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 }); await sleep(150);
  }
  async typeChar(ch) {
    const vk = ch.codePointAt(0);
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: ch, code: 'Key' + ch.toUpperCase(), windowsVirtualKeyCode: vk });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: ch, code: 'Key' + ch.toUpperCase(), windowsVirtualKeyCode: vk });
  }
  async typeLine(text) {
    for (const ch of text) { await this.typeChar(ch); await sleep(25); }
  }
  async pressKey(key, code, vk) {
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key, code, windowsVirtualKeyCode: vk });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key, code, windowsVirtualKeyCode: vk });
  }
}

async function toTitle(cdp) {
  for (let i = 0; i < 60; i++) {
    const s = await cdp.screen();
    if (s === null || s === undefined) { if (i === 0) await cdp.click(640, 360); await sleep(2000); continue; }
    const n = String(s);
    if (n.includes('GuiMainMenu')) { log('title reached'); return; }
    if (n.includes('GuiScreenContentWarning')) { await cdp.click(640, 390); await sleep(500); await cdp.click(640, 450); await sleep(1000); continue; }
    if (n.includes('GuiScreenDefaultUsernameNote')) { await cdp.click(640, 380); await sleep(1000); continue; }
    if (n.includes('GuiScreenEditProfile')) { await cdp.click(640, 420); await sleep(1000); continue; }
    await sleep(2000);
  }
  throw new Error('title timeout');
}

async function toCreateForm(cdp) {
  await cdp.click(640, 258); // Singleplayer
  const want = (s) => String(s).includes('GuiSelectWorld') || String(s).includes('GuiScreenCreateWorldSelection');
  const deadline = Date.now() + 150000;
  while (Date.now() < deadline) {
    const s = await cdp.screen();
    const n = String(s);
    if (s === null) return { alreadyInGame: true };
    if (want(s)) break;
    if (String(s).includes('GuiMainMenu')) { await cdp.click(640, 258); await sleep(1000); continue; }
    await sleep(1500);
  }
  let s = await cdp.screen();
  if (String(s).includes('GuiScreenCreateWorldSelection')) { await cdp.click(640, 242); }
  else { await cdp.click(762, 420); await sleep(3000); await cdp.click(640, 242); }
  await sleep(3000);
  return { alreadyInGame: false };
}

async function enterWorld(cdp) {
  await cdp.click(500, 145); // world name field
  await sleep(500);
  await cdp.pressKey('a', 'KeyA', 65); // select-all not reliable; just type
  await sleep(200);
  for (const ch of 'EaglerFlatValidate') { await cdp.typeChar(ch); await sleep(40); }
  await sleep(1000);
}

async function createWorld(cdp) {
  await cdp.click(444, 470); // Create (left button)
  for (let i = 0; i < 80; i++) {
    await sleep(3000);
    const s = await cdp.screen();
    if (s === null) { log('in-game'); return; }
    if (/auto-validat/i.test(cdp.lines.join(' '))) { log('AutoValidate fired'); }
    if (String(s).includes('GuiSelectWorld') || String(s).includes('GuiMainMenu')) {
      log('bounce to menu, retrying');
      await toCreateForm(cdp); await enterWorld(cdp); await cdp.click(444, 470);
    }
  }
}

async function main() {
  writeFileSync(LOG, '');
  log('START Phase1.3A validation RUN=' + RUN_ID);
  launchChrome();
  await sleep(6000);
  const page = await getPage();
  const cdp = new CDP(page.webSocketDebuggerUrl);
  await cdp.connect();
  await cdp.send('Runtime.enable');
  await cdp.send('Page.enable');
  await cdp.send('Emulation.setDeviceMetricsOverride', { width: 1208, height: 505, deviceScaleFactor: 0, mobile: false });
  await toTitle(cdp);
  await toCreateForm(cdp);
  await enterWorld(cdp);
  await createWorld(cdp);
  await sleep(5000);
  // Wait for in-world
  for (let i = 0; i < 40; i++) {
    await sleep(3000);
    const s = await cdp.screen();
    if (s === null) break;
  }
  // Open chat and run /railrender straight
  await cdp.pressKey('t', 'KeyT', 84); // open chat
  await sleep(1000);
  await cdp.typeLine('/railrender straight 40');
  await sleep(500);
  await cdp.pressKey('Enter', 'Enter', 13);
  await sleep(3000);
  log('command sent');
  await cdp.shot(RUN_ID + '_SS-P1_3A-01_command.png');
  // Teleport to view the rail from above (spawn area)
  await cdp.pressKey('t', 'KeyT', 84);
  await sleep(1000);
  await cdp.typeLine('/tp ~ ~10 ~');
  await sleep(500);
  await cdp.pressKey('Enter', 'Enter', 13);
  await sleep(3000);
  await cdp.shot(RUN_ID + '_SS-P1_3A-02_above.png');
  // Look down
  await cdp.pressKey('t', 'KeyT', 84);
  await sleep(1000);
  await cdp.typeLine('/tp ~ ~20 ~ ~ -90');
  await sleep(500);
  await cdp.pressKey('Enter', 'Enter', 13);
  await sleep(4000);
  await cdp.shot(RUN_ID + '_SS-P1_3A-03_lookdown.png');
  log('DONE shots captured');
  process.exit(0);
}

main().catch((e) => {
  log('FATAL ' + (e.stack || e));
  process.exit(1);
});
