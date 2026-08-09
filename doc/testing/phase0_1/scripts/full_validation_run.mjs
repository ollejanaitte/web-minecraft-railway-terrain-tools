#!/usr/bin/env node
/**
 * Full Phase 0.1 visual validation run with known-good menu coordinates (1280x577).
 */
import { spawn } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, rmSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../../../..');
const PHASE = resolve(ROOT, 'doc/testing/phase0_1');
const S = resolve(PHASE, 'screenshots');
const PROFILE = resolve(PHASE, 'chrome-profile-run2');
const PORT = process.env.CDP_PORT || '9222';
const HTML = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const LOG = resolve(PHASE, 'full_run.log');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const log = (m) => {
  const line = `[${new Date().toISOString()}] ${m}`;
  console.log(line);
  writeFileSync(LOG, line + '\n', { flag: 'a' });
};

class CDP {
  constructor(u) {
    this.u = u;
    this.id = 0;
    this.p = new Map();
    this.lines = [];
  }
  async connect() {
    this.ws = new WebSocket(this.u);
    await new Promise((a, b) => {
      this.ws.onopen = a;
      this.ws.onerror = b;
    });
    this.ws.onmessage = (ev) => {
      const m = JSON.parse(ev.data);
      if (m.method === 'Runtime.consoleAPICalled') {
        const t = (m.params.args || []).map((a) => a.value ?? a.description ?? '').join(' ');
        this.lines.push(t);
        if (/RAILSYSTEM|auto-validat|railsysv2|integrated|Building|Preparing/i.test(t)) log('HIT ' + t.slice(0, 300));
      }
      if (m.id && this.p.has(m.id)) {
        const { resolve, reject } = this.p.get(m.id);
        this.p.delete(m.id);
        if (m.error) reject(new Error(JSON.stringify(m.error)));
        else resolve(m.result);
      }
    };
  }
  send(method, params = {}, timeoutMs = 120000) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      const t = setTimeout(() => {
        this.pendingDelete(id);
        reject(new Error('timeout ' + method));
      }, timeoutMs);
      this.p.set(id, {
        resolve: (v) => {
          clearTimeout(t);
          resolve(v);
        },
        reject: (e) => {
          clearTimeout(t);
          reject(e);
        },
      });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }
  pendingDelete(id) {
    this.p.delete(id);
  }
  async eval(e) {
    return (await this.send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true })).result
      ?.value;
  }
  async shot(f) {
    writeFileSync(f, Buffer.from((await this.send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
    log('shot ' + f);
  }
  async click(x, y) {
    log('click ' + x + ',' + y);
    await this.eval(
      `(()=>{const c=document.querySelector('canvas');const mk=t=>c.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,clientX:${x},clientY:${y},button:0,view:window}));mk('mousemove');mk('mousedown');mk('mouseup');mk('click');})()`
    );
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 });
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 });
  }
}

function analyze(path) {
  return spawnSync('python3', [resolve(PHASE, 'scripts/analyze_screen.py'), path], { encoding: 'utf8' }).stdout.trim();
}

