// Chinese voice for the app, the audio pages and the reader.
// Plays a recorded natural-voice MP3 when one exists in dawah/audio/, otherwise the best voice the device has.
(function(){
const BASE=(document.currentScript&&document.currentScript.src||location.href).replace(/[^/]*$/,'');
const ls={get(k){try{return localStorage.getItem(k)}catch(e){return null}},set(k,v){try{localStorage.setItem(k,v)}catch(e){}}};
let mp3=null,voices=[],voice=null,audio=null,rate=parseFloat(ls.get('zh:rate'))||0.9;
fetch(BASE+'audio/index.json').then(r=>r.ok?r.json():[]).then(a=>{mp3=new Set(a)}).catch(()=>{mp3=new Set()});
// keys ignore punctuation and spaces so "我是穆斯林。" and "我是穆斯林" share one recording
function key(t){t=t.replace(/[^一-鿿0-9A-Za-z]/g,'');let h=0x811c9dc5;for(const c of t){h^=c.codePointAt(0);h=Math.imul(h,0x01000193)>>>0}return ('0000000'+h.toString(16)).slice(-8)}
// higher score = more natural voice
function score(v){const n=v.name;let s=0;
 if(/zh[-_]CN/i.test(v.lang))s+=20;else if(/^zh/i.test(v.lang))s+=8;else return -1;
 if(/natural|online|neural/i.test(n))s+=50;if(/yunxi|yunjian|yunyang|yunze|kangkang|\bmale/i.test(n)&&!/female/i.test(n))s+=40;
 if(/google/i.test(n))s+=30;if(/premium|enhanced|siri/i.test(n))s+=35;if(/tingting|lili|yu-shu|lilian/i.test(n))s+=12;
 if(/huihui|kangkang|yaoyao/i.test(n))s-=5;return s}
function load(){voices=speechSynthesis.getVoices().filter(v=>score(v)>=0).sort((a,b)=>score(b)-score(a));
 const pick=ls.get('zh:voice');voice=voices.find(v=>v.name===pick)||voices[0]||null;
 document.dispatchEvent(new CustomEvent('zhvoices'))}
if('speechSynthesis' in window){load();speechSynthesis.onvoiceschanged=load}
let cur=null;
function done(el){if(el)el.classList.remove('on','say')}
function speak(text,el,opt){opt=opt||{};text=String(text).replace(/[＿_]+/g,'，');
 if(audio){audio.pause();audio=null}if(window.AndroidApp)AndroidApp.stop();else if('speechSynthesis' in window)speechSynthesis.cancel();done(cur);cur=el||null;
 return new Promise(res=>{const fin=()=>{done(el);res()};
  const k=key(text);
  if(mp3&&mp3.has(k)&&!opt.tts){audio=new Audio(BASE+'audio/'+k+'.mp3');audio.playbackRate=Math.max(.5,(opt.rate||rate)/0.9);if(el)el.classList.add('on','say');
   audio.onended=audio.onerror=fin;audio.play().catch(()=>{audio=null;tts(text,el,opt).then(res)});return}
  tts(text,el,opt).then(res)})}
const A=window.AndroidApp||null;let andRes=null;window.__ttsDone=()=>{if(andRes){const r=andRes;andRes=null;r()}};
function tts(text,el,opt){if(A)return new Promise(res=>{if(el)el.classList.add('on','say');const fin=()=>{done(el);res()};andRes=fin;A.speak(text,opt.rate||rate);setTimeout(()=>{if(andRes===fin)window.__ttsDone()},1500+text.length*450)});
 return new Promise(res=>{if(!('speechSynthesis' in window)){res();return}
 const u=new SpeechSynthesisUtterance(text);u.lang='zh-CN';if(voice)u.voice=voice;u.rate=opt.rate||rate;u.pitch=opt.pitch||1;
 if(el)el.classList.add('on','say');u.onend=u.onerror=()=>{done(el);res()};speechSynthesis.speak(u)})}
function openSettings(){
 let m=document.getElementById('zhset');if(m)m.remove();m=document.createElement('div');m.id='zhset';
 m.style.cssText='position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:9999;display:flex;align-items:flex-end;justify-content:center;font:15px/1.45 system-ui,-apple-system,"Segoe UI",Roboto,"Noto Sans Bengali",sans-serif';
 const nat=A?1:voices.filter(v=>score(v)>=50).length;
 m.innerHTML='<div style="background:#fff;color:#1d2a25;width:100%;max-width:520px;max-height:85vh;overflow:auto;border-radius:18px 18px 0 0;padding:18px 18px 26px">'+
 '<div style="display:flex;align-items:center;gap:8px"><b style="font-size:18px;flex:1">🎙 Chinese voice · চাইনিজ কণ্ঠ</b><button id="zhx" style="border:0;background:#eef4f0;border-radius:50%;width:34px;height:34px;font-size:16px;cursor:pointer">✕</button></div>'+
 (mp3&&mp3.size?'<p style="margin:8px 0;padding:8px 10px;background:#e7f5ec;border-radius:10px">✓ Natural recorded audio is installed for '+mp3.size+' sentences. Other text uses the voice below.</p>':'')+
 (nat?'':'<p style="margin:8px 0;padding:8px 10px;background:#fff6dd;border-radius:10px">Your device has no natural Chinese voice yet. <b>Best on a laptop:</b> open this site in <b>Microsoft Edge</b> and choose a voice marked "Natural". <b>Android:</b> Settings → Text-to-speech → Google → install Chinese. <b>iPhone:</b> Settings → Accessibility → Spoken Content → Voices → Chinese → download an "Enhanced" voice.<br>ল্যাপটপে Microsoft Edge ব্রাউজারে খুললে "Natural" কণ্ঠ পাবেন, যেটা মানুষের মতো শোনায়।</p>')+
 (A?'<p style="margin:8px 0;padding:8px 10px;background:#eef4f0;border-radius:10px">This app uses your phone\'s text-to-speech. For the most natural voice choose <b>Speech Services by Google</b> and install <b>Chinese (China)</b>. ফোনের Text-to-speech সেটিংসে Google বেছে Chinese ভয়েস ইনস্টল করুন।</p><button id="zhs" style="width:100%;border:1.5px solid #cfe0d6;background:#fff;border-radius:12px;padding:10px;font:700 14px system-ui;cursor:pointer">⚙ Open text-to-speech settings</button>':'<div style="margin:10px 0 6px;font-weight:700">Voice</div><div id="zhv"></div>')+
 '<div style="margin:14px 0 4px;font-weight:700">Speed · গতি <span id="zhrv"></span></div><input id="zhr" type="range" min="0.5" max="1.2" step="0.05" style="width:100%">'+
 '<button id="zht" style="margin-top:14px;width:100%;border:0;border-radius:12px;background:#0C3A2D;color:#fff;font:700 15px system-ui;padding:12px;cursor:pointer">▶ Test · 你好，我是穆斯林。</button></div>';
 document.body.appendChild(m);
 const list=m.querySelector('#zhv')||document.createElement('div');const ts=m.querySelector('#zhs');if(ts)ts.onclick=()=>A.openTtsSettings();
 list.innerHTML=voices.length?voices.map(v=>'<label style="display:flex;gap:8px;align-items:center;padding:7px 4px;border-bottom:1px solid #eee;cursor:pointer"><input type="radio" name="zhv" value="'+v.name.replace(/"/g,'&quot;')+'"'+(voice&&v.name===voice.name?' checked':'')+'><span>'+v.name+(score(v)>=50?' <b style="color:#1F6B52">· natural</b>':'')+'</span></label>').join(''):'<p>No Chinese voice found on this device.</p>';
 list.onchange=e=>{Zh.setVoice(e.target.value);speak('你好',null,{tts:true})};
 const r=m.querySelector('#zhr'),rv=m.querySelector('#zhrv');r.value=rate;rv.textContent=rate.toFixed(2)+'×';r.oninput=()=>{Zh.setRate(parseFloat(r.value));rv.textContent=rate.toFixed(2)+'×'};
 m.querySelector('#zht').onclick=()=>speak('你好，我是穆斯林。');
 m.querySelector('#zhx').onclick=()=>m.remove();m.onclick=e=>{if(e.target===m)m.remove()};
}
const SITE='https://israfil-hossen.github.io/hsk-vocab/';
function onlineUrl(u){u=u||location.href;return u.replace(/^https:\/\/appassets\.androidplatform\.net\/www\//,SITE)}
async function share(title,url){url=onlineUrl(url);if(A){A.share(title,url);return}
 if(navigator.share){try{await navigator.share({title,url})}catch(e){}return}
 try{await navigator.clipboard.writeText(url);alert('Link copied · লিংক কপি হয়েছে')}catch(e){prompt('Copy this link',url)}}
window.Zh={speak,key,onlineUrl,share,
 get voices(){return voices},get voice(){return voice},get rate(){return rate},
 get recorded(){return mp3?mp3.size:0},
 setVoice(name){voice=voices.find(v=>v.name===name)||voice;ls.set('zh:voice',name)},
 setRate(r){rate=r;ls.set('zh:rate',r)},
 natural(){return !!(voice&&score(voice)>=50)},openSettings};
})();
