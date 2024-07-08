package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.FileStore
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.days

class FileResourcesRepoTest {

    private val fileCleanupStrategy = mockk<FileCleanupStrategy>(relaxed = true)
    private val preloaderStrategy = mockk<FilePreloaderStrategy>(relaxed = true)
    private val inAppAssetStore = mockk<InAppAssetsStore>(relaxed = true)
    private val legacyInAppStore = mockk<LegacyInAppStore>(relaxed = true)
    private val fileStore = mockk<FileStore>(relaxed = true)

    private val mFileResourcesRepoImpl = FileResourcesRepoImpl(
        cleanupStrategy = fileCleanupStrategy,
        preloaderStrategy = preloaderStrategy,
        inAppAssetsStore = inAppAssetStore,
        fileStore = fileStore,
        legacyInAppsStore = legacyInAppStore
    )

    @Test
    fun `fetch all images use case`() {
        val urls = listOf("url1", "url2", "url3")
            .map { Pair(it, CtCacheType.IMAGE) }
        mFileResourcesRepoImpl.preloadFilesAndCache(urls)

        verify {
            preloaderStrategy.preloadFilesAndCache(urls, any(),any(),any(),any())
        }
    }

    @Test
    fun `fetch all gifs use case`() {
        val urls = listOf("url1", "url2", "url3")
            .map { Pair(it, CtCacheType.GIF) }
        mFileResourcesRepoImpl.preloadFilesAndCache(urls)

        verify {
            preloaderStrategy.preloadFilesAndCache(urls, any(),any(),any(),any())
        }
    }

    @Test
    fun `preloadFilesAndCache invokes all callbacks and updates download state`() {
        val urlsImage = listOf("url1", "url2", "url3").map { Pair(it, CtCacheType.IMAGE) }
        val urlsGif = listOf(Pair("url4", CtCacheType.GIF))
        val urlsFile = listOf(Pair("url5", CtCacheType.FILES))

        val urls = urlsImage + urlsGif + urlsFile

        val sUrls = urls.filterIndexed { index, _ -> index != 1 }
        val expectedStatusMap = sUrls.associate { it.first to true } + Pair(urls[1].first, false)
        val successUrls = mutableListOf<Pair<String, CtCacheType>>()
        val failureUrls = mutableListOf<Pair<String, CtCacheType>>()
        val finishedStatus = mutableMapOf<String, Boolean>()

        // Mock preloaderStrategy to simulate success for url1 and url3, failure for url2
        every {
            preloaderStrategy.preloadFilesAndCache(
                urls,captureLambda(),
                captureLambda(),
                any(),
                captureLambda()
            )
        } answers {
            val successCaptor = secondArg<(Pair<String, CtCacheType>) -> Unit>()
            val failureCaptor = arg<(Pair<String, CtCacheType>) -> Unit>(2)
            val finishedCaptor = lastArg<(Map<String, Boolean>) -> Unit>()

            val map = mutableMapOf<String, Boolean>()
            urls.filterIndexed { index, _ -> index != 1 }.forEach {
                successCaptor.invoke(it)
                map[it.first] = true
            }
            failureCaptor.invoke(urls[1])
            map[urls[1].first] = false
            finishedCaptor.invoke(map)
        }

        mFileResourcesRepoImpl.preloadFilesAndCache(
            urls,
            completionCallback = { status -> finishedStatus.putAll(status) },
            successBlock = { url -> successUrls.add(url) },
            failureBlock = { url -> failureUrls.add(url) }
        )

        // Verify callbacks and download state
        assertEquals(sUrls, successUrls)
        assertEquals(listOf(urls[1]), failureUrls)
        assertEquals(expectedStatusMap, finishedStatus)

        // Verify saving of expiry to store
        successUrls.forEach {
            when(it.second)
            {
                CtCacheType.IMAGE,
                CtCacheType.GIF -> verify {
                    inAppAssetStore.saveAssetUrl(it.first, any<Long>())
                    fileStore.saveFileUrl(it.first, any<Long>())
                }
                CtCacheType.FILES -> verify { fileStore.saveFileUrl(it.first,any<Long>()) }
            }

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

        val fourteenDaysFromNow =  System.currentTimeMillis() + 14.days.inWholeMilliseconds
        val oneDayAgo = System.currentTimeMillis() - 1.days.inWholeMilliseconds

        // setup asset store
        every { inAppAssetStore.getAllAssetUrls() } returns allUrls
        every { inAppAssetStore.expiryForUrl(any()) } returns fourteenDaysFromNow
        expiredUrls.forEach { url ->
            every { inAppAssetStore.expiryForUrl(url) } returns oneDayAgo
        }

        // invoke method to test
        mFileResourcesRepoImpl.cleanupStaleFiles(responseUrls)

        // assert
        verify { fileCleanupStrategy.clearFileAssets(expiredUrls.toList(), any()) }
        verify { legacyInAppStore.updateAssetCleanupTs(any()) }
    }

    @Test
    fun `cleanupStaleFiles skips cleanup if not enough time has passed`() {
        val currentTime = System.currentTimeMillis()
        every { legacyInAppStore.lastCleanupTs() } returns currentTime - 13.days.inWholeMilliseconds // Cleanup not needed yet

        mFileResourcesRepoImpl.cleanupStaleFiles(emptyList())

        verify(exactly = 0) { fileCleanupStrategy.clearFileAssets(any(), any()) }
        verify(exactly = 0) { legacyInAppStore.updateAssetCleanupTs(any()) }
    }

    @Test
    fun `cleanupExpiredResources clears expired resources for each type`() {
        val expiredAssetsUrls = setOf("img_expired1", "img_expired2","gif_expired1")
        val expiredFileUrls = setOf("file_expired1", "file_expired2")

        every { inAppAssetStore.getAllAssetUrls() } returns expiredAssetsUrls
        every { fileStore.getAllFileUrls() } returns expiredFileUrls

        mFileResourcesRepoImpl.cleanupExpiredResources(CtCacheType.IMAGE)
        verify { fileCleanupStrategy.clearFileAssets(expiredAssetsUrls.toList(), any()) }

        mFileResourcesRepoImpl.cleanupExpiredResources(CtCacheType.GIF)
        verify { fileCleanupStrategy.clearFileAssets(expiredAssetsUrls.toList(), any()) }

        mFileResourcesRepoImpl.cleanupExpiredResources(CtCacheType.FILES)
        verify { fileCleanupStrategy.clearFileAssets((expiredFileUrls + expiredAssetsUrls).toList(), any()) }
    }

    @Test
    fun `cleanupAllResources clears all resources for each type`() {
        val allImageUrls = setOf("img1", "img2")
        val allGifUrls = setOf("gif1")
        val allFileUrls = setOf("file1", "file2")
        val assetUrls = allImageUrls + allGifUrls

        every { inAppAssetStore.getAllAssetUrls() } returns assetUrls
        every { fileStore.getAllFileUrls() } returns allFileUrls

        mFileResourcesRepoImpl.cleanupAllResources(CtCacheType.IMAGE)
        verify { fileCleanupStrategy.clearFileAssets(assetUrls.toList(), any()) }

        mFileResourcesRepoImpl.cleanupAllResources(CtCacheType.GIF)
        verify { fileCleanupStrategy.clearFileAssets(assetUrls.toList(), any()) }

        mFileResourcesRepoImpl.cleanupAllResources(CtCacheType.FILES)
        verify { fileCleanupStrategy.clearFileAssets((allFileUrls + assetUrls).toList(), any()) }
    }
}