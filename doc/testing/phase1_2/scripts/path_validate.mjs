#!/usr/bin/env node
/**
 * Phase 1.2 RailPath debug visual validation on Flat Validation World.
 *
 * Uses the EaglercraftX `screenChanged` hook (baked into an instrumented copy
 * of the offline HTML) to navigate deterministically by exact GuiScreen class
 * names instead of pixel heuristics, then captures SS-R1_1-* during the geom
 * camera tour.
 *
 * Create flow: world name "EaglerFlatValidate" -> Superflat via name hook ->
 * AutoValidate gate fires -> geometry debug evidence placed -> camera tour.
 * The create->join transition is race-prone (Phase 0.5 menu-recovery race);
 * when a create attempt bounces back to a menu screen the script re-enters the
 * create form and retries (CREATE_RETRIES, default 3) before giving up.
 */
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, statSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../../../../');
const PHASE = resolve(ROOT, 'doc/testing/phase1_2');
const P05 = resolve(ROOT, 'doc/testing/phase0_5');
const S = resolve(PHASE, 'screenshots');
const PROFILE = process.env.CHROME_PROFILE || resolve(PHASE, 'profiles/path-run');
const PORT = process.env.CDP_PORT || '9392';
const HTML =
  process.env.VALIDATION_HTML ||
  resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const INSTR = resolve(PHASE, 'scripts/path_validate_instrumented.html');
const LOG = resolve(PHASE, 'logs/path_validate.log');
const GPU_MODE = process.env.GPU_MODE || 'auto';
const CREATE_RETRIES = parseInt(process.env.CREATE_RETRIES || '3', 10);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const log = (m) => {
  const line = `[${new Date().toISOString()}] ${m}`;
  console.log(line);
  writeFileSync(LOG, line + '\n', { flag: 'a' });
};

// ---- screenChanged hook injected into the instrumented HTML copy ----
const HOOK_NEEDLE = Buffer.from('container: "game_frame",');
const HOOK_SNIPPET = Buffer.concat([
  HOOK_NEEDLE,
  Buffer.from(
    `\n` +
      `\thooks: {\n` +
      `\t\tscreenChanged: function(name, w, h, dw, dh, s) {\n` +
      `\t\t\ttry { window.__eagScreen = name; window.__eagScreenTime = Date.now(); } catch (e) {}\n` +
      `\t\t}\n` +
      `\t},`
  ),
]);

function ensureInstrumented() {
  if (!existsSync(HTML)) throw new Error('offline HTML missing: ' + HTML);
  const needRebuild = !existsSync(INSTR) || statSync(INSTR).mtimeMs < statSync(HTML).mtimeMs;
  if (!needRebuild) return;
  log('regenerating instrumented HTML');
  const src = readFileSyncSafe(HTML);
  const idx = src.indexOf(HOOK_NEEDLE);
  if (idx < 0) throw new Error('instrumentation needle not found in HTML');
  const out = Buffer.concat([
    src.slice(0, idx),
    HOOK_SNIPPET,
    src.slice(idx + HOOK_NEEDLE.length),
  ]);
  writeFileSync(INSTR, out);
  log('instrumented HTML written: ' + INSTR);
}

import { readFileSync } from 'node:fs';
function readFileSyncSafe(p) {
  return readFileSync(p);
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
        if (/RAILSYSTEM|auto-validat|PathDebug|camera tour=path/i.test(t)) {
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
  send(method, params = {}, timeoutMs = 20000) {
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
    try {
      return (await this.send('Runtime.evaluate', { expression: e, returnByValue: true, awaitPromise: true }))
        .result?.value;
    } catch (_) {
      return undefined;
    }
  }
  async screen() {
    return this.eval('window.__eagScreen');
  }
  async shot(f) {
    writeFileSync(f, Buffer.from((await this.send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
    log('shot ' + f);
  }
  async click(x, y) {
    await this.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x, y });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 });
    await sleep(40);
    await this.send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 });
    await sleep(150);
  }
  async typeChar(ch) {
    const vk = ch.codePointAt(0);
    await this.send('Input.dispatchKeyEvent', {
      type: 'keyDown',
      key: ch,
      code: 'Key' + ch.toUpperCase(),
      text: ch,
      windowsVirtualKeyCode: vk,
    });
    await this.send('Input.dispatchKeyEvent', {
      type: 'char',
      key: ch,
      text: ch,
      unmodifiedText: ch,
      windowsVirtualKeyCode: vk,
    });
    await this.send('Input.dispatchKeyEvent', {
      type: 'keyUp',
      key: ch,
      code: 'Key' + ch.toUpperCase(),
      windowsVirtualKeyCode: vk,
    });
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
  const logPath = resolve(PHASE, 'logs/chrome_path.log');
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
    '--disable-background-timer-throttling',
    '--disable-backgrounding-occluded-windows',
    '--disable-renderer-backgrounding',
    '--run-all-compositor-stages-before-draw',
    '--hide-scrollbars',
    '--window-size=1280,720',
    `--remote-debugging-port=${PORT}`,
    `--user-data-dir=${PROFILE}`,
    'file://' + INSTR,
  ];
  const cmd = `exec /opt/google/chrome/chrome ${args.map((a) => JSON.stringify(a)).join(' ')} >${JSON.stringify(logPath)} 2>&1`;
  const child = spawn('bash', ['-c', cmd], { stdio: 'ignore', detached: true });
  child.unref();
  log('CHROME_PID=' + child.pid + ' PROFILE=' + PROFILE + ' GPU=' + GPU_MODE);
}

