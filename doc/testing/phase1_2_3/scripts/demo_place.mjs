#!/usr/bin/env node
/**
 * Demonstrate /railsysplace on a flat world via a CDP-attached instance,
 * capturing each step. Does NOT touch the user's running game.
 */
import { spawn } from 'node:child_process';
import { writeFileSync, mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const ROOT = '/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools';
const PHASE = resolve(ROOT, 'doc/testing/phase1_2_3');
const S = resolve(PHASE, 'screenshots');
const INSTR = resolve(ROOT, 'doc/testing/phase1_2_2/scripts/validation_instrumented.html');
const PROFILE = process.env.VALIDATION_PROFILE || resolve(PHASE, 'profiles/demo-place');
const PORT = process.env.CDP_PORT || '9481';
const RUN_ID = process.env.RUN_ID || 'demo';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
mkdirSync(S, { recursive: true });
function log(m) { const line = `[${new Date().toISOString()}][${RUN_ID}] ${m}`; console.log(line); }

function launchChrome() {
  const args = ['--no-sandbox', '--no-first-run', '--no-default-browser-check',
    '--autoplay-policy=no-user-gesture-required', '--mute-audio', '--hide-scrollbars',
    '--window-size=1280,720', `--user-data-dir=${PROFILE}`, '--remote-debugging-port=' + PORT, 'file://' + INSTR];
  const child = spawn('/opt/google/chrome/chrome', args, { detached: true, stdio: 'ignore' });
  child.unref();
}
async function getPage() {
  for (let i = 0; i < 90; i++) {
    try { const l = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json(); const p = l.find((t) => t.type === 'page' && /EaglercraftX|file:\/\//i.test(t.url || '')); if (p) return p; } catch (_) {}
    await sleep(1000);
  }
  throw new Error('CDP unavailable');
}
class CDP {
  constructor(u) { this.u = u; this.id = 0; this.p = new Map(); this.lines = []; }
  async connect() { this.ws = new WebSocket(this.u); await new Promise((a, b) => { this.ws.onopen = a; this.ws.onerror = b; }); this.ws.onmessage = (ev) => { const m = JSON.parse(ev.data); if (m.method === 'Runtime.consoleAPICalled') { const t = (m.params.args || []).map((a) => a.value ?? a.description ?? '').join(' '); this.lines.push(t); } if (m.id && this.p.has(m.id)) { const { r, j } = this.p.get(m.id); this.p.delete(m.id); m.error ? j(new Error(JSON.stringify(m.error))) : r(m.result); } }; }
  send(method, params = {}, to = 20000) { const id = ++this.id; return new Promise((r, j) => { const t = setTimeout(() => { this.p.delete(id); j(new Error('timeout ' + method)); }, to); this.p.set(id, { r: (v) => { clearTimeout(t); r(v); }, j: (e) => { clearTimeout(t); j(e); } }); this.ws.send(JSON.stringify({ id, method, params })); }); }
  async eval(e) { try { return (await this.send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true })).result?.value; } catch (_) { return undefined; } }
  async screen() { return this.eval('window.__eagScreen'); }
  async shot(tag) { const res = await this.send('Page.captureScreenshot', { format: 'png' }, 60000); writeFileSync(resolve(S, tag), Buffer.from(res.data, 'base64')); }
  async click(x, y) { await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y }); await sleep(40); await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 }); await sleep(40); await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 }); await sleep(150); }
  async typeChar(ch) { const vk = ch.codePointAt(0); await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: ch, code: 'Key' + ch.toUpperCase(), windowsVirtualKeyCode: vk }); await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: ch, code: 'Key' + ch.toUpperCase(), windowsVirtualKeyCode: vk }); }
  async typeLine(t) { for (const ch of t) { await this.typeChar(ch); await sleep(15); } }
  async pressKey(k, c, v) { await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: k, code: c, windowsVirtualKeyCode: v }); await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: k, code: c, windowsVirtualKeyCode: v }); }
}
async function toTitle(c) { for (let i = 0; i < 60; i++) { const s = await c.screen(); if (s === null || s === undefined) { if (i === 0) await c.click(640, 360); await sleep(2000); continue; } const n = String(s); if (n.includes('GuiMainMenu')) { log('title'); return; } if (n.includes('GuiScreenContentWarning')) { await c.click(640, 390); await sleep(500); await c.click(640, 450); await sleep(1000); continue; } if (n.includes('GuiScreenDefaultUsernameNote')) { await c.click(640, 380); await sleep(1000); continue; } if (n.includes('GuiScreenEditProfile')) { await c.click(640, 420); await sleep(1000); continue; } await sleep(2000); } }
async function toCreate(c) { await c.click(640, 258); const want = (s) => String(s).includes('GuiSelectWorld') || String(s).includes('GuiScreenCreateWorldSelection'); const dl = Date.now() + 150000; while (Date.now() < dl) { const s = await c.screen(); if (s === null) return; if (want(s)) break; if (String(s).includes('GuiMainMenu')) { await c.click(640, 258); await sleep(1000); continue; } await sleep(1500); } let s = await c.screen(); if (String(s).includes('GuiScreenCreateWorldSelection')) { await c.click(640, 242); } else { await c.click(762, 420); await sleep(3000); await c.click(640, 242); } await sleep(3000); }
async function enter(c) { await c.click(500, 145); await sleep(500); for (const ch of 'EaglerFlatValidate') { await c.typeChar(ch); await sleep(30); } await sleep(1000); }
async function create(c) { await c.click(444, 470); for (let i = 0; i < 90; i++) { await sleep(3000); const s = await c.screen(); if (s === null) { log('in-game'); return; } if (String(s).includes('GuiSelectWorld') || String(s).includes('GuiMainMenu')) { await toCreate(c); await enter(c); await c.click(444, 470); } } }
async function cmd(c, s) { await c.pressKey('t', 'KeyT', 84); await sleep(800); await c.typeLine(s); await sleep(400); await c.pressKey('Enter', 'Enter', 13); await sleep(2000); }
async function tp(c, x, y, z, yaw, pitch) { await c.pressKey('t', 'KeyT', 84); await sleep(800); await c.typeLine(`/tp ${x} ${y} ${z} ${yaw} ${pitch}`); await sleep(400); await c.pressKey('Enter', 'Enter', 13); await sleep(2000); }

