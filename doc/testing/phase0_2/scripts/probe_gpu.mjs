#!/usr/bin/env node
/**
 * Phase 0.2 GPU path probe: launches Chrome with a given rendering mode,
 * queries WebGL renderer/vendor/version + SystemInfo, writes result to
 * stdout as JSON, then closes Chrome.
 *
 * Usage: node probe_gpu.mjs <label> <profileDir> <port> -- <chrome flags...>
 *   e.g. node probe_gpu.mjs swiftshader /tmp/p 9335 -- --headless=new --no-sandbox --enable-unsafe-swiftshader --use-gl=angle --use-angle=swiftshader
 *
 * Env overrides: PROBE_DISPLAY (X display), PROBE_CHROME (binary path).
 */
import { spawn } from 'node:child_process';
import { mkdirSync, rmSync } from 'node:fs';
import { resolve } from 'node:path';

const [label, profDir, port] = process.argv.slice(2, 5);
const flagStart = process.argv.indexOf('--');
const flags = process.argv.slice(flagStart + 1);
const CHROME = process.env.PROBE_CHROME || '/opt/google/chrome/chrome';
const DISPLAY = process.env.PROBE_DISPLAY;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function getPageWsUrl(PORT) {
  for (let i = 0; i < 80; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${PORT}/json`)).json();
      const page = list.find((t) => t.type === 'page');
      if (page) return { ws: page.webSocketDebuggerUrl, targets: list };
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

const GLINFO = `(() => {
  const out = {};
  const tryG = (name, ctxType) => {
    try {
      const c = document.createElement('canvas');
      const gl = c.getContext(ctxType);
      if (!gl) return { ok: false, reason: 'no ' + ctxType + ' ctx' };
      const ext = gl.getExtension('WEBGL_debug_renderer_info');
      return {
        ok: true,
        renderer: ext ? gl.getParameter(ext.UNMASKED_RENDERER_WEBGL) : String(gl.getParameter(gl.RENDERER)),
        vendor: ext ? gl.getParameter(ext.UNMASKED_VENDOR_WEBGL) : String(gl.getParameter(gl.VENDOR)),
        version: gl.getParameter(gl.VERSION),
        shader: gl.getParameter(gl.SHADING_LANGUAGE_VERSION),
      };
    } catch (e) { return { ok: false, reason: e.message }; }
  };
  out.webgl = tryG('webgl', 'webgl');
  out.webgl2 = tryG('webgl2', 'webgl2');
  out.webgpu = (() => { try { return typeof navigator.gpu !== 'undefined' ? 'present' : 'absent'; } catch (e) { return 'err'; } })();
  out.hardwareAccel = (() => {
    try {
      const el = document.createElement('div');
      const info = {};
      // Chrome exposes GPU status via getContextAttributes/WebGL context creation errors only.
      return info;
    } catch (e) { return {}; }
  })();
  return out;
})()`;

async function main() {
  const PORT = port;
  mkdirSync(profDir, { recursive: true });
  const args = [
    ...flags,
    `--remote-debugging-port=${PORT}`,
    `--user-data-dir=${resolve(profDir)}`,
    '--window-size=800,600',
    'about:blank',
  ];
  const env = { ...process.env };
  if (DISPLAY) env.DISPLAY = DISPLAY;
  const child = spawn(CHROME, args, { env, stdio: ['ignore', 'ignore', 'pipe'], detached: true });
  let stderr = '';
  child.stderr.on('data', (d) => { stderr += d; if (stderr.length > 200000) stderr = stderr.slice(-100000); });
  child.unref();

  const result = { label, flags: args, startedAt: Date.now() };
  try {
    const { ws } = await getPageWsUrl(PORT);
    result.cdpUpMs = Date.now() - result.startedAt;
    const cdp = new CDP(ws);
    await cdp.connect();
    await cdp.send('Runtime.enable');
    result.gl = await cdp.eval(GLINFO);
    let sys;
    try { sys = await cdp.send('SystemInfo.getInfo', {}, 30000); } catch (e) { sys = { error: e.message }; }
    if (sys.gpu) {
      result.gpuInfo = {
        devices: (sys.gpu.devices || []).map((d) => ({ vendor: '0x' + d.vendorId.toString(16), device: '0x' + d.deviceId.toString(16), driver: d.driverVersion, name: d.name })),
        aux: sys.gpu.auxAttributes,
        featureStatus: sys.gpu.featureStatus,
      };
    }
    // chrome://gpu-like features from SystemInfo
    result.systemInfo = {
      modelName: sys.modelName,
      commandLine: sys.commandLine,
    };
    result.pageInfo = await cdp.eval(`({ title: document.title, url: location.href, ua: navigator.userAgent })`);
  } catch (e) {
    result.error = e.message;
  }

  // Extract GL/GPU lines from chrome stderr
  const glLines = stderr.split('\n').filter((l) => /GL_|gl_|GLES|GL Driver|gpu|GPU|SwiftShader|ANGLE|EGL|Vulkan|feature/i.test(l)).slice(0, 60);
  result.chromeStderrGpuLines = glLines;
  result.finishedAt = Date.now();

  console.log(JSON.stringify(result, null, 2));

  // Close chrome (SIGTERM to this browser tree)
  try { process.kill(child.pid, 'SIGTERM'); } catch (_) {}
  await sleep(3000);
  // cleanup profile
  try { rmSync(resolve(profDir), { recursive: true, force: true }); } catch (_) {}
  process.exit(0);
}

main().catch((e) => { console.error('FATAL', e); process.exit(1); });
