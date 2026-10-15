/* Reddit cookie preferences in a WebView — diagnostic build only.
 * No automatic choice, cookie/storage access, fetch requests, or synthetic taps.
 * choose() is invoked only from a user's native Accept / Reject button.
 */
(() => {
  'use strict';
  const hosts = new Set(['reddit.com','www.reddit.com','new.reddit.com','m.reddit.com','old.reddit.com']);
  if (location.protocol !== 'https:' || !hosts.has(location.hostname) || window.__svCookieChoice) return;
  let cachedDialog = null;

  // Reddit uses slotted Web Components and open Shadow DOM. querySelectorAll()
  // on the dialog alone cannot see the effective buttons.
  function rootsUnder(scope, maxRoots = 256) {
    const queue = [scope], found = [], visited = new Set();
    for (let i=0; i<queue.length && i<maxRoots; i++) {
      const root = queue[i];
      if (visited.has(root)) continue;
      visited.add(root);
      found.push(root);
      for (const el of root.querySelectorAll?.('*') || []) {
        if (el.shadowRoot && queue.length<maxRoots) queue.push(el.shadowRoot);
      }
    }
    return found;
  }

  function isConsentSheet(el) {
    if (!el || el.tagName?.toLowerCase()!=='rpl-dialog-sheet' || !el.hasAttribute('open')) return false;
    const signature = ((el.id || '')+' '+(el.getAttribute('class') || '')).toLowerCase();
    return /data[-_]?protection|consent|cookie/.test(signature);
  }
  function dialog() {
    if (cachedDialog?.isConnected && isConsentSheet(cachedDialog)) return cachedDialog;
    cachedDialog = null;
    for (const root of rootsUnder(document)) {
      for (const el of root.querySelectorAll?.('rpl-dialog-sheet[open]') || []) {
        if (isConsentSheet(el)) { cachedDialog = el; return el; }
      }
    }
    return null;
  }
  function relevantText(el) {
    const str = [el.getAttribute?.('aria-label') || '', el.getAttribute?.('title') || '',
      el.textContent || '', el.shadowRoot?.textContent || ''].filter(Boolean).join(' ');
    return str.toLowerCase().replace(/\s+/g,' ').trim();
  }
  function kind(label) {
    if (/rifiuta/.test(label) && /cookie/.test(label) && /opzional/.test(label)) return 'reject';
    if (/reject|decline/.test(label) && /optional|non[-\s]?essential/.test(label) && /cookie/.test(label)) return 'reject';
    if (/\baccetta (?:tutti|tutto)(?: i cookie)?\b/.test(label) || /\baccept all(?: cookies)?\b/.test(label)) return 'accept';
    return null;
  }
  function clickable(root) {
    const options = [];
    for (const scope of rootsUnder(root,128)) {
      for (const el of scope.querySelectorAll?.('button,faceplate-button,[role="button"]') || []) {
        if (el.disabled || el.getAttribute('aria-disabled')==='true') continue;
        const category = kind(relevantText(el));
        if (category) options.push({el,category, priority:el.tagName==='BUTTON'?2:1});
      }
    }
    return options;
  }
  function controls(host) {
    if (!isConsentSheet(host)) return null;
    let candidates = clickable(host);
    // Some sheet implementations portal the actual modal card beside its
    // controller rather than nesting the buttons inside the host.
    if (!(candidates.some(x=>x.category==='reject') && candidates.some(x=>x.category==='accept'))) {
      candidates = clickable(document);
    }
    const pick = category => candidates.filter(x=>x.category===category)
      .sort((a,b)=>b.priority-a.priority)[0]?.el || null;
    const reject = pick('reject'), accept = pick('accept');
    return reject && accept && reject!==accept ? {reject,accept} : null;
  }
  window.__svCookieChoice = Object.freeze({
    state() { return innerWidth<innerHeight && controls(dialog()) ? 'ready':'absent'; },
    choose(choice) {
      if (choice !== 'reject' && choice !== 'accept') return false;
      const options = controls(dialog());
      if (!options) return false;
      // No automatic consent: this function is called only by the user's UI action.
      options[choice].click();
      return true;
    }
  });
})();
