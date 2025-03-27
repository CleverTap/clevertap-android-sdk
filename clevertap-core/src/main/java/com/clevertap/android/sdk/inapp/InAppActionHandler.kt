package com.clevertap.android.sdk.inapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.InAppNotificationActivity
import com.clevertap.android.sdk.PushPermissionHandler
import com.clevertap.android.sdk.Utils
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

internal class InAppActionHandler(
    private val context: Context,
    private val ctConfig: CleverTapInstanceConfig,
    private val pushPermissionHandler: PushPermissionHandler,
    private val playStoreReviewManagerProvider: (Context) -> ReviewManager = {
        ReviewManagerFactory.create(it)
    }
) {

    fun interface PushPermissionPromptPresenter {
        fun showPrompt(activity: Activity)
    }

    private val logger = ctConfig.logger

    fun openUrl(url: String, launchContext: Context? = null): Boolean {
        try {
            val uri = Uri.parse(url.replace("\n", "").replace("\r", ""))
            val queryParamSet = uri.getQueryParameterNames()
            val queryBundle = Bundle()
            if (queryParamSet != null && !queryParamSet.isEmpty()) {
                for (queryName in queryParamSet) {
                    queryBundle.putString(queryName, uri.getQueryParameter(queryName))
                }
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (!queryBundle.isEmpty()) {
                intent.putExtras(queryBundle)
            }

            val context = if (launchContext != null) {
                launchContext
            } else {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                this.context
            }

            Utils.setPackageNameFromResolveInfoList(context, intent)
            context.startActivity(intent)
            return true
        } catch (_: Exception) {
            if (url.startsWith(Constants.WZRK_URL_SCHEMA)) {
                // Ignore logging CT scheme actions
                return true
            }
            logger.debug("No activity found to open url: $url")
            return false
        }
    }

    fun isPlayStoreReviewLibraryAvailable(): Boolean {
        return try {
            Class.forName("com.google.android.play.core.review.ReviewManager")
            true
        } catch (_: Exception) {
            logger.debug("Play store review library not found. App Rating will not work!")
            false
        }
    }

    fun launchPlayStoreReviewFlow(
        onCompleted: () -> Unit,
        onError: (e: Exception?) -> Unit
    ) {
        val manager = try {
            playStoreReviewManagerProvider(context)
        } catch (e: Exception) {
            logger.debug("Could not create Play Store ReviewManager instance.", e)
            onError(e)
            return
        }
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

    fun arePushNotificationsEnabled(): Boolean {
        return pushPermissionHandler.isPushPermissionGranted(context)
    }

    fun launchPushPermissionPrompt(fallbackToSettings: Boolean): Boolean {
        return launchPushPermissionPrompt(
            fallbackToSettings
        ) { activity ->
            if (activity is InAppNotificationActivity) {
                activity.showPushPermissionPrompt(fallbackToSettings)
            } else {
                InAppNotificationActivity.launchForPushPermissionPrompt(
                    activity,
                    ctConfig,
                    fallbackToSettings
                )
            }
        }
    }

    fun launchPushPermissionPrompt(
        fallbackToSettings: Boolean,
        presenter: PushPermissionPromptPresenter
    ): Boolean {
        val currentActivity = CoreMetaData.getCurrentActivity()
        if (currentActivity == null) {
            logger.debug(
                "CurrentActivity reference is null. SDK can't prompt the user with Notification Permission! Ensure the following things:\n" +
                        "1. Calling ActivityLifecycleCallback.register(this) in your custom application class before super.onCreate().\n" +
                        "   Alternatively, register CleverTap SDK's Application class in the manifest using com.clevertap.android.sdk.Application.\n" +
                        "2. Ensure that the promptPushPrimer() API is called from the onResume() lifecycle method, not onCreate()."
            )
            return false
        }

        return pushPermissionHandler.requestPermission(
            currentActivity,
            fallbackToSettings,
            object : PushPermissionHandler.PushPermissionRequestCallback {
                override fun onRequestPermission() {
                    presenter.showPrompt(currentActivity)
                }
            })
    }
}
