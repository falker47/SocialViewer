"""Synthetic Chromium tests for Reddit's portrait-only consent layout.
Execute: python -m unittest discover -s tests -p test_reddit_cookie_consent_fit.py
Requires Playwright and installed Chromium; no live Reddit requests or login.
"""
import os
import unittest
from pathlib import Path
from playwright.sync_api import sync_playwright

SCRIPT=Path(__file__).resolve().parents[1]/'app/src/debug/assets/reddit-consent-fit.js'
HTML='''<!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
<style>body{margin:0} #data-protection-consent-dialog{display:contents}
rpl-modal-card{display:flex;flex-direction:column;gap:12px;position:fixed;top:calc(100vh + 16px);width:100vw;height:0;overflow:hidden;background:#222;color:white}
#other-dialog rpl-modal-card{top:1100px}
@media(min-width:700px){#data-protection-consent-dialog rpl-modal-card{top:20px;height:300px}}
</style></head><body>
<rpl-dialog-sheet id="data-protection-consent-dialog" open>
<rpl-modal-card><h2>Conferma le tue preferenze sui cookie</h2><p>Reddit utilizza cookie</p>
<button id="reject">Rifiuta i cookie opzionali</button><button id="accept">Accetta tutti</button>
</rpl-modal-card></rpl-dialog-sheet>
<rpl-dialog-sheet id="other-dialog" open><rpl-modal-card>Unrelated dialog</rpl-modal-card></rpl-dialog-sheet>
<script>window.rejected=false;window.accepted=false;
document.querySelector('#reject').onclick=()=>window.rejected=true;
document.querySelector('#accept').onclick=()=>window.accepted=true;</script>
</body></html>'''

class TestConsentFit(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pw=sync_playwright().start()
        cls.browser=cls.pw.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH','/usr/bin/chromium'),headless=True,args=['--no-sandbox'])
    @classmethod
    def tearDownClass(cls):
        cls.browser.close()
        cls.pw.stop()
    def setUp(self):
        self.ctx=self.browser.new_context(viewport={'width':396,'height':711})
        self.page=self.ctx.new_page()
        self.page.set_content(HTML)
    def tearDown(self):
        self.ctx.close()
    def install(self):
        self.page.evaluate('([source,href])=>(new Function("location",source))(new URL(href))',
            [SCRIPT.read_text(encoding='utf-8'),'https://www.reddit.com/r/test/comments/abc/example/'])
    def test_portrait_shows_real_choices_without_any_automatic_choice(self):
        self.assertGreaterEqual(self.page.locator('#reject').bounding_box()['y'],711)
        self.install()
        self.page.wait_for_timeout(400)
        for name in ('reject','accept'):
            rect=self.page.locator('#'+name).bounding_box()
            self.assertIsNotNone(rect)
            self.assertGreater(rect['height'],10)
            self.assertLessEqual(rect['y']+rect['height'],711)
        self.assertFalse(self.page.evaluate('window.rejected||window.accepted'))
        self.page.locator('#reject').click()
        self.assertTrue(self.page.evaluate('window.rejected'))
        self.assertFalse(self.page.evaluate('window.accepted'))
    def test_unrelated_modal_unaffected(self):
        self.install()
        self.page.wait_for_timeout(200)
        self.assertEqual('1100px', self.page.locator('#other-dialog rpl-modal-card').evaluate('(e)=>getComputedStyle(e).top'))
    def test_closed_consent_dialog_unaffected(self):
        self.page.locator('#data-protection-consent-dialog').evaluate('(e)=>e.removeAttribute("open")')
        self.install()
        self.page.wait_for_timeout(250)
        self.assertEqual('',self.page.locator('#data-protection-consent-dialog rpl-modal-card').get_attribute('style') or '')
    def test_landscape_consent_dialog_unmodified(self):
        self.page.set_viewport_size({'width':850,'height':410})
        self.install()
        self.page.wait_for_timeout(250)
        self.assertEqual('',self.page.locator('#data-protection-consent-dialog rpl-modal-card').get_attribute('style') or '')
    def test_late_consent_dialog_inside_open_shadow_root(self):
        self.page.set_content('<html><body><app-shell></app-shell></body></html>')
        self.install()
        self.page.evaluate("""() => {
          const root=document.querySelector('app-shell').attachShadow({mode:'open'});
          root.innerHTML='<rpl-dialog-sheet id="data-protection-consent-dialog" open><rpl-modal-card style="display:flex;flex-direction:column;position:fixed;top:727px;height:0"><button id="no">Rifiuta i cookie opzionali</button><button>Accetta tutti</button></rpl-modal-card></rpl-dialog-sheet>';
        }""")
        self.page.wait_for_timeout(850)
        rect=self.page.evaluate("() => document.querySelector('app-shell').shadowRoot.querySelector('#no').getBoundingClientRect().toJSON()")
        self.assertGreater(rect['height'],10)
        self.assertLessEqual(rect['bottom'],711)
    def test_original_styles_restored_on_consent_close(self):
        card=self.page.locator('#data-protection-consent-dialog rpl-modal-card')
        card.evaluate("e=>e.style.setProperty('height','0px')")
        self.install()
        self.page.wait_for_timeout(200)
        self.assertEqual('important',card.evaluate("(e)=>e.style.getPropertyPriority('height')"))
        self.page.locator('#data-protection-consent-dialog').evaluate('(e)=>e.removeAttribute("open")')
        self.page.wait_for_timeout(700)
        self.assertEqual('0px',card.evaluate("(e)=>e.style.getPropertyValue('height')"))
        self.assertEqual('',card.evaluate("(e)=>e.style.getPropertyPriority('height')"))
        self.assertEqual('',card.evaluate("(e)=>e.style.getPropertyValue('position')"))
    def test_original_styles_restored_on_landscape_rotation(self):
        card=self.page.locator('#data-protection-consent-dialog rpl-modal-card')
        self.install()
        self.page.wait_for_timeout(200)
        self.assertEqual('fixed',card.evaluate("(e)=>e.style.getPropertyValue('position')"))
        self.page.set_viewport_size({'width':850,'height':410})
        self.page.wait_for_timeout(700)
        self.assertEqual('',card.evaluate("(e)=>e.style.getPropertyValue('position')"))
    def test_blur_and_site_decision_unmodified(self):
        self.page.evaluate("() => {const e=document.createElement('div');e.id='scrim';e.style.filter='blur(5px)';document.body.append(e)}")
        self.install()
        self.page.wait_for_timeout(200)
        self.assertEqual('blur(5px)',self.page.locator('#scrim').evaluate('(e)=>getComputedStyle(e).filter'))
        self.assertFalse(self.page.evaluate('window.rejected||window.accepted'))

if __name__=='__main__':
    unittest.main()