async function getWs() {
  for (let i = 0; i < 120; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${PORT}/json`)).json();
      const page = list.find((t) => t.type === 'page');
      if (page) return page.webSocketDebuggerUrl;
    } catch (_) {}
    await sleep(500);
  }
  throw new Error('CDP unavailable');
}

function launchChrome() {
  if (existsSync(PROFILE)) rmSync(PROFILE, { recursive: true, force: true });
  mkdirSync(PROFILE, { recursive: true });
  mkdirSync(S, { recursive: true });
  const logPath = resolve(PHASE, 'chrome_run2.log');
  const args = [
    '--headless=new',
    '--no-sandbox',
    '--disable-gpu-sandbox',
    '--enable-unsafe-swiftshader',
    '--use-gl=angle',
    '--use-angle=swiftshader',
    '--autoplay-policy=no-user-gesture-required',
    '--mute-audio',
    '--disable-background-timer-throttling',
    '--disable-backgrounding-occluded-windows',
    '--disable-renderer-backgrounding',
    '--run-all-compositor-stages-before-draw',
    '--hide-scrollbars',
    '--window-size=1280,720',
    `--remote-debugging-port=${PORT}`,
    `--user-data-dir=${PROFILE}`,
    'file://' + HTML,
  ];
  const cmd = `exec /opt/google/chrome/chrome ${args.map((a) => JSON.stringify(a)).join(' ')} >${JSON.stringify(logPath)} 2>&1`;
  const child = spawn('bash', ['-c', cmd], { stdio: 'ignore', detached: true });
  child.unref();
  log('CHROME_PID=' + child.pid);
}

async function main() {
  writeFileSync(LOG, '');
  if (!process.env.SKIP_LAUNCH) {
    launchChrome();
    await sleep(5000);
  }
  const cdp = new CDP(await getWs());
  await cdp.connect();
  await cdp.send('Runtime.enable');
  await cdp.send('Page.enable');
  log('CDP connected');

  // Boot / Content Warning Done ~390
  await cdp.click(640, 360);
  await sleep(3000);
  for (let i = 0; i < 15; i++) {
    await cdp.shot(resolve(S, '_r_boot_' + i + '.png'));
    const m = analyze(resolve(S, '_r_boot_' + i + '.png'));
    log('boot ' + i + ' ' + m);
    const sky = parseFloat((m.match(/sky=([0-9.]+)/) || [])[1] || 0);
    const dirt = parseFloat((m.match(/dirt=([0-9.]+)/) || [])[1] || 0);
    if (sky > 0.15 && dirt < 0.3) break;
    await cdp.click(640, 390);
    await sleep(800);
    await cdp.click(640, 450);
    await sleep(1200);
  }
  await cdp.shot(resolve(S, 'SS-01_GAME_BOOT.png'));

  // Singleplayer ~258
  await cdp.click(640, 258);
  await sleep(5000);
  await cdp.shot(resolve(S, '_r_select.png'));
  log('select ' + analyze(resolve(S, '_r_select.png')));

  // Create New World right button
  await cdp.click(915, 492);
  await sleep(3000);
  await cdp.shot(resolve(S, '_r_submenu.png'));
  log('submenu ' + analyze(resolve(S, '_r_submenu.png')));

  // Submenu Create New World
  await cdp.click(640, 242);
  await sleep(3500);
  await cdp.shot(resolve(S, '_r_form.png'));
  log('form ' + analyze(resolve(S, '_r_form.png')));

  // Creative mode: click game mode a few times from default
  for (let i = 0; i < 3; i++) {
    await cdp.click(640, 248);
    await sleep(300);
  }

  // Confirm create left bottom
  await cdp.click(400, 540);
  await sleep(2000);
  await cdp.shot(resolve(S, '_r_loading.png'));
  log('loading ' + analyze(resolve(S, '_r_loading.png')));

  let inWorld = false;
  for (let i = 1; i <= 50; i++) {
    await sleep(8000);
    await cdp.shot(resolve(S, '_r_join_' + i + '.png'));
    const m = analyze(resolve(S, '_r_join_' + i + '.png'));
    log('join ' + i + ' ' + m);
    const sky = parseFloat((m.match(/sky=([0-9.]+)/) || [])[1] || 0);
    const dirt = parseFloat((m.match(/dirt=([0-9.]+)/) || [])[1] || 1);
    const hot = parseInt((m.match(/hot=([0-9]+)/) || [])[1] || 0, 10);
    if ((hot >= 2 && sky > 0.05 && dirt < 0.3) || cdp.lines.some((t) => /auto-validat/i.test(t))) {
      inWorld = true;
      break;
    }
  }
  if (!inWorld) throw new Error('failed in-world');
  log('IN_WORLD');
  await cdp.shot(resolve(S, 'SS-01b_IN_WORLD.png'));

  // Wait a bit for AutoValidate (80 ticks + place) then shoot series quickly
  await sleep(5000);
  for (const [n, w] of [
    ['SS-02_RAIL_STRAIGHT.png', 1000],
    ['SS-03_RAIL_CURVE.png', 5000],
    ['SS-04_FULL_SCALE_TRAIN.png', 5000],
    ['SS-05_TRAIN_ON_CURVE.png', 10000],
    ['SS-06_FORMATION.png', 8000],
    ['SS-07_PIECE_BOUNDARY.png', 12000],
    ['SS-08_EXTRA.png', 5000],
    ['SS-09_LOOKDOWN.png', 2000],
  ]) {
    await sleep(w);
    try {
      await cdp.shot(resolve(S, n));
      log('ok ' + n + ' ' + analyze(resolve(S, n)));
    } catch (e) {
      log('fail ' + n + ' ' + e.message);
    }
  }
  writeFileSync(resolve(PHASE, 'cursor_console.txt'), cdp.lines.join('\n'));
  log('SUCCESS');
}

main().catch((e) => {
  log('FATAL ' + (e.stack || e));
  process.exit(1);
});
