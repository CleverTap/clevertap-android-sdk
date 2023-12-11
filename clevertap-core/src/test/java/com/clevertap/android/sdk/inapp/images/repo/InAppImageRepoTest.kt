package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.images.cleanup.InAppCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.InAppImagePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
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
}