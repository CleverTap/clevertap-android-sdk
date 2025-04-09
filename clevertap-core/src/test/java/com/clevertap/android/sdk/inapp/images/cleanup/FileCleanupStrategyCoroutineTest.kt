package com.clevertap.android.sdk.inapp.images.cleanup

import TestDispatchers
import android.content.Context
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import io.mockk.*
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileCleanupStrategyCoroutineTest {

    private val testScheduler = TestCoroutineScheduler()
    private val dispatchers = TestDispatchers(testScheduler)
    private val context = mockk<Context>()
    private val mFileResourceProvider = mockk<FileResourceProvider>(relaxed = true)
    private val logger = mockk<ILogger>(relaxed = true)
    private val cleanupStrategy = FileCleanupStrategyCoroutine(
        context = context,
        logger = logger,
        dispatchers = dispatchers
    )

    @Before
    fun setUp() {
        // Mock the singleton getInstance method
        mockkObject(FileResourceProvider.Companion)
        every { FileResourceProvider.getInstance(any(), any()) } returns mFileResourceProvider
    }

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