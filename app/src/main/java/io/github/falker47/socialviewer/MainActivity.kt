package io.github.falker47.socialviewer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.falker47.socialviewer.network.UrlConnectionHttpClient
import io.github.falker47.socialviewer.provider.ProviderRegistry
import io.github.falker47.socialviewer.provider.facebook.FacebookProvider
import io.github.falker47.socialviewer.provider.instagram.InstagramProvider
import io.github.falker47.socialviewer.provider.tiktok.TikTokProvider
import io.github.falker47.socialviewer.ui.SocialViewerApp
import io.github.falker47.socialviewer.util.UrlExtractor

class MainActivity : ComponentActivity() {
    private var incomingUrl by mutableStateOf<String?>(null)
    private var resumeToken by mutableStateOf(0)

    private val registry by lazy {
        ProviderRegistry(
            providers = listOf(
                TikTokProvider(UrlConnectionHttpClient()),
                InstagramProvider(UrlConnectionHttpClient()),
                FacebookProvider(UrlConnectionHttpClient()),
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingUrl = extractUrl(intent)

        setContent {
            SocialViewerApp(
                context = this,
                incomingUrl = incomingUrl,
                registry = registry,
                resumeToken = resumeToken,
                onIncomingConsumed = { incomingUrl = null },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        resumeToken += 1
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUrl = extractUrl(intent)
    }

    private fun extractUrl(intent: Intent): String? = when (intent.action) {
        Intent.ACTION_VIEW -> intent.dataString
        Intent.ACTION_SEND -> UrlExtractor.firstHttpUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
        else -> null
    }
}
