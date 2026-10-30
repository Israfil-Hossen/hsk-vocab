// Network first, so a new version shows at once; the cache keeps the app working offline.
const CACHE='dc-v2';
const ASSETS=['./','./index.html','./manifest.json','./app/content.json','./dawah/voice.js','./dawah/read/toc.js'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(CACHE).then(c=>c.addAll(ASSETS)).catch(()=>{}));self.skipWaiting()});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==CACHE).map(k=>caches.delete(k)))));self.clients.claim()});
self.addEventListener('fetch',e=>{
 if(e.request.method!=='GET')return;
 e.respondWith(fetch(e.request).then(res=>{if(res.status===200&&new URL(e.request.url).origin===location.origin){const c=res.clone();caches.open(CACHE).then(k=>k.put(e.request,c))}return res})
  .catch(()=>caches.match(e.request).then(r=>r||caches.match('./index.html'))))});
