#!/usr/bin/env node
/**
 * Phase 0.1 visual validation orchestrator.
 * Reuses CDP patterns from cdp.mjs; drives boot -> world -> screenshots.
 *
 * Usage:
 *   node run_visual_validation.mjs
 *
 * Env:
 *   CDP_PORT=9222
 *   SKIP_LAUNCH=1   (attach to already-running Chrome)
 */
import { spawn } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, createWriteStream } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../../../..');
const PHASE = resolve(ROOT, 'doc/testing/phase0_1');
const SHOTS = resolve(PHASE, 'screenshots');
const PROFILE = resolve(PHASE, 'chrome-profile-cursor');
const LOG = resolve(PHASE, 'cursor_validation.log');
const CONSOLE_LOG = resolve(PHASE, 'cursor_console.txt');
const PORT = process.env.CDP_PORT || '9222';
const HTML = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const URL = 'file://' + HTML;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const log = (m) => {
  const line = `[${new Date().toISOString()}] ${m}`;
  console.log(line);
  writeFileSync(LOG, line + '\n', { flag: 'a' });
};

async function getPageWsUrl() {
  for (let i = 0; i < 120; i++) {
    try {
      const res = await fetch(`http://127.0.0.1:${PORT}/json`);
      const list = await res.json();
      const page = list.find((t) => t.type === 'page');
      if (page) return page.webSocketDebuggerUrl;
    } catch (_) {}
    await sleep(500);
  }
  throw new Error('CDP not available');
}

class CDP {
  constructor(wsUrl) {
    this.wsUrl = wsUrl;
    this.id = 0;
    this.pending = new Map();
    this.consoleLines = [];
  }
  async connect() {
    this.ws = new WebSocket(this.wsUrl);
    await new Promise((res, rej) => {
      this.ws.onopen = res;
      this.ws.onerror = rej;
    });
    this.ws.onmessage = (ev) => {
      const msg = JSON.parse(ev.data);
      if (msg.method === 'Runtime.consoleAPICalled') {
        const vals = (msg.params.args || []).map((a) => a.value ?? a.description ?? '').join(' ');
        this.consoleLines.push(vals);
        if (/RAILSYSTEM|auto-validat|railsysv2|RailV2/i.test(vals)) {
          log('CONSOLE_HIT: ' + vals.slice(0, 240));
        }
      }
      if (msg.method === 'Log.entryAdded') {
        const t = msg.params?.entry?.text || '';
        this.consoleLines.push(t);
      }
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject } = this.pending.get(msg.id);
        this.pending.delete(msg.id);
        if (msg.error) reject(new Error(JSON.stringify(msg.error)));
        else resolve(msg.result);
      }
    };
    await this.send('Runtime.enable');
    await this.send('Log.enable');
    await this.send('Page.enable');
  }
  send(method, params = {}, timeoutMs = 60000) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(id);
        reject(new Error('CDP timeout ' + method));
      }, timeoutMs);
      this.pending.set(id, {
        resolve: (v) => {
          clearTimeout(timer);
          resolve(v);
        },
        reject: (e) => {
          clearTimeout(timer);
          reject(e);
        },
      });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }
  async eval(expr, timeoutMs = 30000) {
    const r = await this.send(
      'Runtime.evaluate',
      { expression: expr, returnByValue: true, awaitPromise: true },
      timeoutMs
    );
    return r.result ? r.result.value : undefined;
  }
  async shot(file) {
    const r = await this.send('Page.captureScreenshot', { format: 'png' }, 90000);
    writeFileSync(file, Buffer.from(r.data, 'base64'));
    log('SHOT ' + file);
  }
  async click(x, y) {
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await sleep(60);
    await this.send('Input.dispatchMouseEvent', {
      type: 'mousePressed',
      x,
      y,
      button: 'left',
      clickCount: 1,
    });
    await sleep(60);
    await this.send('Input.dispatchMouseEvent', {
      type: 'mouseReleased',
      x,
      y,
      button: 'left',
      clickCount: 1,
    });
    await sleep(100);
  }
  async jsClick(x, y) {
    // Menu path that worked for prior agent: JS-dispatched canvas events
    await this.eval(`(() => {
      const c = document.querySelector('canvas');
      if (!c) return 'no-canvas';
      const r = c.getBoundingClientRect();
      const cx = ${x}, cy = ${y};
      const mk = (type) => {
        const e = new MouseEvent(type, {bubbles:true,cancelable:true,clientX:cx,clientY:cy,button:0});
        c.dispatchEvent(e);
      };
      mk('mousemove'); mk('mousedown'); mk('mouseup'); mk('click');
      return 'ok:' + r.width + 'x' + r.height;
    })()`);
  }
  async setSize(w, h) {
    await this.send('Emulation.setDeviceMetricsOverride', {
      width: w,
      height: h,
      deviceScaleFactor: 1,
      mobile: false,
    });
  }
  async typeText(text) {
    await this.send('Input.insertText', { text });
  }
  async key(key, code, vk) {
    await this.send('Input.dispatchKeyEvent', {
      type: 'keyDown',
      key,
      code: code || key,
      windowsVirtualKeyCode: vk || 0,
    });
    await this.send('Input.dispatchKeyEvent', {
      type: 'keyUp',
      key,
      code: code || key,
      windowsVirtualKeyCode: vk || 0,
    });
  }
  flushConsole() {
    writeFileSync(CONSOLE_LOG, this.consoleLines.join('\n'));
  }
}

