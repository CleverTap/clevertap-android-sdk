package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.task.MockCTExecutors
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

class FileCleanupStrategyExecutorsTest {

    private val mFileResourceProvider = mockk<FileResourceProvider>(relaxed = true)
    private val executors = MockCTExecutors()

    private val cleanupStrategy = FileCleanupStrategyExecutors(
        fileResourceProvider = mFileResourceProvider,
        executor = executors
    )

    @Test
    fun `cleanup deletes all resources`() {
        // setup data
        val urls = listOf("url1", "url2", "url3")
        val successUrls = mutableListOf<String>()

        // invoke method
        cleanupStrategy.clearInAppImagesAndGifsV1(urls) { url ->
            successUrls.add(url)
        }

        // check results
        urls.forEach { url ->
            verify {
                mFileResourceProvider.deleteImageMemoryV1(url)
            }
            verify {
                mFileResourceProvider.deleteGifMemoryV1(url)
            }
        }

        assertEquals(urls.size, successUrls.size)
    }
}