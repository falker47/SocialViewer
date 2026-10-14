package io.github.falker47.socialviewer.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.falker47.socialviewer.R
import io.github.falker47.socialviewer.domain.SocialContent

@Composable
internal fun RedditDiscussionAction(content: SocialContent) {
    val url = remember(content.providerId, content.canonicalUrl) {
        RedditDiscussionPolicy.urlFor(content.providerId, content.canonicalUrl)
    } ?: return
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        OutlinedButton(
            onClick = {
                if (!openRedditDiscussion(context, url)) {
                    Toast.makeText(context, R.string.reddit_discussion_no_browser, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.reddit_discussion_open))
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.Outlined.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