function launchChrome() {
  mkdirSync(PROFILE, { recursive: true });
  const logPath = resolve(PHASE, 'chrome_cursor.log');
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
    URL,
  ];
  // Redirect via shell so Node stdio WriteStream race is avoided.
  const chrome = spawn(
    'bash',
    ['-c', `exec /opt/google/chrome/chrome ${args.map((a) => JSON.stringify(a)).join(' ')} >${JSON.stringify(logPath)} 2>&1`],
    { stdio: 'ignore', detached: true }
  );
  chrome.unref();
  log('CHROME_PID=' + chrome.pid);
  return chrome.pid;
}

/** Analyze PNG buffer via crude sampling in Node without sharp deps. */
async function analyzeShot(cdp, label) {
  // Use canvas read in-page if possible; fallback: screenshot + external py later
  const info = await cdp.eval(`(() => {
    const c = document.querySelector('canvas');
    if (!c) return {ok:false, reason:'no-canvas'};
    try {
      const ctx = c.getContext('2d') || c.getContext('webgl') || c.getContext('webgl2');
      // Can't read WebGL easily; return size only
      return {ok:true, w:c.width, h:c.height, cw:c.clientWidth, ch:c.clientHeight};
    } catch(e) { return {ok:false, reason:String(e)}; }
  })()`);
  log('ANALYZE_' + label + ' ' + JSON.stringify(info));
  return info;
}

async function findGrayButtonRows(cdp) {
  // Capture screenshot to temp and ask python for button y positions
  const tmp = resolve(SHOTS, '_scan.png');
  await cdp.shot(tmp);
  const { spawnSync } = await import('node:child_process');
  const py = `
from PIL import Image
im=Image.open('${tmp}').convert('RGB')
w,h=im.size
rows=[]
for y in range(0,h,2):
  light=0; n=0
  for x in range(w//4, 3*w//4, 4):
    r,g,b=im.getpixel((x,y)); n+=1
    if r>200 and g>200 and b>200 and abs(r-g)<20 and abs(g-b)<20: light+=1
  if n and light/n>0.35: rows.append(y)
# cluster contiguous
clusters=[]
for y in rows:
  if not clusters or y-clusters[-1][-1]>6: clusters.append([y])
  else: clusters[-1].append(y)
centers=[(sum(c)//len(c)) for c in clusters if len(c)>=3]
print(','.join(str(c) for c in centers))
# also classify screen
top=[im.getpixel((x,20)) for x in range(0,w,20)]
bot=[im.getpixel((x,h-30)) for x in range(0,w,20)]
def sky(ps):
  return sum(1 for r,g,b in ps if b>r+15 and b>110)/len(ps)
def dirt(ps):
  return sum(1 for r,g,b in ps if 15<=r<=90 and 10<=g<=70 and 5<=b<=50)/len(ps)
print('META sky=%.2f dirt=%.2f'%(sky(top),dirt(top)))
`;
  const r = spawnSync('python3', ['-c', py], { encoding: 'utf8' });
  const lines = (r.stdout || '').trim().split('\n');
  const centers = lines[0] ? lines[0].split(',').filter(Boolean).map(Number) : [];
  log('BUTTON_ROWS ' + centers.join(',') + ' ' + (lines[1] || ''));
  return centers;
}

