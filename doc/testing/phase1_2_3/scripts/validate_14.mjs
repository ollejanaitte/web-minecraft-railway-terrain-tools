#!/usr/bin/env node
/**
 * Phase 1.4 GUI validation: Marker Placement / Editing.
 * pos1/pos2 -> preview (semi-transparent) -> confirm -> production rail.
 * Also verifies cancel.
 */
import { spawn } from 'node:child_process';
import { writeFileSync, mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const ROOT = '/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools';
const PHASE = resolve(ROOT, 'doc/testing/phase1_2_3');
const S = resolve(PHASE, 'screenshots');
const INSTR = resolve(ROOT, 'doc/testing/phase1_2_2/scripts/validation_instrumented.html');
const PROFILE = process.env.VALIDATION_PROFILE || resolve(PHASE, 'profiles/run-14');
const PORT = process.env.CDP_PORT || '9461';
const RUN_ID = process.env.RUN_ID || new Date().toISOString().replace(/[-:T]/g, '').slice(0, 14);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
mkdirSync(S, { recursive: true });
function log(m) { const line = `[${new Date().toISOString()}][${RUN_ID}] ${m}`; console.log(line); writeFileSync(resolve(PHASE, 'logs/validation_' + RUN_ID + '.log'), line + '\n', { flag: 'a' }); }

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
  constructor(u) { this.u = u; this.id = 0; this.p = new Map(); }
  async connect() { this.ws = new WebSocket(this.u); await new Promise((a, b) => { this.ws.onopen = a; this.ws.onerror = b; }); this.ws.onmessage = (ev) => { const m = JSON.parse(ev.data); if (m.id && this.p.has(m.id)) { const { r, j } = this.p.get(m.id); this.p.delete(m.id); m.error ? j(new Error(JSON.stringify(m.error))) : r(m.result); } }; }
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
async function cmd(c, s) { await c.pressKey('t', 'KeyT', 84); await sleep(800); await c.typeLine(s); await sleep(300); await c.pressKey('Enter', 'Enter', 13); await sleep(1800); }
async function tp(c, x, y, z, yaw, pitch) { await c.pressKey('t', 'KeyT', 84); await sleep(800); await c.typeLine(`/tp ${x} ${y} ${z} ${yaw} ${pitch}`); await sleep(300); await c.pressKey('Enter', 'Enter', 13); await sleep(2000); }

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

  // Clear production render so we only see placement.
  await cmd(c, '/railrender clear');
  // Marker A and B at fixed positions (straight line +Z).
  await cmd(c, '/railsysplace pos1 8 78 8 1.0');
  await cmd(c, '/railsysplace pos2 8 78 48 1.0');
  await cmd(c, '/railsysplace preview');
  await tp(c, 8, 84, 8, 0, -90);
  await sleep(2500);
  await c.shot(RUN_ID + '_SS-P1_4-01_preview.png');

  // Edit: pitch a bit, move handle for a curve.
  await cmd(c, '/railsysplace pos1 8 78 8 2.0');
  await cmd(c, '/railsysplace pos2 16 78 48 2.0');
  await cmd(c, '/railsysplace pitch 0');
  await cmd(c, '/railsysplace preview');
  await tp(c, 8, 85, 8, 0, -90);
  await sleep(2500);
  await c.shot(RUN_ID + '_SS-P1_4-02_edited_preview.png');

  await cmd(c, '/railsysplace asset railsys.straight_1067_ballast');
  await sleep(1000);
  await c.shot(RUN_ID + '_SS-P1_4-03_asset_1067.png');

  // Confirm
  await cmd(c, '/railsysplace confirm');
  await sleep(1500);
  await c.shot(RUN_ID + '_SS-P1_4-04_confirmed.png');

  // Cancel resets
  await cmd(c, '/railsysplace cancel');
  await cmd(c, '/railsysplace status');
  await sleep(1500);
  await c.shot(RUN_ID + '_SS-P1_4-05_after_cancel.png');

  log('DONE');
  process.exit(0);
}
main().catch((e) => { log('FATAL ' + e.message); process.exit(1); });
