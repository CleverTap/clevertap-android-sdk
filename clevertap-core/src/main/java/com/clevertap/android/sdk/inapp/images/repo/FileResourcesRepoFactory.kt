package com.clevertap.android.sdk.inapp.images.repo

import android.content.Context
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategyCoroutine
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategyExecutors
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderCoroutine
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderExecutors
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry

internal class FileResourcesRepoFactory {
    companion object {

        private const val USE_COROUTINES = true

        @JvmStatic
        fun createFileResourcesRepo(
            context: Context,
            logger: Logger,
            storeRegistry: StoreRegistry
        ): FileResourcesRepoImpl {

            val inAppAssetStore = storeRegistry.inAppAssetsStore
            val fileStore = storeRegistry.filesStore
            val legacyInAppStore = storeRegistry.legacyInAppStore

            val fileResourceProvider = FileResourceProvider(
                context = context,
                logger = logger
            )

            val cleanupStrategy: FileCleanupStrategy
            val preloadStrategy: FilePreloaderStrategy

            if (USE_COROUTINES) {
                cleanupStrategy = FileCleanupStrategyCoroutine(
                    fileResourceProvider = fileResourceProvider
                )
                preloadStrategy = FilePreloaderCoroutine(
                    fileResourceProvider = fileResourceProvider,
                    logger = logger
                )
            } else {
                cleanupStrategy = FileCleanupStrategyExecutors(
                    fileResourceProvider = fileResourceProvider
                )
                preloadStrategy = FilePreloaderExecutors(
                    fileResourceProvider = fileResourceProvider,
                    logger = logger
                )
            }

            return FileResourcesRepoImpl(
                cleanupStrategy = cleanupStrategy,
                preloaderStrategy = preloadStrategy,
                inAppAssetsStore = inAppAssetStore,
                fileStore = fileStore,
                legacyInAppsStore = legacyInAppStore
            )
        }
    }
}