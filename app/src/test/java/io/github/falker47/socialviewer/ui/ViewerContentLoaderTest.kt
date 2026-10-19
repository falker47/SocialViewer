package io.github.falker47.socialviewer.ui

import io.github.falker47.socialviewer.domain.SocialContent
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerContentLoaderTest {
    @Test(timeout = 10_000)
    fun cancelledResolutionDoesNotReplaceHome() {
        assertCancelledResolutionKeepsHome()
    }

    @Test(timeout = 10_000)
    fun failureFromCancelledResolutionDoesNotReplaceHome() {
        assertCancelledResolutionKeepsHome(IOException("The abandoned request failed"))
    }

    @Test(timeout = 10_000)
    fun cancelledResolutionDoesNotReplaceNewerContent() = runBlocking {
        val resolver = BlockingResolver()
        var state = "Loading(A)"
        val abandoned = launch {
            state = resolveViewerContent(resolver::resolve).fold(
                onSuccess = { "Ready($it)" },
                onFailure = { "Error(A)" },
            )
        }

        try {
            resolver.awaitStarted()
            state = "Home"
            abandoned.cancel()
            state = "Loading(B)"
            val newer = launch {
                state = resolveViewerContent { "B" }.fold(
                    onSuccess = { "Ready($it)" },
                    onFailure = { "Error(B)" },
                )
            }
            newer.join()
            assertEquals("Ready(B)", state)
        } finally {
            resolver.release()
        }

        abandoned.join()
        assertEquals("Ready(B)", state)
    }

    @Test(timeout = 10_000)
    fun successfulResolutionPublishesContent() = runBlocking {
        val loading = ViewerState.Loading("A")
        var state: ViewerState = loading
        val result = resolveViewerContent { contentFor("A") }
        publishViewerResultIfCurrent(loading, state) {
            state = result.fold(
                onSuccess = { ViewerState.Ready(it) },
                onFailure = { ViewerState.Error("Error", "Failed", "A") },
            )
        }

        assertEquals(ViewerState.Ready(contentFor("A")), state)
    }

    @Test(timeout = 10_000)
    fun ordinaryResolverFailureRemainsVisible() = runBlocking {
        val loading = ViewerState.Loading("A")
        var state: ViewerState = loading
        val failure = IOException("The request failed")
        val result = resolveViewerContent<SocialContent> { throw failure }
        publishViewerResultIfCurrent(loading, state) {
            state = result.fold(
                onSuccess = { ViewerState.Ready(it) },
                onFailure = { ViewerState.Error("Error", "Failed", "A") },
            )
        }

        assertEquals(ViewerState.Error("Error", "Failed", "A"), state)
        val propagated = result.exceptionOrNull()
        assertEquals(IOException::class.java, propagated?.javaClass)
        assertEquals("The request failed", propagated?.message)
        // Coroutine stacktrace recovery may copy the exception and retain the original as its cause.
        assertTrue(
            "The original resolver failure must remain in the cause chain",
            generateSequence(propagated) { it.cause }.any { it === failure },
        )
    }

    @Test(timeout = 10_000)
    fun supersededResolutionDoesNotReplaceHomeBeforeCancellation() {
        assertSupersededResolutionKeepsState(ViewerState.Home)
    }

    @Test(timeout = 10_000)
    fun supersededResolutionDoesNotReplaceNewerContentBeforeCancellation() {
        assertSupersededResolutionKeepsState(ViewerState.Ready(contentFor("B")))
    }

    @Test(timeout = 10_000)
    fun aNewAttemptForTheSameUrlRestartsLoadingAndRejectsThePreviousResult() {
        assertSupersededResolutionKeepsState(ViewerState.Loading("A"))
    }

    private fun assertSupersededResolutionKeepsState(replacement: ViewerState) = runBlocking {
        val loading = ViewerState.Loading("A")
        assertNotEquals("Replacing a request must restart the state-keyed effect", loading, replacement)
        val resolver = BlockingResolver()
        var state: ViewerState = loading
        val superseded = launch {
            val result = resolveViewerContent(resolver::resolve)
            publishViewerResultIfCurrent(loading, state) {
                state = result.fold(
                    onSuccess = { ViewerState.Ready(contentFor(it)) },
                    onFailure = { ViewerState.Error("Error", "Failed", "A") },
                )
            }
        }

        try {
            resolver.awaitStarted()
            state = replacement
            // State callbacks run before Compose disposes the effect: no cancellation yet.
            assertTrue(superseded.isActive)
        } finally {
            resolver.release()
        }

        superseded.join()
        assertSame(replacement, state)
    }

    private fun assertCancelledResolutionKeepsHome(failure: Exception? = null) = runBlocking {
        val resolver = BlockingResolver(failure)
        var state = "Loading(A)"
        val abandoned = launch {
            state = resolveViewerContent(resolver::resolve).fold(
                onSuccess = { "Ready($it)" },
                onFailure = { "Error(A)" },
            )
        }

        try {
            resolver.awaitStarted()
            state = "Home"
            abandoned.cancel()
        } finally {
            resolver.release()
        }

        abandoned.join()
        assertEquals("Home", state)
    }

    private class BlockingResolver(private val failure: Exception? = null) {
        private val started = CompletableDeferred<Unit>()
        private val completion = CountDownLatch(1)

        fun resolve(): String {
            started.complete(Unit)
            check(completion.await(5, TimeUnit.SECONDS)) {
                "Timed out waiting to release the blocking resolver"
            }
            failure?.let { throw it }
            return "A"
        }

        suspend fun awaitStarted() {
            withTimeout(5_000) { started.await() }
        }

        fun release() {
            completion.countDown()
        }
    }

    private fun contentFor(url: String): SocialContent = SocialContent(
        providerId = "test",
        providerName = "Test",
        canonicalUrl = url,
        title = null,
        authorName = null,
        documentBaseUrl = "https://example.test/",
        embedHtml = "<p>Content</p>",
    )
}
