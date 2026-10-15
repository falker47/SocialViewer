/* Debug preview only. No requests, cookies/storage, HTML export, or synthetic taps.
 * The only website mutation is hiding the exact EN/IT same-page skip link requested
 * by the owner. First layout evidence is captured BEFORE this mutation.
 */
(() => {
  'use strict';
  const hosts = ['reddit.com', 'www.reddit.com', 'old.reddit.com', 'new.reddit.com', 'm.reddit.com'];
  if (location.protocol !== 'https:' || !hosts.includes(location.hostname) || window.__svRedditDiag) return;
  const started = performance.now();
  const snapshots = [];
  const observed = new WeakSet();
  const observers = [];
  let hiddenSkipCount = 0, scheduled = false, tapCount = 0;
  const names = new Set(['vai al contenuto principale', 'skip to main content']);
  const round = v => Number.isFinite(v) ? Math.round(v) : 0;
  const label = s => String(s || '').replace(/\s+/g, ' ').trim().toLowerCase();
  const rootsAndElements = () => {
    const roots = [document], elements = [];
    for (let i = 0; i < roots.length && i < 48; i++) {
      for (const el of roots[i].querySelectorAll('*')) {
        if (elements.length >= 6000) return {roots, elements, truncated: true};
        elements.push(el);
        if (el.shadowRoot && roots.length < 48) roots.push(el.shadowRoot);
      }
    }
    return {roots, elements, truncated: false};
  };
  const insideContent = el => {
    for (let n = el; n; n = n.parentElement || n.getRootNode()?.host) {
      if (n.matches?.('shreddit-comment, shreddit-post, article')) return true;
    }
    return false;
  };
  const skipLink = el => {
    if (el.tagName !== 'A' || !names.has(label(el.textContent)) || insideContent(el)) return false;
    const raw = el.getAttribute('href');
    if (!raw) return false;
    try {
      const target = new URL(raw, location.href);
      return target.origin === location.origin && target.pathname === location.pathname && Boolean(target.hash);
    } catch (_) { return false; }
  };
  const identity = el => ({
    tag: (el?.tagName || '').toLowerCase(),
    role: ['dialog', 'alertdialog', 'button', 'link', 'main'].includes(el?.getAttribute?.('role')) ? el.getAttribute('role') : '',
    // Only fixed categories, never arbitrary element IDs/classes or user content.
    component: ['dialog','modal','sheet','overlay','scrim','consent','cookie','xpromo','skip'].filter(k =>
      ((el?.tagName || '')+' '+(el?.id || '')+' '+(el?.getAttribute?.('class') || '')).toLowerCase().includes(k)),
  });
  const blurValue = value => {
    if (!value || value === 'none') return 'none';
    return (String(value).match(/blur\([0-9.]+px\)/g) || ['other']).join(' ').slice(0,80);
  };
  const details = el => {
    const style = getComputedStyle(el), r = el.getBoundingClientRect();
    let backdrop = style.backdropFilter || style.webkitBackdropFilter || 'none';
    if (el.tagName === 'DIALOG') {
      const pseudo = getComputedStyle(el, '::backdrop');
      if (pseudo.backdropFilter && pseudo.backdropFilter !== 'none') backdrop = pseudo.backdropFilter;
    }
    return {...identity(el), open: el.hasAttribute('open'), hidden: el.hidden,
      ariaHidden: el.getAttribute('aria-hidden') === 'true',
      rect: {x: round(r.x), y: round(r.y), width: round(r.width), height: round(r.height)},
      display: style.display, visibility: style.visibility, opacity: style.opacity,
      position: style.position, z: style.zIndex,
      filter: blurValue(style.filter), backdrop: blurValue(backdrop),
    };
  };
  const capture = phase => {
    if (snapshots.length >= 10 && phase !== 'manual') return;
    const {elements, truncated} = rootsAndElements();
    const nodes = [];
    for (const el of elements) {
      const info = identity(el);
      // Bounded inspection of layout candidates, not all comment text nodes.
      const likely = el === document.body || el === document.documentElement ||
        el.parentElement === document.body || info.component.length ||
        ['dialog','alertdialog'].includes(info.role) || el.hasAttribute('aria-modal') || skipLink(el);
      if (!likely) continue;
      const d = details(el);
      if (d.display !== 'none' || d.tag === 'dialog' || info.component.includes('skip')) nodes.push(d);
      if (nodes.length >= 120) break;
    }
    // A sheet can be near the end of the document: prioritize it over headers.
    const score = n => (n.display !== 'none' && n.visibility !== 'hidden' ? 4 : 0) +
      (n.open ? 12 : 0) + (n.filter.includes('blur') || n.backdrop.includes('blur') ? 20 : 0) +
      (n.component.some(k => ['modal','dialog','sheet','overlay','scrim'].includes(k)) ? 8 : 0);
    nodes.sort((a,b) => score(b)-score(a));
    const candidateCount = nodes.length;
    nodes.length = Math.min(nodes.length, 12);
    let focus = document.activeElement;
    for (let i=0; i<8 && focus?.shadowRoot?.activeElement; i++) focus = focus.shadowRoot.activeElement;
    const v = window.visualViewport;
    const snapshot = {phase, ms: round(performance.now()-started), ready: document.readyState,
      viewport: {width: innerWidth, height: innerHeight, visualWidth: round(v?.width || innerWidth),
        visualHeight: round(v?.height || innerHeight), offsetTop: round(v?.offsetTop || 0)},
      scrollY: round(window.scrollY), focus: identity(focus), hiddenSkipCount, truncated, candidateCount, nodes};
    if (phase === 'manual' && snapshots.length >= 10) snapshots.pop();
    snapshots.push(snapshot);
  };
  const sweep = () => {
    scheduled = false;
    const {roots, elements} = rootsAndElements();
    for (const el of elements) {
      if (skipLink(el)) {
        if (!el.hasAttribute('data-sv-hidden-skip')) hiddenSkipCount++;
        el.setAttribute('data-sv-hidden-skip', 'true');
        el.style.setProperty('display', 'none', 'important');
      }
    }
    for (const root of roots) {
      if (observed.has(root) || observers.length >= 48) continue;
      observed.add(root);
      const observer = new MutationObserver(() => {
        if (!scheduled) { scheduled = true; setTimeout(sweep, 200); }
      });
      // Child-list only: our own style/marker changes cannot make an observer loop.
      observer.observe(root, {childList:true, subtree:true});
      observers.push(observer);
    }
  };
  window.__svRedditDiag = {report() { capture('manual'); return {
    schema:'SV_REDDIT_DIAG_V1', host:location.hostname, snapshots,
    limits:'Layout only; open shadow roots only; bounded nodes. No cookies, storage, text, inputs or request bodies.',
  };}};
  capture('before-skip-treatment');
  sweep();
  capture('after-skip-treatment');
  [600, 2500, 8000].forEach(ms => setTimeout(() => { sweep(); capture('after-'+ms+'ms'); }, ms));
  window.addEventListener('pointerdown', () => {
    if (tapCount >= 2) return;
    tapCount++;
    capture('before-page-tap');
  }, {capture:true, passive:true});
  window.addEventListener('pointerup', () => {
    if (tapCount > 0 && tapCount <= 2) setTimeout(() => capture('after-page-tap'), 100);
  }, {capture:true, passive:true});
})();
