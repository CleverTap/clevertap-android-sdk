import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.memory.FileMemoryAccessObject
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToBitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToByteArray
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToFile
import com.clevertap.android.sdk.inapp.images.memory.bytesToBitmap
import com.clevertap.android.sdk.inapp.images.memory.fileToBitmap
import com.clevertap.android.sdk.utils.CTCaches
import com.clevertap.android.sdk.utils.FileCache
import com.clevertap.android.sdk.utils.LruCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FileMemoryAccessObjectTest {

    private val mockCTCaches = mockk<CTCaches>()
    private val mockLogger = mockk<ILogger>(relaxed = true) // Relaxed to avoid unnecessary stubbing
    private val mockMemoryCache = mockk<LruCache<Pair<ByteArray, File>>>()
    private val mockDiskCache = mockk<FileCache>()

    private lateinit var fileMemoryAccessObject: FileMemoryAccessObject

    private val key = "test_key"
    private val mockData = Pair(byteArrayOf(1, 2, 3), mockk<File>())

    @Before
    fun setup() {
        every { mockCTCaches.fileLruCache() } returns mockMemoryCache
        every { mockCTCaches.fileCacheDisk() } returns mockDiskCache
        fileMemoryAccessObject = FileMemoryAccessObject(mockCTCaches, mockLogger)

        mockkStatic("com.clevertap.android.sdk.inapp.images.memory.MemoryAccessObjectKt")
        mockkStatic("kotlin.io.FilesKt__FileReadWriteKt")
    }

    @Test
    fun `fetchInMemory returns data from in-memory cache`() {
        every { mockMemoryCache.get(key) } returns mockData

        val result = fileMemoryAccessObject.fetchInMemory(key)

        assertEquals(mockData, result)
    }

    @Test
    fun `fetchInMemory returns null when data not in in-memory cache`() {
        every { mockMemoryCache.get(key) } returns null

        val result = fileMemoryAccessObject.fetchInMemory(key)

        assertNull(result)
    }

    @Test
    fun `fetchInMemoryAndTransform transforms data from in-memory cache`() {
        every { mockMemoryCache.get(key) } returns mockData

        // Test transforming to ByteArray
        val resultByteArray = fileMemoryAccessObject.fetchInMemoryAndTransform(key, ToByteArray)
        assertEquals(mockData.first, resultByteArray)

        // Test transforming to File
        val resultFile = fileMemoryAccessObject.fetchInMemoryAndTransform(key, ToFile)
        assertEquals(mockData.second, resultFile)

        // Test transforming to Bitmap
        val mockkBitmap = mockk<Bitmap>()
        every { bytesToBitmap(mockData.first) } returns mockkBitmap
        val resultBitmap = fileMemoryAccessObject.fetchInMemoryAndTransform(key, ToBitmap)
        assertNotNull(resultBitmap)
        assertEquals(mockkBitmap, resultBitmap)
    }

    // fetchInMemoryAndTransform when data not in in-memory cache
    @Test
    fun `fetchInMemoryAndTransform returns null when data not in in-memory cache`() {
        every { mockMemoryCache.get(key) } returns null
        val result = fileMemoryAccessObject.fetchInMemoryAndTransform(key, ToByteArray)
        assertNull(result)
    }

    // fetchInMemoryAndTransform when bytesToBitmap returns null
    @Test
    fun `fetchInMemoryAndTransform returns null when bytesToBitmap returns null`() {
        every { mockMemoryCache.get(key) } returns mockData
        every { bytesToBitmap(mockData.first) } returns null
        val result = fileMemoryAccessObject.fetchInMemoryAndTransform(key, ToBitmap)
        assertNull(result)
    }

    // tests for fetchDiskMemoryAndTransform
    @Test
    fun `fetchDiskMemoryAndTransform transforms data from disk cache`() {
        val mockBytes = mockData.first
        val mockFile = mockData.second
        every { mockDiskCache.get(key) } returns mockFile
        every { mockFile.readBytes() } returns mockBytes
        every { mockMemoryCache.add(key, Pair(mockBytes, mockFile)) } returns true

        // Test transforming to ByteArray
        val result = fileMemoryAccessObject.fetchDiskMemoryAndTransform(key, ToByteArray)
        assertEquals(mockBytes, result)
        verify { mockMemoryCache.add(key, Pair(mockBytes, mockFile)) }

        // Test transforming to File
        val resultFile = fileMemoryAccessObject.fetchDiskMemoryAndTransform(key, ToFile)
        assertEquals(mockFile, resultFile)

        // Test transforming to Bitmap
        val mockkBitmap = mockk<Bitmap>()
        every { fileToBitmap(mockFile) } returns mockkBitmap
        val resultBitmap = fileMemoryAccessObject.fetchDiskMemoryAndTransform(key, ToBitmap)
        assertNotNull(resultBitmap)
        assertEquals(mockkBitmap, resultBitmap)
    }

    //fetchDiskMemoryAndTransform when data not in disk cache
    @Test
    fun `fetchDiskMemoryAndTransform returns null when data not in disk cache`() {
        every { mockDiskCache.get(key) } returns null
        val result = fileMemoryAccessObject.fetchDiskMemoryAndTransform(
            key,
            ToByteArray
        )
        assertNull(result)
    }

    //fetchDiskMemoryAndTransform when fileToBitmap returns null
    @Test
    fun `fetchDiskMemoryAndTransform returns null when fileToBitmap returns null`() {
        val mockBytes = mockData.first
        val mockFile = mockData.second
        every { mockDiskCache.get(key) } returns mockFile
        every { mockFile.readBytes() } returns mockBytes
        every { mockMemoryCache.add(key, Pair(mockBytes, mockFile)) } returns true
        every { fileToBitmap(mockFile) } returns null
        val result = fileMemoryAccessObject.fetchDiskMemoryAndTransform(
            key,
            ToBitmap
        )
        verify { mockMemoryCache.add(key, Pair(mockBytes, mockFile)) }
        assertNull(result)
    }


    // tests for fetchDiskMemory
    @Test
    fun `fetchDiskMemory returns data from disk cache`() {
        val mockFile = mockData.second
        every { mockDiskCache.get(key) } returns mockFile
        val result = fileMemoryAccessObject.fetchDiskMemory(key)
        assertEquals(mockFile, result)

    }

    @Test
    fun `fetchDiskMemory returns null when data not in disk cache`() {
        every { mockDiskCache.get(key) } returns null
        val result = fileMemoryAccessObject.fetchDiskMemory(key)
        assertNull(result)
    }

    // test for removeDiskMemory
    @Test
    fun `removeDiskMemory removes data from disk cache`() {
        every { mockDiskCache.remove(key) } returns true
        val result = fileMemoryAccessObject.removeDiskMemory(key)
        assertTrue(result)
        verify { mockDiskCache.remove(key) }
    }

    // test for removeDiskMemory when data not in disk cache
    @Test
    fun `removeDiskMemory returns false when data not in disk cache`() {
        every { mockDiskCache.remove(key) } returns false
        val result = fileMemoryAccessObject.removeDiskMemory(key)
        assertFalse(result)
        verify { mockDiskCache.remove(key) }
    }

    //test for removeInMemory
    @Test
    fun `removeInMemory removes data from in-memory cache`() {
        every { mockMemoryCache.remove(key) } returns mockData
        val result = fileMemoryAccessObject.removeInMemory(key)
        assertEquals(mockData, result)
        verify { mockMemoryCache.remove(key) }
    }

    //test for removeInMemory when data not in in-memory cache
    @Test
    fun `removeInMemory returns null when data not in in-memory cache`() {
        every { mockMemoryCache.remove(key) } returns null
        val result = fileMemoryAccessObject.removeInMemory(key)
        assertNull(result)
        verify { mockMemoryCache.remove(key) }
    }


    // test for saveDiskMemory
    @Test
    fun `saveDiskMemory saves data to disk cache`() {
        val mockBytes = mockData.first
        val mockFile = mockData.second
        every { mockDiskCache.addAndReturnFileInstance(key, mockBytes) } returns mockFile
        val result = fileMemoryAccessObject.saveDiskMemory(key, mockBytes)
        assertEquals(mockFile, result)
        verify { mockDiskCache.addAndReturnFileInstance(key, mockBytes) }
    }

    // test for saveInMemory
    @Test
    fun `saveInMemory saves data to in-memory cache`() {
        every { mockMemoryCache.add(key, mockData) } returns true

        val result = fileMemoryAccessObject.saveInMemory(key, mockData)

        assertTrue(result)
        verify { mockMemoryCache.add(key, mockData) }
    }

}