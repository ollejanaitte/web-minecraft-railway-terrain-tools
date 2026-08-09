// Minimal Chrome DevTools Protocol driver for the Phase 0.1 validation spike.
// Usage:
//   node cdp.mjs launch <url> <port> <profileDir>
//   node cdp.mjs wait <port> <ms>
//   node cdp.mjs shot <port> <outfile>
//   node cdp.mjs eval <port> <expr>
//   node cdp.mjs click <port> <x> <y>
//   node cdp.mjs key <port> <key>            (key names: Enter, Escape, Tab, ...)
//   node cdp.mjs type <port> <text>
//   node cdp.mjs setSize <port> <w> <h>
//   node cdp.mjs close <port>
import { spawn } from 'node:child_process';
import { writeFileSync, readFileSync, existsSync } from 'node:fs';

const PORT = process.argv[3];
const BASE = `http://127.0.0.1:${PORT}`;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function getPageWsUrl() {
  for (let i = 0; i < 60; i++) {
    try {
      const res = await fetch(`${BASE}/json`);
      const list = await res.json();
      const page = list.find((t) => t.type === 'page');
      if (page) return page.webSocketDebuggerUrl;
    } catch (e) { /* not up yet */ }
    await sleep(500);
  }
  throw new Error('CDP not available');
}

class CDP {
  constructor(wsUrl) { this.wsUrl = wsUrl; this.id = 0; this.pending = new Map(); }
  async connect() {
    this.ws = new WebSocket(this.wsUrl);
    await new Promise((res, rej) => { this.ws.onopen = res; this.ws.onerror = rej; });
    this.ws.onmessage = (ev) => {
      const msg = JSON.parse(ev.data);
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject } = this.pending.get(msg.id);
        this.pending.delete(msg.id);
        if (msg.error) reject(new Error(JSON.stringify(msg.error)));
        else resolve(msg.result);
      }
    };
  }
  send(method, params = {}) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }
  async eval(expr) {
    const r = await this.send('Runtime.evaluate', { expression: expr, returnByValue: true, awaitPromise: true });
    return r.result ? r.result.value : undefined;
  }
  async shot(file) {
    const r = await this.send('Page.captureScreenshot', { format: 'png' });
    writeFileSync(file, Buffer.from(r.data, 'base64'));
  }
  async click(x, y) {
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await sleep(80);
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 });
    await sleep(80);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 });
    await sleep(80);
  }
  async key(key) {
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key });
  }
  async typeChar(ch) {
    const code = ch.codePointAt(0);
    const key = ch;
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key, code: 'Key' + ch.toUpperCase(), text: ch, windowsVirtualKeyCode: code });
    await this.send('Input.dispatchKeyEvent', { type: 'char', key, text: ch, unmodifiedText: ch, windowsVirtualKeyCode: code });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key, code: 'Key' + ch.toUpperCase(), windowsVirtualKeyCode: code });
  }
  async chat(text) {
    // open chat with '/'
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: '/', code: 'Slash', text: '/', windowsVirtualKeyCode: 191 });
    await this.send('Input.dispatchKeyEvent', { type: 'char', key: '/', text: '/', windowsVirtualKeyCode: 191 });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: '/', code: 'Slash', windowsVirtualKeyCode: 191 });
    await sleep(300);
    for (const ch of text) {
      await this.typeChar(ch);
      await sleep(30);
    }
    await sleep(200);
    await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13 });
    await this.send('Input.dispatchKeyEvent', { type: 'char', key: 'Enter', text: '\r', windowsVirtualKeyCode: 13 });
    await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13 });
    await sleep(200);
  }
  async typeText(text) {
    await this.send('Input.insertText', { text });
  }
  async setSize(w, h) {
    await this.send('Emulation.setDeviceMetricsOverride', { width: w, height: h, deviceScaleFactor: 1, mobile: false });
  }
}

async function main() {
  const cmd = process.argv[2];
  const cdp = new CDP(await getPageWsUrl());
  await cdp.connect();
  switch (cmd) {
    case 'launch': {
      const url = process.argv[4];
      const prof = process.argv[6];
      const chrome = spawn('google-chrome', [
        '--headless=new', '--no-sandbox', '--disable-gpu-sandbox',
        '--enable-unsafe-swiftshader', '--use-angle=swiftshader',
        '--hide-scrollbars', '--window-size=1280,720',
        '--remote-debugging-port=' + PORT,
        '--user-data-dir=' + prof, url,
      ], { stdio: 'ignore', detached: true });
      console.log('CHROME_PID=' + chrome.pid);
      process.exit(0);
    }
    case 'wait': await sleep(Number(process.argv[4])); break;
    case 'shot': await cdp.shot(process.argv[4]); break;
    case 'eval': console.log(JSON.stringify(await cdp.eval(process.argv[4]))); break;
    case 'click': await cdp.click(Number(process.argv[4]), Number(process.argv[5])); break;
    case 'key': await cdp.key(process.argv[4]); break;
    case 'chat': await cdp.chat(process.argv[4]); break;
    case 'char': await cdp.typeChar(process.argv[4]); break;
    case 'type': await cdp.typeText(process.argv[4]); break;
    case 'setSize': await cdp.setSize(Number(process.argv[4]), Number(process.argv[5])); break;
    default: console.error('unknown cmd', cmd);
  }
  if (cmd !== 'launch') {
    await sleep(150);
  }
  process.exit(0);
}

main().catch((e) => { console.error('ERR', e); process.exit(1); });
