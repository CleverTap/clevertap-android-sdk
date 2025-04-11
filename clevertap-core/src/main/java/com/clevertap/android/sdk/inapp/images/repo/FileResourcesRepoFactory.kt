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
/**
 * A factory class for creating [FileResourcesRepoImpl] instances.
 *
 * This factory provides a way to create [FileResourcesRepoImpl] objects with different strategies for file cleanup and preloading,
 * based on the `USE_COROUTINES` flag.
 */
internal class FileResourcesRepoFactory {
    companion object {
        /**
         * Flag indicating whether to use Kotlin Coroutines for file cleanup and preloading or Executors.
         */
        private const val USE_COROUTINES = true
        /**
         * Creates a [FileResourcesRepoImpl] instance with the appropriate cleanup and preloading strategies.
         *
         * @param context The application context.
         * @param logger The logger for logging events.
         * @param storeRegistry The registry for accessing various stores.
         *
         * @return A new instance of [FileResourcesRepoImpl].
         */
        @JvmStatic
        fun createFileResourcesRepo(
            context: Context,
            logger: Logger,
            storeRegistry: StoreRegistry
        ): FileResourcesRepoImpl {

            val inAppAssetStore = storeRegistry.inAppAssetsStore
            val fileStore = storeRegistry.filesStore
            val legacyInAppStore = storeRegistry.legacyInAppStore

            val cleanupStrategy: FileCleanupStrategy
            val preloadStrategy: FilePreloaderStrategy

            if (USE_COROUTINES) {
                cleanupStrategy = FileCleanupStrategyCoroutine(
                    { FileResourceProvider.getInstance(context, logger) },
                )
                preloadStrategy = FilePreloaderCoroutine(
                    { FileResourceProvider.getInstance(context, logger) },
                    logger = logger
                )
            } else {
                cleanupStrategy = FileCleanupStrategyExecutors(
                    { FileResourceProvider.getInstance(context, logger) },
                )
                preloadStrategy = FilePreloaderExecutors(
                    { FileResourceProvider.getInstance(context, logger) },
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