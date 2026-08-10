#!/usr/bin/env node
/**
 * Phase 0.1 retry: slower world create/join to avoid WORLD_UNLOADING crash.
 * Prefers existing world Play if present; otherwise Create path with long waits.
 */
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../../../..');
const PHASE = resolve(ROOT, 'doc/testing/phase0_2');
const S = resolve(PHASE, 'screenshots');
const PROFILE = process.env.CHROME_PROFILE || resolve(PHASE, 'chrome-profile-run');
const PORT = process.env.CDP_PORT || '9222';
const HTML = process.env.VALIDATION_HTML || resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const LOG = resolve(PHASE, 'retry_run.log');
const GPU_MODE = process.env.GPU_MODE || 'auto';
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
        if (/RAILSYSTEM|auto-validat|railsysv2|integrated|Building|Preparing|Crash|ACK/i.test(t)) {
          log('HIT ' + t.slice(0, 320));
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
  send(method, params = {}, timeoutMs = 120000) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      const t = setTimeout(() => {
        this.p.delete(id);
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
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 });
  }
}

function analyze(path) {
  return spawnSync('python3', [resolve(PHASE, 'scripts/analyze_screen.py'), path], { encoding: 'utf8' }).stdout.trim();
}

async function getWs() {
  for (let i = 0; i < 180; i++) {
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
  mkdirSync(PROFILE, { recursive: true });
  mkdirSync(S, { recursive: true });
  const logPath = resolve(PHASE, 'chrome_retry.log');
  const gpuFlags = GPU_MODE === 'swiftshader'
    ? ['--disable-gpu-sandbox', '--enable-unsafe-swiftshader', '--use-gl=angle', '--use-angle=swiftshader']
    : ['--use-gl=angle', '--use-angle=vulkan'];
  const args = [
    '--headless=new',
    '--no-sandbox',
    ...gpuFlags,
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
  log('CHROME_PID=' + child.pid + ' PROFILE=' + PROFILE);
}

async function waitTitle(cdp) {
  await cdp.click(640, 360);
  await sleep(2500);
  for (let i = 0; i < 24; i++) {
    const p = resolve(S, '_retry_boot_' + i + '.png');
    await cdp.shot(p);
    const m = analyze(p);
    log('boot ' + i + ' ' + m);
    const sky = parseFloat((m.match(/sky=([0-9.]+)/) || [])[1] || 0);
    const dirt = parseFloat((m.match(/dirt=([0-9.]+)/) || [])[1] || 0);
    // Crash UI leftover: only reload when console already reported a crash.
    if (
      i > 0 &&
      sky < 0.05 &&
      dirt > 0.7 &&
      cdp.lines.some((t) => /Game Crashed|WORLD_UNLOADING|fallen and I can't/i.test(t))
    ) {
      log('crash UI — Page.reload');
      await cdp.send('Page.reload', { ignoreCache: true });
      await sleep(8000);
      continue;
    }
    if (sky > 0.15 && dirt < 0.3) {
      await cdp.shot(resolve(S, 'SS-01_GAME_BOOT.png'));
      return;
    }
    // Content Warning / Edit Profile Done
    await cdp.click(640, 390);
    await sleep(1000);
    await cdp.click(640, 450);
    await sleep(1500);
  }
  throw new Error('title timeout');
}

async function enterWorld(cdp) {
  // Create-only path (no Play-first). Play-then-Create races WORLD_UNLOADING.
  await cdp.click(640, 258); // Singleplayer
  await sleep(12000);
  await cdp.shot(resolve(S, '_retry_select.png'));
  log('select ' + analyze(resolve(S, '_retry_select.png')));

  await cdp.click(915, 492); // Create New World
  await sleep(8000);
  await cdp.shot(resolve(S, '_retry_submenu.png'));
  log('submenu ' + analyze(resolve(S, '_retry_submenu.png')));

  await cdp.click(640, 242); // Create
  await sleep(10000);
  await cdp.shot(resolve(S, '_retry_form.png'));
  log('form ' + analyze(resolve(S, '_retry_form.png')));

  // Keep default Survival — AutoValidate enables flight. Do NOT mash Game Mode.
  // Phase 0.2: set the validation world name so the AutoValidate gate fires.
  // The name field holds default text around y~138; click it, select-all,
  // then type char-by-char (Input.insertText is unreliable for the canvas
  // text field; per-char key events work).
  const typeChar = async (ch) => {
    const vk = ch.codePointAt(0);
    await cdp.send('Input.dispatchKeyEvent', { type: 'keyDown', key: ch, code: 'Key' + ch.toUpperCase(), text: ch, windowsVirtualKeyCode: vk });
    await cdp.send('Input.dispatchKeyEvent', { type: 'char', key: ch, text: ch, unmodifiedText: ch, windowsVirtualKeyCode: vk });
    await cdp.send('Input.dispatchKeyEvent', { type: 'keyUp', key: ch, code: 'Key' + ch.toUpperCase(), windowsVirtualKeyCode: vk });
  };
  await cdp.click(495, 138); // world name field
  await sleep(800);
  await cdp.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'a', code: 'KeyA', modifiers: 2, windowsVirtualKeyCode: 65 });
  await cdp.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'a', code: 'KeyA', modifiers: 2, windowsVirtualKeyCode: 65 });
  await sleep(300);
  for (const ch of 'EaglerValidateFlat') {
    await typeChar(ch);
    await sleep(40);
  }
  await sleep(1000);
  // PROBE: instead of confirming, click each form button and screenshot
  for (const [label, x, y] of [['b248', 640, 248], ['b392', 640, 392], ['b540', 400, 540]]) {
    await cdp.shot(resolve(S, '_probe_' + label + '_before.png'));
    await cdp.click(x, y);
    await sleep(6000);
    await cdp.shot(resolve(S, '_probe_' + label + '_after.png'));
    log('PROBE ' + label + ' after: ' + analyze(resolve(S, '_probe_' + label + '_after.png')));
  }
  await sleep(3000);
  return 'probed';
  await cdp.shot(resolve(S, '_retry_loading.png'));
  log('loading ' + analyze(resolve(S, '_retry_loading.png')));
  return 'created';
}

