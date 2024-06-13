package com.clevertap.android.sdk.inapp.images

import android.graphics.Bitmap
import com.clevertap.android.sdk.TestLogger
import io.mockk.*
import org.junit.Test
import java.io.File

class FileResourceProviderTest {
/*
    private val mockCache = Mockito.mock(CTCaches::class.java)
    private val mockBitmapPair = Pair(Mockito.mock(Bitmap::class.java), mockk<File>())
    private val mockBytesPair = Pair(mockk<ByteArray>(), mockk<File>())

    private val mockBitmap = mockk<Bitmap>()

    private val mockImageFileCache = Mockito.mock(FileCache::class.java)
    private val mockGifFileCache = Mockito.mock(FileCache::class.java)

    private val mockLruCache = Mockito.mock(LruCache::class.java) as LruCache<Pair<Bitmap, File>>
    private val mockLruCacheGif = Mockito.mock(LruCache::class.java) as LruCache<Pair<ByteArray, File>>*/

    private val images = File("")
    private val gifs = File("")
    private val files = File("")

    private val bytes = byteArrayOf(0)

    private val provider = FileResourceProvider(
        images,
        gifs,
        files,
        logger = TestLogger(),
        /*ctCaches = mockCache,
        fileToBitmap = ::fileToBitmap,
        fileToBytes = ::fileToBytes,*/
        inAppRemoteSource = TestInAppFetchApi.success(bitmap = mockk<Bitmap>(), bytes = bytes)
    )

/*    private fun fileToBitmap(file: File?) : Bitmap? {
        return if (file == null) {
            null
        } else {
            mockBitmap
        }
    }

    private fun fileToBytes(file: File?) : ByteArray? {
        return if (file == null) {
            null
        } else {
            bytes // we return random array
        }
    }*/
/*
    @Before
    fun before() {
        Mockito.`when`(mockCache.imageCache()).thenReturn(mockLruCache)
        Mockito.`when`(mockCache.imageCacheDisk()).thenReturn(mockImageFileCache)

        Mockito.`when`(mockCache.gifCache()).thenReturn(mockLruCacheGif)
        Mockito.`when`(mockCache.gifCacheDisk()).thenReturn(mockGifFileCache)
    }

    @Test
    fun `save image dumps image in memory and disk cache`() {

        val key = "key"

        provider.saveInAppImageV1(
            cacheKey = key,
            bitmap = mockBitmap,
            bytes = bytes
        )

        assertNotNull(mockCache.imageCache())
        assertNotNull(mockCache.imageCacheDisk())

        // verify add images called
        Mockito.verify(mockCache.imageCache()).add(key, mockBitmapPair)
        Mockito.verify(mockCache.imageCacheDisk()).add(key, bytes)
    }

    @Test
    fun `save gif dumps gif in memory and disk cache`() {

        val key = "key"

        provider.saveInAppGifV1(
            cacheKey = key,
            bytes = bytes
        )

        assertNotNull(mockCache.gifCache())
        assertNotNull(mockCache.gifCacheDisk())

        // verify add images called
        Mockito.verify(mockCache.gifCache()).add(key, mockBytesPair)
        Mockito.verify(mockCache.gifCacheDisk()).add(key, bytes)
    }*/

    @Test
    fun fetchCachedDataTest(){
        /*val inAppGifMemoryAccessObjectV1 = mockk<InAppGifMemoryAccessObjectV1>()
        val fileMemoryAccessObject = mockk<FileMemoryAccessObject>()
        val inAppImageMemoryAccessObjectV1 = mockk<InAppImageMemoryAccessObjectV1>()

        provider.fileMAO = fileMemoryAccessObject
        provider.imageMAO = inAppImageMemoryAccessObjectV1
        provider.gifMAO = inAppGifMemoryAccessObjectV1
        provider.mapOfMAO =
            mapOf<CtCacheType, List<MemoryAccessObject<*>>>(
                CtCacheType.IMAGE to listOf(provider.imageMAO, provider.fileMAO, provider.gifMAO),
                CtCacheType.GIF to listOf(provider.gifMAO, provider.fileMAO, provider.imageMAO),
                CtCacheType.FILES to listOf(provider.fileMAO, provider.imageMAO, provider.gifMAO)
            )

        val expectedData = byteArrayOf(1, 2, 3)
        every { fileMemoryAccessObject.fetchInMemoryAndTransform("abc", MT.ToByteArray) } returns null
        every { inAppImageMemoryAccessObjectV1.fetchInMemoryAndTransform("abc", MT.ToByteArray) } returns null
        every { inAppGifMemoryAccessObjectV1.fetchInMemoryAndTransform("abc", MT.ToByteArray) } returns null
        every { fileMemoryAccessObject.fetchDiskMemoryAndTransform("abc", MT.ToByteArray) } returns null
        every { inAppImageMemoryAccessObjectV1.fetchDiskMemoryAndTransform("abc", MT.ToByteArray) } returns null
        every { inAppGifMemoryAccessObjectV1.fetchDiskMemoryAndTransform("abc", MT.ToByteArray) } returns expectedData


        val result = provider.fetchCachedData(Pair("abc",GIF), MT.ToByteArray)*/
        //assertArrayEquals(expectedData, result)
    }

