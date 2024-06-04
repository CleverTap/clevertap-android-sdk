package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import com.clevertap.android.sdk.task.MockCTExecutors
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

class FileCleanupStrategyExecutorsTest {

    private val inAppResourceProvider = mockk<InAppResourceProvider>(relaxed = true)
    private val executors = MockCTExecutors()

    private val cleanupStrategy = FileCleanupStrategyExecutors(
        inAppResourceProvider = inAppResourceProvider,
        executor = executors
    )

    @Test
    fun `cleanup deletes all resources`() {
        // setup data
        val urls = listOf("url1", "url2", "url3")
        val successUrls = mutableListOf<String>()

        // invoke method
        cleanupStrategy.clearInAppAssets(urls) { url ->
            successUrls.add(url)
        }

        // check results
        urls.forEach { url ->
            verify {
                inAppResourceProvider.deleteImage(url)
            }
            verify {
                inAppResourceProvider.deleteGif(url)
            }
        }

        assertEquals(urls.size, successUrls.size)
    }
}