async function waitInWorld(cdp) {
  let menuStreak = 0;
  for (let i = 1; i <= 60; i++) {
    await sleep(10000);
    const p = resolve(S, '_retry_join_' + i + '.png');
    try {
      await cdp.shot(p);
    } catch (e) {
      log('shot fail ' + e.message);
      continue;
    }
    const m = analyze(p);
    log('join ' + i + ' ' + m);
    const sky = parseFloat((m.match(/sky=([0-9.]+)/) || [])[1] || 0);
    const dirt = parseFloat((m.match(/dirt=([0-9.]+)/) || [])[1] || 1);
    const hot = parseInt((m.match(/hot=([0-9]+)/) || [])[1] || 0, 10);
    // Real crash = crash report overlay; tolerant-assert messages are benign.
    if (cdp.lines.some((t) => /Game Crashed|Minecraft Crash Report|Recieved ACK \d+ while the client state/i.test(t) && !/tolerant-assert/i.test(t))) {
      throw new Error('game crashed during join');
    }
    // Require AutoValidate chat OR a grounded in-world view (not sky-only).
    const validated = cdp.lines.some((t) => /auto-validat/i.test(t));
    const gateLine = cdp.lines.filter((t) => /worldName=.*validation=/.test(t)).pop() || '';
    const grounded = hot >= 2 && sky > 0.05 && sky < 0.75 && dirt > 0.15 && dirt < 0.55;
    if (validated || grounded) {
      log('IN_WORLD validated=' + validated + ' grounded=' + grounded + (gateLine ? ' gate=' + gateLine.slice(-80) : ''));
      await cdp.shot(resolve(S, 'SS-01b_IN_WORLD.png'));
      return { validated, inworld: true };
    }
    // Fast-fail: a sustained menu-like state after create means the create was
    // interrupted by the known race; fail so the launcher retries promptly
    // instead of burning the full 600s window.
    const menuLike = sky > 0.5 || (dirt > 0.6 && hot < 5 && /buttons=/.test(m));
    if (menuLike) {
      menuStreak++;
      if (menuStreak >= 5) {
        throw new Error('stuck in menu after create (menu-recovery race)');
      }
    } else {
      menuStreak = 0;
    }
  }
  return { validated: false, inworld: false };
}

async function captureSeries(cdp) {
  // Align with RailV2AutoValidate camera tour (~20 tps):
  // t20 formation_side, t200 along_track, t360 close, t520 curve, t700 piece3
  await sleep(3000); // ~tour tick 20+
  for (const [n, w] of [
    ['SS-02_RAIL_STRAIGHT.png', 500],
    ['SS-03_RAIL_CURVE.png', 2000],
    ['SS-04_FULL_SCALE_TRAIN.png', 6500],
    ['SS-05_TRAIN_ON_CURVE.png', 8000],
    ['SS-06_FORMATION.png', 8000],
    ['SS-07_PIECE_BOUNDARY.png', 9000],
    ['SS-08_EXTRA.png', 8000],
  ]) {
    await sleep(w);
    try {
      await cdp.shot(resolve(S, n));
      log('ok ' + n + ' ' + analyze(resolve(S, n)));
    } catch (e) {
      log('fail ' + n + ' ' + e.message);
    }
  }
}

async function main() {
  writeFileSync(LOG, '');
  log('START retry PROFILE=' + PROFILE);
  if (!process.env.SKIP_LAUNCH) {
    launchChrome();
    await sleep(6000);
  }
  const cdp = new CDP(await getWs());
  await cdp.connect();
  await cdp.send('Runtime.enable');
  await cdp.send('Page.enable');
  await waitTitle(cdp);
  const how = await enterWorld(cdp);
  log('enter via ' + how);
  const inw = await waitInWorld(cdp);
  if (!inw.inworld) throw new Error('failed in-world');
  if (!inw.validated) {
    writeFileSync(resolve(PHASE, 'cursor_console.txt'), cdp.lines.join('\n'));
    throw new Error('AutoValidate did NOT fire (world name gate blocked) - validation FAIL');
  }
  await captureSeries(cdp);
  writeFileSync(resolve(PHASE, 'cursor_console.txt'), cdp.lines.join('\n'));
  log('SUCCESS');
  process.exit(0);
}

main().catch((e) => {
  log('FATAL ' + (e.stack || e));
  process.exit(1);
});
