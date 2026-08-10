#!/usr/bin/env node
/**
 * Phase 0.2 one-command AI validation runner.
 * Launches Chrome on the Eaglercraft offline HTML, boots to title, creates a
 * fresh world, waits for RailV2AutoValidate, captures SS screenshots + console
 * + CPU samples, then cleans up.
 *
 * Env:
 *   GPU_MODE=auto|vulkan|swiftshader (default auto -> vulkan)
 *   CHROME_BIN=/opt/google/chrome/chrome
 *   CDP_PORT (default 9222)
 *   PROFILE_DIR (default <phase0_2>/profiles/run)
 *   KEEP_PROFILE=1 to keep profile (for debugging)
 *   MAX_TOTAL_SEC (default 900)
 *   SS_DIR (screenshots out)
 *   LOG_DIR (logs out)
 *   SKIP_LAUNCH=1 (attach to running chrome on CDP_PORT)
 */
import { spawn, execSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, rmSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const PHASE = resolve(__dirname, '..');
const ROOT = resolve(__dirname, '../../../../');
const CHROME = process.env.CHROME_BIN || '/opt/google/chrome/chrome';
const PORT = process.env.CDP_PORT || '9222';
const GPU_MODE = process.env.GPU_MODE || 'auto';
const PROFILE = process.env.PROFILE_DIR || resolve(PHASE, 'profiles/run');
const SS = process.env.SS_DIR || resolve(PHASE, 'screenshots');
const LOGD = process.env.LOG_DIR || resolve(PHASE, 'logs');
const MAX_TOTAL = Number(process.env.MAX_TOTAL_SEC || 900) * 1000;
const HTML = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const SLEEP = (ms) => new Promise((r) => setTimeout(r, ms));
const RUNID = new Date().toISOString().replace(/[:.]/g, '-');

function log(m) {
  const line = `[${new Date().toISOString()}] ${m}`;
  console.log(line);
  try { writeFileSync(resolve(LOGD, 'run_validation.log'), line + '\n', { flag: 'a' }); } catch (_) {}
}

function pickFlags() {
  const base = [
    '--headless=new',
    '--no-sandbox',
    '--autoplay-policy=no-user-gesture-required',
    '--mute-audio',
    '--disable-background-timer-throttling',
    '--disable-backgrounding-occluded-windows',
    '--disable-renderer-backgrounding',
    '--hide-scrollbars',
    '--window-size=1280,720',
  ];
  if (GPU_MODE === 'swiftshader') {
    return [...base, '--disable-gpu-sandbox', '--enable-unsafe-swiftshader', '--use-gl=angle', '--use-angle=swiftshader'];
  }
  return [...base, '--use-gl=angle', '--use-angle=vulkan'];
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
        if (/RAILSYSTEM|auto-validat|railsysv2|integrated|Building|Preparing|Crash|ACK|Starting|Done \(/i.test(t)) {
          log('CONSOLE ' + t.slice(0, 300));
        }
      }
      if (m.id && this.p.has(m.id)) {
        const { resolve, reject } = this.p.get(m.id);
        this.p.delete(m.id);
        if (m.error) reject(new Error(JSON.stringify(m.error)));
        else resolve(m.result);
      }
    };
  }
  send(method, params = {}, timeoutMs = 60000) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      const t = setTimeout(() => { this.p.delete(id); reject(new Error('timeout ' + method)); }, timeoutMs);
      this.p.set(id, {
        resolve: (v) => { clearTimeout(t); resolve(v); },
        reject: (e) => { clearTimeout(t); reject(e); },
      });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }
  async eval(e, t = 60000) {
    try {
      const r = await this.send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true }, t);
      return r.result ? r.result.value : r.exceptionDetails;
    } catch (err) { return { __err: err.message }; }
  }
  async shot(f) {
    const r = await this.send('Page.captureScreenshot', { format: 'png' }, 30000);
    writeFileSync(f, Buffer.from(r.data, 'base64'));
    return f;
  }
  async click(x, y) {
    await this.eval(`(()=>{const c=document.querySelector('canvas');for(const t of ['mousemove','mousedown','mouseup','click'])c.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,clientX:${x},clientY:${y},button:0,view:window}));})()`);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await SLEEP(60);
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 });
    await SLEEP(60);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 });
  }
  async key(k, code, vk) {
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: k, code, windowsVirtualKeyCode: vk });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: k, code, windowsVirtualKeyCode: vk });
  }
  async insertText(t) { await this.send('Input.insertText', { text: t }); }
}

