package com.clevertap.android.sdk.inapp.images

import android.graphics.Bitmap
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.utils.CTCaches
import com.clevertap.android.sdk.utils.FileCache
import com.clevertap.android.sdk.utils.LruCache
import io.mockk.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Ignore
class InAppResourceProviderTest {

    private val mockCache = Mockito.mock(CTCaches::class.java)
    private val mockBitmapPair = Pair(Mockito.mock(Bitmap::class.java), mockk<File>())
    private val mockBytesPair = Pair(Mockito.mock(ByteArray::class.java), mockk<File>())

    private val mockBitmap = mockk<Bitmap>()

    private val mockImageFileCache = Mockito.mock(FileCache::class.java)
    private val mockGifFileCache = Mockito.mock(FileCache::class.java)

    private val mockLruCache = Mockito.mock(LruCache::class.java) as LruCache<Pair<Bitmap, File>>
    private val mockLruCacheGif = Mockito.mock(LruCache::class.java) as LruCache<Pair<ByteArray, File>>

    private val images = File("")
    private val gifs = File("")
    private val files = File("")

    private val bytes = byteArrayOf(0)

    private val provider = InAppResourceProvider(
        images,
        gifs,
        files,
        logger = TestLogger(),
        /*ctCaches = mockCache,
        fileToBitmap = ::fileToBitmap,
        fileToBytes = ::fileToBytes,*/
        inAppRemoteSource = TestInAppFetchApi.success(bitmap = mockk<Bitmap>(), bytes = mockk<ByteArray>())
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

        provider.saveImage(
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

        provider.saveGif(
            cacheKey = key,
            bytes = bytes
        )

        assertNotNull(mockCache.gifCache())
        assertNotNull(mockCache.gifCacheDisk())

        // verify add images called
        Mockito.verify(mockCache.gifCache()).add(key, mockBytesPair)
        Mockito.verify(mockCache.gifCacheDisk()).add(key, bytes)
    }

    @Test
    fun `image isCached and cached image returns true and bitmap if image is present in either memory or disk cache`() {

        val url = "key"
        val savedImage = Mockito.mock(File::class.java)

        Mockito.`when`(mockLruCache.get(url)).thenReturn(mockBitmapPair)
        val res1 = provider.isImageCached(url = url)
        assertEquals(true, res1)

        val op1 = provider.cachedImage(cacheKey = url)
        assertEquals(mockBitmap, op1)

        // reset image cache
        Mockito.`when`(mockLruCache.get(url)).thenReturn(null)

        // setup
        Mockito.`when`(mockImageFileCache.get(url)).thenReturn(savedImage)
        Mockito.`when`(savedImage.exists()).thenReturn(true)

        // assert
        val res2 = provider.isImageCached(url = url)
        assertEquals(true, res2)

        val op2 = provider.cachedImage(cacheKey = url)
        assertEquals(mockBitmap, op2)
    }

    @Test
    fun `gif isCached returns true if gif is present in either memory or disk cache`() {

        val url = "key"
        val savedGif = Mockito.mock(File::class.java)

        Mockito.`when`(mockLruCacheGif.get(url)).thenReturn(mockBytesPair)
        val res1 = provider.isGifCached(url = url)
        assertEquals(true, res1)

        val op1 = provider.cachedGif(cacheKey = url)
        assertEquals(bytes, op1)

        // reset gif cache
        Mockito.`when`(mockLruCacheGif.get(url)).thenReturn(null)

        val resNone = provider.isGifCached(url = url)
        assertEquals(false, resNone)

        // setup
        Mockito.`when`(mockGifFileCache.get(url)).thenReturn(savedGif)
        Mockito.`when`(savedGif.exists()).thenReturn(true)

        // assert
        val res2 = provider.isGifCached(url = url)
        assertEquals(true, res2)

        val op2 = provider.cachedGif(cacheKey = url)
        assertEquals(bytes, op2)
    }

    @Test
    fun `fetchInAppImage returns from cache when data exists in cache`() {

        // setup - warm up cache
        val url = "key"
        Mockito.`when`(mockLruCache.get(url)).thenReturn(mockBitmapPair)

        // invocation
        val bitmap = provider.fetchInAppImage(url = url, Bitmap::class.java)

        // assertions
        assertEquals(mockBitmap, bitmap)
    }

    @Test
    fun `fetchInAppImage returns from remote service (http api) when data does not exist in cache`() {

        // setup
        val url = "key"

        // invocation
        val bitmap = provider.fetchInAppImage(url = url, Bitmap::class.java)

        // assertions
        assertEquals(mockBitmap, bitmap)
    }

    @Test
    fun `fetchInAppGif returns from cache when data exists in cache`() {

        // setup - warm up cache
        val url = "key"
        Mockito.`when`(mockLruCacheGif.get(url)).thenReturn(mockBytesPair)

        // invocation
        val opBytes = provider.fetchInAppGif(url = url)

        // assertions
        assertEquals(bytes, opBytes)
    }

    @Test
    fun `fetchInAppGif returns gif from remote service when data does not exist in cache`() {

        // setup
        val url = "key"

        // invocation
        val opBytes = provider.fetchInAppGif(url = url)

        // assertions
        assertEquals(bytes, opBytes)
    }

    // TODO : test api failure cases

}