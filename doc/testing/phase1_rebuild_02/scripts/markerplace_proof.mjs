#!/usr/bin/env node
/**
 * markerplace_proof.mjs — Phase 1-R7/R8/R9 GUI validation.
 *
 * RUN A: Create "EaglerCleanFlat" (clean scene). Isolation check: marker/cant/
 *        markerplace hooks must NOT fire (no markers set -> no arrows).
 * RUN B: Create "EaglerMarkerPlace" (flat + client-driven placement proof).
 *        -> wait for [MARKERPLACE] R7/R8/R9 phase logs and stage tags
 *           r7/r8/r9; capture SS-R7_MARKER_PLACEMENT_CONFIRM.png,
 *           SS-R8_ANCHOR_EDIT_PREVIEW.png, SS-R9_ASSET_A.png,
 *           SS-R9_ASSET_B.png, SS-R7-R9_FINAL.png.
 * RUN C: regressions (R6 markercant / R5 continuousrail / R4 curvegradient).
 *
 * Uses validation-only Chrome profiles (NEVER runtime/profiles/game).
 */
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, statSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const ROOT = '/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools';
const PHASE = resolve(ROOT, 'doc/testing/phase1_rebuild_02');
const S = resolve(PHASE, 'screenshots');
const LOGD = resolve(PHASE, 'logs');
const HTML_RAW = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const INSTR = resolve(PHASE, 'scripts/instrumented.html');
const PORT = process.env.CDP_PORT || '9507';
const RUN_ID = process.env.RUN_ID || 'markerplace';
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
        if (/MARKERPLACE|MARKERARROW|marker place proof|railsys:|CANTPROOF|CONTRAIL|CURVEGRAD|STRAIGHTRAIL/i.test(t)) log('CHAT ' + t.slice(0, 300));
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
    const vk = ch.codePointAt(0);
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: ch, code: 'Key' + ch.toUpperCase(), windowsVirtualKeyCode: vk });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: ch, code: 'Key' + ch.toUpperCase(), windowsVirtualKeyCode: vk });
  }
  async typeName(name) {
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'a', code: 'KeyA', modifiers: 2, windowsVirtualKeyCode: 65 });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'a', code: 'KeyA', modifiers: 2, windowsVirtualKeyCode: 65 });
    await sleep(300);
    for (const ch of name) { await this.typeChar(ch); await sleep(35); }
    await sleep(800);
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
  await c.typeName(name);
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

async function runA(c, worldName) {
  log('=== RUN A: Clean Validation Scene (isolation check) ===');
  await toTitle(c);
  await gotoCreateForm(c);
  await enterWorld(c, worldName);
  await createWorld(c);
  await sleep(15000);
  const leaked = c.lines.find((t) => /MARKERARROW hook FIRED|railsys: POS1 at/.test(t));
  if (leaked) {
    log('ISOLATION FAIL: marker hook fired in clean world');
    return false;
  }
  log('ISOLATION OK: no marker/placement hook in cleanflat world');
  return true;
}

async function runB(c, worldName) {
  log('=== RUN B: Marker Rail Placement (R7/R8/R9) ===');
  await toTitle(c);
  await gotoCreateForm(c);
  await enterWorld(c, worldName);
  await createWorld(c);
  // Wait for the client-driven phases to complete (R7 select/confirm, R8 edit,
  // R9 asset switch) and the server tour tags.
  await waitConsole(c, /MARKERPLACE\] R7 confirmed/, 240000);
  await waitConsole(c, /marker place proof stage=r7/, 120000);
  await sleep(4000);
  const f1 = resolve(S, 'SS-R7_MARKER_PLACEMENT_CONFIRM.png');
  await c.shot(f1);
  await waitConsole(c, /MARKERPLACE\] R8 edited/, 120000);
  await waitConsole(c, /marker place proof stage=r8/, 120000);
  await sleep(4000);
  const f2 = resolve(S, 'SS-R8_ANCHOR_EDIT_PREVIEW.png');
  await c.shot(f2);
  // R9 recovery: the server holds one deterministic close oblique camera from
  // stage=r9 until after Asset B. Only the active asset changes between shots.
  await waitConsole(c, /marker place proof stage=r9/, 120000);
  await sleep(5000);
  const f4 = resolve(S, 'SS-R9-RECOVERY-01_ASSET_A_CLOSE.png');
  await c.shot(f4);
  await waitConsole(c, /MARKERPLACE\] R9 asset B active/, 120000);
  await sleep(5000);
  const f3 = resolve(S, 'SS-R9-RECOVERY-02_ASSET_B_CLOSE.png');
  await c.shot(f3);
  const f5 = resolve(S, 'SS-R9-RECOVERY-03_FINAL_OVERVIEW.png');
  await c.shot(f5);
  try {
    await waitConsole(c, /railsys: confirmed/, 30000);
    log('confirm chat confirmed');
  } catch (_) { log('WARN: confirm chat not seen'); }
  try {
    await waitConsole(c, /railsys: preview ready/, 30000);
    log('preview chat confirmed');
  } catch (_) { log('WARN: preview chat not seen'); }
  return { f1, f2, f3, f4, f5 };
}

