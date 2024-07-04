package com.clevertap.android.sdk.inapp.images.repo

import android.content.Context
import androidx.annotation.NonNull
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategyCoroutine
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderCoroutine
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry

internal class FileResourcesRepoFactory {
    companion object {

        @JvmStatic
        @NonNull
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
            val cleanupStrategy: FileCleanupStrategy = FileCleanupStrategyCoroutine(
                fileResourceProvider = fileResourceProvider
            )
            val preloadStrategy: FilePreloaderStrategy = FilePreloaderCoroutine(
                fileResourceProvider = fileResourceProvider,
                logger = logger
            )

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