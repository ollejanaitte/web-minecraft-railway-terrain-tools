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
const ROOT = resolve(__dirname, '../../../../');
const PHASE = resolve(ROOT, 'doc/testing/phase0_5');
const S = resolve(PHASE, 'screenshots');
const PROFILE = process.env.CHROME_PROFILE || resolve(PHASE, 'profiles/flat-reentry');
const PORT = process.env.CDP_PORT || '9222';
const HTML = process.env.VALIDATION_HTML || resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const LOG = resolve(PHASE, 'logs/reentry.log');
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

async function findBlueButton(cdp) {
  // Detect the blue (default/hover) button band = "Play Selected World".
  const p = resolve(S, '_re_blue.png');
  try { await cdp.shot(p); } catch (e) { return null; }
  const r = spawnSync('python3', ['-c', `
import sys
from PIL import Image
im = Image.open("${p}").convert("RGB")
px = im.load(); w, h = im.size
rows = []
for y in range(140, h - 20, 2):
    cnt = 0; xs = []
    for x in range(380, 900, 3):
        r, g, b = px[x, y]
        if b > 120 and b > r + 40 and b > g + 30:
            cnt += 1; xs.append(x)
    if cnt >= 20:
        rows.append((y, (min(xs) + max(xs)) // 2))
# cluster rows into button bands
best = None
cur = []
for (y, cx) in rows:
    if cur and y - cur[-1][0] > 8:
        if len(cur) >= 6:
            cxs = [c for _, c in cur]
            best = ((sum(cxs) // len(cxs)), (cur[0][0] + cur[-1][0]) // 2)
            break
        cur = []
    cur.append((y, cx))
if cur and len(cur) >= 6:
    cxs = [c for _, c in cur]
    best = ((sum(cxs) // len(cxs)), (cur[0][0] + cur[-1][0]) // 2)
print((best[0], best[1]) if best else "none")
`], { encoding: 'utf8' });
  const out = r.stdout.trim();
  if (out && out !== 'none') {
    const m = out.match(/(\d+),\s*(\d+)/);
    if (m) return [Number(m[1]), Number(m[2])];
  }
  return null;
}

async function findBlueThumbnail(cdp) {
  // Detect the world-entry blue thumbnail in the select-world screen.
  const p = resolve(S, '_re_bluescan.png');
  try { await cdp.shot(p); } catch (e) { return null; }
  const r = spawnSync('python3', ['-c', `
import sys
from PIL import Image
im = Image.open("${p}").convert("RGB")
px = im.load(); w, h = im.size
best = None
for y in range(80, min(420, h), 3):
    xs = [x for x in range(220, 460, 3) if px[x, y][2] > 130 and px[x, y][2] > px[x, y][0] + 40]
    if len(xs) >= 8:
        best = ((min(xs) + max(xs)) // 2, y)
print((best[0], best[1]) if best else "none")
`], { encoding: 'utf8' });
  const out = r.stdout.trim();
  if (out && out !== 'none') {
    const m = out.match(/(\d+),\s*(\d+)/);
    if (m) return [Number(m[1]), Number(m[2])];
  }
  return null;
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
  // Phase 0.5 re-entry: play the EXISTING flat validation world.
  await cdp.click(640, 258); // Singleplayer
  await sleep(12000);
  await cdp.shot(resolve(S, '_re_select.png'));
  const sel = analyze(resolve(S, '_re_select.png'));
  log('select ' + sel);

  // Click the world entry (blue world thumbnail region) to select it, then
  // click "Play Selected World" (a top button band).
  // The world entry thumbnail sits in the upper-middle area of the select
  // screen; click a few candidate positions, then Play.
  const tryClick = async (x, y) => { await cdp.click(x, y); await sleep(2500); };
  await tryClick(360, 250);  // world entry thumbnail
  await cdp.shot(resolve(S, '_re_sel2.png'));
  const sel2 = analyze(resolve(S, '_re_sel2.png'));
  log('after entry click ' + sel2);
  // Detect buttons; "Play Selected World" is the top-most button band.
  const b = (sel2.match(/buttons=([\d,]+)/) || [])[1] || '';
  const bs = b ? b.split(',').map(Number) : [];
  const playY = bs.length ? bs[0] : 340;
  log('play at y=' + playY);
  await cdp.click(640, playY);
  await sleep(15000);
  await cdp.shot(resolve(S, '_re_loading.png'));
  log('loading ' + analyze(resolve(S, '_re_loading.png')));
  return 're-entered';
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
    // Fast-fail: a sustained menu/form-like state (not loading, not in-world)
    // means the join was interrupted; fail so the retry loop proceeds promptly.
    const menuLike = sky > 0.5 || (dirt > 0.6 && /buttons=/.test(m));
    if (menuLike) {
      menuStreak++;
      if (menuStreak >= 4) {
        throw new Error('stuck in menu after join (menu-recovery race)');
      }
    } else {
      menuStreak = 0;
    }
  }
  return { validated: false, inworld: false };
}

