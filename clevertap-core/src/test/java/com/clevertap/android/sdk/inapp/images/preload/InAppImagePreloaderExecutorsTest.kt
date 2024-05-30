package com.clevertap.android.sdk.inapp.images.preload

import android.graphics.Bitmap
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import com.clevertap.android.sdk.task.MockCTExecutors
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

class InAppImagePreloaderExecutorsTest {

    private val mockBitmap = mockk<Bitmap>()
    private val byteArray = ByteArray(10) { pos ->
        pos.toByte()
    }
    private val inAppResourceProvider = mockk<InAppResourceProvider>()
    private val executors = MockCTExecutors()

    private val logger = TestLogger()

    private val inAppImagePreloader = FilePreloaderExecutors(
        inAppImageProvider = inAppResourceProvider,
        logger = logger,
        executor = executors
    )

    @Test
    fun `preload image fetches images from all urls`() {
        val urls = mutableListOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")
        val successUrls = mutableListOf<String>()

        for (url in urls) {
            every {
                inAppResourceProvider.fetchInAppImage(url)
            } returns mockBitmap
        }

        inAppImagePreloader.preloadImages(urls) { url ->
            successUrls.add(url)
        }

        for (count in 0 until urls.size) {
            val url = urls[count]
            verify {
                inAppResourceProvider.fetchInAppImage(url)
            }
        }
        assertEquals(urls.size, successUrls.size)
    }

    @Test
    fun `preload gifs fetches gif from all urls`() {
        val urls = mutableListOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")
        val successUrls = mutableListOf<String>()

        for (url in urls) {
            every {
                inAppResourceProvider.fetchInAppGif(url)
            } returns byteArray
        }

        inAppImagePreloader.preloadGifs(urls) { url ->
            successUrls.add(url)
        }

        for (count in 0 until urls.size) {
            val url = urls[count]
            verify {
                inAppResourceProvider.fetchInAppGif(url)
            }
        }
        assertEquals(urls.size, successUrls.size)
    }
}