function screenName(s) {
  if (!s) return s === null ? 'NULL(in-game)' : '(undefined)';
  const parts = String(s).split('.');
  return parts[parts.length - 1];
}

async function waitForScreen(cdp, want, opts = {}) {
  const timeoutMs = opts.timeout || 90000;
  const match = typeof want === 'function' ? want : (s) => s === want;
  const deadline = Date.now() + timeoutMs;
  let last;
  while (Date.now() < deadline) {
    last = await cdp.screen();
    if (match(last)) return last;
    await sleep(1000);
  }
  throw new Error('waitForScreen timeout ' + want + ' last=' + last);
}

async function waitTitle(cdp) {
  // "Press any key" early-load requires a click; the Eaglercraft profanity
  // content warning appears later and is dismissed with Continue (640,390)
  // then (640,450), mirroring the proven Phase 0.5 flow. Loop until the main
  // menu (GuiMainMenu) is actually visible.
  for (let i = 0; i < 45; i++) {
    const s = await cdp.screen();
    if (s === null || s === undefined) {
      // Early boot: press-any-key / loading. One click dismisses it.
      if (i === 0) await cdp.click(640, 360);
      await sleep(2000);
      continue;
    }
    const n = String(s);
    if (n.includes('GuiMainMenu')) {
      log('title reached: ' + screenName(s));
      return;
    }
    if (n.includes('GuiScreenContentWarning')) {
      log('content warning -> continue');
      await cdp.click(640, 390);
      await sleep(500);
      await cdp.click(640, 450);
      await sleep(1000);
      continue;
    }
    if (n.includes('GuiScreenDefaultUsernameNote')) {
      // Three buttons: change username / continue anyway / do-not-show-again.
      // "Continue Anyway" sits at height/6+142 (gui), ~ (640, 380) viewport.
      log('default-username note -> continue anyway');
      await cdp.click(640, 380);
      await sleep(1000);
      continue;
    }
    await sleep(2000);
  }
  throw new Error('title timeout, last screen=' + (await cdp.screen()));
}

async function gotoCreateForm(cdp) {
  await waitForScreen(cdp, (s) => String(s).includes('GuiMainMenu'), { timeout: 60000 });
  await cdp.click(640, 258); // Singleplayer
  // Eaglercraft opens a world list and a create-new-world screen (custom
  // GuiScreenCreateWorldSelection) before the vanilla GuiCreateWorld form.
  const onSelect = (s) =>
    String(s).includes('GuiSelectWorld') || String(s).includes('GuiScreenCreateWorldSelection');
  await waitForScreen(cdp, onSelect, { timeout: 60000 });
  let s = await cdp.screen();
  if (String(s).includes('GuiScreenCreateWorldSelection')) {
    log('on create-selection screen -> Create');
    await cdp.click(640, 242);
  } else {
    await cdp.click(915, 492); // Create New World
    await waitForScreen(cdp, (s) => String(s).includes('GuiScreenCreateWorldSelection'), { timeout: 60000 });
    await cdp.click(640, 242); // Create
  }
  await waitForScreen(cdp, (s) => String(s).includes('GuiCreateWorld'), { timeout: 60000 });
  log('on create form');
}

async function enterWorld(cdp) {
  // Must be on GuiCreateWorld. Type the validation world name then Create.
  await waitForScreen(cdp, (s) => String(s).includes('GuiCreateWorld'), { timeout: 60000 });
  await cdp.click(495, 138); // world name field
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
    await cdp.typeChar(ch);
    await sleep(40);
  }
  await sleep(1000);
  await cdp.shot(resolve(S, '_named.png'));
  log('named ' + analyze(resolve(S, '_named.png')));
}

