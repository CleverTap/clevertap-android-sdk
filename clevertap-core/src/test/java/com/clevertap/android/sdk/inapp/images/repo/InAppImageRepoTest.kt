package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.images.cleanup.InAppCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.InAppImagePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class InAppImageRepoTest {

    private val inAppImageCleanupStrategy = mockk<InAppCleanupStrategy>(relaxed = true)
    private val preloaderStrategy = mockk<InAppImagePreloaderStrategy>(relaxed = true)
    private val inAppAssetStore = mockk<InAppAssetsStore>(relaxed = true)

    private val inAppImageRepoImpl = InAppImageRepoImpl(
        cleanupStrategy = inAppImageCleanupStrategy,
        preloaderStrategy = preloaderStrategy,
        inAppAssetsStore = inAppAssetStore
    )

    @Test
    fun `fetch all images use case`() {
        val urls = listOf("url1", "url2", "url3")
        inAppImageRepoImpl.fetchAllImages(urls)

        verify {
            preloaderStrategy.preloadImages(urls, any())
        }
    }

    @Test
    fun `cleanup all images use case`() {

        // urls from api call
        val responseUrls = listOf("url1", "url2", "url3")

        // urls in asset store
        val expiredUrls = setOf("url_expired1", "url_expired2")
        val validUrls = setOf("url1", "url2", "url3", "url_valid_not_in_response")
        val allUrls = expiredUrls + validUrls

        val fourteenDaysFromNow = System.currentTimeMillis() + InAppImageRepoImpl.EXPIRY_OFFSET_MILLIS
        val oneDayAgo = System.currentTimeMillis() - InAppImageRepoImpl.DAY_IN_MILLIS

        // setup asset store
        every { inAppAssetStore.getAllAssetUrls() } returns allUrls
        every { inAppAssetStore.expiryForUrl(any()) } returns fourteenDaysFromNow
        expiredUrls.forEach { url ->
            every { inAppAssetStore.expiryForUrl(url) } returns oneDayAgo
        }

        // invoke method to test
        inAppImageRepoImpl.cleanupStaleImages(responseUrls)

        // assert
        verify { inAppImageCleanupStrategy.clearAssets(expiredUrls.toList(), any()) }
    }
}