const $=s=>document.querySelector(s);
let rate=0.9, voice=null, playing=null;
function pickVoice(){const v=speechSynthesis.getVoices();voice=v.find(x=>/zh[-_]CN/i.test(x.lang))||v.find(x=>/^zh/i.test(x.lang))||null;const w=$('#warn');if(w)w.style.display=(v.length&&!voice)?'block':'none'}
if('speechSynthesis' in window){pickVoice();speechSynthesis.onvoiceschanged=pickVoice}else{const w=$('#warn');if(w)w.style.display='block'}
function say(text,btn,pitch){return new Promise(res=>{speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(text.replace(/＿+/g,'，'));u.lang='zh-CN';if(voice)u.voice=voice;u.rate=rate;u.pitch=pitch||1;
 if(playing)playing.classList.remove('on');if(btn){btn.classList.add('on');playing=btn}
 u.onend=u.onerror=()=>{if(btn)btn.classList.remove('on');res()};speechSynthesis.speak(u)})}
const strip=t=>t.replace(/^[^：:，。]{1,4}[：:]/,'');
function controls(){
 const s=$('#slow'),q=$('#quiz'),t=$('#theme');
 if(s)s.onclick=e=>{const on=s.getAttribute('aria-pressed')!=='true';s.setAttribute('aria-pressed',on);rate=on?0.6:0.9};
 if(q)q.onclick=e=>{const on=q.getAttribute('aria-pressed')!=='true';q.setAttribute('aria-pressed',on);document.body.classList.toggle('quiz',on)};
 if(t)t.onclick=()=>{const r=document.documentElement;const dark=r.dataset.theme?r.dataset.theme==='dark':matchMedia('(prefers-color-scheme: dark)').matches;r.dataset.theme=dark?'light':'dark'};
}
function renderPart(D){
 const list=$('#list'),sel=$('#jump');
 D.secs.forEach(s=>{
  const o=document.createElement('option');o.value=s.id;o.textContent=s.title;sel.appendChild(o);
  const h=document.createElement('h2');h.id=s.id;h.textContent=s.title;list.appendChild(h);
  const dialog=s.items.some(i=>i.n==='A')&&s.items.some(i=>i.n==='B');
  const b=document.createElement('button');b.className='allbtn';b.textContent='▶ Play all ('+s.items.length+')';
  b.onclick=async()=>{for(const it of s.items){if(document.hidden)break;await say(strip(it.zh),it._btn,dialog&&it.n==='B'?0.8:1)}};list.appendChild(b);
  let sub=null;
  s.items.forEach(it=>{
   if(it.sub&&it.sub!==sub){sub=it.sub;const h3=document.createElement('h3');h3.textContent=sub;list.appendChild(h3)}
   const d=document.createElement('div');d.className='it';
   const tr=it.ctx?'<div class="tr ctx">'+esc(it.en)+'</div>':'<div class="tr">'+esc(it.en)+(it.bn?'<span class="bn">'+esc(it.bn)+'</span>':'')+'</div>';
   d.innerHTML='<button class="play" aria-label="Play">▶</button>'+(it.n?'<div class="n">'+esc(it.n)+'</div>':'')+'<div class="zh">'+esc(it.zh)+'</div><div class="py">'+it.py+'</div>'+tr;
   const btn=d.querySelector('.play');it._btn=btn;btn.onclick=e=>{e.stopPropagation();say(strip(it.zh),btn,dialog&&it.n==='B'?0.8:1)};
   d.onclick=()=>d.classList.toggle('reveal');list.appendChild(d)})
 });
 sel.onchange=()=>{location.hash=sel.value};
 const go=()=>{if(location.hash){const t=document.getElementById(location.hash.slice(1));if(t){t.scrollIntoView();sel.value=location.hash.slice(1)}}};
 window.addEventListener('hashchange',go);setTimeout(go,30);
}
function esc(s){return (s||'').replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]))}
controls();
