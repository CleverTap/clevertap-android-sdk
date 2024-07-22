package com.clevertap.android.sdk.inapp.images

import android.graphics.Bitmap
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.data.CtCacheType.FILES
import com.clevertap.android.sdk.inapp.data.CtCacheType.GIF
import com.clevertap.android.sdk.inapp.data.CtCacheType.IMAGE
import com.clevertap.android.sdk.inapp.images.memory.FileMemoryAccessObject
import com.clevertap.android.sdk.inapp.images.memory.InAppGifMemoryAccessObjectV1
import com.clevertap.android.sdk.inapp.images.memory.InAppImageMemoryAccessObjectV1
import com.clevertap.android.sdk.inapp.images.memory.MemoryAccessObject
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType
import com.clevertap.android.sdk.network.DownloadedBitmap
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class FileResourceProviderTest {
    private val mockLogger = TestLogger()
    private val mockFileFetchApi = mockk<FileFetchApiContract>()
    private val mockImageMAO = mockk<InAppImageMemoryAccessObjectV1>()
    private val mockGifMAO = mockk<InAppGifMemoryAccessObjectV1>()
    private val mockFileMAO = mockk<FileMemoryAccessObject>()
    private lateinit var fileResourceProvider: FileResourceProvider
    private lateinit var mapOfMAO: Map<CtCacheType, List<MemoryAccessObject<*>>>

    @Before
    fun setup() {
        // Initialize FileResourceProvider with mock dependencies
        fileResourceProvider = FileResourceProvider(
            images = mockk(relaxed = true),
            gifs = mockk(relaxed = true),
            allFileTypesDir = mockk(relaxed = true),
            logger = mockLogger,
            inAppRemoteSource = mockFileFetchApi,
            ctCaches = mockk(relaxed = true),
            imageMAO = mockImageMAO,
            gifMAO = mockGifMAO,
            fileMAO = mockFileMAO
        )
        mapOfMAO =
            mapOf<CtCacheType, List<MemoryAccessObject<*>>>(
                IMAGE to listOf(mockImageMAO, mockFileMAO, mockGifMAO),
                GIF to listOf(mockGifMAO, mockFileMAO, mockImageMAO),
                FILES to listOf(mockFileMAO, mockImageMAO, mockGifMAO)
            )
        // All MAO mocks returns null
        mapOfMAO[IMAGE]!!.forEach{
            every { it.fetchInMemory(any()) } returns null
            every { it.fetchDiskMemory(any()) } returns null
        }
    }
    @Test
    fun `isFileCached returns true for IMAGE found in disk-memory`() {
        val url = "https://example.com/image.jpg"
        every { mockImageMAO.fetchDiskMemory(url) } returns mockk()
        assertTrue(fileResourceProvider.isFileCached(url))
    }

    @Test
    fun `isFileCached returns true for GIF found in disk-memory`() {
        val url = "https://example.com/animation.gif"
        every { mockGifMAO.fetchDiskMemory(url) } returns mockk() // Simulate file found on disk
        assertTrue(fileResourceProvider.isFileCached(url))
    }
    @Test
    fun `isFileCached returns true for FILES found in disk-memory`() {
        val url = "https://example.com/document.pdf"
        every { mockFileMAO.fetchDiskMemory(url) } returns mockk() // Simulate file found on disk
        assertTrue(fileResourceProvider.isFileCached(url))
    }
    @Test
    fun `isFileCached returns true for IMAGE found in in-memory`() {
        val url = "https://example.com/image.jpg"
        every { mockImageMAO.fetchInMemory(url) } returns mockk() // Simulate file found in in-memory

        assertTrue(fileResourceProvider.isFileCached(url))
        verify(exactly = 0) { mockImageMAO.fetchDiskMemory(url) } // Verify disk-memory is not checked
    }

    @Test
    fun `isFileCached returns true for GIF found in in-memory`() {
        val url = "https://example.com/animation.gif"
        every { mockGifMAO.fetchInMemory(url) } returns mockk() // Simulate file found in in-memory

        assertTrue(fileResourceProvider.isFileCached(url))
        verify(exactly = 0) { mockGifMAO.fetchDiskMemory(url) } // Verify disk-memory is not checked
    }

    @Test
    fun `isFileCachedreturns true for FILES found in in-memory`() {
        val url = "https://example.com/document.pdf"
        every { mockFileMAO.fetchInMemory(url) } returns mockk() // Simulate file found in in-memory

        assertTrue(fileResourceProvider.isFileCached(url))
        verify(exactly = 0) { mockFileMAO.fetchDiskMemory(url) } // Verify disk-memory is not checked
    }

    @Test
    fun `isFileCached returns false for non-cached file`() {
        val url = "https://example.com/file.pdf"
        assertFalse(fileResourceProvider.isFileCached(url))
    }

    @Test
    fun `fetchInAppImageV1 returns cached image if available`() {
        val url = "https://example.com/image.jpg"
        val mockBitmap = mockk<Bitmap>()
        every { mockImageMAO.fetchInMemoryAndTransform(url, MemoryDataTransformationType.ToBitmap) } returns mockBitmap

        val result = fileResourceProvider.fetchInAppImageV1(url)

        assertEquals(mockBitmap, result)
        verify(exactly = 0) { mockFileFetchApi.makeApiCallForFile(any()) } // No network call
    }

    @Test
    fun `fetchInAppImageV1 fetches and caches image if not cached`() {
        val mockSavedFile = mockk<File>()
        val url = "https://example.com/image.jpg"
        val mockDownloadedBitmap = DownloadedBitmap(
            bitmap = mockk(),
            status = DownloadedBitmap.Status.SUCCESS,
            downloadTime = 0L,
            bytes = byteArrayOf()
        )
        mapOfMAO[IMAGE]!!.forEach {
            every { it.fetchInMemoryAndTransform(url, MemoryDataTransformationType.ToBitmap) } returns null
            every { it.fetchDiskMemoryAndTransform(url, MemoryDataTransformationType.ToBitmap) } returns null
        }
        every { mockFileFetchApi.makeApiCallForFile(Pair(url, CtCacheType.IMAGE)) } returns mockDownloadedBitmap
        every { mockImageMAO.saveDiskMemory(url, any()) } returns mockSavedFile
        every { mockImageMAO.saveInMemory(url, any()) } returns true

        val result = fileResourceProvider.fetchInAppImageV1(url)

        assertEquals(mockDownloadedBitmap.bitmap, result) // Verify image was fetched
        verify { mockImageMAO.saveDiskMemory(url, mockDownloadedBitmap.bytes!!) }
        verify { mockImageMAO.saveInMemory(url, Pair(mockDownloadedBitmap.bitmap!!,mockSavedFile)) } // Verify caching
    }

    @Test
    fun `fetchInAppGifV1 returns cached GIF if available`() {
        val url = "https://example.com/animation.gif"
        val mockGifBytes = byteArrayOf(1, 2, 3)
        every { mockGifMAO.fetchInMemoryAndTransform(url, MemoryDataTransformationType.ToByteArray) } returns mockGifBytes

        val result = fileResourceProvider.fetchInAppGifV1(url)

        assertEquals(mockGifBytes, result)
        verify(exactly = 0) { mockFileFetchApi.makeApiCallForFile(any()) } // No network call
    }

    @Test
    fun `fetchInAppGifV1 fetches and caches GIF if not cached`() {
        val mockSavedFile = mockk<File>()
        val url = "https://example.com/animation.gif"
        val mockDownloadedGif = DownloadedBitmap(
            bitmap = null, // GIFs don't have Bitmaps
            status = DownloadedBitmap.Status.SUCCESS,
            downloadTime = 0L,
            bytes = byteArrayOf(1, 2, 3)
        )
        mapOfMAO[GIF]!!.forEach {
            every { it.fetchInMemoryAndTransform(url, MemoryDataTransformationType.ToByteArray) } returns null
            every { it.fetchDiskMemoryAndTransform(url, MemoryDataTransformationType.ToByteArray) } returns null
        }
        every { mockFileFetchApi.makeApiCallForFile(Pair(url, CtCacheType.GIF)) } returns mockDownloadedGif
        every { mockGifMAO.saveDiskMemory(url, any()) } returns mockSavedFile
        every { mockGifMAO.saveInMemory(url, any()) } returns true

        val result = fileResourceProvider.fetchInAppGifV1(url)

        assertEquals(mockDownloadedGif.bytes, result) // Verify GIF was fetched
        verify { mockGifMAO.saveDiskMemory(url, mockDownloadedGif.bytes!!) }
        verify { mockGifMAO.saveInMemory(url, Pair(mockDownloadedGif.bytes!!, mockSavedFile)) } // Verify caching
    }

    @Test
    fun `fetchFile returns cached file if available`() {
        val url = "https://example.com/document.pdf"
        val mockFileBytes = byteArrayOf(4, 5, 6)
        every { mockFileMAO.fetchInMemoryAndTransform(url, MemoryDataTransformationType.ToByteArray) } returns mockFileBytes

        val result = fileResourceProvider.fetchFile(url)

        assertEquals(mockFileBytes, result)
        verify(exactly = 0) { mockFileFetchApi.makeApiCallForFile(any()) } // No network call
    }

    @Test
    fun `fetchFile fetches and caches file if not cached`() {
        val mockSavedFile = mockk<File>()
        val url = "https://example.com/document.pdf"
        val downloadSuccess = DownloadedBitmap(
            bitmap = null, // Files don't have Bitmaps
            status = DownloadedBitmap.Status.SUCCESS,
            downloadTime = 0L,
            bytes = byteArrayOf(4, 5, 6)
        )
        mapOfMAO[FILES]!!.forEach {
            every { it.fetchInMemoryAndTransform(url, MemoryDataTransformationType.ToByteArray) } returns null
            every { it.fetchDiskMemoryAndTransform(url, MemoryDataTransformationType.ToByteArray) } returns null
        }
        every { mockFileFetchApi.makeApiCallForFile(Pair(url, CtCacheType.FILES)) } returns downloadSuccess
        every { mockFileMAO.saveDiskMemory(url, any()) } returns mockSavedFile
        every { mockFileMAO.saveInMemory(url, any()) } returns true

        val result = fileResourceProvider.fetchFile(url)

        assertEquals(downloadSuccess.bytes, result) // Verify file was fetched
        verify { mockFileMAO.saveDiskMemory(url, downloadSuccess.bytes!!) }
        verify { mockFileMAO.saveInMemory(url, Pair(downloadSuccess.bytes!!, mockSavedFile)) } // Verify caching
    }

    @Test
    fun `fetchFile does not caches file if it doesn't exist in cache and api call fails as well`() {
        val mockSavedFile = mockk<File>()
        val url = "https://example.com/document.pdf"
        val downloadFailed = DownloadedBitmap(
            bitmap = null, // Files don't have Bitmaps
            status = DownloadedBitmap.Status.DOWNLOAD_FAILED,
            downloadTime = 0L,
            bytes = null
        )
        mapOfMAO[FILES]!!.forEach {
            every { it.fetchInMemoryAndTransform(url, MemoryDataTransformationType.ToByteArray) } returns null
            every { it.fetchDiskMemoryAndTransform(url, MemoryDataTransformationType.ToByteArray) } returns null
        }
        every { mockFileFetchApi.makeApiCallForFile(Pair(url, CtCacheType.FILES)) } returns downloadFailed

        val result = fileResourceProvider.fetchFile(url)

        assertEquals(expected = null, actual = result) // Verify file was null
    }

    @Test
    fun `deleteData removes data from all caches`() {
        val cacheKey = "someKey"
        mapOfMAO.values.flatten().forEach { mao ->
            every { mao.removeInMemory(cacheKey) } returns mockk()
            every { mao.removeDiskMemory(cacheKey) } returns true
        }

        fileResourceProvider.deleteData(cacheKey)

        // convert mapOfMAO to list of MAO
        mapOfMAO.values.flatten().forEach { mao ->
            verify { mao.removeInMemory(cacheKey) }
            verify { mao.removeDiskMemory(cacheKey) }
        }
    }

}