async function captureSeries(cdp) {
  // Phase 0.5 re-entry camera-tour shots (SS-FLAT-RE-xx).
  await sleep(1000);
  await cdp.shot(resolve(S, 'SS-FLAT-RE_01_REENTRY.png'));
  log('ok SS-FLAT-RE_01 ' + analyze(resolve(S, 'SS-FLAT-RE_01_REENTRY.png')));
  await sleep(4000);
  await cdp.shot(resolve(S, 'SS-FLAT-RE_02_FLAT_GROUND.png'));
  log('ok SS-FLAT-RE_02 ' + analyze(resolve(S, 'SS-FLAT-RE_02_FLAT_GROUND.png')));
  for (const [n, w] of [
    ['SS-FLAT-RE_03_STRAIGHT_RAIL.png', 4000],
    ['SS-FLAT-RE_04_FULL_SCALE_TRAIN.png', 7000],
    ['SS-FLAT-RE_05_FORMATION.png', 7000],
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
  log('START reentry PROFILE=' + PROFILE);
  if (!process.env.SKIP_LAUNCH) {
    launchChrome();
    await sleep(6000);
  }
  const cdp = new CDP(await getWs());
  await cdp.connect();
  await cdp.send('Runtime.enable');
  await cdp.send('Page.enable');
  await waitTitle(cdp);
  // Retry the Play navigation until the world stays in-world and AutoValidate
  // fires (the known menu-recovery race can bounce the client back to menu).
  let success = false;
  for (let attempt = 1; attempt <= 8; attempt++) {
    log('re-entry attempt ' + attempt);
    await cdp.click(640, 258); // Singleplayer
    await sleep(12000);
    // Select the (only) flat world entry by clicking its name row, then click
    // the blue "Play Selected World" button that appears in the world-info view.
    await cdp.click(640, 100); // world name row -> world-info view
    await sleep(5000);
    let inw = { inworld: false, validated: false };
    const blue = await findBlueButton(cdp);
    if (blue) {
      log('blue Play button at ' + blue[0] + ',' + blue[1]);
      await cdp.click(blue[0], blue[1]);
      await sleep(12000);
      inw = await waitInWorld(cdp);
    } else {
      const sweep = [[640, 344], [640, 450], [640, 244], [640, 292], [640, 424], [560, 492]];
      for (const [x, y] of sweep) {
        await cdp.click(x, y);
        await sleep(8000);
        const p = resolve(S, '_re_sweep.png');
        try { await cdp.shot(p); } catch (_) { continue; }
        const m = analyze(p);
        const sky = parseFloat((m.match(/sky=([0-9.]+)/) || [])[1] || 0);
        const hot = parseInt((m.match(/hot=([0-9]+)/) || [])[1] || '0', 10);
        const val = cdp.lines.some((t) => /auto-validat/i.test(t));
        log('sweep(' + x + ',' + y + ') sky=' + sky + ' hot=' + hot + (val ? ' VALIDATED' : ''));
        if ((sky > 0.15 && hot >= 2) || val) {
          inw = await waitInWorld(cdp);
          break;
        }
      }
    }
    if (inw.inworld && inw.validated) {
      log('RE-ENTRY SUCCESS attempt=' + attempt);
      success = true;
      break;
    }
    log('attempt ' + attempt + ' not validated, reloading');
    await cdp.send('Page.reload', { ignoreCache: true });
    await sleep(20000);
    await waitTitle(cdp);
  }
  if (!success) throw new Error('re-entry failed after attempts');
  await captureSeries(cdp);
  writeFileSync(resolve(PHASE, 'cursor_console.txt'), cdp.lines.join('\n'));
  log('SUCCESS');
  process.exit(0);
}

main().catch((e) => {
  log('FATAL ' + (e.stack || e));
  process.exit(1);
});
