// Send a chat command (keypress emulation) and capture console for N seconds.
// Usage: node cmd.mjs <command> <captureSeconds>
const PORT = '9222';
const cmd = process.argv[2];
const seconds = Number(process.argv[3] || 15);
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
      const args = (m.params.args || []).map((a) => a.value !== undefined ? String(a.value) : (a.description || a.type));
      events.push(`[console.${m.params.type}] ${args.join(' ')}`);
    } else if (m.method === 'Log.entryAdded') {
      events.push(`[log.${m.params.entry.level}] ${m.params.entry.text}`);
    }
  };
  send('Runtime.enable');
  send('Log.enable');
  // emulate chat command
  const js = `(function(){
    function kd(k,c){document.dispatchEvent(new KeyboardEvent("keydown",{key:k,which:c,keyCode:c,charCode:c,bubbles:true}));
      document.dispatchEvent(new KeyboardEvent("keypress",{key:k,which:c,keyCode:c,charCode:c,bubbles:true}));
      document.dispatchEvent(new KeyboardEvent("keyup",{key:k,which:c,keyCode:c,bubbles:true}));}
    document.dispatchEvent(new KeyboardEvent("keydown",{key:"Escape",which:27,keyCode:27,bubbles:true}));
    setTimeout(function(){document.dispatchEvent(new KeyboardEvent("keyup",{key:"Escape",which:27,keyCode:27,bubbles:true}));},80);
    setTimeout(function(){
      kd("/",191);
      setTimeout(function(){
        var cmd=${JSON.stringify(cmd)};
        for(var i=0;i<cmd.length;i++){setTimeout(function(ch){kd(ch,ch.charCodeAt(0));},i*60);}
        setTimeout(function(){
          document.dispatchEvent(new KeyboardEvent("keydown",{key:"Enter",code:"Enter",which:13,keyCode:13,bubbles:true}));
          setTimeout(function(){document.dispatchEvent(new KeyboardEvent("keyup",{key:"Enter",code:"Enter",which:13,keyCode:13,bubbles:true}));},120);
        }, cmd.length*60+250);
      },250);
    },200);
    return "ok";})()`;
  send('Runtime.evaluate', { expression: js, returnByValue: true });
  await new Promise((r) => setTimeout(r, seconds * 1000));
  const marked = events.filter((e) => e.includes('RAILSYSTEM'));
  console.log('--- RAILSYSTEM EVENTS ---');
  if (marked.length === 0) console.log('(none)');
  else marked.forEach((e) => console.log(e));
  console.log('--- last 8 events ---');
  events.slice(-8).forEach((e) => console.log(e));
  process.exit(0);
}
main().catch((e) => { console.error('ERR', e); process.exit(1); });
