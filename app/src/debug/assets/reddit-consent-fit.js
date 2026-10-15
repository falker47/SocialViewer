/* Debug-only: keep Reddit's cookie choices usable in a short portrait WebView.
 * No consent decision, synthetic click, cookie read/write or backdrop removal.
 */
(() => {
  'use strict';
  const allowedHosts = new Set(['reddit.com', 'www.reddit.com', 'm.reddit.com', 'new.reddit.com', 'old.reddit.com']);
  if (location.protocol !== 'https:' || !allowedHosts.has(location.hostname) || window.__svConsentFit) return;

  const props = ['display','visibility','opacity','position','top','bottom','left','right',
    'width','height','min-height','max-height','overflow-x','overflow-y','box-sizing',
    'transform','pointer-events','z-index'];
  const fix = {
    display:'flex', visibility:'visible', opacity:'1', position:'fixed',
    top:'8px', bottom:'auto', left:'8px', right:'8px',
    width:'calc(100vw - 16px)', height:'auto', 'min-height':'0',
    'max-height':'calc(100dvh - 16px)', 'overflow-x':'hidden',
    'overflow-y':'auto', 'box-sizing':'border-box', transform:'none',
    'pointer-events':'auto', 'z-index':'2147483000'
  };

  let patched = null;
  let cachedConsent = null;
  function restore() {
    if (!patched) return;
    const {card, saved} = patched;
    for (const p of props) {
      const item = saved.get(p);
      if (item.value) card.style.setProperty(p, item.value, item.priority);
      else card.style.removeProperty(p);
    }
    patched = null;
  }

  function findConsent() {
    if (cachedConsent?.isConnected) return cachedConsent;
    const queue = [document], visited = new Set();
    for (let i = 0; i < queue.length && i < 256; i++) {
      const root = queue[i];
      if (visited.has(root)) continue;
      visited.add(root);
      const hit = root.querySelector?.('#data-protection-consent-dialog');
      if (hit) { cachedConsent = hit; return hit; }
      for (const el of root.querySelectorAll?.('*') || []) {
        if (el.shadowRoot && !visited.has(el.shadowRoot) && queue.length < 256)
          queue.push(el.shadowRoot);
      }
    }
    return null;
  }

  function adjust() {
    const host = findConsent();
    if (!host || !host.isConnected || !host.hasAttribute('open') || innerWidth >= innerHeight) {
      restore();
      return;
    }
    const card = host.querySelector('rpl-modal-card') || host.shadowRoot?.querySelector('rpl-modal-card');
    if (!card || !card.isConnected) { restore(); return; }
    if (patched?.card === card) return;
    restore();
    const rect = card.getBoundingClientRect();
    const viewportHeight = visualViewport?.height || innerHeight;
    // Don't interfere if Reddit already displays both choices in the viewport.
    if (rect.height > 0 && rect.top >= 0 && rect.bottom <= viewportHeight - 4) return;
    const saved = new Map(props.map(p => [p, {
      value:card.style.getPropertyValue(p),
      priority:card.style.getPropertyPriority(p)
    }]));
    patched = {card, saved};
    for (const [p, value] of Object.entries(fix))
      card.style.setProperty(p, value, 'important');
  }

  window.__svConsentFit = {refresh:adjust};
  adjust();
  window.addEventListener('resize', adjust, {passive:true});
  let attempts = 0;
  const timer = setInterval(() => {
    adjust();
    if (++attempts >= 30 && !cachedConsent) clearInterval(timer);
  }, 500);
  window.addEventListener('pagehide', () => { clearInterval(timer); restore(); }, {once:true});
})();