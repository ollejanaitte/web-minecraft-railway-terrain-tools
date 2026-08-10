#!/usr/bin/env node
/**
 * Phase 0.2 game boot test + CPU sampling.
 * Launches Chrome on the Eaglercraft offline HTML with given flags, samples
 * the chrome process tree CPU every 2s, detects title screen, reports results.
 *
 * Usage: node game_boot.mjs <label> <profileDir> <port> <maxSec> -- <flags...>
 * Env: GAME_HTML (path to HTML), OUT (json path), KEEP_PROFILE (1=keep)
 */
import { spawn, execSync } from 'node:child_process';
import { mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { resolve } from 'node:path';

const [label, profDir, port, maxSecStr] = process.argv.slice(2, 6);
const maxSec = Number(maxSecStr);
const flagStart = process.argv.indexOf('--');
const flags = process.argv.slice(flagStart + 1);
const CHROME = process.env.PROBE_CHROME || '/opt/google/chrome/chrome';
const DISPLAY = process.env.PROBE_DISPLAY;
const GAME_HTML = process.env.GAME_HTML || resolve(process.cwd(), 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const OUT = process.env.OUT;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function getPageWsUrl(PORT) {
  for (let i = 0; i < 120; i++) {
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
  async shot(f) {
    const r = await this.send('Page.captureScreenshot', { format: 'png' });
    writeFileSync(f, Buffer.from(r.data, 'base64'));
  }
}

function sampleChrome(pids) {
  const out = { browser: 0, gpu: 0, renderer: 0, utility: 0, other: 0, total: 0, memRssMB: 0, n: 0 };
  const pidSet = new Set(pids);
  let g = '';
  try { g = execSync(`pgrep -af chrome`, { encoding: 'utf8' }); } catch (_) {}
  for (const line of g.split('\n')) {
    const m = line.match(/^(\d+)\s+(.*)$/);
    if (!m || !pidSet.has(Number(m[1]))) continue;
    const args = m[2];
    let cpu = 0, rss = 0;
    try { const s = execSync(`ps -o pcpu,rss --no-headers -p ${m[1]}`, { encoding: 'utf8' }); const mm = s.trim().match(/^\s*([\d.]+)\s+(\d+)/); if (mm) { cpu = Number(mm[1]); rss = Number(mm[2]) / 1024; } } catch (_) {}
    out.total += cpu; out.memRssMB += rss; out.n++;
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
  for (let i = 0; i < 6; i++) {
    try { const res = execSync(`pgrep -P ${browserPid}`, { encoding: 'utf8' }).trim(); if (res) for (const p of res.split('\n')) { const n = Number(p); if (n) set.add(n); } } catch (_) {}
  }
  return [...set];
}

function analyzePng(path) {
  // quick python pixel stats: sky/dirt/hotbar
  const r = execSync(`python3 -c "from PIL import Image;import sys
im=Image.open('${path}').convert('RGB');w,h=im.size;px=im.load()
def frac(y0,y1,pred):
  n=0;t=0
  for y in range(int(h*y0),int(h*y1),2):
    for x in range(0,w,4):
      r,g,b=px[x,y];t+=1
      if pred(r,g,b):n+=1
  return n/max(1,t)
sky=frac(0,0.33,lambda r,g,b:b>r+10 and b>g+10 and b>100)
dirt=frac(0,1,lambda r,g,b:r>50 and r>g+10 and g>b+5 and r<180)
hot=0
for y in range(int(h*0.9),h,2):
  for x in range(0,w,4):
    r,g,b=px[x,y]
    if r<70 and g<70 and b<70: hot+=1
tot=(h-int(h*0.9))* (w//4)
print('sky=%.3f dirt=%.3f hot=%d'%(sky,dirt,hot))"`, { encoding: 'utf8' });
  return r.stdout.trim();
}

async function main() {
  mkdirSync(profDir, { recursive: true });
  const args = [...flags, `--remote-debugging-port=${port}`, `--user-data-dir=${resolve(profDir)}`, 'file://' + GAME_HTML];
  const env = { ...process.env };
  if (DISPLAY) env.DISPLAY = DISPLAY;
  const child = spawn(CHROME, args, { env, stdio: ['ignore', 'ignore', 'pipe'], detached: true });
  let stderr = '';
  child.stderr.on('data', (d) => { stderr += d; if (stderr.length > 300000) stderr = stderr.slice(-150000); });
  child.unref();

  const result = { label, args, startedAt: Date.now(), samples: [] };
  const t0 = Date.now();
  let cdp = null;
  try {
    const ws = await getPageWsUrl(port);
    result.cdpUpMs = Date.now() - t0;
    cdp = new CDP(ws);
    await cdp.connect();
    await cdp.send('Runtime.enable');
    await cdp.send('Page.enable');
    result.renderer = await cdp.eval(`(()=>{const c=document.createElement('canvas');const gl=c.getContext('webgl');const ext=gl.getExtension('WEBGL_debug_renderer_info');return ext?gl.getParameter(ext.UNMASKED_RENDERER_WEBGL):String(gl.getParameter(gl.RENDERER));})()`).catch(()=>null);
  } catch (e) {
    result.error = e.message;
  }

  const pids = await collectPids(child.pid);
  result.pidCount = pids.length;

  let titleAt = null;
  let titleImage = null;
  let firstWorldish = null;
  let consoleLines = [];
  if (cdp) {
    cdp.ws.onmessage = null; // keep default handler
  }

  while (Date.now() - t0 < maxSec * 1000) {
    await sleep(2000);
    const s = sampleChrome(pids);
    s.at = Date.now() - t0;
    result.samples.push(s);
    // periodic screenshot to detect title
    if (cdp && (Date.now() - t0) % 20000 < 2000 && !titleAt) {
      try {
        const p = resolve(profDir, 'probe_' + Date.now() + '.png');
        await cdp.shot(p);
        const m = analyzePng(p);
        const sky = parseFloat((m.match(/sky=([\d.]+)/)||[])[1]||0);
        const dirt = parseFloat((m.match(/dirt=([\d.]+)/)||[])[1]||0);
        const hot = parseInt((m.match(/hot=(\d+)/)||[])[1]||'0',10);
        result.lastShotAnalysis = m;
        if (!titleAt && (dirt > 0.3 || sky > 0.05)) {
          titleAt = Date.now() - t0;
          titleImage = p;
          result.titleAnalysis = m;
        }
      } catch (e) { /* page busy */ }
    }
  }

  result.titleAtMs = titleAt;
  result.titleImage = titleImage;
  const avgs = {};
  for (const k of ['browser','gpu','renderer','utility','other','total','memRssMB']) {
    const vals = result.samples.map((s) => s[k]);
    if (vals.length) { avgs[k+'_avg'] = vals.reduce((a,b)=>a+b,0)/vals.length; avgs[k+'_max'] = Math.max(...vals); avgs[k+'_last'] = vals[vals.length-1]; }
  }
  result.aggregate = avgs;
  // save chrome stderr gpu lines
  result.stderrLines = stderr.split('\n').filter((l)=>/(GL |GL_|GLES|GPU|gpu|SwiftShader|ANGLE|EGL|Vulkan|DRI|Fatal)/i.test(l)).slice(-60);
  console.log(JSON.stringify(result, null, 2));
  if (OUT) writeFileSync(OUT, JSON.stringify(result, null, 2));

  try { process.kill(child.pid, 'SIGTERM'); } catch (_) {}
  await sleep(2500);
  if (process.env.KEEP_PROFILE !== '1') { try { rmSync(resolve(profDir), { recursive: true, force: true }); } catch (_) {} }
  process.exit(0);
}

main().catch((e) => { console.error('FATAL', e); process.exit(1); });
