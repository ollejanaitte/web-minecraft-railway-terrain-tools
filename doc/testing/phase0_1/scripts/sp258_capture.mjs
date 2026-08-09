#!/usr/bin/env node
import { writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { resolve } from 'node:path';

const S = resolve('doc/testing/phase0_1/screenshots');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

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
# Strict MC button faces: uniform mid-gray ~100-120 across wide center
rows=[]
for y in range(120,h-20):
  face=0; n=0
  for x in range(500,780,2):
    r,g,b=im.getpixel((x,y)); n+=1
    if 100<=r<=130 and abs(r-g)<=8 and abs(g-b)<=8: face+=1
  if n and face/n>0.55: rows.append(y)
clusters=[]
for y in rows:
  if not clusters or y-clusters[-1][-1]>6: clusters.append([y])
  else: clusters[-1].append(y)
centers=[sum(c)//len(c) for c in clusters if len(c)>=8]
hot=sum(1 for y in range(h-60,h-8,2) if sum(1 for x in range(w//3,2*w//3,4) if sum(im.getpixel((x,y)))<140)>25)
print('sky=%.3f dirt=%.3f hot=%d buttons=%s'%(sky/N,dirt/N,hot,','.join(map(str,centers))))
`,
    ],
    { encoding: 'utf8' }
  ).stdout.trim();
}

function parse(m) {
  const n = (k) => parseFloat((m.match(new RegExp(k + '=([0-9.]+)')) || [])[1] || 0);
  const buttons = ((m.match(/buttons=([0-9,]*)/) || [])[1] || '').split(',').filter(Boolean).map(Number);
  return { sky: n('sky'), dirt: n('dirt'), hot: n('hot'), buttons };
}

async function main() {
  const list = await (await fetch('http://127.0.0.1:9222/json')).json();
  const cdp = new CDP(list.find((t) => t.type === 'page').webSocketDebuggerUrl);
  await cdp.connect();
  await cdp.send('Runtime.enable');
  await cdp.send('Page.enable');

  let p = resolve(S, '_tnow.png');
  await cdp.shot(p);
  let meta = parse(analyze(p));
  console.log('now', meta);

  // Ensure title
  if (meta.dirt > 0.4) {
    const cancelY = meta.buttons[meta.buttons.length - 1] || 550;
    await cdp.click(900, cancelY);
    await sleep(1500);
    await cdp.shot(p);
    meta = parse(analyze(p));
    console.log('after cancel', meta);
  }

  await cdp.shot(resolve(S, 'SS-01_GAME_BOOT.png'));
  meta = parse(analyze(resolve(S, 'SS-01_GAME_BOOT.png')));
  console.log('title', meta);

  // Singleplayer ~258 on 577p title
  const sp = meta.buttons.find((y) => y > 240 && y < 280) || 258;
  console.log('SP', sp);
  await cdp.click(640, sp);
  await sleep(5000);
  await cdp.shot(resolve(S, '_select_sp.png'));
  meta = parse(analyze(resolve(S, '_select_sp.png')));
  console.log('select', meta);

  if (meta.sky > 0.15) {
    for (const y of [250, 258, 265, 270, 245]) {
      await cdp.click(640, y);
      await sleep(3000);
      await cdp.shot(resolve(S, '_select_sp.png'));
      meta = parse(analyze(resolve(S, '_select_sp.png')));
      console.log('SP try', y, meta);
      if (meta.dirt > 0.4) break;
    }
  }

  // Create New World - on Select World, typically a lower button. Prefer ~450 range.
  // Avoid Cancel if possible.
  let create = meta.buttons.find((y) => y > 400 && y < 500) || meta.buttons.filter((y) => y > 380).pop() || 450;
  console.log('create', create, meta.buttons);
  await cdp.click(640, create);
  await sleep(4000);
  await cdp.shot(resolve(S, '_create_sp.png'));
  meta = parse(analyze(resolve(S, '_create_sp.png')));
  console.log('createform', meta);

  // If still select world (many bottom buttons), try known Create New World coords from handoff
  if (meta.buttons.filter((y) => y > 450).length >= 2 && meta.dirt > 0.5) {
    for (const y of [430, 450, 470, 410, 490]) {
      await cdp.click(640, y);
      await sleep(2500);
      await cdp.shot(resolve(S, '_create_sp.png'));
      meta = parse(analyze(resolve(S, '_create_sp.png')));
      console.log('create try', y, meta);
      // Create World form usually has fewer bottom buttons / name field area
      if (meta.buttons.length <= 3 || meta.buttons.some((yy) => yy < 200)) break;
    }
  }

  await cdp.click(640, 150);
  await sleep(200);
  await cdp.send('Input.insertText', { text: 'RailV2Val' });
  await sleep(400);
  await cdp.shot(resolve(S, '_named_sp.png'));
  meta = parse(analyze(resolve(S, '_named_sp.png')));
  const conf = meta.buttons.filter((y) => y > 400).pop() || meta.buttons[meta.buttons.length - 1] || 450;
  console.log('confirm', conf, meta);
  await cdp.click(640, conf);

  for (let i = 1; i <= 50; i++) {
    await sleep(10000);
    const lp = resolve(S, '_wl_' + i + '.png');
    try {
      await cdp.shot(lp);
    } catch (e) {
      console.log('shot fail', e.message);
      continue;
    }
    meta = parse(analyze(lp));
    console.log('wl', i, meta);
    const menuish = meta.buttons.some((y) => y > 230 && y < 330);
    if (meta.hot >= 2 && meta.sky > 0.05 && meta.dirt < 0.25 && !menuish) {
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
    if (cdp.lines.some((t) => /RAILSYSTEM|auto-validat/i.test(t))) console.log('console hit');
  }
  writeFileSync(resolve('doc/testing/phase0_1/cursor_console.txt'), cdp.lines.join('\n'));
  console.log('TIMEOUT');
  process.exit(2);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