    @Test
    fun fetchCachedDataTest1(){
       /*
        val mockCTCaches = mockk<CTCaches>()
        val expectedData = byteArrayOf(1, 2, 3)
        //val result = provider.fetchCachedData(Pair("abc",GIF), MT.ToByteArray)
        val f = FileMemoryAccessObject(mockCTCaches)
        val lru = mockk<LruCache<Pair<ByteArray, File>>>()
        every { mockCTCaches.fileLruCache() } returns  lru
        every { lru.get("abc") } returns Pair(expectedData, mock<File>())

        val r = f.fetchInMemoryAndTransform("abc",MT.ToFile)*/
        //assertArrayEquals(expectedData, result)
    }
/*
    @Test
    fun `image isCached and cached image returns true and bitmap if image is present in either memory or disk cache`() {

        val url = "key"
        val savedImage = Mockito.mock(File::class.java)

        Mockito.`when`(mockLruCache.get(url)).thenReturn(mockBitmapPair)
        val res1 = provider.isInAppImageCachedV1(url = url)
        assertEquals(true, res1)

        val op1 = provider.cachedInAppImageV1(cacheKey = url)
        assertEquals(mockBitmap, op1)

        // reset image cache
        Mockito.`when`(mockLruCache.get(url)).thenReturn(null)

        // setup
        Mockito.`when`(mockImageFileCache.get(url)).thenReturn(savedImage)
        Mockito.`when`(savedImage.exists()).thenReturn(true)

        // assert
        val res2 = provider.isInAppImageCachedV1(url = url)
        assertEquals(true, res2)

        val op2 = provider.cachedInAppImageV1(cacheKey = url)
        assertEquals(mockBitmap, op2)
    }

    @Test
    fun `gif isCached returns true if gif is present in either memory or disk cache`() {

        val url = "key"
        val savedGif = Mockito.mock(File::class.java)

        Mockito.`when`(mockLruCacheGif.get(url)).thenReturn(mockBytesPair)
        val res1 = provider.isInAppGifCachedV1(url = url)
        assertEquals(true, res1)

        val op1 = provider.cachedInAppGifV1(cacheKey = url)
        assertEquals(bytes, op1)

        // reset gif cache
        Mockito.`when`(mockLruCacheGif.get(url)).thenReturn(null)

        val resNone = provider.isInAppGifCachedV1(url = url)
        assertEquals(false, resNone)

        // setup
        Mockito.`when`(mockGifFileCache.get(url)).thenReturn(savedGif)
        Mockito.`when`(savedGif.exists()).thenReturn(true)

        // assert
        val res2 = provider.isInAppGifCachedV1(url = url)
        assertEquals(true, res2)

        val op2 = provider.cachedInAppGifV1(cacheKey = url)
        assertEquals(bytes, op2)
    }*/
/*
    @Test
    fun `fetchInAppImage returns from cache when data exists in cache`() {

        // setup - warm up cache
        val url = "key"
        Mockito.`when`(mockLruCache.get(url)).thenReturn(mockBitmapPair)

        // invocation
        val bitmap = provider.fetchInAppImageV1(url = url)

        // assertions
        assertEquals(mockBitmap, bitmap)
    }

    @Test
    fun `fetchInAppImage returns from remote service (http api) when data does not exist in cache`() {

        // setup
        val url = "key"

        // invocation
        val bitmap = provider.fetchInAppImageV1(url = url)

        // assertions
        assertEquals(mockBitmap, bitmap)
    }

    @Test
    fun `fetchInAppGif returns from cache when data exists in cache`() {

        // setup - warm up cache
        val url = "key"
        Mockito.`when`(mockLruCacheGif.get(url)).thenReturn(mockBytesPair)

        // invocation
        val opBytes = provider.fetchInAppGifV1(url = url)

        // assertions
        assertEquals(bytes, opBytes)
    }

    @Test
    fun `fetchInAppGif returns gif from remote service when data does not exist in cache`() {

        // setup
        val url = "key"

        // invocation
        val opBytes = provider.fetchInAppGifV1(url = url)

        // assertions
        assertEquals(bytes, opBytes)
    }*/

    // TODO : test api failure cases

}