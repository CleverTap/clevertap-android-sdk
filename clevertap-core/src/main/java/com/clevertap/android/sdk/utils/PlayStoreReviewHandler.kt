package com.clevertap.android.sdk.utils

import android.content.Context
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.Logger
import com.google.android.play.core.review.ReviewManagerFactory

internal class PlayStoreReviewHandler {

    private val reviewManagerFactoryClass: Class<*>? by lazy {
        try {
            Class.forName("com.google.android.play.core.review.ReviewManagerFactory")
        } catch (_: ClassNotFoundException) {
            null
        }
    }

    fun isPlayStoreReviewLibraryAvailable(): Boolean {
        return reviewManagerFactoryClass != null
    }

    fun launchReview(
        context: Context,
        logger: Logger,
        onCompleted: () -> Unit,
        onError: (Exception?) -> Unit
    ) {
        if (!isPlayStoreReviewLibraryAvailable()) {
            logger.debug("Could not launch Play Store Review flow: Play store review library not found.")
            onError(null)
            return
        }

        val manager = ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val currentActivity = CoreMetaData.getCurrentActivity()
                if (currentActivity != null) {
                    val flow = manager.launchReviewFlow(currentActivity, reviewInfo)
                    flow.addOnCompleteListener { task ->
                        // The API does not indicate whether the user reviewed or not,
                        // or even whether the review dialog was shown.
                        onCompleted()
                    }
                } else {
                    logger.debug("Could not launch Play Store Review flow: current Activity is null.")
                    onError(null)
                }
            } else {
                logger.debug("Could not launch Play Store Review flow.", task.exception)
                onError(task.exception)
            }
        }
    }
}