async function runC(c, worldName, tagRe) {
  log('=== RUN C: regression ' + worldName + ' ===');
  await toTitle(c);
  await gotoCreateForm(c);
  await enterWorld(c, worldName);
  await createWorld(c);
  await waitConsole(c, tagRe, 180000);
  log('regression OK for ' + worldName);
  return true;
}

async function main() {
  ensureInstrumented();
  const guiMode = process.env.GUI_MODE || (process.env.DISPLAY ? 'gui' : 'headless');
  log('GUI_MODE=' + guiMode);

  const profileA = resolve(PHASE, 'profiles/r79clean-' + RUN_ID);
  const profileB = resolve(PHASE, 'profiles/r79place-' + RUN_ID);
  const profileC = resolve(PHASE, 'profiles/r79mc-' + RUN_ID);
  const profileD = resolve(PHASE, 'profiles/r79cr-' + RUN_ID);
  const profileE = resolve(PHASE, 'profiles/r79cg-' + RUN_ID);

  let isolationOk = false;
  let rB = { f1: null, f2: null, f3: null, f4: null, f5: null };
  let regMc = false, regCr = false, regCg = false;

  launchChrome(guiMode, profileA);
  await sleep(8000);
  const cA = new CDP(await getPage());
  await cA.connect();
  await cA.send('Runtime.enable');
  await cA.send('Page.enable');
  await cA.send('Emulation.setDeviceMetricsOverride', { width: 1208, height: 505, deviceScaleFactor: 0, mobile: false });
  try {
    isolationOk = await runA(cA, 'EaglerCleanFlat');
  } catch (e) {
    log('RUN A FAIL: ' + e.message);
  } finally {
    killChrome(profileA);
    await sleep(4000);
  }

  launchChrome(guiMode, profileB);
  await sleep(8000);
  const cB = new CDP(await getPage());
  await cB.connect();
  await cB.send('Runtime.enable');
  await cB.send('Page.enable');
  await cB.send('Emulation.setDeviceMetricsOverride', { width: 1208, height: 505, deviceScaleFactor: 0, mobile: false });
  try {
    rB = await runB(cB, 'EaglerMarkerPlace');
  } catch (e) {
    log('RUN B FAIL: ' + e.message);
  } finally {
    killChrome(profileB);
    await sleep(4000);
  }

  for (const [name, profile, re] of [
    ['EaglerMarkerCant', profileC, /MARKERARROW hook FIRED/],
    ['EaglerContinuousRail', profileD, /CONTRAIL\] render .*spans=113 expected=113/],
    ['EaglerCurveGradient', profileE, /CURVEGRAD\] render .*total=75 expected=75/],
  ]) {
    launchChrome(guiMode, profile);
    await sleep(8000);
    const c = new CDP(await getPage());
    await c.connect();
    await c.send('Runtime.enable');
    await c.send('Page.enable');
    await c.send('Emulation.setDeviceMetricsOverride', { width: 1208, height: 505, deviceScaleFactor: 0, mobile: false });
    try {
      const ok = await runC(c, name, re);
      if (name.includes('MarkerCant')) regMc = ok;
      else if (name.includes('ContinuousRail')) regCr = ok;
      else regCg = ok;
    } catch (e) {
      log('RUN C (' + name + ') FAIL: ' + e.message);
    } finally {
      killChrome(profile);
      await sleep(4000);
    }
  }

  log('=== SUMMARY ===');
  log('isolation: ' + (isolationOk ? 'OK' : 'FAIL/MISSING'));
  log('SS-R7 confirm:  ' + (rB.f1 ? 'captured' : 'MISSING'));
  log('SS-R8 edit:     ' + (rB.f2 ? 'captured' : 'MISSING'));
  log('SS-R9 asset B:  ' + (rB.f3 ? 'captured' : 'MISSING'));
  log('SS-R9 asset A:  ' + (rB.f4 ? 'captured' : 'MISSING'));
  log('SS-R7-R9 final: ' + (rB.f5 ? 'captured' : 'MISSING'));
  log('R6 markercant regression: ' + (regMc ? 'OK' : 'FAIL'));
  log('R5 continuousrail regression: ' + (regCr ? 'OK' : 'FAIL'));
  log('R4 curvegradient regression: ' + (regCg ? 'OK' : 'FAIL'));

  const ok = isolationOk && rB.f1 && rB.f2 && rB.f3 && rB.f4 && rB.f5 && regMc && regCr && regCg;
  if (ok) {
    log('=== VALIDATION VISUAL PASS (all screenshots + regressions OK) ===');
  } else {
    log('=== VALIDATION INCOMPLETE — manual review required ===');
  }
  process.exit(ok ? 0 : 1);
}

main().catch((e) => { log('FATAL ' + e.message); process.exit(1); });
