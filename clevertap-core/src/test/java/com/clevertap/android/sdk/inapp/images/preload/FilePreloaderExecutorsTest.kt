package com.clevertap.android.sdk.inapp.images.preload

import android.graphics.Bitmap
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.task.MockCTExecutors
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

class FilePreloaderExecutorsTest {

    private val mockBitmap = mockk<Bitmap>()
    private val byteArray = ByteArray(10) { pos ->
        pos.toByte()
    }
    private val mFileResourceProvider = mockk<FileResourceProvider>()
    private val executors = MockCTExecutors()

    private val logger = TestLogger()

    private val filePreloader = FilePreloaderExecutors(
        fileResourceProvider = mFileResourceProvider,
        logger = logger,
        executor = executors
    )

    @Test
    fun `preload image fetches images from all urls`() {
        val urls = mutableListOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")
            .map { Pair(it, CtCacheType.IMAGE) }

        val successUrls = mutableListOf<String>()

        urls.forEach{
            every {
                mFileResourceProvider.fetchInAppImageV1(it.first)
            } returns mockBitmap
        }

        filePreloader.preloadFilesAndCache(urls, { url ->
            successUrls.add(url.first)
        },{},{},{})

        urls.forEach{
            verify {
                mFileResourceProvider.fetchInAppImageV1(it.first)
            }
        }
        assertEquals(urls.size, successUrls.size)
    }

    @Test
    fun `preload gifs fetches gif from all urls`() {
        val urls = mutableListOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")
            .map { Pair(it, CtCacheType.GIF) }
        val successUrls = mutableListOf<String>()

        // replace with forEach
        urls.forEach{
            every {
                mFileResourceProvider.fetchInAppGifV1(it.first)
            } returns byteArray
        }

        filePreloader.preloadFilesAndCache(urls, { url ->
            successUrls.add(url.first)
        },{},{},{})

        // replace with forEach
        urls.forEach{
            verify {
                mFileResourceProvider.fetchInAppGifV1(it.first)
            }
        }
        assertEquals(urls.size, successUrls.size)
    }

    @Test
    fun `preload files fetches files from all urls`() {
        val urls = mutableListOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")
            .map { Pair(it, CtCacheType.FILES) }
        val successUrls = mutableListOf<String>()

        // replace with forEach
        urls.forEach{
            every {
                mFileResourceProvider.fetchFile(it.first)
            } returns byteArray
        }

        filePreloader.preloadFilesAndCache(urls, { url ->
            successUrls.add(url.first)
        },{},{},{})

        urls.forEach {
            verify {
                mFileResourceProvider.fetchFile(it.first)
            }
        }
        assertEquals(urls.size, successUrls.size)
    }

}