async function waitForTitle(cdp, maxMs = 300000) {
  const t0 = Date.now();
  let clicked = false;
  while (Date.now() - t0 < maxMs) {
    try {
      await cdp.setSize(1280, 720);
      if (!clicked) {
        await cdp.jsClick(640, 360);
        await cdp.click(640, 360);
        clicked = true;
        log('clicked canvas center to unblock');
      }
      const rows = await findGrayButtonRows(cdp);
      // Title has multiple gray buttons; dirt menus also have gray buttons
      const tmp = resolve(SHOTS, '_boot_scan.png');
      // classify via python
      const { spawnSync } = await import('node:child_process');
      const cls = spawnSync(
        'python3',
        [
          '-c',
          `
from PIL import Image
im=Image.open('${resolve(SHOTS, '_scan.png')}').convert('RGB')
w,h=im.size
# look for reddish logo pixels
red=0
for y in range(h//8, h//3, 3):
  for x in range(w//4, 3*w//4, 3):
    r,g,b=im.getpixel((x,y))
    if r>180 and g<80 and b<80: red+=1
# or gray stone logo (high luminance mid)
print(red)
`,
        ],
        { encoding: 'utf8' }
      );
      const red = Number((cls.stdout || '0').trim() || 0);
      log('title_red_pixels=' + red + ' buttons=' + rows.length);
      if (rows.length >= 2) {
        await cdp.shot(resolve(SHOTS, 'SS-01_GAME_BOOT.png'));
        return rows;
      }
    } catch (e) {
      log('waitTitle err: ' + e.message);
    }
    await sleep(15000);
  }
  throw new Error('title timeout');
}

async function enterSingleplayerAndCreateWorld(cdp, buttonRows) {
  // Prefer a button near vertical center (Singleplayer)
  const candidates = buttonRows.filter((y) => y > 180 && y < 420);
  const spY = candidates[0] || buttonRows[0];
  log('click Singleplayer at y=' + spY);
  await cdp.jsClick(640, spY);
  await cdp.click(640, spY);
  await sleep(8000);
  await cdp.shot(resolve(SHOTS, '_cursor_sp.png'));

  let rows = await findGrayButtonRows(cdp);
  // Select World screen: look for Create New World near bottom
  const createY = rows.filter((y) => y > 380).pop() || rows[rows.length - 1];
  // Also try clicking a world entry if present (middle)
  const mid = rows.filter((y) => y > 100 && y < 320);
  if (mid.length) {
    log('click world entry y=' + mid[0]);
    await cdp.jsClick(640, mid[0]);
    await cdp.click(640, mid[0]);
    await sleep(2000);
    // Play Selected World button often near bottom
    rows = await findGrayButtonRows(cdp);
    const playY = rows.filter((y) => y > 350).pop();
    if (playY) {
      log('click Play/Create bottom y=' + playY);
      await cdp.jsClick(640, playY);
      await cdp.click(640, playY);
      await sleep(5000);
      await cdp.shot(resolve(SHOTS, '_cursor_play.png'));
      // If still on menu, fall through to create
    }
  }

  // Create New World path
  rows = await findGrayButtonRows(cdp);
  const createCandidates = rows.filter((y) => y > 360);
  if (createCandidates.length) {
    const cy = createCandidates[createCandidates.length - 1];
    log('click Create New World y=' + cy);
    await cdp.jsClick(640, cy);
    await cdp.click(640, cy);
    await sleep(5000);
    await cdp.shot(resolve(SHOTS, '_cursor_create.png'));
  }

  // Name field ~ (639,165); type a short name then Create
  await cdp.jsClick(639, 165);
  await cdp.click(639, 165);
  await sleep(500);
  await cdp.typeText('P01val');
  await sleep(500);
  await cdp.shot(resolve(SHOTS, '_cursor_named.png'));
  rows = await findGrayButtonRows(cdp);
  const finalCreate = rows.filter((y) => y > 380).pop() || rows[rows.length - 1];
  if (finalCreate) {
    log('confirm create y=' + finalCreate);
    await cdp.jsClick(640, finalCreate);
    await cdp.click(640, finalCreate);
  }
}

