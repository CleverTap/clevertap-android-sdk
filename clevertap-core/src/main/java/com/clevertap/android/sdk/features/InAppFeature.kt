package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.InAppFCManager
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
import org.json.JSONObject

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
        inAppResponse.processResponse(response, false, false,
            inAppController.getInAppFCManager(), this)
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
