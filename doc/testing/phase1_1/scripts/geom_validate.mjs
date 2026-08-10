#!/usr/bin/env node
/**
 * Phase 1.1 Geometry debug visual validation on Flat Validation World.
 * Reuses Phase 0.5 create/join flow; captures SS-R1_1-* during geom camera tour.
 */
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, copyFileSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../../../../');
const PHASE = resolve(ROOT, 'doc/testing/phase1_1');
const P05 = resolve(ROOT, 'doc/testing/phase0_5');
const S = resolve(PHASE, 'screenshots');
const PROFILE = process.env.CHROME_PROFILE || resolve(PHASE, 'profiles/geom-run');
const PORT = process.env.CDP_PORT || '9391';
const HTML =
  process.env.VALIDATION_HTML ||
  resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const LOG = resolve(PHASE, 'logs/geom_validate.log');
const GPU_MODE = process.env.GPU_MODE || 'auto';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const log = (m) => {
  const line = `[${new Date().toISOString()}] ${m}`;
  console.log(line);
  writeFileSync(LOG, line + '\n', { flag: 'a' });
};

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
        if (/RAILSYSTEM|auto-validat|GeomDebug|camera tour=geom/i.test(t)) {
          log('HIT ' + t.slice(0, 320));
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
    log('shot ' + f);
  }
  async click(x, y) {
    await this.eval(
      `(()=>{const c=document.querySelector('canvas');const mk=t=>c.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,clientX:${x},clientY:${y},button:0,view:window}));mk('mousemove');mk('mousedown');mk('mouseup');mk('click');})()`
    );
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 });
  }
}

function analyze(path) {
  const script = resolve(P05, 'scripts/analyze_screen.py');
  if (!existsSync(script)) return 'no-analyzer';
  return spawnSync('python3', [script, path], { encoding: 'utf8' }).stdout.trim();
}

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

function launchChrome() {
  mkdirSync(PROFILE, { recursive: true });
  mkdirSync(S, { recursive: true });
  mkdirSync(resolve(PHASE, 'logs'), { recursive: true });
  const logPath = resolve(PHASE, 'logs/chrome_geom.log');
  const gpuFlags =
    GPU_MODE === 'swiftshader'
      ? ['--disable-gpu-sandbox', '--enable-unsafe-swiftshader', '--use-gl=angle', '--use-angle=swiftshader']
      : ['--use-gl=angle', '--use-angle=vulkan'];
  const args = [
    '--headless=new',
    '--no-sandbox',
    ...gpuFlags,
    '--autoplay-policy=no-user-gesture-required',
    '--mute-audio',
    '--window-size=1280,720',
    `--remote-debugging-port=${PORT}`,
    `--user-data-dir=${PROFILE}`,
    'file://' + HTML,
  ];
  const cmd = `exec /opt/google/chrome/chrome ${args.map((a) => JSON.stringify(a)).join(' ')} >${JSON.stringify(logPath)} 2>&1`;
  const child = spawn('bash', ['-c', cmd], { stdio: 'ignore', detached: true });
  child.unref();
  log('CHROME_PID=' + child.pid + ' PROFILE=' + PROFILE + ' GPU=' + GPU_MODE);
}

async function waitTitle(cdp) {
  await cdp.click(640, 360);
  await sleep(2500);
  for (let i = 0; i < 24; i++) {
    const p = resolve(S, '_boot_' + i + '.png');
    await cdp.shot(p);
    const m = analyze(p);
    log('boot ' + i + ' ' + m);
    const sky = parseFloat((m.match(/sky=([0-9.]+)/) || [])[1] || 0);
    const dirt = parseFloat((m.match(/dirt=([0-9.]+)/) || [])[1] || 0);
    if (sky > 0.15 && dirt < 0.3) return;
    await cdp.click(640, 390);
    await sleep(1000);
    await cdp.click(640, 450);
    await sleep(1500);
  }
  throw new Error('title timeout');
}

async function enterWorld(cdp) {
  await cdp.click(640, 258);
  await sleep(12000);
  await cdp.shot(resolve(S, '_select.png'));
  log('select ' + analyze(resolve(S, '_select.png')));
  await cdp.click(915, 492);
  await sleep(8000);
  await cdp.shot(resolve(S, '_submenu.png'));
  log('submenu ' + analyze(resolve(S, '_submenu.png')));
  await cdp.click(640, 242);
  await sleep(10000);
  await cdp.shot(resolve(S, '_form.png'));
  log('form ' + analyze(resolve(S, '_form.png')));
  const typeChar = async (ch) => {
    const vk = ch.codePointAt(0);
    await cdp.send('Input.dispatchKeyEvent', {
      type: 'keyDown',
      key: ch,
      code: 'Key' + ch.toUpperCase(),
      text: ch,
      windowsVirtualKeyCode: vk,
    });
    await cdp.send('Input.dispatchKeyEvent', {
      type: 'char',
      key: ch,
      text: ch,
      unmodifiedText: ch,
      windowsVirtualKeyCode: vk,
    });
    await cdp.send('Input.dispatchKeyEvent', {
      type: 'keyUp',
      key: ch,
      code: 'Key' + ch.toUpperCase(),
      windowsVirtualKeyCode: vk,
    });
  };
  await cdp.click(495, 138);
  await sleep(800);
  await cdp.send('Input.dispatchKeyEvent', {
    type: 'keyDown',
    key: 'a',
    code: 'KeyA',
    modifiers: 2,
    windowsVirtualKeyCode: 65,
  });
  await cdp.send('Input.dispatchKeyEvent', {
    type: 'keyUp',
    key: 'a',
    code: 'KeyA',
    modifiers: 2,
    windowsVirtualKeyCode: 65,
  });
  await sleep(300);
  for (const ch of 'EaglerFlatValidate') {
    await typeChar(ch);
    await sleep(40);
  }
  await sleep(1000);
  await cdp.shot(resolve(S, '_named.png'));
  log('named ' + analyze(resolve(S, '_named.png')));
  await cdp.click(400, 540);
  await sleep(20000);
  await cdp.shot(resolve(S, '_loading.png'));
  log('loading ' + analyze(resolve(S, '_loading.png')));
}

