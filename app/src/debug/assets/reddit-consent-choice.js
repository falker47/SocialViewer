/* Debug-only consent handoff. No automatic choice, cookies, storage or requests. */
(() => {
  'use strict';
  if (location.protocol !== 'https:' ||
      !['reddit.com','www.reddit.com','new.reddit.com','m.reddit.com','old.reddit.com'].includes(location.hostname) ||
      window.__svCookieChoice) return;
  let cachedDialog = null;
  function dialog() {
    if (cachedDialog?.isConnected) return cachedDialog;
    const roots = [document], visited = new Set();
    for (let i = 0; i < roots.length && i < 256; i++) {
      const root = roots[i];
      if (visited.has(root)) continue;
      visited.add(root);
      const found = root.querySelector?.('#data-protection-consent-dialog');
      if (found) { cachedDialog = found; return found; }
      for (const element of root.querySelectorAll?.('*') || []) {
        if (element.shadowRoot && roots.length < 256) roots.push(element.shadowRoot);
      }
    }
    return null;
  }
  function controls(host) {
    if (!host || !host.hasAttribute('open')) return null;
    const buttons = Array.from(host.querySelectorAll('button'));
    const label = b => (b.textContent || '').trim().toLocaleLowerCase('en-US').replace(/\s+/g, ' ');
    const reject = buttons.find(b => /^(rifiuta (i )?cookie opzionali|reject optional cookies|reject non-essential cookies|reject nonessential cookies)$/.test(label(b)));
    const accept = buttons.find(b => /^(accetta tutti|accept all)$/.test(label(b)));
    return reject && accept && reject !== accept && !reject.disabled && !accept.disabled ? {reject, accept} : null;
  }
  window.__svCookieChoice = Object.freeze({
    state() { return innerWidth < innerHeight && controls(dialog()) ? 'ready' : 'absent'; },
    choose(choice) {
      if (choice !== 'reject' && choice !== 'accept') return false;
      const options = controls(dialog());
      if (!options) return false;
      // No automatic click: invoked by a native user button press only.
      options[choice].click();
      return true;
    }
  });
})();
