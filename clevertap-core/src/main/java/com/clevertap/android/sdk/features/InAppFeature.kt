package com.clevertap.android.sdk.features

import android.content.Context
import android.os.Bundle
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.InAppFCManager
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.features.callbacks.InAppCallbackManager
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoFactory
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.response.InAppResponse
import com.clevertap.android.sdk.task.CTExecutors
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.text.split

/**
 * In-app messaging feature
 * Manages in-app notifications, templates, evaluations, and impressions
 */
internal data class InAppFeature(
    val storeRegistry: StoreRegistry,
    val inAppController: InAppController,
    val evaluationManager: EvaluationManager,
    val impressionManager: ImpressionManager,
    val templatesManager: TemplatesManager,
    val triggerManager: TriggerManager,
    val inAppResponse: InAppResponse,
    val executors: CTExecutors,
    val inAppCallbackManager: InAppCallbackManager = InAppCallbackManager()
) : CleverTapFeature, InAppFeatureMethods {

    lateinit var coreContract: CoreContract
    var inAppFCManager: InAppFCManager? = null

    override fun coreContract(coreContract: CoreContract) {
        this.coreContract = coreContract
    }

    override fun handleApiData(
        response: JSONObject,
        stringBody: String,
        context: Context,
    ) {
        handleInAppResponse(response)
    }

    private fun handleInAppResponse(response: JSONObject) {
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing inapp messages"
            )
            return
        }
        if (storeRegistry.isNotInitialised()) {
            // In legacy we did not process response but this case is unlikely as we init stores
            // on IO or as soon as device id is ready which is first call in sdk.
            // Keeping for coherent behaviour and can be removed after specific testing.
            return
        }
        if (response.length() == 0) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "There is no inapps data to handle"
            )
            return
        }
        // todo: fix user switch flush flag, full response flag.
        inAppResponse.processResponse(
            response, false, false,
            inAppController.getInAppFCManager(), this
        )
    }

    fun batchSent(batch: JSONArray, success: Boolean) {
        if (batch.length() == 0) {
            // picked from legacy FetchInAppListener.kt
            inAppCallbackManager.getFetchInAppsCallback()?.onInAppsFetched(success)
            return
        }
        checkAppLaunched(batch, success)
        checkWzrkFetch(batch, success)
    }

    private fun checkWzrkFetch(batch: JSONArray, success: Boolean) {
        for (i in 0 until batch.length()) {
            val batchItem = batch.optJSONObject(i) ?: JSONObject()
            val batchItemEvtData = batchItem.optJSONObject(Constants.KEY_EVT_DATA) ?: JSONObject()

            if (batchItem.optString(Constants.KEY_EVT_NAME) == Constants.WZRK_FETCH
                && batchItemEvtData.optInt(Constants.KEY_T) == Constants.FETCH_TYPE_IN_APPS
            ) {
                inAppCallbackManager.getFetchInAppsCallback()?.onInAppsFetched(success)
                break
            }
        }
    }

    private fun checkAppLaunched(batch: JSONArray, success: Boolean) {
        for (i in 0 until batch.length()) {
            val item = batch.getJSONObject(i)
            if (item.optString("evtName") == Constants.APP_LAUNCHED_EVENT && success) {
                inAppController.evaluateAppLaunchedInApps()
                break
            }
        }
    }

    override fun clearStaleInAppCache(inappStaleList: JSONArray) {
        //Stale in-app ids used to remove in-app counts and triggers
        for (i in 0..<inappStaleList.length()) {
            val inappStaleId = inappStaleList.optString(i)
            storeRegistry.impressionStore?.clear(inappStaleId)
            triggerManager.removeTriggers(inappStaleId)
        }
    }

    override fun handleAppLaunchServerSide(inapps: JSONArray) {
        try {
            inAppController.onAppLaunchServerSideInAppsResponse(
                inapps,
                coreContract.coreMetaData().locationFromUser
            )
        } catch (e: Throwable) {
            val logger = coreContract.logger()
            val accountId = coreContract.config().accountId
            logger.verbose(accountId, "InAppManager: Malformed AppLaunched ServerSide inApps")
            logger.verbose(accountId, "InAppManager: Reason: " + e.message, e)
        }
    }

    override fun displayInApp(inappNotifsArray: JSONArray) {
        // Fire the first notification, if any
        val task = executors.postAsyncSafelyTask<Unit>(Constants.TAG_FEATURE_IN_APPS)
        task.execute("InAppResponse#processResponse") {
            inAppController.addInAppNotificationsToQueue(inappNotifsArray)
        }
    }

    override fun storeServerSideInApps(jsonArray: JSONArray) {
        storeRegistry.inAppStore?.storeServerSideInApps(jsonArray)
    }

    override fun storeClientSideInApps(jsonArray: JSONArray) {
        storeRegistry.inAppStore?.storeClientSideInApps(jsonArray)
    }

    override fun setMode(mode: String) {
        storeRegistry.inAppStore?.mode = mode
    }

    override fun preloadFilesAndCache(meta: List<Pair<String, CtCacheType>>) {
        FileResourcesRepoFactory.createFileResourcesRepo(
            coreContract.context(),
            coreContract.logger(),
            storeRegistry
        ).also {
            it.preloadFilesAndCache(meta)
        }
    }

    override fun cleanupStaleFiles(files: List<String>) {
        FileResourcesRepoFactory.createFileResourcesRepo(
            coreContract.context(),
            coreContract.logger(),
            storeRegistry
        ).also {
            it.cleanupStaleFiles(files)
        }
    }

    override fun updateInAppFcManager(perDay: Int, perSession: Int, response: JSONObject) {
        inAppFCManager?.updateLimits(coreContract.context(), perDay, perSession)
        inAppFCManager?.processResponse(coreContract.context(), response)
    }

    /**
     * Called when user changes, updates the inapp stores to save data against this new user.
     */
    fun userChanged(deviceId: String) {
        storeRegistry.inAppStore?.onChangeUser(
            deviceId = deviceId,
            accountId = coreContract.config().accountId
        )
        storeRegistry.impressionStore?.onChangeUser(
            deviceId = deviceId,
            accountId = coreContract.config().accountId
        )
    }

    @WorkerThread
    fun handleInAppPreview(extras: Bundle) {
        try {
            // Use requireNotNull for values that must exist, or let for safer parsing
            val payloadString = extras.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY)
            if (payloadString.isNullOrEmpty()) {
                Logger.v("In-app preview payload string is missing or empty.")
                return
            }

            val payloadType = extras.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY)
            val payloadJson = JSONObject(payloadString)

            // Determine the content based on the payload type
            val notificationContent = when (payloadType) {
                Constants.INAPP_IMAGE_INTERSTITIAL_TYPE,
                Constants.INAPP_ADVANCED_BUILDER_TYPE -> getHalfInterstitialInApp(payloadJson)

                else -> payloadJson
            }

            // Construct the final JSON response
            val notificationsArray = JSONArray().put(notificationContent)
            val apiResponseJson = JSONObject().apply {
                put(Constants.INAPP_JSON_RESPONSE_KEY, notificationsArray)
            }

            handleApiData(apiResponseJson, "", coreContract.context())

        } catch (e: JSONException) {
            // Catch specific exceptions
            Logger.v("Failed to parse in-app preview JSON from push payload", e)
        } catch (t: Throwable) {
            // A fallback for any other unexpected errors
            Logger.v("An unexpected error occurred while handling in-app preview", t)
        }
    }

    @Throws(JSONException::class)
    private fun getHalfInterstitialInApp(inapp: JSONObject): JSONObject? {
        val inAppConfig = inapp.optString(Constants.INAPP_IMAGE_INTERSTITIAL_CONFIG)
        val htmlContent: String? = wrapImageInterstitialContent(inAppConfig)

        if (htmlContent != null) {
            inapp.put(Constants.KEY_TYPE, Constants.KEY_CUSTOM_HTML)
            val data = inapp.opt(Constants.INAPP_DATA_TAG)

            if (data is JSONObject) {
                var dataObject = data
                dataObject = JSONObject(dataObject.toString()) // Create a mutable copy
                // Update the html
                dataObject.put(Constants.INAPP_HTML_TAG, htmlContent)
                inapp.put(Constants.INAPP_DATA_TAG, dataObject)
            } else {
                // If data key is not present or it is not a JSONObject,
                // set it and overwrite it
                val newData = JSONObject()
                newData.put(Constants.INAPP_HTML_TAG, htmlContent)
                inapp.put(Constants.INAPP_DATA_TAG, newData)
            }
        } else {
            coreContract.config().getLogger()
                .debug(coreContract.config().accountId, "Failed to parse the image-interstitial notification")
            return null
        }

        return inapp
    }

    /**
     * Wraps the provided content with HTML obtained from the image-interstitial file.
     *
     * @param content The content to be wrapped within the image-interstitial HTML.
     * @return The wrapped content, or null if an error occurs during HTML retrieval or processing.
     */
    fun wrapImageInterstitialContent(content: String?): String? {
        try {
            val html = Utils.readAssetFile(coreContract.context(), Constants.INAPP_IMAGE_INTERSTITIAL_HTML_NAME)
            if (html != null && content != null) {
                val parts: Array<String?> =
                    html.split(Constants.INAPP_HTML_SPLIT.toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                if (parts.size == 2) {
                    return parts[0] + content + parts[1]
                }
            }
        } catch (e: IOException) {
            coreContract.config().getLogger()
                .debug(/* suffix = */ coreContract.config().accountId, /* message = */ "Failed to read the image-interstitial HTML file")
        }
        return null
    }
}

internal interface InAppFeatureMethods {
    fun clearStaleInAppCache(inappStaleList: JSONArray)
    fun handleAppLaunchServerSide(inapps: JSONArray)
    fun displayInApp(inappNotifsArray: JSONArray)
    fun storeServerSideInApps(jsonArray: JSONArray)
    fun storeClientSideInApps(jsonArray: JSONArray)
    fun setMode(mode: String)
    fun preloadFilesAndCache(meta: List<Pair<String, CtCacheType>> )
    fun cleanupStaleFiles(files: List<String>)
    fun updateInAppFcManager(perDay: Int, perSession: Int, response: JSONObject)
}
