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

    override fun preloadImages(urls: List<String>, successBlock: (url: String) -> Unit) {

        for (url in urls) {
            val task = executor.ioTaskNonUi<Unit>()

            task.execute("tag") {
                val bitmap = inAppImageProvider.fetchInAppImage(url)
                if (bitmap != null) {
                    successBlock.invoke(url)
                }
            }
        }
    }

    override fun cleanup() {
        //executor?.shutdown
    }
}