async function main() {
  launchChrome();
  await sleep(6000);
  const pg = await getPage();
  const c = new CDP(pg.webSocketDebuggerUrl);
  await c.connect();
  await c.send('Runtime.enable'); await c.send('Page.enable');
  await c.send('Emulation.setDeviceMetricsOverride', { width: 1208, height: 505, deviceScaleFactor: 0, mobile: false });
  await toTitle(c); await toCreate(c); await enter(c); await create(c);
  await sleep(4000);
  for (let i = 0; i < 40; i++) { await sleep(3000); if (await c.screen() === null) break; }
  await cmd(c, '/railrender clear');

  // STEP 1: pos1 (Marker A at player)
  await cmd(c, '/railsysplace pos1');
  await c.shot(RUN_ID + '_step1_pos1.png');
  // STEP 2: move a bit (teleport east), pos2
  await tp(c, 8, 78, 30, 0, 0);
  await sleep(1500);
  await cmd(c, '/railsysplace pos2');
  await c.shot(RUN_ID + '_step2_pos2.png');
  // STEP 3: preview
  await cmd(c, '/railsysplace preview');
  await c.shot(RUN_ID + '_step3_preview.png');
  // STEP 4: status
  await cmd(c, '/railsysplace status');
  await c.shot(RUN_ID + '_step4_status.png');
  // STEP 5: confirm
  await cmd(c, '/railsysplace confirm');
  await tp(c, 8, 85, 8, 0, -90);
  await sleep(2500);
  await c.shot(RUN_ID + '_step5_confirm_view.png');
  log('DONE');
  process.exit(0);
}
main().catch((e) => { log('FATAL ' + e.message); process.exit(1); });
