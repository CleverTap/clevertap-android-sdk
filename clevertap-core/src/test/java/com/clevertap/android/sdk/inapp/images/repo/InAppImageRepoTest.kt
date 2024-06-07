package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.FileStore
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class InAppImageRepoTest {

    private val inAppImageCleanupStrategy = mockk<FileCleanupStrategy>(relaxed = true)
    private val preloaderStrategy = mockk<FilePreloaderStrategy>(relaxed = true)
    private val inAppAssetStore = mockk<InAppAssetsStore>(relaxed = true)
    private val legacyInAppStore = mockk<LegacyInAppStore>(relaxed = true)
    private val fileStore = mockk<FileStore>(relaxed = true)

    private val mFileResourcesRepoImpl = FileResourcesRepoImpl(
        cleanupStrategy = inAppImageCleanupStrategy,
        preloaderStrategy = preloaderStrategy,
        inAppAssetsStore = inAppAssetStore,
        fileStore = fileStore,
        legacyInAppsStore = legacyInAppStore
    )

    @Test
    fun `fetch all images use case`() {
        val urls = listOf("url1", "url2", "url3")
        mFileResourcesRepoImpl.fetchAllInAppImagesV1(urls)

        verify {
            preloaderStrategy.preloadInAppImagesV1(urls, any())
        }
    }

    @Test
    fun `fetch all gifs use case`() {
        val urls = listOf("url1", "url2", "url3")
        mFileResourcesRepoImpl.fetchAllInAppGifsV1(urls)

        verify {
            preloaderStrategy.preloadInAppGifsV1(urls, any())
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

        val fourteenDaysFromNow = System.currentTimeMillis() + FileResourcesRepoImpl.EXPIRY_OFFSET_MILLIS
        val oneDayAgo = System.currentTimeMillis() - FileResourcesRepoImpl.DAY_IN_MILLIS

        // setup asset store
        every { inAppAssetStore.getAllAssetUrls() } returns allUrls
        every { inAppAssetStore.expiryForUrl(any()) } returns fourteenDaysFromNow
        expiredUrls.forEach { url ->
            every { inAppAssetStore.expiryForUrl(url) } returns oneDayAgo
        }

        // invoke method to test
        mFileResourcesRepoImpl.cleanupStaleInAppImagesAndGifsV1(responseUrls)

        // assert
        verify { inAppImageCleanupStrategy.clearInAppImagesAndGifsV1(expiredUrls.toList(), any()) }
        verify { legacyInAppStore.updateAssetCleanupTs(any()) }
    }
}