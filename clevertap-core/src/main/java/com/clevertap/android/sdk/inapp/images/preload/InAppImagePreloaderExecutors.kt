package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider
import com.clevertap.android.sdk.task.CTExecutors

internal class InAppImagePreloaderExecutors @JvmOverloads constructor(
        private val executor: CTExecutors,
        override val inAppImageProvider: InAppResourceProvider,
        override val logger: ILogger? = null,
        override val config: InAppImagePreloadConfig = InAppImagePreloadConfig.default()
) : InAppImagePreloaderStrategy {

    override fun preloadImages(urls: List<String>) {

        for (url in urls) {
            val task = executor.ioTaskNonUi<Void>()

            task.execute("tag") {
                inAppImageProvider.fetchInAppImage(url)
                null
            }
        }
    }

    override fun cleanup() {
        //executor?.shutdown
    }
}