package com.clevertap.android.sdk.inapp.images.repo

import android.content.Context
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategyExecutors
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderExecutors
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry

internal class FileResourcesRepoFactory {
    companion object {

        @JvmStatic
        fun createFileResourcesRepo(
            context: Context,
            logger: Logger,
            storeRegistry: StoreRegistry
        ): FileResourcesRepoImpl? {

            val inAppAssetStore = storeRegistry.inAppAssetsStore
            val fileStore = storeRegistry.filesStore
            val legacyInAppStore = storeRegistry.legacyInAppStore

            if (inAppAssetStore == null || legacyInAppStore == null || fileStore == null) {
                return null
            }

            val inAppResourceProvider = InAppResourceProvider(context, logger)
            val cleanupStrategy: FileCleanupStrategy = FileCleanupStrategyExecutors(inAppResourceProvider)
            val preloadStrategy: FilePreloaderStrategy = FilePreloaderExecutors(
                inAppResourceProvider,
                logger
            )

            return FileResourcesRepoImpl(
                cleanupStrategy,
                preloadStrategy,
                inAppAssetStore,
                fileStore,
                legacyInAppStore
            )
        }
    }
}