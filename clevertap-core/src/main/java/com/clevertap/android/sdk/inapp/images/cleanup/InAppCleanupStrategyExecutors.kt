package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import com.clevertap.android.sdk.task.CTExecutors

internal class InAppCleanupStrategyExecutors(
    override val inAppResourceProvider: InAppResourceProvider,
    private val executor: CTExecutors
) : InAppCleanupStrategy {

    companion object {
        private const val TAG = "InAppCleanupStrategyExecutors"
    }
    override fun clearAssets(urls: List<String>) {

        for (url in urls) {
            val task = executor.ioTaskNonUi<Unit>()

            task.execute(TAG) {
                inAppResourceProvider.deleteImage(url)
                inAppResourceProvider.deleteGif(url)
            }
        }
    }

    override fun stop() {
        // executor.stop()
    }
}