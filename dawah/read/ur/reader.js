(function(){
const R=document.documentElement,B=document.body,ls={get(k){try{return localStorage.getItem(k)}catch(e){return null}},set(k,v){try{localStorage.setItem(k,v)}catch(e){}}};
const th=ls.get('rd:theme');if(th)R.dataset.theme=th;
let fs=parseInt(ls.get('rd:fs'))||17;R.style.setProperty('--fs',fs+'px');
if(ls.get('rd:nobn')==='1')B.classList.add('nobn');
const $=s=>document.querySelector(s);
const on=(s,f)=>{const e=$(s);if(e)e.onclick=f};
on('#menuBtn',()=>B.classList.toggle('menu'));
document.querySelectorAll('aside a').forEach(a=>a.addEventListener('click',()=>B.classList.remove('menu')));
on('#themeBtn',()=>{const d=R.dataset.theme?R.dataset.theme==='dark':matchMedia('(prefers-color-scheme: dark)').matches;R.dataset.theme=d?'light':'dark';ls.set('rd:theme',R.dataset.theme)});
on('#fsBtn',()=>{fs=fs>=21?15:fs+2;R.style.setProperty('--fs',fs+'px');ls.set('rd:fs',fs)});
on('#bnBtn',()=>{B.classList.toggle('nobn');ls.set('rd:nobn',B.classList.contains('nobn')?'1':'0')});
// tap any Chinese text to hear it
document.addEventListener('click',e=>{const el=e.target.closest('.cjk,td.zh');if(!el||!window.Zh)return;Zh.speak(el.innerText,el)});
on('#voiceBtn',()=>window.Zh&&Zh.openSettings());
// reading progress and resume
const key='rd:pos:ur:'+location.pathname;const bar=$('.prog');
addEventListener('scroll',()=>{const h=document.documentElement.scrollHeight-innerHeight;if(bar)bar.style.width=(h>0?scrollY/h*100:0)+'%'},{passive:true});
let t;addEventListener('scroll',()=>{clearTimeout(t);t=setTimeout(()=>{ls.set(key,Math.round(scrollY));if(B.dataset.page)ls.set('rd:last:ur',JSON.stringify({u:location.pathname,t:document.title.split(' · ')[0],y:Math.round(scrollY)}))},400)},{passive:true});
if(!location.hash&&B.dataset.page){const y=parseInt(ls.get(key));if(y>200)setTimeout(()=>scrollTo(0,y),60)}
// highlight current section in the menu
const links=[...document.querySelectorAll('aside a.s')];
if('IntersectionObserver' in window&&links.length){const io=new IntersectionObserver(es=>{es.forEach(en=>{if(en.isIntersecting){links.forEach(a=>a.classList.toggle('on',a.getAttribute('href')==='#'+en.target.id))}})},{rootMargin:'-60px 0px -70% 0px'});document.querySelectorAll('main h2[id]').forEach(h=>io.observe(h))}
// continue reading on the contents page
const cont=$('#continue');if(cont){try{const l=JSON.parse(ls.get('rd:last:ur'));if(l&&l.u){cont.innerHTML='<a class="btn" href="'+l.u+'">▶ پڑھنا جاری رکھیں: '+l.t+'</a>';cont.hidden=false}}catch(e){}}
// search
const sb=$('.sbox'),si=$('#sq'),sr=$('#sr');let idx=null;
on('#searchBtn',async()=>{sb.classList.add('on');si.focus();if(!idx){try{idx=await (await fetch(B.dataset.root+'search.json')).json()}catch(e){sr.textContent='Search is not available offline.'}}});
if(sb)sb.addEventListener('click',e=>{if(e.target===sb)sb.classList.remove('on')});
if(si)si.addEventListener('input',()=>{const q=si.value.trim().toLowerCase();if(!idx||q.length<2){sr.innerHTML='';return}
 const res=[];for(const s of idx){const i=s.x.toLowerCase().indexOf(q),j=s.h.toLowerCase().indexOf(q);if(i<0&&j<0)continue;
  const at=Math.max(0,i-60),snip=i<0?s.x.slice(0,120):(at?'…':'')+s.x.slice(at,i+q.length+80)+'…';res.push([j>=0?0:1,s,snip]);if(res.length>60)break}
 res.sort((a,b)=>a[0]-b[0]);const esc=x=>x.replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));const re=new RegExp(q.replace(/[.*+?^${}()|[\]\\]/g,'\\$&'),'ig');
 sr.innerHTML=res.length?res.slice(0,40).map(([,s,sn])=>'<a href="'+B.dataset.root+s.u+'"><b>'+esc(s.h)+'</b><small>'+esc(s.p)+'</small><small>'+esc(sn).replace(re,m=>'<mark>'+m+'</mark>')+'</small></a>').join(''):'<p>کچھ نہیں ملا</p>'});
