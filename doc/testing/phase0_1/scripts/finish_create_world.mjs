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
        if (/RAILSYSTEM|auto-validat|railsysv2|integrated|Building|Preparing/i.test(t)) {
          console.log('HIT', t.slice(0, 240));
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
    console.log('shot', f);
  }
  async click(x, y) {
    console.log('click', x, y);
    await this.eval(
      `(()=>{const c=document.querySelector('canvas');const mk=t=>c.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,clientX:${x},clientY:${y},button:0,view:window}));mk('mousemove');mk('mousedown');mk('mouseup');mk('click');})()`
    );
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 });
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 });
  }
}

function analyze(path) {
  return spawnSync('python3', ['doc/testing/phase0_1/scripts/analyze_screen.py', path], { encoding: 'utf8' }).stdout.trim();
}

async function main() {
  const list = await (await fetch('http://127.0.0.1:9222/json')).json();
  const cdp = new CDP(list.find((t) => t.type === 'page').webSocketDebuggerUrl);
  await cdp.connect();
  await cdp.send('Runtime.enable');
  await cdp.send('Page.enable');

  await cdp.shot(resolve(S, '_submenu.png'));
  console.log('submenu', analyze(resolve(S, '_submenu.png')));

  await cdp.click(640, 242);
  await sleep(4000);
  await cdp.shot(resolve(S, '_worldopts.png'));
  console.log('opts', analyze(resolve(S, '_worldopts.png')));

  await cdp.click(640, 150);
  await sleep(200);
  await cdp.send('Input.insertText', { text: 'RailV2Val' });
  await sleep(400);
  await cdp.shot(resolve(S, '_worldnamed.png'));
  console.log('named', analyze(resolve(S, '_worldnamed.png')));

  for (const y of [500, 480, 460, 520, 440, 420]) {
    await cdp.click(480, y);
    await sleep(1500);
    await cdp.shot(resolve(S, '_worldgo.png'));
    const m = analyze(resolve(S, '_worldgo.png'));
    console.log('go', y, m);
    const dirt = parseFloat((m.match(/dirt=([0-9.]+)/) || [])[1] || 1);
    const sky = parseFloat((m.match(/sky=([0-9.]+)/) || [])[1] || 0);
    if (dirt < 0.5 || sky > 0.05) break;
  }

  for (let i = 1; i <= 50; i++) {
    await sleep(10000);
    const lp = resolve(S, '_ig_' + i + '.png');
    try {
      await cdp.shot(lp);
    } catch (e) {
      console.log('fail', e.message);
      continue;
    }
    const m = analyze(lp);
    console.log('ig', i, m);
    const sky = parseFloat((m.match(/sky=([0-9.]+)/) || [])[1] || 0);
    const dirt = parseFloat((m.match(/dirt=([0-9.]+)/) || [])[1] || 1);
    const hot = parseInt((m.match(/hot=([0-9]+)/) || [])[1] || 0, 10);
    if (hot >= 2 && sky > 0.05 && dirt < 0.25) {
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
