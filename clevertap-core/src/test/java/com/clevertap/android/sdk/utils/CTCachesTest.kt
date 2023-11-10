package com.clevertap.android.sdk.utils

import com.clevertap.android.sdk.TestLogger
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

class CTCachesTest {

    private val config1: CTCachesConfig = CTCachesConfig(
            minImageCacheKb = 10,
            minGifCacheKb = 10,
            optimistic = 20,
            maxImageSizeDiskKb = 2
    )

    private val config2: CTCachesConfig = CTCachesConfig(
            minImageCacheKb = 100,
            minGifCacheKb = 100,
            optimistic = 20,
            maxImageSizeDiskKb = 2
    )

    private val logger = TestLogger()

    @After
    fun after() {
        CTCaches.clear()
    }

    @Test
    fun `optimistic size is assigned when optimistic size is greater than minimum cache size`() {
        val cache = CTCaches.instance(
                config = config1,
                logger = logger
        )

        val opi = cache.imageCacheSize()
        val opg = cache.gifCacheSize()

        assertEquals(config1.optimistic.toInt(), opi)
        assertEquals(config1.optimistic.toInt(), opg)
    }

    @Test
    fun `minimum size is assigned when optimistic size is less than minimum cache size`() {
        val cache = CTCaches.instance(
                config = config2,
                logger = logger
        )

        val opi = cache.imageCacheSize()
        val opg = cache.gifCacheSize()

        assertEquals(config2.minImageCacheKb.toInt(), opi)
        assertEquals(config2.minGifCacheKb.toInt(), opg)
    }
}