package io.github.falker47.socialviewer.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.widget.Button
import androidx.activity.ComponentDialog
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EmbedFullscreenControllerTest {
    private lateinit var activityController: ActivityController<Activity>
    private lateinit var activity: Activity
    private var client: WebChromeClient? = null

    @Before fun setUp() {
        activityController = Robolectric.buildActivity(Activity::class.java).setup()
        activity = activityController.get()
    }

    @After fun tearDown() {
        (client as? AutoCloseable)?.close()
        activityController.pause().stop().destroy()
    }

    private fun newClient(): WebChromeClient {
        // Reflection lets the test-first commit compile against the unmodified baseline.
        val type = runCatching {
            Class.forName("io.github.falker47.socialviewer.ui.EmbedFullscreenController")
        }.getOrNull()
        assertNotNull("The WebView needs a native fullscreen controller", type)
        return (type!!.getDeclaredConstructor(Context::class.java).newInstance(activity) as WebChromeClient)
            .also { client = it }
    }

    @Test fun showsCustomViewInFullscreenAndRestoresOnHide() {
        val chrome = newClient()
        val view = View(activity)
        var hidden = 0
        val originalFlags = activity.window.attributes.flags
        chrome.onShowCustomView(view) { hidden++ }
        val dialog = ShadowDialog.getLatestDialog()
        assertTrue(dialog.isShowing)
        assertNotNull(view.parent)
        assertTrue(dialog.window!!.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0)
        chrome.onHideCustomView()
        chrome.onHideCustomView()
        assertFalse(dialog.isShowing)
        assertNull(view.parent)
        assertEquals(1, hidden)
        assertEquals(originalFlags, activity.window.attributes.flags)
    }

    @Test fun backExitsFullscreenWithoutFinishingActivity() {
        val chrome = newClient()
        var hidden = 0
        chrome.onShowCustomView(View(activity)) { hidden++ }
        val dialog = ShadowDialog.getLatestDialog() as ComponentDialog
        dialog.onBackPressedDispatcher.onBackPressed()
        assertFalse(dialog.isShowing)
        assertFalse(activity.isFinishing)
        assertEquals(1, hidden)
    }

    @Test fun explicitExitButtonClosesFullscreen() {
        val chrome = newClient()
        var hidden = 0
        chrome.onShowCustomView(View(activity)) { hidden++ }
        val dialog = ShadowDialog.getLatestDialog()
        val button = descendants(dialog.window!!.decorView).filterIsInstance<Button>().single()
        assertEquals("Exit fullscreen", button.text.toString())
        button.performClick()
        assertFalse(dialog.isShowing)
        assertEquals(1, hidden)
    }

    @Test fun duplicateRequestIsRejectedWithoutReplacingActiveView() {
        val chrome = newClient()
        val first = View(activity)
        val second = View(activity)
        var firstHidden = 0
        var secondHidden = 0
        chrome.onShowCustomView(first) { firstHidden++ }
        val dialog = ShadowDialog.getLatestDialog()
        chrome.onShowCustomView(second) { secondHidden++ }
        assertSame(dialog, ShadowDialog.getLatestDialog())
        assertTrue(dialog.isShowing)
        assertNotNull(first.parent)
        assertNull(second.parent)
        assertEquals(0, firstHidden)
        assertEquals(1, secondHidden)
    }

    @Test fun closeReleasesTheViewAndRejectsLateCallbacks() {
        val chrome = newClient()
        val view = View(activity)
        var hidden = 0
        chrome.onShowCustomView(view) { hidden++ }
        val dialog = ShadowDialog.getLatestDialog()
        (chrome as AutoCloseable).close()
        chrome.close()
        assertFalse(dialog.isShowing)
        assertNull(view.parent)
        assertEquals(1, hidden)
        chrome.onShowCustomView(View(activity)) { hidden++ }
        assertEquals(2, hidden)
        assertFalse(dialog.isShowing)
    }

    @Test fun canEnterAgainAfterExit() {
        val chrome = newClient()
        repeat(3) {
            var hidden = 0
            chrome.onShowCustomView(View(activity)) { hidden++ }
            assertTrue(ShadowDialog.getLatestDialog().isShowing)
            chrome.onHideCustomView()
            assertFalse(ShadowDialog.getLatestDialog().isShowing)
            assertEquals(1, hidden)
        }
    }

    @Test fun sharedWebViewWiresAndDisposesTheFullscreenController() {
        val source = File("src/main/java/io/github/falker47/socialviewer/ui/EmbedWebView.kt").readText()
        assertTrue("The shared WebView must install a WebChromeClient", source.contains("webChromeClient = fullscreenController"))
        assertTrue("Disposal must release the native fullscreen window", source.contains("fullscreenController.close()"))
    }

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
        }
    }
}
