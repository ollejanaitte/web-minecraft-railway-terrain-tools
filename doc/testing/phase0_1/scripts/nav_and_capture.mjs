#!/usr/bin/env node
/**
 * Phase 0.1: boot EaglercraftX offline -> title -> create world -> wait in-world
 * -> capture SS-01..SS-08. Uses CDP + pixel classification.
 */
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../../../..');
const PHASE = resolve(ROOT, 'doc/testing/phase0_1');
const SHOTS = resolve(PHASE, 'screenshots');
const PROFILE = resolve(PHASE, 'chrome-profile-cursor');
const PORT = process.env.CDP_PORT || '9222';
const HTML = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const LOG = resolve(PHASE, 'cursor_nav.log');

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const log = (m) => {
  const line = `[${new Date().toISOString()}] ${m}`;
  console.log(line);
  writeFileSync(LOG, line + '\n', { flag: 'a' });
};

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

class CDP {
  constructor(url) {
    this.url = url;
    this.id = 0;
    this.pending = new Map();
    this.console = [];
  }
  async connect() {
    this.ws = new WebSocket(this.url);
    await new Promise((res, rej) => {
      this.ws.onopen = res;
      this.ws.onerror = rej;
    });
    this.ws.onmessage = (ev) => {
      const msg = JSON.parse(ev.data);
      if (msg.method === 'Runtime.consoleAPICalled') {
        const t = (msg.params.args || []).map((a) => a.value ?? a.description ?? '').join(' ');
        this.console.push(t);
        if (/RAILSYSTEM|auto-validat|railsysv2/i.test(t)) log('HIT ' + t.slice(0, 300));
      }
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject } = this.pending.get(msg.id);
        this.pending.delete(msg.id);
        if (msg.error) reject(new Error(JSON.stringify(msg.error)));
        else resolve(msg.result);
      }
    };
    await this.send('Runtime.enable');
    await this.send('Page.enable');
  }
  send(method, params = {}, timeoutMs = 90000) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      const t = setTimeout(() => {
        this.pending.delete(id);
        reject(new Error('timeout ' + method));
      }, timeoutMs);
      this.pending.set(id, {
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
  async eval(expr) {
    const r = await this.send('Runtime.evaluate', {
      expression: expr,
      returnByValue: true,
      awaitPromise: true,
    });
    return r.result ? r.result.value : undefined;
  }
  async shot(file) {
    const r = await this.send('Page.captureScreenshot', { format: 'png' }, 120000);
    writeFileSync(file, Buffer.from(r.data, 'base64'));
  }
  async click(x, y) {
    await this.eval(`(()=>{const c=document.querySelector('canvas');if(!c)return;
      const mk=t=>c.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,clientX:${x},clientY:${y},button:0,view:window}));
      mk('mousemove');mk('mousedown');mk('mouseup');mk('click');})()`);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', {
      type: 'mousePressed',
      x,
      y,
      button: 'left',
      clickCount: 1,
    });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', {
      type: 'mouseReleased',
      x,
      y,
      button: 'left',
      clickCount: 1,
    });
  }
}

function classify(path) {
  const py = `
from PIL import Image
im=Image.open(${JSON.stringify(path)}).convert('RGB'); w,h=im.size
sky=sum(1 for y in range(10,120,6) for x in range(0,w,20) if (lambda p:p[2]>p[0]+12 and p[2]>100)(im.getpixel((x,y))))
dirt=sum(1 for y in range(10,120,6) for x in range(0,w,20) if (lambda p:15<=p[0]<=90 and 10<=p[1]<=70)(im.getpixel((x,y))))
N=max(1,((120-10)//6)*(w//20))
rows=[]
for y in range(140,h-30):
  light=0;n=0
  for x in range(380,900,2):
    r,g,b=im.getpixel((x,y));n+=1
    if r>=180 and g>=180 and b>=180 and abs(r-g)<30 and abs(g-b)<30: light+=1
  if n and light/n>0.35: rows.append(y)
clusters=[]
for y in rows:
  if not clusters or y-clusters[-1][-1]>8: clusters.append([y])
  else: clusters[-1].append(y)
centers=[sum(c)//len(c) for c in clusters if 4<=len(c)<=50]
hot=0
for y in range(h-70,h-12,2):
  dark=sum(1 for x in range(w//3,2*w//3,4) if sum(im.getpixel((x,y)))<140)
  if dark>28: hot+=1
cx,cy=w//2,h//2
cross=sum(1 for dy in range(-1,2) for dx in range(-1,2) if min(im.getpixel((cx+dx,cy+dy)))>200)
# title logo red easter-egg not always; detect gray logo band
print('sky=%.3f dirt=%.3f hot=%d cross=%d buttons=%s'%(sky/N,dirt/N,hot,cross,','.join(map(str,centers))))
`;
  return spawnSync('python3', ['-c', py], { encoding: 'utf8' }).stdout.trim();
}

function parseMeta(m) {
  const num = (k) => {
    const r = m.match(new RegExp(k + '=([0-9.]+)'));
    return r ? parseFloat(r[1]) : 0;
  };
  const buttons = ((m.match(/buttons=([0-9,]*)/) || [])[1] || '')
    .split(',')
    .filter(Boolean)
    .map(Number);
  return { sky: num('sky'), dirt: num('dirt'), hot: num('hot'), cross: num('cross'), buttons };
}