async function waitInWorld(cdp, maxMs = 420000) {
  const t0 = Date.now();
  let n = 0;
  while (Date.now() - t0 < maxMs) {
    n++;
    const path = resolve(SHOTS, `_cursor_load_${n}.png`);
    try {
      await cdp.shot(path);
    } catch (e) {
      log('shot fail during load: ' + e.message);
      await sleep(10000);
      continue;
    }
    const { spawnSync } = await import('node:child_process');
    const r = spawnSync(
      'python3',
      [
        '-c',
        `
from PIL import Image
im=Image.open('${path}').convert('RGB')
w,h=im.size
top=[im.getpixel((x,y)) for y in range(10,h//4,8) for x in range(0,w,24)]
bot=[im.getpixel((x,y)) for y in range(h-80,h-10,4) for x in range(w//4,3*w//4,8)]
sky=sum(1 for r,g,b in top if b>r+10 and b>100)/len(top)
darkbot=sum(1 for r,g,b in bot if r+g+b < 180)/len(bot)
# hotbar-ish dark band
# crosshair: near-white or contrasting at center
cx,cy=w//2,h//2
cross=0
for dy in range(-2,3):
  for dx in range(-2,3):
    r,g,b=im.getpixel((cx+dx,cy+dy))
    if r>200 and g>200 and b>200: cross+=1
# dirt menu?
dirt=sum(1 for r,g,b in top if 15<=r<=90 and 10<=g<=70 and 5<=b<=50)/len(top)
print('sky=%.3f darkbot=%.3f cross=%d dirt=%.3f'%(sky,darkbot,cross,dirt))
`,
      ],
      { encoding: 'utf8' }
    );
    const meta = (r.stdout || '').trim();
    log('load_scan ' + meta);
    // In-world: sky present, not dirt menu, some dark bottom (hotbar)
    if (meta.includes('dirt=0.0') || true) {
      const m = Object.fromEntries(
        meta.split(/\s+/).map((p) => {
          const [k, v] = p.split('=');
          return [k, parseFloat(v)];
        })
      );
      if (m.sky > 0.15 && m.dirt < 0.2 && m.darkbot > 0.2) {
        log('IN_WORLD detected');
        await cdp.shot(resolve(SHOTS, 'SS-01b_IN_WORLD.png'));
        return true;
      }
    }
    // also accept if console saw auto-validate
    if (cdp.consoleLines.some((l) => /auto-validat|railsysv2/i.test(l))) {
      log('IN_WORLD inferred from AutoValidate console');
      return true;
    }
    await sleep(12000);
  }
  return false;
}

async function captureValidationSeries(cdp) {
  // Rapid shots while train moves; rename later after review
  const series = [
    ['SS-02_RAIL_STRAIGHT.png', 0],
    ['SS-03_RAIL_CURVE.png', 8000],
    ['SS-04_FULL_SCALE_TRAIN.png', 8000],
    ['SS-05_TRAIN_ON_CURVE.png', 12000],
    ['SS-06_FORMATION.png', 8000],
    ['SS-07_PIECE_BOUNDARY.png', 10000],
    ['SS-08_OVERVIEW.png', 5000],
    ['SS-09_FOLLOW.png', 8000],
  ];
  for (const [name, wait] of series) {
    if (wait) await sleep(wait);
    try {
      await cdp.shot(resolve(SHOTS, name));
    } catch (e) {
      log('series shot fail ' + name + ': ' + e.message);
    }
    cdp.flushConsole();
  }
}

async function main() {
  writeFileSync(LOG, '');
  mkdirSync(SHOTS, { recursive: true });
  log('START visual validation');
  log('HTML=' + HTML + ' exists=' + existsSync(HTML));
  if (!process.env.SKIP_LAUNCH) {
    launchChrome();
    await sleep(5000);
  }
  const cdp = new CDP(await getPageWsUrl());
  await cdp.connect();
  await cdp.setSize(1280, 720);
  log('CDP connected');

  const rows = await waitForTitle(cdp);
  await enterSingleplayerAndCreateWorld(cdp, rows);
  const ok = await waitInWorld(cdp);
  if (!ok) {
    log('FAILED to reach in-world');
    cdp.flushConsole();
    process.exit(2);
  }
  log('capturing validation series immediately');
  await captureValidationSeries(cdp);
  cdp.flushConsole();
  log('DONE');
  process.exit(0);
}

main().catch((e) => {
  log('FATAL ' + (e && e.stack ? e.stack : e));
  process.exit(1);
});
