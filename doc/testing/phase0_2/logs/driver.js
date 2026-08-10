const fs=require('fs');
const {execSync}=require('child_process');
const ROOT='/home/masaharu/Web Minecraft with Railway Mod & Terrain Editing Tools';
const sleep=(ms)=>new Promise(r=>setTimeout(r,ms));
const LOG=(m)=>{console.log(m); try{fs.appendFileSync(ROOT+'/doc/testing/phase0_2/logs/driver_out.txt', m+'\n');}catch(_){}};
(async()=>{
  let page=null;
  for(let i=0;i<180;i++){
    try{const l=await (await fetch('http://127.0.0.1:9362/json')).json(); page=l.find(t=>t.type==='page'); if(page)break;}catch(_){}
    await sleep(1000);
  }
  if(!page){LOG('no cdp');process.exit(1);}
  const ws=new WebSocket(page.webSocketDebuggerUrl);
  await new Promise((a,b)=>{ws.onopen=a;ws.onerror=b;});
  let id=0; const pending=new Map(); const lines=[];
  ws.onmessage=(ev)=>{const m=JSON.parse(ev.data);
    if(m.method==='Runtime.consoleAPICalled'){const t=(m.params.args||[]).map(a=>a.value??a.description??'').join(' '); lines.push(t); if(/integrated|server|railsysv2|auto-validat|Preparing|Crash|ACK/i.test(t)) LOG('CONSOLE: '+t.slice(0,200));}
    if(m.id&&pending.has(m.id)){const {res,rej}=pending.get(m.id); pending.delete(m.id); m.error?rej(m.error):res(m.result);}};
  const send=(method,params={})=>new Promise((res,rej)=>{const i=++id; pending.set(i,{res,rej}); ws.send(JSON.stringify({id:i,method,params}));});
  await send('Runtime.enable');
  const ev=async(e)=>{const r=await send('Runtime.evaluate',{expression:e,returnByValue:true,awaitPromise:true}); return r.result?r.result.value:r.exceptionDetails;};
  const click=async(x,y)=>{await ev(`(()=>{const c=document.querySelector('canvas');for(const t of ['mousemove','mousedown','mouseup','click'])c.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,clientX:${x},clientY:${y},button:0,view:window}));})()`); await send('Input.dispatchMouseEvent',{type:'mouseMoved',x,y}); await sleep(40); await send('Input.dispatchMouseEvent',{type:'mousePressed',x,y,button:'left',clickCount:1}); await sleep(40); await send('Input.dispatchMouseEvent',{type:'mouseReleased',x,y,button:'left',clickCount:1});};
  const shot=async(n)=>{const r=await send('Page.captureScreenshot',{format:'png'}); fs.writeFileSync(ROOT+'/doc/testing/phase0_2/logs/d_'+n+'.png',Buffer.from(r.data,'base64'));};
  const analyze=(n)=>execSync('python3 '+JSON.stringify(ROOT+'/doc/testing/phase0_2/scripts/analyze_buttons.py')+' '+JSON.stringify(ROOT+'/doc/testing/phase0_2/logs/d_'+n+'.png'),{encoding:'utf8'}).trim();
  // Phase 1: wait for boot with periodic center click to unblock
  LOG('waiting for menu...');
  let clicks=0;
  for(let i=0;i<40;i++){
    await sleep(8000);
    await shot('s'+i);
    const a=analyze('s'+i);
    const isDark=/dark=0\.9/.test(a)||/state=loading/.test(a);
    if(i%4===0 && clicks<6){ await click(640,360); clicks++; } // unblock render occasionally
    LOG('['+i+'] '+a+(isDark?'':isDark?'':''));
  }
  process.exit(0);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});
