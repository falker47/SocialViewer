"""Synthetic real-Chromium tests for the user-controlled Reddit cookie handoff."""
import os
import unittest
from pathlib import Path
from playwright.sync_api import sync_playwright

SCRIPT=(Path(__file__).resolve().parents[1]/'app/src/debug/assets/reddit-consent-choice.js').read_text()
HTML='''<!doctype html><html><body><rpl-dialog-sheet id="data-protection-consent-dialog" open><rpl-modal-card><button id="reject">Rifiuta i cookie opzionali</button><button id="accept">Accetta tutti</button></rpl-modal-card></rpl-dialog-sheet><rpl-dialog-sheet id="other" open><button>Accetta tutti</button></rpl-dialog-sheet><script>window.choices=[];for(const action of ['reject','accept'])document.getElementById(action).addEventListener('click',()=>{window.choices.push(action);document.getElementById('data-protection-consent-dialog').removeAttribute('open')});</script></body></html>'''

class CookieHandoffTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pw=sync_playwright().start()
        cls.browser=cls.pw.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH','/usr/bin/chromium'),headless=True,args=['--no-sandbox'])
    @classmethod
    def tearDownClass(cls):
        cls.browser.close();cls.pw.stop()
    def setUp(self):
        self.context=self.browser.new_context(viewport={'width':396,'height':711})
        self.page=self.context.new_page()
        self.page.set_content(HTML)
    def tearDown(self): self.context.close()
    def install(self,url='https://www.reddit.com/r/AskReddit/comments/abcdef/example/'):
        self.page.evaluate('([source,href]) => (new Function("location",source))(new URL(href))',[SCRIPT,url])
    def test_no_automatic_choice(self):
        self.install()
        self.assertEqual('ready',self.page.evaluate('window.__svCookieChoice.state()'))
        self.assertEqual([],self.page.evaluate('window.choices'))
    def test_reject(self):
        self.install()
        self.assertTrue(self.page.evaluate('window.__svCookieChoice.choose("reject")'))
        self.assertEqual(['reject'],self.page.evaluate('window.choices'))
        self.assertEqual('absent',self.page.evaluate('window.__svCookieChoice.state()'))
    def test_accept(self):
        self.install()
        self.assertTrue(self.page.evaluate('window.__svCookieChoice.choose("accept")'))
        self.assertEqual(['accept'],self.page.evaluate('window.choices'))
    def test_unknown_choice_blocked(self):
        self.install()
        self.assertFalse(self.page.evaluate('window.__svCookieChoice.choose("other")'))
        self.assertEqual([],self.page.evaluate('window.choices'))
    def test_non_reddit_page_refused(self):
        self.install('https://example.com/')
        self.assertFalse(self.page.evaluate('Boolean(window.__svCookieChoice)'))
    def test_non_active_cookie_dialog_refused(self):
        self.page.locator('#data-protection-consent-dialog').evaluate('e=>e.removeAttribute("open")')
        self.install()
        self.assertEqual('absent',self.page.evaluate('window.__svCookieChoice.state()'))
        self.assertFalse(self.page.evaluate('window.__svCookieChoice.choose("accept")'))
    def test_requires_both_choices(self):
        self.page.locator('#accept').evaluate('e=>e.remove()')
        self.install()
        self.assertEqual('absent',self.page.evaluate('window.__svCookieChoice.state()'))
    def test_in_english(self):
        self.page.locator('#reject').evaluate("e=>e.textContent='Reject optional cookies'")
        self.page.locator('#accept').evaluate("e=>e.textContent='Accept all'")
        self.install()
        self.assertEqual('ready',self.page.evaluate('window.__svCookieChoice.state()'))
    def test_open_shadow_root(self):
        self.page.set_content('<html><body><test-shell></test-shell><script>window.choices=[]</script></body></html>')
        self.page.evaluate("""() => {
          const root=document.querySelector('test-shell').attachShadow({mode:'open'});
          root.innerHTML='<rpl-dialog-sheet id="data-protection-consent-dialog" open><button id="reject">Rifiuta i cookie opzionali</button><button>Accetta tutti</button></rpl-dialog-sheet>';
          root.querySelector('#reject').onclick=()=>window.choices.push('reject');
        }""")
        self.install()
        self.assertEqual('ready',self.page.evaluate('window.__svCookieChoice.state()'))
        self.assertTrue(self.page.evaluate('window.__svCookieChoice.choose("reject")'))
        self.assertEqual(['reject'],self.page.evaluate('window.choices'))
    def test_nested_open_shadow_buttons_are_found(self):
        self.page.set_content('<html><body><rpl-dialog-sheet id="data-protection-consent-dialog" open><custom-wrapper></custom-wrapper></rpl-dialog-sheet><script>window.choices=[]</script></body></html>')
        self.page.evaluate("""() => {
          const root=document.querySelector('custom-wrapper').attachShadow({mode:'open'});
          root.innerHTML='<faceplate-button><button id="no">Rifiuta i cookie opzionali</button></faceplate-button><faceplate-button><button id="yes">Accetta tutti</button></faceplate-button>';
          root.querySelector('#no').onclick=()=>window.choices.push('reject');
          root.querySelector('#yes').onclick=()=>window.choices.push('accept');
        }""")
        self.install()
        self.assertEqual('ready',self.page.evaluate('window.__svCookieChoice.state()'))
        self.assertEqual([],self.page.evaluate('window.choices'))
        self.assertTrue(self.page.evaluate('window.__svCookieChoice.choose("reject")'))
        self.assertEqual(['reject'],self.page.evaluate('window.choices'))

    def test_consent_component_can_be_class_marked_instead_of_exact_id(self):
        self.page.set_content('<html><body><rpl-dialog-sheet class="consent-modal" open><custom-wrapper></custom-wrapper></rpl-dialog-sheet><script>window.choices=[]</script></body></html>')
        self.page.evaluate("""() => {
          const root=document.querySelector('custom-wrapper').attachShadow({mode:'open'});
          root.innerHTML='<button id="no">Rifiuta i cookie opzionali</button><button id="yes">Accetta tutti</button>';
          root.querySelector('#yes').onclick=()=>window.choices.push('accept');
        }""")
        self.install()
        self.assertEqual('ready',self.page.evaluate('window.__svCookieChoice.state()'))
        self.assertEqual([],self.page.evaluate('window.choices'))
        self.assertTrue(self.page.evaluate('window.__svCookieChoice.choose("accept")'))
        self.assertEqual(['accept'],self.page.evaluate('window.choices'))

    def test_portaled_cookie_choices_require_an_open_consent_dialog(self):
        self.page.set_content('<html><body><rpl-dialog-sheet id="data-protection-consent-dialog" open></rpl-dialog-sheet><div class="modal-root"><button id="no">Rifiuta i cookie opzionali</button><button id="yes">Accetta tutti</button></div><script>window.choices=[];document.querySelector("#no").onclick=()=>window.choices.push("reject");</script></body></html>')
        self.install()
        self.assertEqual('ready',self.page.evaluate('window.__svCookieChoice.state()'))
        self.assertEqual([],self.page.evaluate('window.choices'))
        self.assertTrue(self.page.evaluate('window.__svCookieChoice.choose("reject")'))
        self.assertEqual(['reject'],self.page.evaluate('window.choices'))

    def test_non_consent_sheet_never_triggers_native_cookie_controls(self):
        self.page.set_content('<html><body><rpl-dialog-sheet id="unrelated-sheet" open><button>Rifiuta i cookie opzionali</button><button>Accetta tutti</button></rpl-dialog-sheet></body></html>')
        self.install()
        self.assertEqual('absent',self.page.evaluate('window.__svCookieChoice.state()'))
        self.assertFalse(self.page.evaluate('window.__svCookieChoice.choose("accept")'))

    def test_landscape_does_not_duplicate_cookie_dialog(self):
        self.page.set_viewport_size({'width':850,'height':410})
        self.install()
        self.assertEqual('absent',self.page.evaluate('window.__svCookieChoice.state()'))

if __name__=='__main__': unittest.main()
