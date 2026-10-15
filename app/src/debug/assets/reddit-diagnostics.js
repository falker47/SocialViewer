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
  const elementIds = new WeakMap();
  let nextElementId = 1;
  const nodeRef = el => {
    if (!el) return null;
    if (!elementIds.has(el)) elementIds.set(el, 'n' + nextElementId++);
    return elementIds.get(el);
  };
  const parentOf = el => el.assignedSlot || el.parentElement || el.getRootNode()?.host || null;
  let hiddenSkipCount = 0, scheduled = false, tapCount = 0;
  const names = new Set(['vai al contenuto principale', 'skip to main content']);
  const round = v => Number.isFinite(v) ? Math.round(v) : 0;
  const label = s => String(s || '').replace(/\s+/g, ' ').trim().toLowerCase();
  const rootsAndElements = () => {
    const roots = [document], elements = [];
    let rootLimitReached = false;
    for (let i = 0; i < roots.length && i < 48; i++) {
      for (const el of roots[i].querySelectorAll('*')) {
        if (elements.length >= 6000) return {roots, elements, truncated: true};
        elements.push(el);
        if (el.shadowRoot) {
          if (roots.length < 48) roots.push(el.shadowRoot);
          else rootLimitReached = true;
        }
      }
    }
    return {roots, elements, truncated: rootLimitReached};
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
    component: ['dialog','modal','sheet','overlay','scrim','backdrop','consent','cookie','xpromo','skip'].filter(k =>
      ((el?.tagName || '')+' '+(el?.id || '')+' '+(el?.getAttribute?.('class') || '')).toLowerCase().includes(k)),
  });
  const blurValue = value => {
    if (!value || value === 'none') return 'none';
    return (String(value).match(/blur\([0-9.]+px\)/g) || ['other']).join(' ').slice(0,80);
  };
  // The hit-tested path can contain an unnamed scrim or a ::before backdrop.
  // Probe geometry only: no event dispatch, DOM changes, content or input reads.
  const hitElements = () => {
    const found = new Set();
    const points = [[0.5,0.5],[0.5,0.95],[0.5,0.12]];
    for (const [fx,fy] of points) {
      const queue = [document], visited = new Set();
      for (let i=0; i<queue.length && i<16; i++) {
        const root = queue[i];
        if (visited.has(root)) continue;
        visited.add(root);
        const hits = root.elementsFromPoint?.(innerWidth*fx, innerHeight*fy) || [];
        for (const el of hits.slice(0,6)) {
          for (let n=el, depth=0; n && depth<6; n=parentOf(n), depth++) found.add(n);
          if (el.shadowRoot && !visited.has(el.shadowRoot)) queue.push(el.shadowRoot);
        }
      }
    }
    return found;
  };
  const details = (el, hitTested) => {
    const style = getComputedStyle(el), r = el.getBoundingClientRect();
    const pseudo = [];
    const kinds = el.tagName === 'DIALOG' ? ['::before','::after','::backdrop'] : ['::before','::after'];
    for (const kind of kinds) {
      const ps = getComputedStyle(el, kind);
      const filter = blurValue(ps.filter), backdrop = blurValue(ps.backdropFilter || ps.webkitBackdropFilter);
      if (filter.includes('blur') || backdrop.includes('blur')) {
        // These are computed rules, not a claim that the pseudo-element is painted.
        pseudo.push({kind, filter, backdrop, display:ps.display, visibility:ps.visibility,
          opacity:ps.opacity, position:ps.position,
          generated:kind === '::backdrop' ? null : !['none','normal',''].includes(ps.content)});
      }
    }
    return {...identity(el), ref:nodeRef(el), parent:nodeRef(parentOf(el)),
      connected:el.isConnected, styleAvailable:style.length > 0, boxCount:el.getClientRects().length,
      inViewport:r.width > 0 && r.height > 0 && r.bottom > 0 && r.right > 0 && r.top < innerHeight && r.left < innerWidth,
      hitTested, childCount:el.childElementCount, openShadow:!!el.shadowRoot, assignedSlot:!!el.assignedSlot,
      open:el.hasAttribute('open'), hidden:el.hidden,
      ariaHidden:el.getAttribute('aria-hidden') === 'true',
      rect:{x:round(r.x), y:round(r.y), width:round(r.width), height:round(r.height)},
      display:style.display, visibility:style.visibility, opacity:style.opacity,
      position:style.position, z:style.zIndex,
      filter:blurValue(style.filter), backdrop:blurValue(style.backdropFilter || style.webkitBackdropFilter),
      ...(pseudo.length ? {pseudo} : {}),
    };
  };
  const capture = phase => {
    if (snapshots.length >= 10 && phase !== 'manual') return;
    const {elements, truncated:scanTruncated} = rootsAndElements();
    const hits = hitElements();
    const nodes = [];
    let candidateLimitReached = false;
    for (const el of elements) {
      const info = identity(el);
      // Bounded inspection of layout candidates, not all comment text nodes.
      const likely = hits.has(el) || el === document.body || el === document.documentElement ||
        el.parentElement === document.body || info.component.length ||
        ['dialog','alertdialog'].includes(info.role) || el.hasAttribute('aria-modal') || skipLink(el);
      if (!likely) continue;
      const d = details(el, hits.has(el));
      if (d.display !== 'none' || d.tag === 'dialog' || info.component.includes('skip')) nodes.push(d);
      if (nodes.length >= 120) { candidateLimitReached = true; break; }
    }
    // Keep actual sheet renderers even when collapsed/outside the viewport. V1
    // incorrectly tied them with many non-rendering display:contents controllers.
    const score = n =>
      (n.filter.includes('blur') || n.backdrop.includes('blur') || n.pseudo?.some(p => p.filter.includes('blur') || p.backdrop.includes('blur')) ? 200 : 0) +
      (n.tag === 'faceplate-bottom-sheet' ? 160 : 0) +
      (n.open && (n.component.includes('sheet') || n.tag === 'dialog') ? 150 : 0) +
      (n.tag === 'rpl-modal-card' ? 90 : 0) +
      (n.hitTested ? 60 : 0) + (n.inViewport ? 30 : 0) +
      (n.position === 'fixed' ? 20 : 0) + (n.open ? 20 : 0) +
      (n.component.some(k => ['modal','dialog','sheet','overlay','scrim','backdrop'].includes(k)) ? 8 : 0);
    nodes.sort((a,b) => score(b)-score(a));
    const candidateCount = nodes.length;
    nodes.length = Math.min(nodes.length, 12);
    const omittedCandidates = candidateCount - nodes.length;
    const truncated = scanTruncated || candidateLimitReached || omittedCandidates > 0;
    let focus = document.activeElement;
    for (let i=0; i<8 && focus?.shadowRoot?.activeElement; i++) focus = focus.shadowRoot.activeElement;
    const v = window.visualViewport;
    const snapshot = {phase, ms: round(performance.now()-started), ready: document.readyState,
      viewport: {width: innerWidth, height: innerHeight, visualWidth: round(v?.width || innerWidth),
        visualHeight: round(v?.height || innerHeight), offsetTop: round(v?.offsetTop || 0)},
      scrollY: round(window.scrollY), focus: identity(focus), hiddenSkipCount, truncated, scanTruncated, candidateLimitReached, candidateCount, omittedCandidates, nodes};
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
  window.__svRedditDiag = {report() {
    capture('manual');
    const report = {
      schema:'SV_REDDIT_DIAG_V1', probeRevision:2, host:location.hostname,
      snapshots:snapshots.slice(), omittedSnapshots:0,
      limits:'Layout only; open shadow roots only; bounded nodes. No cookies, storage, text, inputs or request bodies. Pseudo styles are computed, not proof of painting.',
    };
    // Keep the native 48k copy limit. Any history loss is explicit; always keep
    // the first baseline and current manual sample, prioritizing the real tap pair.
    while (JSON.stringify(report).length > 47000 && report.snapshots.length > 2) {
      let i=report.snapshots.findIndex((s,i) => i>0 && i<report.snapshots.length-1 && /^after-[0-9]+ms$/.test(s.phase));
      if (i<0) i=report.snapshots.findIndex((s,i) => i>0 && i<report.snapshots.length-1 && s.phase === 'manual');
      if (i<0) i=1;
      report.snapshots.splice(i,1);
      report.omittedSnapshots++;
    }
    return report;
  }};
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
