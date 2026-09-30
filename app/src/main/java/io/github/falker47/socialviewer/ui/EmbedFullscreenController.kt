package io.github.falker47.socialviewer.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.falker47.socialviewer.R
import kotlin.math.roundToInt

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
        val root = LinearLayout(dialog.context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        val toolbar = FrameLayout(dialog.context)
        val exitButton = Button(dialog.context).apply {
            setText(R.string.exit_fullscreen)
            isAllCaps = false
            minHeight = dp(48)
            setOnClickListener { onHideCustomView() }
        }
        toolbar.addView(
            exitButton,
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL)
                .apply { marginEnd = dp(8) },
        )
        // Reserve a native toolbar instead of covering the provider's own controls.
        root.addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        val playerHost = FrameLayout(dialog.context).apply {
            setBackgroundColor(Color.BLACK)
            addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        root.addView(playerHost, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        val current = Session(dialog, playerHost, view, callback)
        session = current
        dialog.setContentView(root)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener { finishSession(current) }
        dialog.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishSession(current)
            }
        })
        ViewCompat.setOnApplyWindowInsetsListener(root) { target, insets ->
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
            ViewCompat.requestApplyInsets(root)
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

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).roundToInt()

    private fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.takeIf { it !== this }?.findActivity()
        else -> null
    }
}
