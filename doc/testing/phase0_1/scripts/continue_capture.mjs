#!/usr/bin/env node
import { writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { resolve } from 'node:path';

const PORT = process.env.CDP_PORT || '9222';
const S = resolve('doc/testing/phase0_1/screenshots');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function getWs() {
  const list = await (await fetch(`http://127.0.0.1:${PORT}/json`)).json();
  return list.find((t) => t.type === 'page').webSocketDebuggerUrl;
}

class CDP {
  constructor(url) {
    this.url = url;
    this.id = 0;
    this.pending = new Map();
    this.lines = [];
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
        this.lines.push(t);
        if (/RAILSYSTEM|auto-validat|railsysv2/i.test(t)) console.log('HIT', t.slice(0, 240));
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
    console.log('shot', file);
  }
  async click(x, y) {
    console.log('click', x, y);
    await this.eval(`(()=>{const c=document.querySelector('canvas');if(!c)return;
      const mk=t=>c.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,clientX:${x},clientY:${y},button:0,view:window}));
      mk('mousemove');mk('mousedown');mk('mouseup');mk('click');})()`);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await sleep(30);
    await this.send('Input.dispatchMouseEvent', {
      type: 'mousePressed',
      x,
      y,
      button: 'left',
      clickCount: 1,
    });
    await sleep(30);
    await this.send('Input.dispatchMouseEvent', {
      type: 'mouseReleased',
      x,
      y,
      button: 'left',
      clickCount: 1,
    });
  }
}

function analyze(path) {
  const py = `
from PIL import Image
im=Image.open(${JSON.stringify(path)}).convert('RGB'); w,h=im.size
sky=sum(1 for y in range(8,110,5) for x in range(0,w,18) if (lambda p:p[2]>p[0]+12 and p[2]>100)(im.getpixel((x,y))))
dirt=sum(1 for y in range(8,110,5) for x in range(0,w,18) if (lambda p:15<=p[0]<=90 and 10<=p[1]<=70)(im.getpixel((x,y))))
N=max(1,((110-8)//5)*(w//18))
red=sum(1 for y in range(70,150,2) for x in range(w//3,2*w//3,3) if (lambda p:p[0]>180 and p[1]<90 and p[2]<90)(im.getpixel((x,y))))
# button face centers (~gray 100-130 wide band)
rows=[]
for y in range(120,h-20):
  face=sum(1 for x in range(420,860,2) if (lambda p:95<=p[0]<=140 and abs(p[0]-p[1])<20 and abs(p[1]-p[2])<20)(im.getpixel((x,y))))
  if face>80: rows.append(y)
clusters=[]
for y in rows:
  if not clusters or y-clusters[-1][-1]>6: clusters.append([y])
  else: clusters[-1].append(y)
centers=[sum(c)//len(c) for c in clusters if len(c)>=6]
hot=0
for y in range(h-60,h-8,2):
  dark=sum(1 for x in range(w//3,2*w//3,4) if sum(im.getpixel((x,y)))<140)
  if dark>25: hot+=1
print('h=%d sky=%.3f dirt=%.3f red=%d hot=%d buttons=%s'%(h,sky/N,dirt/N,red,hot,','.join(map(str,centers))))
`;
  return spawnSync('python3', ['-c', py], { encoding: 'utf8' }).stdout.trim();
}

function parse(m) {
  const n = (k) => {
    const r = m.match(new RegExp(k + '=(-?[0-9.]+)'));
    return r ? parseFloat(r[1]) : 0;
  };
  const buttons = ((m.match(/buttons=([0-9,]*)/) || [])[1] || '')
    .split(',')
    .filter(Boolean)
    .map(Number);
  return { h: n('h'), sky: n('sky'), dirt: n('dirt'), red: n('red'), hot: n('hot'), buttons };
}

async function main() {
  const info = await (await fetch(`http://127.0.0.1:${PORT}/json`)).json();
  console.log('canvas page', info[0]?.title);
  const cdp = new CDP(await getWs());
  await cdp.connect();
  const canvas = await cdp.eval(
    `(()=>{const c=document.querySelector('canvas');return c?{w:c.width,h:c.height,cw:c.clientWidth,ch:c.clientHeight}:null;})()`
  );
  console.log('canvas', canvas);

  let p = resolve(S, '_cur.png');
  await cdp.shot(p);
  let meta = parse(analyze(p));
  console.log('start', meta);

  // Dismiss Content Warning / Edit Profile
  for (let i = 0; i < 12; i++) {
    if (meta.sky > 0.12 && meta.dirt < 0.3 && meta.red < 50) {
      console.log('likely title');
      break;
    }
    // Done is usually the lowest button face
    const done = meta.buttons.length ? meta.buttons[meta.buttons.length - 1] : 390;
    await cdp.click(640, done);
    await sleep(1500);
    await cdp.shot(p);
    meta = parse(analyze(p));
    console.log('after done', i, meta);
  }

  await cdp.shot(resolve(S, 'SS-01_GAME_BOOT.png'));
  meta = parse(analyze(resolve(S, 'SS-01_GAME_BOOT.png')));
  console.log('SS-01', meta);

  // Singleplayer: first mid button on title (~280-360 at 577h). Prior 720p was ~355 -> ~284 at 577.
  let sp = meta.buttons.find((y) => y > 240 && y < 340) || meta.buttons[0] || 280;
  console.log('SP', sp, meta.buttons);
  await cdp.click(640, sp);
  await sleep(5000);
  await cdp.shot(resolve(S, '_select.png'));
  meta = parse(analyze(resolve(S, '_select.png')));
  console.log('select', meta);

  // If still title (sky), try alternate SP Y
  if (meta.sky > 0.15) {
    for (const y of [260, 280, 300, 320, 250]) {
      await cdp.click(640, y);
      await sleep(2500);
      await cdp.shot(resolve(S, '_select.png'));
      meta = parse(analyze(resolve(S, '_select.png')));
      console.log('SP try', y, meta);
      if (meta.dirt > 0.4) break;
    }
  }

  // Create New World - lower button on select screen
  let create = meta.buttons.filter((y) => y > 350).pop() || 420;
  console.log('create', create);
  await cdp.click(640, create);
  await sleep(4000);
  await cdp.shot(resolve(S, '_create.png'));
  meta = parse(analyze(resolve(S, '_create.png')));
  console.log('createform', meta);

  await cdp.click(640, 140);
  await sleep(200);
  await cdp.send('Input.insertText', { text: 'RailV2Val' });
  await sleep(300);
  await cdp.shot(resolve(S, '_named.png'));
  meta = parse(analyze(resolve(S, '_named.png')));
  const conf = meta.buttons.filter((y) => y > 350).pop() || meta.buttons[meta.buttons.length - 1] || 430;
  console.log('confirm', conf, meta);
  await cdp.click(640, conf);

  for (let i = 1; i <= 40; i++) {
    await sleep(10000);
    const lp = resolve(S, `_load_${i}.png`);
    try {
      await cdp.shot(lp);
    } catch (e) {
      console.log('shot fail', e.message);
      continue;
    }
    meta = parse(analyze(lp));
    console.log('load', i, meta);
    const menuish = meta.buttons.some((y) => y > 220 && y < 360);
    if (meta.hot >= 2 && meta.sky > 0.05 && meta.dirt < 0.3 && !menuish) {
      console.log('IN_WORLD');
      await cdp.shot(resolve(S, 'SS-01b_IN_WORLD.png'));
      for (const [name, wait] of [
        ['SS-02_RAIL_STRAIGHT.png', 1500],
        ['SS-03_RAIL_CURVE.png', 7000],
        ['SS-04_FULL_SCALE_TRAIN.png', 7000],
        ['SS-05_TRAIN_ON_CURVE.png', 12000],
        ['SS-06_FORMATION.png', 8000],
        ['SS-07_PIECE_BOUNDARY.png', 12000],
        ['SS-08_EXTRA.png', 5000],
      ]) {
        await sleep(wait);
        try {
          await cdp.shot(resolve(S, name));
          console.log('captured', name);
        } catch (e) {
          console.log('fail', name, e.message);
        }
      }
      writeFileSync(resolve('doc/testing/phase0_1/cursor_console.txt'), cdp.lines.join('\n'));
      console.log('SUCCESS');
      process.exit(0);
    }
    if (cdp.lines.some((t) => /RAILSYSTEM|auto-validat/i.test(t))) {
      console.log('IN_WORLD via console');
      await cdp.shot(resolve(S, 'SS-01b_IN_WORLD.png'));
    }
  }
  writeFileSync(resolve('doc/testing/phase0_1/cursor_console.txt'), cdp.lines.join('\n'));
  console.log('TIMEOUT');
  process.exit(2);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
