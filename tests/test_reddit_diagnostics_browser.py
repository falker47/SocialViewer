"""Real Chromium fixture tests; no live Reddit requests or accounts.
Run: python -m unittest discover -s tests -p 'test_reddit_diagnostics_browser.py'
Requires playwright and Chromium (CHROMIUM_PATH optional).
The location binding is supplied by the fixture; the DOM/layout and script body are real.
"""
import json
import os
from pathlib import Path
import unittest
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / 'app/src/debug/assets/reddit-diagnostics.js'
HTML = '''<html><head><meta name="viewport" content="width=device-width, initial-scale=1">
<style>#content {filter:blur(4px)} #skip {position:fixed;top:0} dialog {position:fixed;top:150vh} </style>
</head><body><a id="skip" href="#main-content">Vai al contenuto principale</a>
<a id="original" href="https://example.org/">Originale</a><main id="main-content">
<div id="content">PRIVATE_POST_TEXT <shreddit-comment><a id="comment-link" href="#main-content">Vai al contenuto principale</a></shreddit-comment></div>
<dialog open id="first-visit-dialog"><h2>PRIVATE_HEADING</h2><input value="PRIVATE_INPUT"><button>Accept all cookies</button></dialog>
<test-shadow></test-shadow></main><script>
document.querySelector('test-shadow').attachShadow({mode:'open'}).innerHTML='<a id="shadow-skip" href="#main-content">Skip to main content</a><div class="overlay" style="backdrop-filter:blur(2px);position:fixed;inset:0">PRIVATE_OVERLAY_TEXT</div>';
window.clicked=0; document.addEventListener('click',()=>window.clicked++);
Object.defineProperty(document,'cookie',{get(){throw Error('must not read cookies')},set(){throw Error('must not write cookies')}}); Object.defineProperty(window,'localStorage',{get(){throw Error('must not read storage')}});
</script></body></html>'''

class BrowserTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pw = sync_playwright().start()
        cls.browser = cls.pw.chromium.launch(executable_path=os.getenv('CHROMIUM_PATH','/usr/bin/chromium'), headless=True, args=['--no-sandbox'])
    @classmethod
    def tearDownClass(cls):
        cls.browser.close(); cls.pw.stop()
    def setUp(self):
        self.ctx = self.browser.new_context(viewport={'width':390,'height':720})
        self.page = self.ctx.new_page()
        self.page.route('**/*',lambda r:r.fulfill(status=200,content_type='text/html',body=HTML))
        self.page.set_content(HTML)
        self.host = 'www.reddit.com'
    def tearDown(self):
        self.ctx.close()
    def install(self):
        # Empty adapter is the baseline: the website's link remains visible.
        self.page.evaluate('([source,host]) => (new Function("location", source))(new URL("https://"+host+"/r/AskReddit/comments/53scc5/example/"))', [SCRIPT.read_text() if SCRIPT.exists() else 'void 0', self.host])
    def test_01_hides_only_the_requested_page_skip_link(self):
        self.install()
        self.assertEqual('none',self.page.locator('#skip').evaluate('(e)=>getComputedStyle(e).display'))
        self.assertNotEqual('none',self.page.locator('#original').evaluate('(e)=>getComputedStyle(e).display'))
        self.assertNotEqual('none',self.page.locator('#comment-link').evaluate('(e)=>getComputedStyle(e).display'))
        self.assertEqual('none',self.page.locator('test-shadow').evaluate('(e)=>getComputedStyle(e.shadowRoot.querySelector("a")).display'))
    def test_02_does_not_dismiss_panels_remove_blur_or_change_site_storage(self):
        self.install()
        self.assertEqual('blur(4px)',self.page.locator('#content').evaluate('(e)=>getComputedStyle(e).filter'))
        self.assertTrue(self.page.locator('dialog').evaluate('(e)=>e.open'))
        self.assertEqual(0,self.page.evaluate('window.clicked'))
        self.assertTrue(self.page.evaluate('Boolean(document.querySelector("input").value === "PRIVATE_INPUT")'))
    def test_03_captures_layout_before_and_after_skip_treatment_without_content(self):
        self.install()
        report=self.page.evaluate('window.__svRedditDiag.report()')
        self.assertEqual('SV_REDDIT_DIAG_V1',report['schema'])
        self.assertEqual(['before-skip-treatment','after-skip-treatment'],[s['phase'] for s in report['snapshots'][:2]])
        nodes=report['snapshots'][0]['nodes']
        self.assertTrue(any(n['tag']=='dialog' and n['rect']['y']>720 for n in nodes))
        self.assertTrue(any('blur' in (n['filter']+n['backdrop']) for n in nodes))
        self.assertNotIn('PRIVATE_',json.dumps(report))
        self.assertNotIn('53scc5',json.dumps(report))
    def test_04_handles_reinserted_and_late_shadow_links(self):
        self.install()
        self.page.evaluate('''() => {const a=document.createElement('a');a.id='late';a.href='#main-content';a.textContent='Skip to main content';document.body.prepend(a);const host=document.createElement('late-shadow');document.body.append(host);host.attachShadow({mode:'open'}).innerHTML='<a href="#main-content">Vai al contenuto principale</a>';}''')
        self.page.wait_for_timeout(1000)
        self.assertEqual('none',self.page.locator('#late').evaluate('(e)=>getComputedStyle(e).display'))
        self.assertEqual('none',self.page.locator('late-shadow').evaluate('(e)=>getComputedStyle(e.shadowRoot.querySelector("a")).display'))
    def test_05_keeps_first_snapshots_and_records_real_tap_without_consuming_it(self):
        self.install()
        self.page.mouse.click(365,120)
        self.page.wait_for_timeout(250)
        self.assertGreater(self.page.evaluate('window.clicked'),0)
        report=self.page.evaluate('window.__svRedditDiag.report()')
        self.assertTrue(any(s['phase']=='before-page-tap' for s in report['snapshots']))
        self.assertTrue(any(s['phase']=='after-page-tap' for s in report['snapshots']))
        self.assertLess(len(json.dumps(report)),48000)
    def test_06_is_idempotent_and_refuses_other_origins(self):
        self.install(); self.install()
        report=self.page.evaluate('window.__svRedditDiag.report()')
        self.assertEqual(1,sum(s['phase']=='before-skip-treatment' for s in report['snapshots']))
        self.page.close()
        self.page = self.ctx.new_page()
        self.page.set_content(HTML)
        self.host = 'example.com'
        self.install()
        self.assertFalse(self.page.evaluate('Boolean(window.__svRedditDiag)'))
        self.assertNotEqual('none',self.page.locator('#skip').evaluate('(e)=>getComputedStyle(e).display'))

if __name__=='__main__':unittest.main()