function launchChrome() {
  mkdirSync(PROFILE, { recursive: true });
  mkdirSync(SHOTS, { recursive: true });
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
    'file://' + HTML,
  ];
  const cmd = `exec /opt/google/chrome/chrome ${args.map((a) => JSON.stringify(a)).join(' ')} >${JSON.stringify(logPath)} 2>&1`;
  const child = spawn('bash', ['-c', cmd], { stdio: 'ignore', detached: true });
  child.unref();
  log('CHROME_PID=' + child.pid);
}

async function main() {
  writeFileSync(LOG, '');
  if (!existsSync(HTML)) throw new Error('missing offline html');
  if (!process.env.SKIP_LAUNCH) launchChrome();
  await sleep(4000);
  const cdp = new CDP(await getWs());
  await cdp.connect();
  log('CDP connected');

  // Unblock splash
  await cdp.click(640, 360);
  await sleep(2000);

  // Dismiss Content Warning / Edit Profile until title (sky high, dirt low)
  for (let i = 0; i < 20; i++) {
    const p = resolve(SHOTS, `_boot_${i}.png`);
    await cdp.shot(p);
    const meta = parseMeta(classify(p));
    log(`boot ${i} ${JSON.stringify(meta)}`);
    if (meta.sky > 0.15 && meta.dirt < 0.25 && meta.buttons.some((y) => y > 300 && y < 420)) {
      await cdp.shot(resolve(SHOTS, 'SS-01_GAME_BOOT.png'));
      log('TITLE ok');
      break;
    }
    // Prefer bottom Done buttons (warning/profile)
    const done = meta.buttons.filter((y) => y > 500).pop() || meta.buttons.filter((y) => y > 400).pop();
    if (done) {
      log('click Done-ish ' + done);
      await cdp.click(640, done);
    } else {
      await cdp.click(640, 560);
      await cdp.click(640, 640);
    }
    await sleep(2000);
  }

  let p = resolve(SHOTS, 'SS-01_GAME_BOOT.png');
  if (!existsSync(p)) await cdp.shot(p);
  let meta = parseMeta(classify(p));
  // Singleplayer ~355 on 720p title
  let sp = meta.buttons.find((y) => y > 320 && y < 390) || 355;
  log('click SP ' + sp);
  await cdp.click(640, sp);
  await sleep(5000);
  p = resolve(SHOTS, '_select.png');
  await cdp.shot(p);
  meta = parseMeta(classify(p));
  log('select ' + JSON.stringify(meta));

  // Create New World
  let create = meta.buttons.filter((y) => y > 400).pop() || 450;
  if (meta.dirt < 0.2) {
    // still title? click SP again
    await cdp.click(640, 355);
    await sleep(4000);
    await cdp.shot(p);
    meta = parseMeta(classify(p));
    create = meta.buttons.filter((y) => y > 400).pop() || 450;
  }
  log('click create ' + create);
  await cdp.click(640, create);
  await sleep(4000);
  p = resolve(SHOTS, '_create.png');
  await cdp.shot(p);
  meta = parseMeta(classify(p));
  log('createform ' + JSON.stringify(meta));

  await cdp.click(640, 165);
  await sleep(200);
  await cdp.send('Input.insertText', { text: 'RailV2Val' });
  await sleep(300);
  p = resolve(SHOTS, '_named.png');
  await cdp.shot(p);
  meta = parseMeta(classify(p));
  const conf = meta.buttons.filter((y) => y > 420).pop() || meta.buttons[meta.buttons.length - 1] || 500;
  log('confirm ' + conf);
  await cdp.click(640, conf);

  // Wait in-world: hotbar + sky, no mid menu buttons
  let inWorld = false;
  for (let i = 1; i <= 45; i++) {
    await sleep(10000);
    p = resolve(SHOTS, `_load_${i}.png`);
    try {
      await cdp.shot(p);
    } catch (e) {
      log('shot fail ' + e.message);
      continue;
    }
    meta = parseMeta(classify(p));
    log(`load ${i} ${JSON.stringify(meta)}`);
    const menuish = meta.buttons.some((y) => y > 250 && y < 430);
    if (meta.hot >= 3 && meta.sky > 0.08 && meta.dirt < 0.25 && !menuish) {
      inWorld = true;
      await cdp.shot(resolve(SHOTS, 'SS-01b_IN_WORLD.png'));
      log('IN_WORLD');
      break;
    }
    if (cdp.console.some((t) => /RAILSYSTEM|auto-validat/i.test(t))) {
      inWorld = true;
      log('IN_WORLD via console');
      break;
    }
  }
  if (!inWorld) {
    writeFileSync(resolve(PHASE, 'cursor_console.txt'), cdp.console.join('\n'));
    throw new Error('failed to reach in-world');
  }

  writeFileSync(resolve(PHASE, 'cursor_console.txt'), cdp.console.join('\n'));
  const series = [
    ['SS-02_RAIL_STRAIGHT.png', 1500],
    ['SS-03_RAIL_CURVE.png', 7000],
    ['SS-04_FULL_SCALE_TRAIN.png', 7000],
    ['SS-05_TRAIN_ON_CURVE.png', 12000],
    ['SS-06_FORMATION.png', 8000],
    ['SS-07_PIECE_BOUNDARY.png', 12000],
    ['SS-08_EXTRA.png', 5000],
  ];
  for (const [name, wait] of series) {
    await sleep(wait);
    try {
      await cdp.shot(resolve(SHOTS, name));
      log('captured ' + name);
    } catch (e) {
      log('capture fail ' + name + ' ' + e.message);
    }
  }
  writeFileSync(resolve(PHASE, 'cursor_console.txt'), cdp.console.join('\n'));
  log('DONE');
}

main().catch((e) => {
  log('FATAL ' + (e.stack || e));
  process.exit(1);
});
