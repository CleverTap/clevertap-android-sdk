package com.clevertap.android.sdk.inapp.images.cleanup

import android.content.Context
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.task.MockCTExecutors
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileCleanupStrategyExecutorsTest {

    private val mFileResourceProvider = mockk<FileResourceProvider>(relaxed = true)
    private val executors = MockCTExecutors()
    private val cleanupStrategy = FileCleanupStrategyExecutors(
        { mFileResourceProvider },
        executor = executors
    )

    @Test
    fun `cleanup deletes all resources`() {
        // setup data
        val urls = listOf("url1", "url2", "url3")
        val successUrls = mutableListOf<String>()

        // invoke method
        cleanupStrategy.clearFileAssets(urls) { url ->
            successUrls.add(url)
        }

        // check results
        urls.forEach { url ->
            verify {
                mFileResourceProvider.deleteData(url)
            }
        }

        assertEquals(urls.size, successUrls.size)
    }

    @Test
    fun `clearFileAssets with empty list does nothing`() = runTest {
        // setup
        val emptyUrls = emptyList<String>()
        val successUrls = mutableListOf<String>()

        // invoke
        cleanupStrategy.clearFileAssets(emptyUrls) { url ->
            successUrls.add(url)
        }

        // verify no interactions with file provider
        verify(exactly = 0) { mFileResourceProvider.deleteData(any()) }

        // verify no callbacks
        assertTrue(successUrls.isEmpty())
    }
}