async function getWs() {
  for (let i = 0; i < 240; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${PORT}/json`)).json();
      const page = list.find((t) => t.type === 'page');
      if (page) return page.webSocketDebuggerUrl;
    } catch (_) {}
    await SLEEP(500);
  }
  throw new Error('CDP unavailable');
}

function analyze(path) {
  try {
    return execSync(`python3 ${JSON.stringify(resolve(PHASE, 'scripts/analyze_buttons.py'))} ${JSON.stringify(path)}`, { encoding: 'utf8' }).trim();
  } catch (e) { return 'ERR ' + e.message; }
}

function launchChrome() {
  mkdirSync(PROFILE, { recursive: true });
  mkdirSync(SS, { recursive: true });
  mkdirSync(LOGD, { recursive: true });
  const flags = pickFlags();
  const args = [...flags, `--remote-debugging-port=${PORT}`, `--user-data-dir=${resolve(PROFILE)}`, 'file://' + HTML];
  const logPath = resolve(LOGD, `chrome_${RUNID}.log`);
  const cmd = `exec ${JSON.stringify(CHROME)} ${args.map((a) => JSON.stringify(a)).join(' ')} >${JSON.stringify(logPath)} 2>&1`;
  const child = spawn('bash', ['-c', cmd], { stdio: 'ignore', detached: true });
  child.unref();
  log('CHROME_LAUNCH pid=' + child.pid + ' mode=' + GPU_MODE + ' port=' + PORT + ' profile=' + PROFILE);
  return child.pid;
}

async function waitTitle(cdp) {
  const t0 = Date.now();
  let last = '';
  let menuSeen = false;
  while (Date.now() - t0 < 900000) {
    // click center to unblock render, then dialog-dismiss clicks
    await cdp.click(640, 360);
    await SLEEP(1500);
    if (!menuSeen) {
      await cdp.click(640, 390);
      await SLEEP(700);
      await cdp.click(640, 450);
      await SLEEP(700);
    }
    const p = resolve(SS, '_boot.png');
    try { await cdp.shot(p); } catch (_) { continue; }
    last = analyze(p);
    log('boot ' + last);
    const m = last.match(/state=(\S+)/);
    const state = m ? m[1] : '';
    const btns = (last.match(/buttons=([\d,]+)/) || [])[1] || '';
    const count = btns ? btns.split(',').length : 0;
    const dirt = parseFloat((last.match(/dirt=([\d.]+)/) || [])[1] || '0');
    const sky = parseFloat((last.match(/sky=([\d.]+)/) || [])[1] || '0');
    // Menu = dirt dialog with buttons OR cloud background menu (not the pure
    // loading dark screen, not the in-world sky).
    if (!/loading|unknown/.test(state) && count >= 1 && !(sky > 0.3 && dirt < 0.1)) {
      log('MAIN_MENU ' + last);
      return btns.split(',').map(Number);
    }
    menuSeen = true;
  }
  throw new Error('title timeout last=' + last);
}

// Navigation helpers -------------------------------------------------------
async function clickAndWait(cdp, x, y, waitMs) { await cdp.click(x, y); await SLEEP(waitMs); }
async function shotAndAnalyze(cdp, name) {
  const p = resolve(SS, name);
  try { await cdp.shot(p); } catch (_) {}
  return analyze(p);
}

async function createWorldPath(cdp, menuButtons) {
  // From main menu click Singleplayer = topmost button.
  const spY = menuButtons.length ? menuButtons[0] : 258;
  await clickAndWait(cdp, 640, spY, 10000);
  let a = await shotAndAnalyze(cdp, '_select.png');
  log('select ' + a);
  const buttons1 = (a.match(/buttons=([\d,]+)/) || [])[1] || '';
  const btns1 = buttons1 ? buttons1.split(',').map(Number) : [];
  // On the Singleplayer screen click the bottom-most button ('Create New World').
  const cnwY = btns1.length ? btns1[btns1.length - 1] : 492;
  await clickAndWait(cdp, 915, cnwY, 8000);
  a = await shotAndAnalyze(cdp, '_submenu.png');
  log('submenu ' + a);
  const buttons2 = (a.match(/buttons=([\d,]+)/) || [])[1] || '';
  const btns2 = buttons2 ? buttons2.split(',').map(Number) : [];
  // Submenu: 'Create' is the top-most button band.
  const createY = btns2.length ? btns2[0] : 242;
  await clickAndWait(cdp, 640, createY, 8000);
  a = await shotAndAnalyze(cdp, '_form.png');
  log('form ' + a);
  // World name field near y~165; type the validation marker name then confirm.
  await cdp.click(640, 165);
  await SLEEP(800);
  await cdp.insertText('EaglerValidate');
  await SLEEP(1000);
  await cdp.key('Enter', 'Enter', 13);
  await SLEEP(2000);
  const buttons3 = (a.match(/buttons=([\d,]+)/) || [])[1] || '';
  const btns3 = buttons3 ? buttons3.split(',').map(Number) : [];
  const confirmY = btns3.length ? btns3[btns3.length - 1] : 540;
  await clickAndWait(cdp, 400, confirmY, 6000);
  await cdp.click(640, 360);
  await SLEEP(8000);
  return true;
}

async function waitInWorld(cdp) {
  const t0 = Date.now();
  let grounded = false;
  while (Date.now() - t0 < 600000) {
    await SLEEP(8000);
    const p = resolve(SS, '_join.png');
    try { await cdp.shot(p); } catch (_) { continue; }
    const m = analyze(p);
    const sky = parseFloat((m.match(/sky=([\d.]+)/) || [])[1] || 0);
    const dirt = parseFloat((m.match(/dirt=([\d.]+)/) || [])[1] || 0);
    const hot = parseInt((m.match(/hot=(\d+)/) || [])[1] || '0', 10);
    if (cdp.lines.some((t) => /Game Crashed|WORLD_UNLOADING/i.test(t))) {
      throw new Error('game crashed during join');
    }
    const validated = cdp.lines.some((t) => /auto-validat/i.test(t));
    const inworld = validated || (hot >= 2 && sky > 0.05 && sky < 0.8 && dirt > 0.1 && dirt < 0.6);
    if (inworld) {
      log('IN_WORLD validated=' + validated + ' sky=' + sky.toFixed(2) + ' hot=' + hot);
      return { validated, inworld: true };
    }
  }
  return { validated: false, inworld: false };
}

async function captureSeries(cdp) {
  await SLEEP(3000);
  const shots = [
    ['SS-01_GAME_BOOT.png', 0],
    ['SS-02_RAIL_STRAIGHT.png', 2500],
    ['SS-03_RAIL_CURVE.png', 5000],
    ['SS-04_FULL_SCALE_TRAIN.png', 7000],
    ['SS-05_TRAIN_ON_CURVE.png', 7000],
    ['SS-06_FORMATION.png', 7000],
    ['SS-07_PIECE_BOUNDARY.png', 8000],
  ];
  for (const [n, w] of shots) {
    await SLEEP(w);
    try { await cdp.shot(resolve(SS, n)); log('shot ' + n); } catch (e) { log('shot fail ' + n + ' ' + e.message); }
  }
}

// CPU sampling ------------------------------------------------------------
function sampleChrome() {
  let g = '';
  try { g = execSync(`pgrep -af chrome`, { encoding: 'utf8' }); } catch (_) {}
  const rows = [];
  for (const line of g.split('\n')) {
    const m = line.match(/^(\d+)\s+(.*)$/);
    if (!m) continue;
    const args = m[2];
    if (!args.includes(HTML) && !args.includes(PROFILE)) continue;
    let cpu = 0, rss = 0;
    try { const s = execSync(`ps -o pcpu,rss --no-headers -p ${m[1]}`, { encoding: 'utf8' }); const mm = s.trim().match(/^\s*([\d.]+)\s+(\d+)/); if (mm) { cpu = Number(mm[1]); rss = Number(mm[2]) / 1024; } } catch (_) {}
    let kind = 'browser';
    if (args.includes('--type=gpu-process')) kind = 'gpu';
    else if (args.includes('--type=renderer')) kind = 'renderer';
    else if (args.includes('--type=utility')) kind = 'utility';
    rows.push({ pid: Number(m[1]), kind, cpu, rssMB: rss });
  }
  return rows;
}

function cleanup(browserPid) {
  log('cleanup start');
  const targets = [];
  try {
    const g = execSync(`pgrep -af chrome`, { encoding: 'utf8' });
    for (const line of g.split('\n')) {
      const m = line.match(/^(\d+)\s+(.*)$/);
      if (!m) continue;
      const pid = Number(m[1]);
      if (pid === browserPid || m[2].includes(PROFILE) || (m[2].includes('file://' + HTML) && !m[2].includes('--type'))) targets.push(pid);
    }
  } catch (_) {}
  for (const pid of targets) { try { process.kill(pid, 'SIGTERM'); } catch (_) {} }
  SLEEP(4000).then(() => {
    for (const pid of targets) { try { process.kill(pid, 'SIGKILL'); } catch (_) {} }
    if (process.env.KEEP_PROFILE !== '1') { try { rmSync(PROFILE, { recursive: true, force: true }); } catch (_) {} }
  });
}

async function main() {
  mkdirSync(LOGD, { recursive: true });
  writeFileSync(resolve(LOGD, 'run_validation.log'), '');
  const result = { runId: RUNID, mode: GPU_MODE, startedAt: Date.now() };
  const t0 = Date.now();
  let browserPid = null;
  const deadline = setInterval(() => {
    if (Date.now() - t0 > MAX_TOTAL) {
      log('HARD_TIMEOUT exceeded');
      result.error = 'hard timeout';
      const out = resolve(LOGD, `result_${RUNID}.json`);
      writeFileSync(out, JSON.stringify(result, null, 2));
      cleanup(browserPid);
      process.exit(2);
    }
  }, 30000);
  try {
    if (!process.env.SKIP_LAUNCH) {
      browserPid = launchChrome();
      await SLEEP(6000);
    }
    const cdp = new CDP(await getWs());
    await cdp.connect();
    await cdp.send('Runtime.enable');
    await cdp.send('Page.enable');
    const renderer = await cdp.eval(`(()=>{const c=document.createElement('canvas');const gl=c.getContext('webgl');const ext=gl.getExtension('WEBGL_debug_renderer_info');return ext?gl.getParameter(ext.UNMASKED_RENDERER_WEBGL):String(gl.getParameter(gl.RENDERER));})()`);
    result.renderer = renderer;
    log('RENDERER ' + renderer);

    const menuButtons = await waitTitle(cdp);
    result.titleSec = (Date.now() - t0) / 1000;
    await createWorldPath(cdp, menuButtons);
    const inw = await waitInWorld(cdp);
    result.inWorld = inw;
    if (inw.inworld) {
      await captureSeries(cdp);
      result.captureDone = true;
    }
    result.elapsedSec = (Date.now() - t0) / 1000;
    result.consoleLines = cdp.lines;
  } catch (e) {
    result.error = String(e.stack || e);
    log('FATAL ' + (e.stack || e));
    try {
      const st = await (async () => {
        const cdp = new CDP(await getWs());
        await cdp.connect();
        const p = resolve(SS, '_timeout.png');
        await cdp.shot(p);
        return analyze(p);
      })();
      result.timeoutScreen = st;
    } catch (_) {}
  }
  // final CPU sample
  result.cpu = sampleChrome();
  result.endedAt = Date.now();
  const out = resolve(LOGD, `result_${RUNID}.json`);
  writeFileSync(out, JSON.stringify(result, null, 2));
  log('RESULT ' + out);
  cleanup(browserPid);
  await SLEEP(6000);
  clearInterval(deadline);
  console.log(JSON.stringify({ verdict: result.inWorld && result.inWorld.validated ? 'PASS' : (result.inWorld && result.inWorld.inworld ? 'IN_WORLD_NO_VALIDATE' : 'FAIL'), renderer: result.renderer, elapsedSec: Math.round(result.elapsedSec || 0), error: result.error || null, resultFile: out }, null, 2));
  process.exit(result.inWorld && result.inWorld.validated ? 0 : 1);
}

main().catch((e) => { log('FATAL2 ' + (e.stack || e)); process.exit(1); });
