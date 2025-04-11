package com.clevertap.android.sdk.inapp.images.cleanup

import TestDispatchers
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import io.mockk.*
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileCleanupStrategyCoroutineTest {

    private val testScheduler = TestCoroutineScheduler()
    private val dispatchers = TestDispatchers(testScheduler)
    private val mFileResourceProvider = mockk<FileResourceProvider>(relaxed = true)
    private val cleanupStrategy = FileCleanupStrategyCoroutine(
        { mFileResourceProvider },
        dispatchers = dispatchers
    )

    @Test
    fun `cleanup deletes all resources`() = testScheduler.run {
        // setup data
        val urls = listOf("url1", "url2", "url3")
        val successUrls = mutableListOf<String>()

        // invoke method
        cleanupStrategy.clearFileAssets(urls) { url ->
            successUrls.add(url)
        }

        advanceUntilIdle()

        urls.forEach { url ->
            verify {mFileResourceProvider.deleteData(url) }
        }

        // assert
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

        testScheduler.advanceUntilIdle()

        // verify no interactions with file provider
        verify(exactly = 0) { mFileResourceProvider.deleteData(any()) }

        // verify no callbacks
        assertTrue(successUrls.isEmpty())
    }
}