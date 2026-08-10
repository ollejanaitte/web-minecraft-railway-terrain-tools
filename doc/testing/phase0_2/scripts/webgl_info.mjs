#!/usr/bin/env node
// Query WebGL / ANGLE / GPU info from a CDP-connected Chrome page.
// Usage: node webgl_info.mjs <port>
const PORT = process.argv[2] || '9222';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function getPageWsUrl() {
  for (let i = 0; i < 30; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${PORT}/json`)).json();
      const page = list.find((t) => t.type === 'page');
      if (page) return page.webSocketDebuggerUrl;
    } catch (_) {}
    await sleep(500);
  }
  throw new Error('CDP unavailable on ' + PORT);
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

const EXPR = `(() => {
  const out = { userAgent: navigator.userAgent, platform: navigator.platform };
  const tryG = (fn) => { try { return fn(); } catch (e) { return 'ERR: ' + e.message; } };
  out.webgl = tryG(() => { const c = document.createElement('canvas'); const gl = c.getContext('webgl'); if (!gl) return 'no webgl ctx'; const ext = gl.getExtension('WEBGL_debug_renderer_info'); return { renderer: ext ? gl.getParameter(ext.UNMASKED_RENDERER_WEBGL) : gl.getParameter(gl.RENDERER), vendor: ext ? gl.getParameter(ext.UNMASKED_VENDOR_WEBGL) : gl.getParameter(gl.VENDOR), version: gl.getParameter(gl.VERSION), shader: gl.getParameter(gl.SHADING_LANGUAGE_VERSION) }; });
  out.webgl2 = tryG(() => { const c = document.createElement('canvas'); const gl = c.getContext('webgl2'); if (!gl) return 'no webgl2 ctx'; const ext = gl.getExtension('WEBGL_debug_renderer_info'); return { renderer: ext ? gl.getParameter(ext.UNMASKED_RENDERER_WEBGL) : gl.getParameter(gl.RENDERER), vendor: ext ? gl.getParameter(ext.UNMASKED_VENDOR_WEBGL) : gl.getParameter(gl.VENDOR), version: gl.getParameter(gl.VERSION) }; });
  return out;
})()`;

async function main() {
  const cdp = new CDP(await getPageWsUrl());
  await cdp.connect();
  const info = await cdp.eval(EXPR);
  console.log(JSON.stringify(info, null, 2));
  // Also fetch SystemInfo via Browser domain
  const sys = await cdp.send('SystemInfo.getInfo', {}, 30000).catch((e) => ({ error: e.message }));
  if (sys.gpu) {
    console.log('GPU (SystemInfo):');
    for (const d of sys.gpu.devices || []) console.log('  device', d.vendorId.toString(16), d.deviceId.toString(16), d.driverVendor, d.driverVersion);
    console.log('  auxAttributes:', JSON.stringify(sys.gpu.auxAttributes || {}));
  }
  const features = await cdp.send('SystemInfo.getFeatureState', { featureState: 'canvas-2d' }, 30000).catch(() => null);
  console.log('featureState:', JSON.stringify(features));
}

main().catch((e) => { console.error('ERR', e); process.exit(1); });
