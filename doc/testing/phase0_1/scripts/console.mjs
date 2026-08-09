// Attach to the page, reload, and dump console/log events for N seconds.
import { writeFileSync } from 'node:fs';

const PORT = process.argv[2];
const seconds = Number(process.argv[3] || 20);
const out = process.argv[4] || 'console.txt';

async function main() {
  const list = await (await fetch(`http://127.0.0.1:${PORT}/json`)).json();
  const page = list.find((t) => t.type === 'page');
  const ws = new WebSocket(page.webSocketDebuggerUrl);
  await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
  let id = 0;
  const send = (method, params = {}) => ws.send(JSON.stringify({ id: ++id, method, params }));
  const events = [];
  ws.onmessage = (ev) => {
    const m = JSON.parse(ev.data);
    if (m.method === 'Runtime.consoleAPICalled') {
      const args = (m.params.args || []).map((a) => a.value !== undefined ? JSON.stringify(a.value) : a.description || a.type);
      events.push(`[console.${m.params.type}] ${args.join(' ')}`);
    } else if (m.method === 'Runtime.exceptionThrown') {
      events.push('[exception] ' + JSON.stringify(m.params.exceptionDetails && m.params.exceptionDetails.exception ? m.params.exceptionDetails.exception.description : m.params));
    } else if (m.method === 'Log.entryAdded') {
      events.push(`[log.${m.params.entry.level}] ${m.params.entry.text}`);
    }
  };
  send('Runtime.enable');
  send('Log.enable');
  send('Page.enable');
  send('Page.reload', { ignoreCache: true });
  await new Promise((r) => setTimeout(r, seconds * 1000));
  writeFileSync(out, events.join('\n'));
  console.log('captured', events.length, 'events ->', out);
  process.exit(0);
}
main().catch((e) => { console.error('ERR', e); process.exit(1); });