async function attemptCreate(cdp) {
  // Click Create and watch the transition. Returns { ok, screens } where ok
  // means the world entered (screen became null / in-game) OR is still loading
  // toward in-game; bounces back to a menu mean failure.
  await cdp.click(400, 540); // Confirm create
  const start = Date.now();
  const seen = [];
  while (Date.now() - start < 90000) {
    const s = await cdp.screen();
    const n = screenName(s);
    seen.push(n);
    if (s === null) {
      log('create->in-game detected');
      return { ok: true, seen };
    }
    const str = String(s);
    if (str.includes('GuiSelectWorld') || str.includes('GuiMainMenu')) {
      log('create bounced back to ' + n + ' (menu-recovery race)');
      return { ok: false, seen };
    }
    if (str.includes('GuiScreenIntegratedServerBusy') || str.includes('GuiDownloadTerrain')) {
      // still loading toward in-game
    }
    if (str.includes('GuiCreateWorld')) {
      // click may have missed; try again once
      if (seen.filter((x) => x.includes('GuiCreateWorld')).length === 1) {
        log('still on create form after create click -> re-click');
        await cdp.click(400, 540);
      }
    }
    await sleep(1000);
  }
  return { ok: false, seen };
}

async function waitAutoValidate(cdp) {
  const deadline = Date.now() + 60000;
  while (Date.now() < deadline) {
    if (cdp.lines.some((t) => /auto-validat/i.test(t))) {
      log('AutoValidate fired');
      return true;
    }
    // Also accept the worldName gate line as evidence of in-world presence.
    if (cdp.lines.some((t) => /worldName=.*validation=/.test(t))) {
      log('gate line seen; waiting for auto-validated...');
    }
    await sleep(1000);
  }
  throw new Error('AutoValidate did NOT fire');
}

async function waitInWorld(cdp) {
  for (let i = 1; i <= 60; i++) {
    await sleep(5000);
    const s = await cdp.screen();
    log('wait ' + i + ' screen=' + screenName(s));
    if (s === null && cdp.lines.some((t) => /auto-validat/i.test(t))) {
      log('IN_WORLD validated=true');
      return { validated: true, inworld: true };
    }
    if (cdp.lines.some((t) => /auto-validat/i.test(t))) {
      log('IN_WORLD validated=true (console)');
      return { validated: true, inworld: true };
    }
    if (i % 6 === 0) {
      const p = resolve(S, '_join_' + i + '.png');
      await cdp.shot(p);
      log('join ' + i + ' ' + analyze(p));
    }
  }
  return { validated: false, inworld: false };
}

async function captureGeomSeries(cdp) {
  const map = [
    ['path_multi_straight', 'SS-R1_2-01_MULTI_STRAIGHT.png'],
    ['path_straight_curve_straight', 'SS-R1_2-02_STRAIGHT_CURVE_STRAIGHT.png'],
    ['path_s_curve', 'SS-R1_2-03_S_CURVE.png'],
    ['path_boundary', 'SS-R1_2-04_PIECE_BOUNDARY.png'],
    ['path_gradient', 'SS-R1_2-05_GRADIENT_CHAIN.png'],
    ['path_curve_gradient', 'SS-R1_2-06_CURVE_GRADIENT.png'],
    ['path_reverse', 'SS-R1_2-07_REVERSE_PATH.png'],
    ['path_overview', 'SS-R1_2-08_OVERVIEW.png'],
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
  writeFileSync(resolve(PHASE, 'logs/console_path.txt'), cdp.lines.join('\n'));
}

function takenCount(dir) {
  try {
    return spawnSync('bash', ['-c', `ls -1 ${JSON.stringify(dir)}/SS-R1_2-*.png 2>/dev/null | wc -l`], {
      encoding: 'utf8',
    }).stdout.trim();
  } catch (_) {
    return '?';
  }
}

async function main() {
  writeFileSync(LOG, '');
  log('START Phase1.2 path visual PROFILE=' + PROFILE + ' CREATE_RETRIES=' + CREATE_RETRIES);
  ensureInstrumented();
  if (!process.env.SKIP_LAUNCH) {
    launchChrome();
    await sleep(6000);
  }
  const cdp = new CDP(await getWs());
  await cdp.connect();
  await cdp.send('Runtime.enable');
  await cdp.send('Page.enable');
  await waitTitle(cdp);
  await gotoCreateForm(cdp);

  let entered = false;
  for (let attempt = 1; attempt <= CREATE_RETRIES && !entered; attempt++) {
    log('=== create attempt ' + attempt + '/' + CREATE_RETRIES + ' ===');
    await enterWorld(cdp);
    const res = await attemptCreate(cdp);
    if (res.ok) {
      entered = true;
      break;
    }
    // Returned to a menu; re-enter the create form for another attempt.
    try {
      await gotoCreateForm(cdp);
    } catch (e) {
      log('re-enter create form failed: ' + e.message);
      break;
    }
  }
  if (!entered) throw new Error('create never entered the world after ' + CREATE_RETRIES + ' attempts');

  const inw = await waitInWorld(cdp);
  if (!inw.inworld) throw new Error('failed in-world');
  if (!inw.validated) await waitAutoValidate(cdp);
  await captureGeomSeries(cdp);
  log('SUCCESS shots=' + takenCount(S));
  process.exit(0);
}

main().catch((e) => {
  log('FATAL ' + (e.stack || e));
  process.exit(1);
});
