#!/usr/bin/env node
/**
 * Phase 0.2 CPU benchmark for a Chrome rendering mode.
 * Loads a WebGL stress page, samples per-process CPU + total for N seconds.
 *
 * Usage: node gpu_bench.mjs <label> <profileDir> <port> <sampleSec> -- <flags...>
 *   Sample rate: every 2s, aggregates chrome process tree CPU%.
 */
import { spawn, execSync } from 'node:child_process';
import { mkdirSync, rmSync } from 'node:fs';
import { resolve } from 'node:path';

const [label, profDir, port, sampleSecStr] = process.argv.slice(2, 6);
const sampleSec = Number(sampleSecStr);
const flagStart = process.argv.indexOf('--');
const flags = process.argv.slice(flagStart + 1);
const CHROME = process.env.PROBE_CHROME || '/opt/google/chrome/chrome';
const DISPLAY = process.env.PROBE_DISPLAY;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// WebGL stress page: animated fullscreen triangles with heavy draw + clear
const STRESS_HTML = `data:text/html,<style>body{margin:0;background:#000}</style><canvas id=c width=800 height=600></canvas><script>
const c=document.getElementById('c');const gl=c.getContext('webgl')||c.getContext('experimental-webgl');
let v=0;
function frame(){ if(!gl) return;
  gl.viewport(0,0,800,600); gl.clearColor(0,0,0,1); gl.clear(gl.COLOR_BUFFER_BIT);
  // issue many uniform/state changes to exercise driver
  for(let i=0;i<500;i++){ gl.clearColor((i%255)/255,((i*7)%255)/255,0.5,1); gl.clear(gl.COLOR_BUFFER_BIT); }
  gl.flush(); v++;
  requestAnimationFrame(frame);
}
frame();</script>`;

async function getPageWsUrl(PORT) {
  for (let i = 0; i < 80; i++) {
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
  constructor(u) { this.u = u; this.id = 0; this.p = new Map(); }
  async connect() {
    this.ws = new WebSocket(this.u);
    await new Promise((a, b) => { this.ws.onopen = a; this.ws.onerror = b; });
    this.ws.onmessage = (ev) => {
      const m = JSON.parse(ev.data);
      if (m.id && this.p.has(m.id)) {
        const { resolve, reject } = this.p.get(m.id);
        this.p.delete(m.id);
        if (m.error) reject(new Error(JSON.stringify(m.error)));
        else resolve(m.result);
      }
    };
  }
  send(method, params = {}, timeoutMs = 60000) {
    const id = ++this.id;
    return new Promise((resolve, reject) => {
      const t = setTimeout(() => { this.p.delete(id); reject(new Error('timeout ' + method)); }, timeoutMs);
      this.p.set(id, {
        resolve: (v) => { clearTimeout(t); resolve(v); },
        reject: (e) => { clearTimeout(t); reject(e); },
      });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }
  async eval(e) {
    const r = await this.send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true });
    return r.result ? r.result.value : undefined;
  }
}

function sampleChrome(pids) {
  const out = { browser: 0, gpu: 0, renderer: 0, utility: 0, other: 0, total: 0, memRssMB: 0, n: 0 };
  const pidSet = new Set(pids);
  const g = execSync(`pgrep -af chrome`, { encoding: 'utf8' }).split('\n').filter(Boolean);
  for (const line of g) {
    const m = line.match(/^(\d+)\s+(.*)$/);
    if (!m || !pidSet.has(Number(m[1]))) continue;
    const pid = Number(m[1]);
    const args = m[2];
    let cpu = 0, rss = 0;
    try { const s = execSync(`ps -o pcpu,rss --no-headers -p ${pid}`, { encoding: 'utf8' }); const mm = s.trim().match(/^\s*([\d.]+)\s+(\d+)/); if (mm) { cpu = Number(mm[1]); rss = Number(mm[2]) / 1024; } } catch (_) {}
    out.total += cpu;
    out.memRssMB += rss;
    out.n++;
    if (args.includes('--type=gpu-process')) out.gpu += cpu;
    else if (args.includes('--type=renderer')) out.renderer += cpu;
    else if (args.includes('--type=utility')) out.utility += cpu;
    else if (args.includes('--type=zygote') || args.includes('--type=crashpad')) out.other += cpu;
    else out.browser += cpu;
  }
  return out;
}

async function collectPids(browserPid) {
  const set = new Set([browserPid]);
  for (let i = 0; i < 5; i++) {
    const res = execSync(`pgrep -P ${browserPid}`, { encoding: 'utf8' }).trim();
    if (res) for (const p of res.split('\n')) { const n = Number(p); if (n) set.add(n); }
    // also find orphan children (PPID=1 but our profile dir)
  }
  return [...set];
}

async function main() {
  const PORT = port;
  mkdirSync(profDir, { recursive: true });
  const args = [...flags, `--remote-debugging-port=${PORT}`, `--user-data-dir=${resolve(profDir)}`, STRESS_HTML];
  const env = { ...process.env };
  if (DISPLAY) env.DISPLAY = DISPLAY;
  const child = spawn(CHROME, args, { env, stdio: ['ignore', 'ignore', 'pipe'], detached: true });
  child.unref();

  const result = { label, flags: args };
  try {
    const ws = await getPageWsUrl(PORT);
    const cdp = new CDP(ws);
    await cdp.connect();
    await cdp.send('Runtime.enable');
    await cdp.eval(`new Promise(r=>setTimeout(r,3000))`);
    const gl = await cdp.eval(`(()=>{const c=document.getElementById('c');const gl=c.getContext('webgl');const ext=gl.getExtension('WEBGL_debug_renderer_info');return ext?gl.getParameter(ext.UNMASKED_RENDERER_WEBGL):String(gl.getParameter(gl.RENDERER));})()`);
    result.renderer = gl;

    const pids = await collectPids(child.pid);
    result.pidCount = pids.length;
    result.samples = [];
    const t0 = Date.now();
    while (Date.now() - t0 < sampleSec * 1000) {
      await sleep(2000);
      result.samples.push({ at: Date.now() - t0, ...sampleChrome(pids) });
    }
    const avgs = {};
    for (const k of ['browser', 'gpu', 'renderer', 'utility', 'other', 'total', 'memRssMB']) {
      const vals = result.samples.map((s) => s[k]);
      avgs[k + '_avg'] = vals.reduce((a, b) => a + b, 0) / vals.length;
      avgs[k + '_max'] = Math.max(...vals);
    }
    result.aggregate = avgs;
  } catch (e) {
    result.error = e.message;
  }
  console.log(JSON.stringify(result, null, 2));
  try { process.kill(child.pid, 'SIGTERM'); } catch (_) {}
  await sleep(2500);
  try { rmSync(resolve(profDir), { recursive: true, force: true }); } catch (_) {}
  process.exit(0);
}

main().catch((e) => { console.error('FATAL', e); process.exit(1); });
