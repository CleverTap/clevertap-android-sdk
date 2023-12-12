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
    override fun clearAssets(urls: List<String>, successBlock: (url: String) -> Unit) {

        for (url in urls) {
            val task = executor.ioTaskNonUi<Unit>()

            task.execute(TAG) {
                inAppResourceProvider.deleteImage(url)
                inAppResourceProvider.deleteGif(url)
                successBlock.invoke(url)
            }
        }
    }

    override fun stop() {
        // executor.stop()
    }
}