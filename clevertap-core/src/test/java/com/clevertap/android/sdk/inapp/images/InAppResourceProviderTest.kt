package com.clevertap.android.sdk.inapp.images

import android.graphics.Bitmap
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.utils.CTCaches
import com.clevertap.android.sdk.utils.FileCache
import com.clevertap.android.sdk.utils.LruCache
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InAppResourceProviderTest {

    private val mockCache = Mockito.mock(CTCaches::class.java)
    private val mockBitmap = Mockito.mock(Bitmap::class.java)

    private val mockImageFileCache = Mockito.mock(FileCache::class.java)
    private val mockGifFileCache = Mockito.mock(FileCache::class.java)

    private val mockLruCache = Mockito.mock(LruCache::class.java) as LruCache<Bitmap>
    private val mockLruCacheGif = Mockito.mock(LruCache::class.java) as LruCache<ByteArray>

    private val images = File("")
    private val gifs = File("")

    private val bytes = byteArrayOf(0)

    private val provider = InAppResourceProvider(
            images = images,
            gifs = gifs,
            logger = TestLogger(),
            ctCaches = mockCache,
            fileToBitmap = ::fileToBitmap,
            fileToBytes = ::fileToBytes
    )

    private fun fileToBitmap(file: File?) : Bitmap? {
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
    }

    @Before
    fun before() {
        Mockito.`when`(mockCache.imageCache()).thenReturn(mockLruCache)
        Mockito.`when`(mockCache.imageCacheDisk(images)).thenReturn(mockImageFileCache)

        Mockito.`when`(mockCache.gifCache()).thenReturn(mockLruCacheGif)
        Mockito.`when`(mockCache.gifCacheDisk(images)).thenReturn(mockGifFileCache)
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
        assertNotNull(mockCache.imageCacheDisk(gifs))

        // verify add images called
        Mockito.verify(mockCache.imageCache()).add(key, mockBitmap)
        Mockito.verify(mockCache.imageCacheDisk(images)).add(key, bytes)
    }

    @Test
    fun `save gif dumps gif in memory and disk cache`() {

        val key = "key"

        provider.saveGif(
            cacheKey = key,
            bytes = bytes
        )

        assertNotNull(mockCache.gifCache())
        assertNotNull(mockCache.gifCacheDisk(gifs))

        // verify add images called
        Mockito.verify(mockCache.gifCache()).add(key, bytes)
        Mockito.verify(mockCache.gifCacheDisk(images)).add(key, bytes)
    }

    @Test
    fun `image isCached and cached image returns true and bitmap if image is present in either memory or disk cache`() {

        val url = "key"
        val savedImage = Mockito.mock(File::class.java)

        Mockito.`when`(mockLruCache.get(url)).thenReturn(mockBitmap)
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

        Mockito.`when`(mockLruCacheGif.get(url)).thenReturn(bytes)
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
    fun `cachedImage returns `() {

    }

}