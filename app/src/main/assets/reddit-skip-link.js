(() => {
'use strict';
if(location.protocol!=='https:'||!['www.reddit.com','reddit.com','old.reddit.com','new.reddit.com','m.reddit.com'].includes(location.hostname))return;
const names=new Set(['vai al contenuto principale','skip to main content']);
function hide(){
const roots=[document],visited=new Set();
for(let i=0;i<roots.length&&i<96;i++){
const root=roots[i];if(visited.has(root))continue;visited.add(root);
for(const el of root.querySelectorAll?.('*')||[])if(el.shadowRoot&&roots.length<96)roots.push(el.shadowRoot);
for(const a of root.querySelectorAll?.('a[href]')||[]){
if(!names.has((a.textContent||'').trim().replace(/\s+/g,' ').toLowerCase())||a.closest('shreddit-post,shreddit-comment,article'))continue;
const href=a.getAttribute('href');if(!href)continue;
const u=new URL(href,location.href);if(u.origin===location.origin&&u.pathname===location.pathname&&u.hash)a.style.setProperty('display','none','important');
}
}}
hide();let n=0;const timer=setInterval(()=>{hide();if(++n>=40)clearInterval(timer)},500);
})();