addEventListener('keydown',e=>{if(e.key==='Escape'&&sb)sb.classList.remove('on')});
// share
on('#shareBtn',()=>{if(window.Zh)Zh.share(document.title,location.href.split('#')[0])});
})();
// mark sections as read, show progress, go to the next unread section
(function(){
const ls={get(k){try{return localStorage.getItem(k)}catch(e){return null}},set(k,v){try{localStorage.setItem(k,v)}catch(e){}}};
const BK=window.BOOK||[];const all=[];BK.forEach(p=>p.s.forEach(s=>all.push({u:p.u,id:s[0],t:s[1],p:p.t})));
let done;try{done=new Set(JSON.parse(ls.get('rd:done:ur')||'[]'))}catch(e){done=new Set()}
const save=()=>ls.set('rd:done:ur',JSON.stringify([...done]));
const k=(u,id)=>u+'#'+id;const root=document.body.dataset.root||'';
const nextUnread=()=>all.find(s=>!done.has(k(s.u,s.id)));
function refresh(){
 document.querySelectorAll('[data-k]').forEach(el=>el.classList.toggle('done',done.has(el.dataset.k)));
 document.querySelectorAll('.mk').forEach(b=>{const d=done.has(b.dataset.k);b.classList.toggle('done',d);b.querySelector('.l').textContent=d?'✓ پڑھ لیا':'پڑھ لیا ✓'});
 const n=all.filter(s=>done.has(k(s.u,s.id))).length;document.querySelectorAll('.bkprog').forEach(e=>e.textContent=n+'/'+all.length);
 document.querySelectorAll('.bkbar i').forEach(e=>e.style.width=(all.length?n/all.length*100:0)+'%');
 const nx=nextUnread();document.querySelectorAll('.nextun').forEach(e=>{e.innerHTML=nx?'<a class="btn" href="'+root+nx.u+'#'+nx.id+'">▶ اگلا پڑھیں: '+nx.t+'<small>'+nx.p+'</small></a>':'<p class="alld">✓ ماشاءاللہ، آپ نے پوری کتاب پڑھ لی!</p>'});
}
const page=document.body.dataset.page;
if(page){
 document.querySelectorAll('main h2[id]').forEach(h=>{
  let end=h.nextElementSibling;while(end&&end.tagName!=='H2'&&!end.classList.contains('pn'))end=end.nextElementSibling;
  const box=document.createElement('div');box.className='mkbox';const key=k(page,h.id);
  const i=all.findIndex(s=>s.u===page&&s.id===h.id),nx=all[i+1];
  box.innerHTML='<button class="mk" data-k="'+key+'"><span class="l"></span></button>'+(nx?'<a class="nx" href="'+(nx.u===page?'':root+nx.u)+'#'+nx.id+'">اگلا حصہ: '+nx.t+' →</a>':'');
  box.querySelector('.mk').onclick=()=>{if(done.has(key))done.delete(key);else{done.add(key);try{const d=new Date(),dk=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');const ac=JSON.parse(ls.get('rd:act')||'{}');ac[dk]=(ac[dk]||0)+1;ls.set('rd:act',JSON.stringify(ac))}catch(e){}}save();refresh()};
  h.parentNode.insertBefore(box,end);
 });
}
refresh();
})();
