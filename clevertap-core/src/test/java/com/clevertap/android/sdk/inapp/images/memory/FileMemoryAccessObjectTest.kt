import android.graphics.Bitmap
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.memory.FileMemoryAccessObject
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToBitmap
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToByteArray
import com.clevertap.android.sdk.inapp.images.memory.MemoryDataTransformationType.ToFile
import com.clevertap.android.sdk.inapp.images.memory.bytesToBitmap
import com.clevertap.android.sdk.inapp.images.memory.fileToBitmap
import com.clevertap.android.sdk.utils.CTCaches
import com.clevertap.android.sdk.utils.DiskMemory
import com.clevertap.android.sdk.utils.InMemoryLruCache
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

    private lateinit var fileMemoryAccessObject: FileMemoryAccessObject
    private val mockCTCaches = mockk<CTCaches>()
    private val mockLogger = mockk<ILogger>(relaxed = true) // Relaxed to avoid unnecessary stubbing
    private val mockMemoryCache = mockk<InMemoryLruCache<Pair<ByteArray, File>>>()
    private val mockDiskCache = mockk<DiskMemory>()

    private val key = "test_key"
    private val mockByteArray = byteArrayOf(1, 2, 3)
    private val mockFile = mockk<File>()
    private val mockBitmap = mockk<Bitmap>()
    private val mockData = Pair(mockByteArray, mockFile)

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
        assertEquals(mockData, fileMemoryAccessObject.fetchInMemory(key))
    }

    @Test
    fun `fetchInMemory returns null when data not in in-memory cache`() {
        every { mockMemoryCache.get(key) } returns null
        assertNull(fileMemoryAccessObject.fetchInMemory(key))
    }

    @Test
    fun `fetchInMemoryAndTransform transforms data from in-memory cache`() {
        every { mockMemoryCache.get(key) } returns mockData

        // Test transforming to ByteArray
        assertEquals(
            mockByteArray,
            fileMemoryAccessObject.fetchInMemoryAndTransform(key, ToByteArray)
        )

        // Test transforming to File
        assertEquals(mockFile, fileMemoryAccessObject.fetchInMemoryAndTransform(key, ToFile))

        // Test transforming to Bitmap
        every { bytesToBitmap(mockByteArray) } returns mockBitmap
        val resultBitmap = fileMemoryAccessObject.fetchInMemoryAndTransform(key, ToBitmap)
        assertNotNull(resultBitmap)
        assertEquals(mockBitmap, resultBitmap)
    }

    // fetchInMemoryAndTransform when data not in in-memory cache
    @Test
    fun `fetchInMemoryAndTransform returns null when data not in in-memory cache`() {
        every { mockMemoryCache.get(key) } returns null
        assertNull(fileMemoryAccessObject.fetchInMemoryAndTransform(key, ToByteArray))
    }

    // fetchInMemoryAndTransform when bytesToBitmap returns null
    @Test
    fun `fetchInMemoryAndTransform returns null when bytesToBitmap returns null`() {
        every { mockMemoryCache.get(key) } returns mockData
        every { bytesToBitmap(mockByteArray) } returns null
        assertNull(fileMemoryAccessObject.fetchInMemoryAndTransform(key, ToBitmap))
    }

    // tests for fetchDiskMemoryAndTransform
    @Test
    fun `fetchDiskMemoryAndTransform transforms data from disk cache`() {
        every { mockDiskCache.get(key) } returns mockFile
        every { mockFile.readBytes() } returns mockByteArray
        every { mockMemoryCache.add(key, mockData) } returns true

        // Test transforming to ByteArray
        assertEquals(
            mockByteArray,
            fileMemoryAccessObject.fetchDiskMemoryAndTransform(key, ToByteArray)
        )
        verify { mockMemoryCache.add(key, mockData) }

        // Test transforming to File
        assertEquals(mockFile, fileMemoryAccessObject.fetchDiskMemoryAndTransform(key, ToFile))

        // Test transforming to Bitmap
        every { fileToBitmap(mockFile) } returns mockBitmap
        val resultBitmap = fileMemoryAccessObject.fetchDiskMemoryAndTransform(key, ToBitmap)
        assertNotNull(resultBitmap)
        assertEquals(mockBitmap, resultBitmap)
    }

    //fetchDiskMemoryAndTransform when data not in disk cache
    @Test
    fun `fetchDiskMemoryAndTransform returns null when data not in disk cache`() {
        every { mockDiskCache.get(key) } returns null
        assertNull(fileMemoryAccessObject.fetchDiskMemoryAndTransform(key, ToByteArray)
        )
    }

    //fetchDiskMemoryAndTransform when fileToBitmap returns null
    @Test
    fun `fetchDiskMemoryAndTransform returns null when fileToBitmap returns null`() {
        every { mockDiskCache.get(key) } returns mockFile
        every { mockFile.readBytes() } returns mockByteArray
        every { mockMemoryCache.add(key, mockData) } returns true
        every { fileToBitmap(mockFile) } returns null

        assertNull(fileMemoryAccessObject.fetchDiskMemoryAndTransform(key, ToBitmap))
        verify { mockMemoryCache.add(key, Pair(mockByteArray, mockFile)) }
    }


    // tests for fetchDiskMemory
    @Test
    fun `fetchDiskMemory returns data from disk cache`() {
        every { mockDiskCache.get(key) } returns mockFile
        assertEquals(mockFile, fileMemoryAccessObject.fetchDiskMemory(key))
    }

    @Test
    fun `fetchDiskMemory returns null when data not in disk cache`() {
        every { mockDiskCache.get(key) } returns null
        assertNull(fileMemoryAccessObject.fetchDiskMemory(key))
    }

    // test for removeDiskMemory
    @Test
    fun `removeDiskMemory removes data from disk cache`() {
        every { mockDiskCache.remove(key) } returns true
        assertTrue(fileMemoryAccessObject.removeDiskMemory(key))
        verify { mockDiskCache.remove(key) }
    }

    // test for removeDiskMemory when data not in disk cache
    @Test
    fun `removeDiskMemory returns false when data not in disk cache`() {
        every { mockDiskCache.remove(key) } returns false
        assertFalse(fileMemoryAccessObject.removeDiskMemory(key))
        verify { mockDiskCache.remove(key) }
    }

    //test for removeInMemory
    @Test
    fun `removeInMemory removes data from in-memory cache`() {
        every { mockMemoryCache.remove(key) } returns mockData
        assertEquals(mockData, fileMemoryAccessObject.removeInMemory(key))
        verify { mockMemoryCache.remove(key) }
    }

    //test for removeInMemory when data not in in-memory cache
    @Test
    fun `removeInMemory returns null when data not in in-memory cache`() {
        every { mockMemoryCache.remove(key) } returns null
        assertNull(fileMemoryAccessObject.removeInMemory(key))
        verify { mockMemoryCache.remove(key) }
    }


    // test for saveDiskMemory
    @Test
    fun `saveDiskMemory saves data to disk cache`() {
        every { mockDiskCache.addAndReturnFileInstance(key, mockByteArray) } returns mockFile
        assertEquals(mockFile, fileMemoryAccessObject.saveDiskMemory(key, mockByteArray))
        verify { mockDiskCache.addAndReturnFileInstance(key, mockByteArray) }
    }

    // test for saveInMemory
    @Test
    fun `saveInMemory saves data to in-memory cache`() {
        every { mockMemoryCache.add(key, mockData) } returns true
        assertTrue(fileMemoryAccessObject.saveInMemory(key, mockData))
        verify { mockMemoryCache.add(key, mockData) }
    }

}