package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoFactory
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.network.ContentFetchItem
import com.clevertap.android.sdk.task.CTExecutorFactory
import org.json.JSONArray
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class InAppResponse(
    private val config: CleverTapInstanceConfig,
    private val controllerManager: ControllerManager,
    private val isSendTest: Boolean,
    private val storeRegistry: StoreRegistry,
    private val triggerManager: TriggerManager,
    private val templatesManager: TemplatesManager,
    private val coreMetaData: CoreMetaData
) : CleverTapResponseDecorator() {

    private val logger: Logger = config.logger

    // A missing controller fails inside processResponse's catch-all, as the Java NPE did.
    private val inAppController: InAppController
        get() = checkNotNull(controllerManager.inAppController) { "InAppController is not initialised" }

    override fun processResponse(
        jsonBody: JSONObject?,
        stringBody: String?,
        context: Context
    ) {
        processResponse(jsonBody, stringBody, context, ResponseContext.DEFAULT)
    }

    override fun processResponse(
        jsonBody: JSONObject?,
        stringBody: String?,
        context: Context,
        responseContext: ResponseContext
    ) {
        try {
            if (config.isAnalyticsOnly) {
                logger.verbose(
                    config.accountId,
                    "CleverTap instance is configured to analytics only, not processing inapp messages"
                )
                // process metadata response
                return
            }

            if (jsonBody == null || jsonBody.length() == 0) {
                logger.verbose(
                    config.accountId,
                    "There is no inapps data to handle"
                )
                return
            }

            val res = InAppResponseAdapter(jsonBody, templatesManager)
            val impressionStore = storeRegistry.impressionStore
            val inAppStore = storeRegistry.inAppStore
            val inAppAssetStore = storeRegistry.inAppAssetsStore
            val fileStore = storeRegistry.filesStore
            val legacyInAppStore = storeRegistry.legacyInAppStore

            if (impressionStore == null || inAppStore == null || inAppAssetStore == null || legacyInAppStore == null || fileStore == null) {
                logger.verbose(config.accountId, "Stores are not initialised, ignoring inapps!!!!")
                return
            }

            logger.verbose(config.accountId, "InApp: Processing response")

            val perSession = res.inAppsPerSession
            val perDay = res.inAppsPerDay

            val inAppFCManager = controllerManager.inAppFCManager
            if (!isSendTest && inAppFCManager != null) {
                Logger.v("Updating InAppFC Limits")
                inAppFCManager.updateLimits(context, perDay, perSession)
                inAppFCManager.processResponse(context, jsonBody)
            } else {
                logger.verbose(
                    config.accountId,
                    "controllerManager.getInAppFCManager() is NULL, not Updating InAppFC Limits"
                )
            }

            val (hasStaleInApps, staleInApps) = res.staleInApps
            if (hasStaleInApps) {
                clearStaleInAppCache(checkNotNull(staleInApps), impressionStore, triggerManager)
            }

            val mode = res.inAppMode
            if (mode.isNotEmpty()) {
                inAppStore.mode = mode
            }

            if (responseContext.isUserSwitching) {
                return
            }

            // If this /a1 carries a content_fetch that can yield an app-launch in-app, open an
            // arbitration window so the app-launch winner below is held and merged with the /content
            // winner rather than shown twice. Content-fetch responses never open a window, and never
            // feed their directive into the Option 2 fast path (so the list stays empty).
            val contentFetchItems: List<ContentFetchItem> =
                if (responseContext.source != CTResponseSource.CONTENT_FETCH) {
                    res.contentFetchItems.also { inAppController.openAppLaunchArbitrationWindowIfNeeded(it) }
                } else {
                    emptyList()
                }

            handleLegacyInApps(res)
            handleAppLaunchInApps(res, contentFetchItems)
            handleClientSideInApps(res, inAppStore, responseContext.source)
            handleServerSideInAppsMeta(res, inAppStore)
            preloadAssetsAndEvictStaleFiles(res, context, responseContext.isFullResponse)
        } catch (t: Throwable) {
            Logger.v("InAppManager: Failed to parse response", t)
        }
    }

    // Legacy SS in-apps: inapp_notifs (NORMAL/DELAYED) + inapp_notifs_meta (IN-ACTION),
    // campaigns WITHOUT advance display rules.
    private fun handleLegacyInApps(res: InAppResponseAdapter) {
        val partitionedLegacyInApps = res.partitionedLegacyInApps
        if (partitionedLegacyInApps.hasImmediateInApps()) {
            displayInApp(partitionedLegacyInApps.immediateInApps)
        }
        if (partitionedLegacyInApps.hasDelayedInApps()) {
            scheduleDelayedLegacyInApps(partitionedLegacyInApps.delayedInApps)
        }

        val partitionedLegacyMetaInApps = res.partitionedLegacyMetaInApps
        if (partitionedLegacyMetaInApps.hasInActionInApps()) {
            inAppController.scheduleInActionInApps(partitionedLegacyMetaInApps.inActionInApps)
        }
    }

    // App-launch SS in-apps: inapp_notifs_applaunched (NORMAL/DELAYED) + inapp_notifs_applaunched_meta
    // (IN-ACTION), evaluated against the App Launched event.
    private fun handleAppLaunchInApps(res: InAppResponseAdapter, contentFetchItems: List<ContentFetchItem>) {
        val partitionedAppLaunchServerSideInApps = res.partitionedAppLaunchServerSideInApps
        if (partitionedAppLaunchServerSideInApps.hasImmediateInApps()) {
            inAppController.onAppLaunchServerSideInAppsResponse(
                partitionedAppLaunchServerSideInApps.immediateInApps,
                contentFetchItems,
                coreMetaData.locationFromUser
            )
        }
        if (partitionedAppLaunchServerSideInApps.hasDelayedInApps()) {
            inAppController.onAppLaunchServerSideDelayedInAppsResponse(
                partitionedAppLaunchServerSideInApps.delayedInApps,
                coreMetaData.locationFromUser
            )
        }

        val partitionedAppLaunchServerSideMetaInApps = res.partitionedAppLaunchServerSideMetaInApps
        if (partitionedAppLaunchServerSideMetaInApps.hasInActionInApps()) {
            inAppController.onAppLaunchServerSideInactionInAppsResponse(
                partitionedAppLaunchServerSideMetaInApps.inActionInApps,
                coreMetaData.locationFromUser
            )
        }
    }

    // CS in-apps (inapp_notifs_cs). Guard: storeClientSideInApps is a full replace of a persisted
    // store. A content-fetch response carries only a partial (or empty) set, so applying it here
    // would wipe the client-side campaigns delivered by /a1. Only /a1 is authoritative for CS in-apps.
    private fun handleClientSideInApps(res: InAppResponseAdapter, inAppStore: InAppStore, source: CTResponseSource) {
        if (source == CTResponseSource.CONTENT_FETCH) {
            logger.verbose(
                config.accountId,
                "Ignoring inapp_notifs_cs from a content fetch response to protect the client-side store"
            )
            return
        }
        val partitionedClientSideInApps = res.partitionedClientSideInApps
        if (partitionedClientSideInApps.hasImmediateInApps()) {
            inAppStore.storeClientSideInApps(partitionedClientSideInApps.immediateInApps)
        }
        if (partitionedClientSideInApps.hasDelayedInApps()) {
            inAppStore.storeClientSideDelayedInApps(partitionedClientSideInApps.delayedInApps)
        }
    }

    // SS in-apps metadata (inapp_notifs_ss -> IN-ACTION + NORMAL campaigns WITH advance display rules).
    private fun handleServerSideInAppsMeta(res: InAppResponseAdapter, inAppStore: InAppStore) {
        val partitionedServerSideInAppsMeta = res.partitionedServerSideInAppsMeta
        // delayAfterTrigger only comes within inapp_notifs (Legacy SS, with in-app content)
        if (partitionedServerSideInAppsMeta.hasUnknownDurationInApps()) {
            inAppStore.storeServerSideInAppsMetaData(partitionedServerSideInAppsMeta.unknownDurationInApps)
        }
        if (partitionedServerSideInAppsMeta.hasInActionInApps()) {
            inAppStore.storeServerSideInActionMetaData(partitionedServerSideInAppsMeta.inActionInApps)
        }
    }

    private fun preloadAssetsAndEvictStaleFiles(res: InAppResponseAdapter, context: Context, isFullResponse: Boolean) {
        val preloadAssetsMeta: List<Pair<String, CtCacheType>> = res.preloadAssetsMeta

        val assetRepo = FileResourcesRepoFactory.createFileResourcesRepo(context, logger, storeRegistry)
        if (preloadAssetsMeta.isNotEmpty()) {
            assetRepo.preloadFilesAndCache(preloadAssetsMeta)
        }

        if (isFullResponse) {
            logger.verbose(config.accountId, "Handling cache eviction")
            assetRepo.cleanupStaleFiles(res.preloadAssets)
        } else {
            logger.verbose(config.accountId, "Ignoring cache eviction")
        }
    }

    private fun clearStaleInAppCache(
        inappStaleList: JSONArray,
        impressionStore: ImpressionStore,
        triggerManager: TriggerManager
    ) {
        //Stale in-app ids used to remove in-app counts and triggers
        for (i in 0 until inappStaleList.length()) {
            val inappStaleId = inappStaleList.optString(i)
            impressionStore.clear(inappStaleId)
            triggerManager.removeTriggers(inappStaleId)
        }
    }

    private fun displayInApp(inappNotifsList: List<JSONObject>) {
        // Fire the first notification, if any
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void?>(Constants.TAG_FEATURE_IN_APPS)
        task.execute("InAppResponse#processResponse") {
            inAppController.addInAppNotificationsToQueue(inappNotifsList)
            null
        }
    }

    private fun scheduleDelayedLegacyInApps(delayedLegacyInApps: List<JSONObject>) {
        val controller = inAppController

        // Schedule using delay functionality
        controller.scheduleDelayedInAppsForAllModes(delayedLegacyInApps, false)

        val activeDelays = controller.getActiveDelayedInAppsCount()
        logger.verbose(
            config.accountId,
            "InApp: scheduling ${delayedLegacyInApps.size} delayed in-apps. Active delays: $activeDelays"
        )
    }
}
