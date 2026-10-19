package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.domain.SocialContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

sealed interface ViewerState {
    data object Home : ViewerState
    data class XConsent(val url: String) : ViewerState
    // Each attempt is a distinct effect key, even when its URL is unchanged.
    class Loading(val url: String) : ViewerState
    data class Ready(val content: SocialContent) : ViewerState
    data class Error(
        val title: String,
        val message: String,
        val url: String?,
    ) : ViewerState
}

internal suspend fun <T> resolveViewerContent(resolve: () -> T): Result<T> {
    val result = try {
        Result.success(withContext(Dispatchers.IO) { resolve() })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    // A blocking provider can also fail after the viewer has cancelled its request.
    currentCoroutineContext().ensureActive()
    return result
}

internal fun publishViewerResultIfCurrent(
    loading: ViewerState.Loading,
    currentState: ViewerState,
    publish: () -> Unit,
) {
    if (currentState === loading) publish()
}
