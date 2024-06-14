package com.clevertap.android.sdk.inapp.images.cleanup

import TestDispatchers
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Test
import org.mockito.Mockito
import kotlin.test.assertEquals

class FileCleanupStrategyCoroutineTest {

    private val mFileResourceProvider = Mockito.mock(FileResourceProvider::class.java)

    private val testScheduler = TestCoroutineScheduler()
    private val dispatchers = TestDispatchers(testScheduler)

    private val cleanupStrategy = FileCleanupStrategyCoroutine(
        fileResourceProvider = mFileResourceProvider,
        dispatchers = dispatchers
    )
/*
    @Test
    fun `cleanup deletes all resources`() = testScheduler.run {
        // setup data
        val urls = listOf("url1", "url2", "url3")
        val successUrls = mutableListOf<String>()

        // invoke method
        cleanupStrategy.clearInAppImagesAndGifsV1(urls) { url ->
            successUrls.add(url)
        }

        advanceUntilIdle()

        urls.forEach { url ->
            Mockito.verify(mFileResourceProvider).deleteImageMemoryV1(url)
            Mockito.verify(mFileResourceProvider).deleteGifMemoryV1(url)
        }

        // assert
        assertEquals(urls.size, successUrls.size)
    }*/
}