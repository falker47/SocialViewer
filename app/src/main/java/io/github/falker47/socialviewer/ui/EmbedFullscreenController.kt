package io.github.falker47.socialviewer.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Hosts the provider's custom view without reparenting or reloading its WebView. */
internal class EmbedFullscreenController(private val context: Context) : WebChromeClient(), AutoCloseable {
    private data class Session(
        val dialog: ComponentDialog,
        val playerHost: FrameLayout,
        val view: View,
        val callback: CustomViewCallback,
    )

    private var session: Session? = null
    private var closed = false

    // WebChromeClient explicitly requires FLAG_FULLSCREEN on the custom-view window.
    @Suppress("DEPRECATION")
    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        val activity = context.findActivity()
        if (closed || session != null || view.parent != null ||
            activity == null || activity.isFinishing || activity.isDestroyed
        ) {
            callback.onCustomViewHidden()
            return
        }

        val dialog = ComponentDialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val playerHost = FrameLayout(dialog.context).apply {
            setBackgroundColor(Color.BLACK)
            addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        val current = Session(dialog, playerHost, view, callback)
        session = current
        dialog.setContentView(playerHost)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener { finishSession(current) }
        dialog.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishSession(current)
            }
        })
        ViewCompat.setOnApplyWindowInsetsListener(playerHost) { target, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            target.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        dialog.window?.apply {
            addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
            WindowCompat.setDecorFitsSystemWindows(this, false)
        }
        try {
            dialog.show()
            dialog.window?.apply {
                setLayout(MATCH_PARENT, MATCH_PARENT)
                WindowCompat.getInsetsController(this, decorView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            ViewCompat.requestApplyInsets(playerHost)
            view.requestFocus()
        } catch (_: WindowManager.BadTokenException) {
            // The host may have been destroyed between the callback and window attachment.
            finishSession(current)
        }
    }

    override fun onHideCustomView() {
        session?.let(::finishSession)
    }

    private fun finishSession(current: Session) {
        if (session !== current) return
        // Clear ownership before dismiss/callback: WebView can re-enter onHideCustomView.
        session = null
        current.playerHost.removeView(current.view)
        current.dialog.setOnDismissListener(null)
        current.dialog.dismiss()
        current.callback.onCustomViewHidden()
    }

    override fun close() {
        closed = true
        onHideCustomView()
    }

    private fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.takeIf { it !== this }?.findActivity()
        else -> null
    }
}
