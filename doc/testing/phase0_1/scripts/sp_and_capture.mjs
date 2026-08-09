#!/usr/bin/env node
import { writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { resolve } from 'node:path';

const S = resolve('doc/testing/phase0_1/screenshots');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function getWs() {
  const list = await (await fetch('http://127.0.0.1:9222/json')).json();
  return list.find((t) => t.type === 'page').webSocketDebuggerUrl;
}

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
        if (/RAILSYSTEM|auto-validat|railsysv2/i.test(t)) console.log('HIT', t.slice(0, 240));
      }
      if (m.id && this.p.has(m.id)) {
        const { resolve, reject } = this.p.get(m.id);
        this.p.delete(m.id);
        if (m.error) reject(new Error(JSON.stringify(m.error)));
        else resolve(m.result);
      }
    };
  }
  send(method, params = {}, timeoutMs = 90000) {
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
    return (await this.send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true }))
      .result?.value;
  }
  async shot(f) {
    writeFileSync(f, Buffer.from((await this.send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
    console.log('shot', f);
  }
  async click(x, y) {
    console.log('click', x, y);
    await this.eval(`(()=>{const c=document.querySelector('canvas');const mk=t=>c.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,clientX:${x},clientY:${y},button:0,view:window}));mk('mousemove');mk('mousedown');mk('mouseup');mk('click');})()`);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 });
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 });
  }
}

function analyze(path) {
  return spawnSync(
    'python3',
    [
      '-c',
      `
from PIL import Image
im=Image.open(${JSON.stringify(path)}).convert('RGB'); w,h=im.size
sky=sum(1 for y in range(8,110,5) for x in range(0,w,18) if (lambda p:p[2]>p[0]+12 and p[2]>100)(im.getpixel((x,y))))
dirt=sum(1 for y in range(8,110,5) for x in range(0,w,18) if (lambda p:15<=p[0]<=90 and 10<=p[1]<=70)(im.getpixel((x,y))))
N=max(1,((110-8)//5)*(w//18))
rows=[]
for y in range(120,h-20):
  face=sum(1 for x in range(420,860,2) if (lambda p:95<=p[0]<=140 and abs(p[0]-p[1])<20 and abs(p[1]-p[2])<20)(im.getpixel((x,y))))
  if face>80: rows.append(y)
clusters=[]
for y in rows:
  if not clusters or y-clusters[-1][-1]>6: clusters.append([y])
  else: clusters[-1].append(y)
centers=[sum(c)//len(c) for c in clusters if len(c)>=6]
hot=sum(1 for y in range(h-60,h-8,2) if sum(1 for x in range(w//3,2*w//3,4) if sum(im.getpixel((x,y)))<140)>25)
print('sky=%.3f dirt=%.3f hot=%d buttons=%s'%(sky/N,dirt/N,hot,','.join(map(str,centers))))
`,
    ],
    { encoding: 'utf8' }
  ).stdout.trim();
}

function parse(m) {
  const n = (k) => parseFloat((m.match(new RegExp(k + '=([0-9.]+)')) || [])[1] || 0);
  const buttons = ((m.match(/buttons=([0-9,]*)/) || [])[1] || '')
    .split(',')
    .filter(Boolean)
    .map(Number);
  return { sky: n('sky'), dirt: n('dirt'), hot: n('hot'), buttons };
}

