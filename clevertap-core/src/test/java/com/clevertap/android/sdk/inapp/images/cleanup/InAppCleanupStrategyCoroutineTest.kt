package com.clevertap.android.sdk.inapp.images.cleanup

import TestDispatchers
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Test
import org.mockito.Mockito
import kotlin.test.assertEquals

class InAppCleanupStrategyCoroutineTest {

    private val inAppResourceProvider = Mockito.mock(InAppResourceProvider::class.java)

    private val testScheduler = TestCoroutineScheduler()
    private val dispatchers = TestDispatchers(testScheduler)

    private val cleanupStrategy = InAppCleanupStrategyCoroutine(
        inAppResourceProvider = inAppResourceProvider,
        dispatchers = dispatchers
    )

    @Test
    fun `cleanup deletes all resources`() = testScheduler.run {
        // setup data
        val urls = listOf("url1", "url2", "url3")
        val successUrls = mutableListOf<String>()

        // invoke method
        cleanupStrategy.clearAssets(urls) { url ->
            successUrls.add(url)
        }

        advanceUntilIdle()

        urls.forEach { url ->
            Mockito.verify(inAppResourceProvider).deleteImage(url)
            Mockito.verify(inAppResourceProvider).deleteGif(url)
        }

        // assert
        assertEquals(urls.size, successUrls.size)
    }
}