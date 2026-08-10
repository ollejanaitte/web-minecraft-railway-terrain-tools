#!/usr/bin/env node
import { spawn, spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '../../../../');
const PHASE = resolve(ROOT, 'doc/testing/phase0_2');
const S = resolve(PHASE, 'screenshots');
const PROFILE = process.env.CHROME_PROFILE || resolve(PHASE, 'chrome-profile-run');
const PORT = process.env.CDP_PORT || '9222';
const HTML = resolve(ROOT, 'target_teavm_javascript/javascript/EaglercraftX_1.8_Offline_International.html');
const LOG = resolve(ROOT, 'doc/testing/phase0_5/logs/option_probe.log');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const log = (m) => { const l=`[${new Date().toISOString()}] ${m}`; console.log(l); writeFileSync(LOG, l+'\n', {flag:'a'}); };

class CDP {
  constructor(u){this.u=u;this.id=0;this.p=new Map();this.lines=[];}
  async connect(){this.ws=new WebSocket(this.u);await new Promise((a,b)=>{this.ws.onopen=a;this.ws.onerror=b;});this.ws.onmessage=(ev)=>{const m=JSON.parse(ev.data);if(m.method==='Runtime.consoleAPICalled'){this.lines.push((m.params.args||[]).map(a=>a.value??a.description??'').join(' '));}if(m.id&&this.p.has(m.id)){const {res,rej}=this.p.get(m.id);this.p.delete(m.id);m.error?rej(new Error(JSON.stringify(m.error))):res(m.result);}};}
  send(method,params={},timeoutMs=60000){const id=++this.id;return new Promise((resolve,reject)=>{const t=setTimeout(()=>{this.p.delete(id);reject(new Error('timeout '+method));},timeoutMs);this.p.set(id,{resolve:(v)=>{clearTimeout(t);resolve(v);},reject:(e)=>{clearTimeout(t);reject(e);}});this.ws.send(JSON.stringify({id,method,params}));});}
  async eval(e){const r=await this.send('Runtime.evaluate',{expression:e,returnByValue:true,awaitPromise:true});return r.result?r.result.value:r.exceptionDetails;}
  async shot(f){const r=await this.send('Page.captureScreenshot',{format:'png'});writeFileSync(f,Buffer.from(r.data,'base64'));}
  async click(x,y){await this.eval(`(()=>{const c=document.querySelector('canvas');for(const t of ['mousemove','mousedown','mouseup','click'])c.dispatchEvent(new MouseEvent(t,{bubbles:true,cancelable:true,clientX:${x},clientY:${y},button:0,view:window}));})()`);await this.send('Input.dispatchMouseEvent',{type:'mouseMoved',x,y});await sleep(40);await this.send('Input.dispatchMouseEvent',{type:'mousePressed',x,y,button:'left',clickCount:1});await sleep(40);await this.send('Input.dispatchMouseEvent',{type:'mouseReleased',x,y,button:'left',clickCount:1});}
}
async function getWs(){for(let i=0;i<240;i++){try{const l=await (await fetch(`http://127.0.0.1:${PORT}/json`)).json();const p=l.find(t=>t.type==='page');if(p)return p.webSocketDebuggerUrl;}catch(_){}await sleep(500);}throw new Error('no cdp');}
function analyze(path){return spawnSync('python3',[resolve(PHASE,'scripts/analyze_buttons.py'),path],{encoding:'utf8'}).stdout.trim();}
function launch(){mkdirSync(PROFILE,{recursive:true});const args=['--headless=new','--no-sandbox','--use-gl=angle','--use-angle=vulkan','--autoplay-policy=no-user-gesture-required','--mute-audio','--hide-scrollbars','--window-size=1280,720',`--remote-debugging-port=${PORT}`,`--user-data-dir=${PROFILE}`,'file://'+HTML];const cmd=`exec /opt/google/chrome/chrome ${args.map(a=>JSON.stringify(a)).join(' ')} >${JSON.stringify(resolve(ROOT,'doc/testing/phase0_5/logs/option_chrome.log'))} 2>&1`;const ch=spawn('bash',['-c',cmd],{stdio:'ignore',detached:true});ch.unref();log('CHROME '+ch.pid);}
async function waitMenu(cdp){for(let i=0;i<90;i++){await cdp.click(640,360);await sleep(1500);if(i===0){await cdp.click(640,390);await sleep(700);await cdp.click(640,450);await sleep(700);}const p=resolve(S,'_ob_boot.png');await cdp.shot(p);const a=analyze(p);log('boot '+i+' '+a);if(/menu-clouds|dirt-screen/.test(a)&&!/loading/.test(a)){const bt=(a.match(/buttons=([\d,]+)/)||[])[1]||'';if(bt.split(',').length>=1)return bt.split(',');}}throw new Error('no menu');}
async function main(){
  writeFileSync(LOG,'');
  launch();await sleep(6000);
  const cdp=new CDP(await getWs());await cdp.connect();await cdp.send('Runtime.enable');
  const menu=await waitMenu(cdp);log('MENU buttons='+menu.join(','));
  // Singleplayer
  await cdp.click(640,menu[0]);await sleep(10000);
  const p1=resolve(S,'_ob_sp.png');await cdp.shot(p1);const a1=analyze(p1);log('SP '+a1);
  const b1=(a1.match(/buttons=([\d,]+)/)||[])[1]||'';const y1=b1?Number(b1.split(',').pop()):492;
  await cdp.click(915,y1);await sleep(7000);
  const p2=resolve(S,'_ob_sub.png');await cdp.shot(p2);const a2=analyze(p2);log('SUB '+a2);
  const b2=(a2.match(/buttons=([\d,]+)/)||[])[1]||'';const y2=b2?Number(b2.split(',').slice(-2,-1)[0]||b2.split(',')[0]):242;
  await cdp.click(640,y2);await sleep(7000);
  const p3=resolve(S,'_ob_form.png');await cdp.shot(p3);const a3=analyze(p3);log('FORM '+a3);
  // type name
  await cdp.click(495,138);await sleep(800);
  const tc=async(ch)=>{const vk=ch.codePointAt(0);await cdp.send('Input.dispatchKeyEvent',{type:'keyDown',key:ch,code:'Key'+ch.toUpperCase(),text:ch,windowsVirtualKeyCode:vk});await cdp.send('Input.dispatchKeyEvent',{type:'char',key:ch,text:ch,unmodifiedText:ch,windowsVirtualKeyCode:vk});await cdp.send('Input.dispatchKeyEvent',{type:'keyUp',key:ch,code:'Key'+ch.toUpperCase(),windowsVirtualKeyCode:vk});};
  for(const ch of 'EaglerValidateFlat'){await tc(ch);await sleep(40);}
  await sleep(800);
  // probe candidate y positions for More World Options
  for(const y of [197,225,248,153,115]){
    await cdp.shot(resolve(S,'_ob_p'+y+'_before.png'));
    await cdp.click(640,y);await sleep(5000);
    await cdp.shot(resolve(S,'_ob_p'+y+'_after.png'));
    log('CLICK y'+y+' -> '+analyze(resolve(S,'_ob_p'+y+'_after.png')));
  }
  process.exit(0);
}
main().catch(e=>{log('FATAL '+e.stack);process.exit(1);});