async function main() {
  const cdp = new CDP(await getWs());
  await cdp.connect();
  await cdp.send('Runtime.enable');
  await cdp.send('Page.enable');

  let p = resolve(S, '_now.png');
  await cdp.shot(p);
  let meta = parse(analyze(p));
  console.log('now', meta);

  // Cancel multiplayer (Cancel is bottom-right)
  const cancelY = meta.buttons[meta.buttons.length - 1] || 552;
  for (const x of [1100, 1000, 900, 800, 640]) {
    await cdp.click(x, cancelY);
    await sleep(900);
    await cdp.shot(p);
    meta = parse(analyze(p));
    console.log('cancel', x, meta);
    if (meta.sky > 0.15) break;
  }
  if (meta.sky < 0.1) {
    await cdp.send('Input.dispatchKeyEvent', {
      type: 'keyDown',
      key: 'Escape',
      code: 'Escape',
      windowsVirtualKeyCode: 27,
    });
    await cdp.send('Input.dispatchKeyEvent', {
      type: 'keyUp',
      key: 'Escape',
      code: 'Escape',
      windowsVirtualKeyCode: 27,
    });
    await sleep(1500);
    await cdp.shot(p);
    meta = parse(analyze(p));
    console.log('after esc', meta);
  }

  await cdp.shot(resolve(S, 'SS-01_GAME_BOOT.png'));
  meta = parse(analyze(resolve(S, 'SS-01_GAME_BOOT.png')));
  console.log('title', meta);
  // Singleplayer is the FIRST large button (~209), NOT Multiplayer (~306)
  let sp = meta.buttons.find((y) => y > 180 && y < 250) || 209;
  console.log('clicking SP', sp);
  await cdp.click(640, sp);
  await sleep(5000);
  await cdp.shot(resolve(S, '_spworld.png'));
  meta = parse(analyze(resolve(S, '_spworld.png')));
  console.log('spworld', meta);

  if (meta.sky > 0.15) {
    for (const y of [200, 210, 220, 230, 240, 190]) {
      await cdp.click(640, y);
      await sleep(3000);
      await cdp.shot(resolve(S, '_spworld.png'));
      meta = parse(analyze(resolve(S, '_spworld.png')));
      console.log('try SP', y, meta);
      if (meta.dirt > 0.4) break;
    }
  }

  // Select World: Create New World is usually a lower button
  let create = meta.buttons.filter((y) => y > 380 && y < 520).pop() || meta.buttons.filter((y) => y > 350).pop() || 450;
  console.log('create', create, meta.buttons);
  await cdp.click(640, create);
  await sleep(4000);
  await cdp.shot(resolve(S, '_newworld.png'));
  meta = parse(analyze(resolve(S, '_newworld.png')));
  console.log('newworld', meta);

  await cdp.click(640, 150);
  await sleep(200);
  await cdp.send('Input.insertText', { text: 'RailV2Val' });
  await sleep(300);
  await cdp.shot(resolve(S, '_named2.png'));
  meta = parse(analyze(resolve(S, '_named2.png')));
  const conf = meta.buttons.filter((y) => y > 380).pop() || meta.buttons[meta.buttons.length - 1] || 450;
  console.log('confirm create', conf, meta);
  await cdp.click(640, conf);

  for (let i = 1; i <= 45; i++) {
    await sleep(10000);
    const lp = resolve(S, '_wload_' + i + '.png');
    try {
      await cdp.shot(lp);
    } catch (e) {
      console.log('fail', e.message);
      continue;
    }
    meta = parse(analyze(lp));
    console.log('wload', i, meta);
    const menuish = meta.buttons.some((y) => y > 200 && y < 360);
    if (meta.hot >= 2 && meta.sky > 0.05 && meta.dirt < 0.3 && !menuish) {
      console.log('IN_WORLD');
      await cdp.shot(resolve(S, 'SS-01b_IN_WORLD.png'));
      for (const [n, w] of [
        ['SS-02_RAIL_STRAIGHT.png', 1500],
        ['SS-03_RAIL_CURVE.png', 7000],
        ['SS-04_FULL_SCALE_TRAIN.png', 7000],
        ['SS-05_TRAIN_ON_CURVE.png', 12000],
        ['SS-06_FORMATION.png', 8000],
        ['SS-07_PIECE_BOUNDARY.png', 12000],
        ['SS-08_EXTRA.png', 5000],
      ]) {
        await sleep(w);
        try {
          await cdp.shot(resolve(S, n));
          console.log('ok', n);
        } catch (e) {
          console.log('fail', n, e.message);
        }
      }
      writeFileSync(resolve('doc/testing/phase0_1/cursor_console.txt'), cdp.lines.join('\n'));
      console.log('SUCCESS');
      process.exit(0);
    }
    if (cdp.lines.some((t) => /RAILSYSTEM|auto-validat/i.test(t))) console.log('console hit while loading');
  }
  writeFileSync(resolve('doc/testing/phase0_1/cursor_console.txt'), cdp.lines.join('\n'));
  console.log('TIMEOUT');
  process.exit(2);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