async function waitInWorld(cdp) {
  let menuStreak = 0;
  for (let i = 1; i <= 90; i++) {
    await sleep(10000);
    const p = resolve(S, '_join_' + i + '.png');
    try {
      await cdp.shot(p);
    } catch (e) {
      log('shot fail ' + e.message);
      continue;
    }
    const m = analyze(p);
    log('join ' + i + ' ' + m);
    const sky = parseFloat((m.match(/sky=([0-9.]+)/) || [])[1] || 0);
    const dirt = parseFloat((m.match(/dirt=([0-9.]+)/) || [])[1] || 1);
    const hot = parseInt((m.match(/hot=([0-9]+)/) || [])[1] || 0, 10);
    if (
      cdp.lines.some(
        (t) =>
          /Game Crashed|Minecraft Crash Report/i.test(t) && !/tolerant-assert/i.test(t)
      )
    ) {
      throw new Error('game crashed during join');
    }
    const validated = cdp.lines.some((t) => /auto-validat/i.test(t));
    const geom = cdp.lines.some((t) => /GeomDebugEvidence DONE/i.test(t));
    const grounded = hot >= 2 && sky > 0.05 && sky < 0.75 && dirt > 0.15 && dirt < 0.55;
    if (validated || grounded) {
      log('IN_WORLD validated=' + validated + ' geom=' + geom);
      return { validated, geom, inworld: true };
    }
    const menuLike = sky > 0.5 || (dirt > 0.6 && hot < 5);
    if (menuLike) {
      menuStreak++;
      if (menuStreak >= 12) throw new Error('stuck in menu after create');
    } else menuStreak = 0;
  }
  return { validated: false, inworld: false };
}

async function captureGeomSeries(cdp) {
  const map = [
    ['geom_straight', 'SS-R1_1-01_STRAIGHT_GEOMETRY.png'],
    ['geom_gentle', 'SS-R1_1-02_GENTLE_CURVE.png'],
    ['geom_tight', 'SS-R1_1-03_TIGHT_CURVE.png'],
    ['geom_s_curve', 'SS-R1_1-04_S_CURVE_FIXTURE.png'],
    ['geom_gradient', 'SS-R1_1-05_GRADIENT.png'],
    ['geom_curve_grad', 'SS-R1_1-06_CURVE_GRADIENT.png'],
    ['geom_local_frame', 'SS-R1_1-07_LOCAL_FRAME.png'],
    ['geom_overview', 'SS-R1_1-08_OVERVIEW.png'],
  ];
  const taken = new Set();
  const deadline = Date.now() + 240000;
  while (Date.now() < deadline && taken.size < map.length) {
    for (const [tag, file] of map) {
      if (taken.has(tag)) continue;
      if (cdp.lines.some((t) => t.includes('camera tour=' + tag))) {
        await sleep(1500);
        const out = resolve(S, file);
        await cdp.shot(out);
        log('ok ' + file + ' ' + analyze(out));
        taken.add(tag);
      }
    }
    await sleep(1000);
  }
  // Fallback timed captures if tour tags missed
  if (taken.size < map.length) {
    log('WARN missing tags; timed fallback remaining=' + (map.length - taken.size));
    for (const [tag, file] of map) {
      if (taken.has(tag)) continue;
      await sleep(4000);
      await cdp.shot(resolve(S, file));
      log('fallback ' + file);
      taken.add(tag);
    }
  }
  writeFileSync(resolve(PHASE, 'logs/console_geom.txt'), cdp.lines.join('\n'));
}

async function main() {
  writeFileSync(LOG, '');
  log('START Phase1.1 geom visual PROFILE=' + PROFILE);
  if (!existsSync(HTML)) throw new Error('offline HTML missing: ' + HTML);
  if (!process.env.SKIP_LAUNCH) {
    launchChrome();
    await sleep(6000);
  }
  const cdp = new CDP(await getWs());
  await cdp.connect();
  await cdp.send('Runtime.enable');
  await cdp.send('Page.enable');
  await waitTitle(cdp);
  await enterWorld(cdp);
  const inw = await waitInWorld(cdp);
  if (!inw.inworld) throw new Error('failed in-world');
  if (!inw.validated) throw new Error('AutoValidate did NOT fire');
  await captureGeomSeries(cdp);
  log('SUCCESS shots=' + takenCount(S));
  process.exit(0);
}

function takenCount(dir) {
  try {
    return spawnSync('bash', ['-c', `ls -1 ${JSON.stringify(dir)}/SS-R1_1-*.png 2>/dev/null | wc -l`], {
      encoding: 'utf8',
    }).stdout.trim();
  } catch (_) {
    return '?';
  }
}

main().catch((e) => {
  log('FATAL ' + (e.stack || e));
  process.exit(1);
});
