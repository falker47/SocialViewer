package io.github.falker47.socialviewer.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.falker47.socialviewer.R

/** Native user choices relay to Reddit's existing consent controls. */
@Composable
internal fun RedditCookieConsentOverlay(
    busy: Boolean,
    error: Boolean,
    onReject: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.reddit_native_cookie_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.reddit_native_cookie_message), style = MaterialTheme.typography.bodyMedium)
            if (error) {
                Text(
                    stringResource(R.string.reddit_native_cookie_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }
            OutlinedButton(
                onClick = onReject,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.reddit_native_cookie_reject))
            }
            OutlinedButton(
                onClick = onAccept,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.reddit_native_cookie_accept))
            }
        }
    }
}
