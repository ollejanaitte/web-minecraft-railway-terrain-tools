#!/usr/bin/env node
// Watch console + periodically screenshot. Usage: node watch_console.mjs <port> <sec> <outprefix>
import { writeFileSync } from 'node:fs';
import { execSync } from 'node:child_process';
const PORT = process.argv[2];
const SEC = Number(process.argv[3]);
const OUT = process.argv[4];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function getWs() {
  for (let i = 0; i < 120; i++) {
    try { const l = await (await fetch(`http://127.0.0.1:${PORT}/json`)).json(); const p = l.find((t) => t.type === 'page'); if (p) return p.webSocketDebuggerUrl; } catch (_) {}
    await sleep(500);
  }
  throw new Error('no cdp');
}

const ws = new WebSocket(await getWs());
await new Promise((a, b) => { ws.onopen = a; ws.onerror = b; });
let id = 0; const pending = new Map();
const lines = [];
ws.onmessage = (ev) => {
  const m = JSON.parse(ev.data);
  if (m.method === 'Runtime.consoleAPICalled') {
    const t = (m.params.args || []).map((a) => a.value ?? a.description ?? '').join(' ').slice(0, 300);
    lines.push(t);
    const ts = new Date().toISOString();
    try { execSync(`echo '${ts} ${t.replace(/'/g, '')}' >> ${OUT}_console.log`); } catch (_) {}
  }
  if (m.id && pending.has(m.id)) { const { resolve, reject } = pending.get(m.id); pending.delete(m.id); m.error ? reject(new Error(JSON.stringify(m.error))) : resolve(m.result); }
};
const send = (method, params = {}) => new Promise((resolve, reject) => { const i = ++id; pending.set(i, { resolve, reject }); ws.send(JSON.stringify({ id: i, method, params })); });

await send('Runtime.enable');
await send('Page.enable');
const t0 = Date.now();
let shots = 0;
while (Date.now() - t0 < SEC * 1000) {
  await sleep(5000);
  shots++;
  try {
    const r = await send('Page.captureScreenshot', { format: 'png' }, );
    writeFileSync(`${OUT}_shot${shots}.png`, Buffer.from(r.data, 'base64'));
    const a = execSync(`python3 -c "from PIL import Image
im=Image.open('${OUT}_shot${shots}.png').convert('RGB');w,h=im.size;px=im.load()
from collections import Counter
c=Counter()
for y in range(0,h,3):
  for x in range(0,w,5):
    c[px[x,y]]+=1
top=c.most_common(3)
def fr(y0,y1,pred):
  n=t=0
  for y in range(int(h*y0),int(h*y1),3):
    for x in range(0,w,5):
      r,g,b=px[x,y];t+=1
      if pred(r,g,b):n+=1
  return round(n/max(1,t),3)
print('top', top, 'sky', fr(0,.33,lambda r,g,b:b>100 and b>r+10 and b>g+10), 'dirt', fr(0,1,lambda r,g,b:r>50 and r>g+10 and g>b+5 and r<200))"`, { encoding: 'utf8' });
    console.log('shot', shots, (Date.now()-t0)/1000, 's', a.stdout.trim());
  } catch (e) { console.log('shot fail', shots, e.message.slice(0,80)); }
}
console.log('--- console lines:', lines.length);
console.log(lines.slice(-30).join('\n'));
writeFileSync(`${OUT}_console_final.txt`, lines.join('\n'));
process.exit(0);
