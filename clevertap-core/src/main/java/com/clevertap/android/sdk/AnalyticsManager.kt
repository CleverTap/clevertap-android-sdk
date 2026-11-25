package com.clevertap.android.sdk

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.AnalyticsManagerBundler.wzrkBundleToJson
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.FlattenedEventData
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.InAppPreviewHandler
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.android.sdk.inbox.EventSuppressor
import com.clevertap.android.sdk.profile.ProfileCommand
import com.clevertap.android.sdk.profile.traversal.ProfileOperation
import com.clevertap.android.sdk.pushnotification.preview.PushPreviewPayload
import com.clevertap.android.sdk.pushnotification.preview.PushPreviewStore
import com.clevertap.android.sdk.response.CleverTapResponse
import com.clevertap.android.sdk.response.DisplayUnitResponse
import com.clevertap.android.sdk.response.InboxResponse
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.sdk.utils.CTJsonConverter.getWzrkFields
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.utils.JsonFlattener
import com.clevertap.android.sdk.utils.UriHelper
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.pipeline.ValidationPipelineProvider
import java.util.ArrayList
import java.util.Collections.emptyMap
import java.util.Collections.emptySet
import java.util.HashMap
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Future
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal open class AnalyticsManager(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val baseEventQueueManager: BaseEventQueueManager,
    private val validationPipelineProvider: ValidationPipelineProvider,
    private val validationConfig: ValidationConfig,
    private val coreMetaData: CoreMetaData,
    private val deviceInfo: DeviceInfo,
    private val callbackManager: BaseCallbackManager,
    private val controllerManager: ControllerManager,
    private val ctLockManager: CTLockManager,
    private val currentTimeProvider: Clock,
    private val executors: CTExecutors,
    private val localDataStore: LocalDataStore,
    private val inAppPreviewHandler: InAppPreviewHandler,
    private val pushPreviewStore: PushPreviewStore
) : BaseAnalyticsManager() {

    private val installReferrerMap = HashMap<String, Int>(8)
    private val notificationMapLock = Any()
    private val inboxViewedSuppressor = EventSuppressor(2_000L)
    private val inboxClickedSuppressor = EventSuppressor(5_000L)

    private val notificationIdTagMap = HashMap<String?, Long>()
    private val notificationViewedIdTagMap = HashMap<String?, Long>()

    override fun addMultiValuesForKey(key: String?, values: ArrayList<String?>?) {
        config.logger.verbose(config.accountId, "addMultiValuesForKey: key=$key, values=$values")
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("addMultiValuesForKey", Callable<Void?> {
            _handleMultiValues(values, key, ProfileCommand.ADD)
            null
        })
    }

    override fun incrementValue(key: String?, value: Number?) {
        config.logger.verbose(config.accountId, "incrementValue: key=$key, value=$value")
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("incrementValue", Callable<Void?> {
            _constructIncrementDecrementValues(value, key, ProfileCommand.INCREMENT)
            null
        })
    }

    override fun decrementValue(key: String?, value: Number?) {
        config.logger.verbose(config.accountId, "decrementValue: key=$key, value=$value")
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("decrementValue", Callable<Void?> {
            _constructIncrementDecrementValues(value, key, ProfileCommand.DECREMENT)
            null
        })
    }

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method manually
     */
    override fun fetchFeatureFlags() {
        if (config.isAnalyticsOnly) {
            return
        }
        val event = JSONObject()
        val notif = JSONObject()
        try {
            notif.put("t", Constants.FETCH_TYPE_FF)
            event.put("evtName", Constants.WZRK_FETCH)
            event.put("evtData", notif)
        } catch (e: JSONException) {
            // should not happen
        }
        sendFetchEvent(event)
    }

    //Event
    override fun forcePushAppLaunchedEvent() {
        coreMetaData.isAppLaunchPushed = false
        pushAppLaunchedEvent()
    }

    override fun pushAppLaunchedEvent() {
        //Will not run for Apps which disable App Launched event
        if (config.isDisableAppLaunchedEvent) {
            coreMetaData.isAppLaunchPushed = true
            config.logger
                .debug(config.accountId, "App Launched Events disabled in the Android Manifest file")
            return
        }
        if (coreMetaData.isAppLaunchPushed) {
            config.logger
                .verbose(config.accountId, "App Launched has already been triggered. Will not trigger it ")
            return
        } else {
            config.logger.verbose(config.accountId, "Firing App Launched event")
        }
        coreMetaData.isAppLaunchPushed = true
        val event = JSONObject()
        try {
            event.put("evtName", Constants.APP_LAUNCHED_EVENT)

            event.put("evtData", deviceInfo.appLaunchedFields)
        } catch (t: Throwable) {
            // We won't get here
        }

        // Flattened data is an empty map since AppLaunched fields are attached separately before inapp evaluation. AppLaunched Fields never have nesting
        baseEventQueueManager.queueEvent(
            context, event, Constants.RAISED_EVENT,
            FlattenedEventData.EventProperties(emptyMap())
        )
    }

    override fun pushDefineVarsEvent(data: JSONObject?) {
        baseEventQueueManager.queueEvent(context, data, Constants.DEFINE_VARS_EVENT)
    }

    override fun pushDisplayUnitClickedEventForID(unitID: String?) {
        val event = JSONObject()

        try {
            event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME)

            //wzrk fields
            val cache = controllerManager.displayUnitCache
            if (cache != null) {
                val displayUnit = cache.getDisplayUnitForID(unitID)
                if (displayUnit != null) {
                    val eventExtraData = displayUnit.wzrkFields
                    if (eventExtraData != null) {
                        event.put("evtData", eventExtraData)
                        try {
                            coreMetaData.wzrkParams = eventExtraData
                        } catch (t: Throwable) {
                            // no-op
                        }
                        baseEventQueueManager.queueEvent(
                            context, event, Constants.RAISED_EVENT,
                            getFlattenedEventProperties(eventExtraData)
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            // We won't get here
            config.logger.verbose(
                config.accountId,
                Constants.FEATURE_DISPLAY_UNIT + "Failed to push Display Unit clicked event" + t
            )
        }
    }

    /**
     * Raises a Native Display element click event.
     *
     * Element-level analog of [pushDisplayUnitClickedEventForID] — for
     * Native Display units that host multiple interactive child elements (buttons,
     * images, etc.), this method records which child element was clicked alongside
     * the existing wzrk_* campaign attribution.
     *
     * Caller's additionalProperties (which should include wzrk_element_id from the
     * action metadata injected by the BE) are merged verbatim first; the cached
     * unit's wzrk_* fields are then layered on top.
     */
    override fun pushDisplayUnitElementClickedEventForID(
        unitID: String?,
        additionalProperties: HashMap<String, Any>?
    ) {
        val event = JSONObject()
        try {
            event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME)

            val cache = controllerManager.displayUnitCache
            if (cache == null) {
                config.logger.verbose(
                    config.accountId,
                    Constants.FEATURE_DISPLAY_UNIT + "Element click dropped — no display-unit cache installed"
                )
                return
            }
            val displayUnit = cache.getDisplayUnitForID(unitID)
            if (displayUnit == null) {
                config.logger.verbose(
                    config.accountId,
                    Constants.FEATURE_DISPLAY_UNIT + "Element click dropped — no unit found for id: " + unitID
                )
                return
            }

            val eventExtraData = JSONObject()
            mergeAdditionalProperties(eventExtraData, additionalProperties)
            val cachedWzrkFields = displayUnit.wzrkFields
            if (cachedWzrkFields != null) {
                val it = cachedWzrkFields.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    try {
                        eventExtraData.put(k, cachedWzrkFields.get(k))
                    } catch (ignored: JSONException) {
                    }
                }
            }

            event.put("evtData", eventExtraData)
            try {
                coreMetaData.wzrkParams = filterWzrkFields(eventExtraData)
            } catch (t: Throwable) {
                // no-op
            }
            baseEventQueueManager.queueEvent(
                context, event, Constants.RAISED_EVENT,
                getFlattenedEventProperties(eventExtraData)
            )
        } catch (t: Throwable) {
            config.logger.verbose(
                config.accountId,
                Constants.FEATURE_DISPLAY_UNIT
                        + "Failed to push Display Unit element clicked event" + t
            )
        }
    }

    /**
     * Merge caller-supplied `additionalProperties` verbatim into the click
     * event's `evtData`. The `wzrk_*` namespace is enforced by the
     * caller of this helper — by layering the cached unit's `wzrk_*`
     * fields on top after this merge, so server-controlled attribution wins
     * over any same-named caller key.
     */
    private fun mergeAdditionalProperties(
        eventData: JSONObject,
        extras: HashMap<String, Any>?
    ) {
        if (extras == null || extras.isEmpty()) {
            return
        }
        // Keys/values may be null when the map came from Java.
        for ((key: String?, value: Any?) in extras) {
            if (key == null || key.isEmpty() || value == null) {
                continue
            }
            try {
                eventData.put(key, value)
            } catch (ignored: JSONException) {
                // skip unserialisable entries
            }
        }
    }

    /**
     * Project only the `wzrk_*` keys back out of the merged event data so
     * [CoreMetaData.wzrkParams] retains its existing
     * server-namespace contract (it feeds `wzrk_ref` batch headers; caller-
     * supplied non-wzrk extras must not ride along on unrelated subsequent events).
     */
    private fun filterWzrkFields(merged: JSONObject): JSONObject {
        val out = JSONObject()
        val it = merged.keys()
        while (it.hasNext()) {
            val k = it.next()
            if (k.startsWith(Constants.WZRK_PREFIX)) {
                try {
                    out.put(k, merged.get(k))
                } catch (ignored: JSONException) {
                }
            }
        }
        return out
    }

    /**
     * Raises a Native Display element view event.
     *
     * Viewed analog of [pushDisplayUnitElementClickedEventForID] —
     * records that a single child element of a unit came on screen (e.g. one carousel
     * slide) instead of the whole unit.
     *
     * Caller's additionalProperties are merged verbatim first; the cached unit's
     * wzrk_* fields are then layered on top, so server-controlled attribution wins.
     */
    override fun pushDisplayUnitElementViewedEventForID(
        unitID: String?,
        additionalProperties: HashMap<String, Any>?
    ) {
        val event = JSONObject()
        try {
            event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)

            val cache = controllerManager.displayUnitCache
            if (cache == null) {
                config.logger.verbose(
                    config.accountId,
                    Constants.FEATURE_DISPLAY_UNIT + "Element view dropped — no display-unit cache installed"
                )
                return
            }
            val displayUnit = cache.getDisplayUnitForID(unitID)
            if (displayUnit == null) {
                config.logger.verbose(
                    config.accountId,
                    Constants.FEATURE_DISPLAY_UNIT + "Element view dropped — no unit found for id: " + unitID
                )
                return
            }

            val eventExtraData = JSONObject()
            mergeAdditionalProperties(eventExtraData, additionalProperties)
            val cachedWzrkFields = displayUnit.wzrkFields
            if (cachedWzrkFields != null) {
                val it = cachedWzrkFields.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    try {
                        eventExtraData.put(k, cachedWzrkFields.get(k))
                    } catch (ignored: JSONException) {
                    }
                }
            }

            event.put("evtData", eventExtraData)
            baseEventQueueManager.queueEvent(
                context, event, Constants.RAISED_EVENT,
                getFlattenedEventProperties(eventExtraData)
            )
        } catch (t: Throwable) {
            config.logger.verbose(
                config.accountId,
                Constants.FEATURE_DISPLAY_UNIT
                        + "Failed to push Display Unit element viewed event" + t
            )
        }
    }

    override fun pushDisplayUnitViewedEventForID(unitID: String?) {
        val event = JSONObject()

        try {
            event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)

            //wzrk fields
            val cache = controllerManager.displayUnitCache
            if (cache != null) {
                val displayUnit = cache.getDisplayUnitForID(unitID)
                if (displayUnit != null) {
                    // The viewed event is the ND impression hook.
                    val ndFCManager = controllerManager.ndFCManager
                    if (ndFCManager != null) {
                        val unitJson = displayUnit.jsonObject
                        // Key on the stable campaign id (ti), not unitID (= wzrk_id, which rotates daily);
                        // the evaluator's whenLimits are keyed by ti. Mirrors in-app's InAppFCManager, which
                        // records by inapp.getId() (ti). didShow no-ops on a null/empty id.
                        // (Java dereferenced unitJson without a null check; the NPE is caught below.)
                        val ndCampaignId = unitJson!!.optString(Constants.INAPP_ID_IN_PAYLOAD)
                        // Count toward the global ndmp/ndtlc budget only for units inside the ND fcap regime
                        // and not globally excluded (shared predicate — the gate and App-Launched trim read the
                        // same NdFCManager helpers, so the three can never disagree). Exempt units still record
                        // a session impression inside didShow, but must not spend a global slot — the server
                        // caps against these counts and would otherwise starve ceiling-bound campaigns.
                        ndFCManager.didShow(ndCampaignId, NdFCManager.countsTowardCaps(unitJson))
                    }

                    val eventExtras = displayUnit.wzrkFields
                    if (eventExtras != null) {
                        event.put("evtData", eventExtras)
                        baseEventQueueManager.queueEvent(
                            context, event, Constants.RAISED_EVENT,
                            getFlattenedEventProperties(eventExtras)
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            // We won't get here
            config.logger.verbose(
                config.accountId,
                Constants.FEATURE_DISPLAY_UNIT + "Failed to push Display Unit viewed event" + t
            )
        }
    }

    override fun pushError(errorMessage: String?, errorCode: Int) {
        val props = HashMap<String, Any?>()
        props["Error Message"] = errorMessage
        props["Error Code"] = errorCode

        try {
            val activityName = CoreMetaData.getCurrentActivityName()
            if (activityName != null) {
                props["Location"] = activityName
            } else {
                props["Location"] = "Unknown"
            }
        } catch (t: Throwable) {
            // Ignore
            props["Location"] = "Unknown"
        }

        pushEvent("Error Occurred", props)
    }

    override fun pushEvent(eventName: String?, eventActions: Map<String, Any?>?) {
        config.logger.verbose(config.accountId, "pushEvent: eventName=$eventName, eventActions=$eventActions")
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("pushEvent", Callable<Void?> {
            _pushEvent(eventName, eventActions)
            null
        })
    }

    @WorkerThread
    private fun _pushEvent(eventName: String?, eventActions: Map<String, Any?>?) {
        val event = JSONObject()
        try {
            // Validate event name with regular config
            val nameValidationResult = validationPipelineProvider.eventNamePipeline
                .execute(eventName, validationConfig)

            // Check for an error
            if (nameValidationResult.shouldDrop()) {
                return
            }

            // Create config WITHOUT restrictedMultiValueFields for event data
            val eventConfig = ValidationConfig.Builder()
                .from(validationConfig)
                .setRestrictedMultiValueFields(emptySet())
                .build()

            // Validate event data with modified config (no restrictions on multi-value fields)
            val dataValidationResult = validationPipelineProvider.eventDataPipeline
                .execute(eventActions, eventConfig)

            if (dataValidationResult.shouldDrop()) {
                return
            }

            val eventData = dataValidationResult.cleanedData
            val flattenedData = getFlattenedEventProperties(eventData)
            event.put("evtName", nameValidationResult.cleanedName)
            event.put("evtData", eventData)

            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT, flattenedData)
        } catch (t: Throwable) {
            // We won't get here
        }
    }

    /**
     * Raises the Notification Clicked event, if `clicked` is true,
     * otherwise the Notification Viewed event, if `clicked` is false.
     *
     * @param clicked    Whether or not this notification was clicked
     * @param data       The data to be attached as the event data
     * @param customData Additional data such as form input to to be added to the event data
     */
    override fun pushInAppNotificationStateEvent(clicked: Boolean, data: CTInAppNotification?, customData: Bundle?) {
        val event = JSONObject()
        try {
            val notif = getWzrkFields(data)

            if (customData != null) {
                for (x in customData.keySet()) {
                    @Suppress("DEPRECATION")
                    val value = customData.get(x)
                    if (value is Map<*, *>) {
                        // Nested payloads (e.g. wzrk_data for a key-values action) are carried as a
                        // Map and emitted as a nested JSON object.
                        notif.put(x, JSONObject(value))
                    } else if (value != null) {
                        notif.put(x, value)
                    }
                }
            }

            if (clicked) {
                try {
                    coreMetaData.wzrkParams = notif
                } catch (t: Throwable) {
                    // no-op
                }
                event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME)
            } else {
                event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)
            }

            event.put("evtData", notif)
            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT, getFlattenedEventProperties(notif))
        } catch (ignored: Throwable) {
            // We won't get here
        }
    }

    override fun pushInstallReferrer(url: String?) {
        try {
            config.logger.verbose(config.accountId, "Referrer received: $url")

            if (url == null) {
                return
            }
            val now = (System.currentTimeMillis() / 1000).toInt()

            if (installReferrerMap.containsKey(url) && now - installReferrerMap[url]!! < 10) {
                config.logger
                    .verbose(
                        config.accountId,
                        "Skipping install referrer due to duplicate within 10 seconds"
                    )
                return
            }

            installReferrerMap[url] = now

            val uri = Uri.parse("wzrk://track?install=true&$url")

            pushDeepLink(uri, true)
        } catch (t: Throwable) {
            // no-op
        }
    }

    @Synchronized
    override fun pushInstallReferrer(source: String?, medium: String?, campaign: String?) {
        var source = source
        var medium = medium
        var campaign = campaign
        if (source == null && medium == null && campaign == null) {
            return
        }
        try {
            // If already pushed, don't send it again
            val status = StorageHelper.getInt(context, "app_install_status", 0)
            if (status != 0) {
                Logger.d("Install referrer has already been set. Will not override it")
                return
            }
            StorageHelper.putInt(context, "app_install_status", 1)

            if (source != null) {
                source = Uri.encode(source)
            }
            if (medium != null) {
                medium = Uri.encode(medium)
            }
            if (campaign != null) {
                campaign = Uri.encode(campaign)
            }

            var uriStr = "wzrk://track?install=true"
            if (source != null) {
                uriStr += "&utm_source=$source"
            }
            if (medium != null) {
                uriStr += "&utm_medium=$medium"
            }
            if (campaign != null) {
                uriStr += "&utm_campaign=$campaign"
            }

            val uri = Uri.parse(uriStr)
            pushDeepLink(uri, true)
        } catch (t: Throwable) {
            Logger.v("Failed to push install referrer", t)
        }
    }

    override fun pushNotificationClickedEvent(extras: Bundle?) {
        if (config.isAnalyticsOnly) {
            config.logger
                .debug(
                    config.accountId,
                    "is Analytics Only - will not process Notification Clicked event."
                )
            return
        }

        @Suppress("DEPRECATION")
        if (extras == null || extras.isEmpty || extras.get(Constants.NOTIFICATION_TAG) == null) {
            config.logger.debug(
                config.accountId,
                "Push notification not from CleverTap - will not process Notification Clicked event."
            )
            return
        }

        var accountId: String? = null
        try {
            accountId = extras.getString(Constants.WZRK_ACCT_ID_KEY)
        } catch (t: Throwable) {
            // no-op
        }

        val shouldProcess = (accountId == null && config.isDefaultInstance) || config.accountId == accountId

        if (!shouldProcess) {
            config.logger.debug(
                config.accountId,
                "Push notification not targeted at this instance, not processing Notification Clicked Event"
            )
            return
        }

        if (PushPreviewPayload.hasPreviewFields(extras)) {
            // SDK-6282: the tapped intent may come from anywhere. Render only the copy
            // the SDK saved when the push arrived, never the intent's own content.
            renderTrustedPushPreview(extras)
            return
        }

        if (!extras.containsKey(Constants.NOTIFICATION_ID_TAG) || (extras.getString(Constants.NOTIFICATION_ID_TAG) == null)) {
            config.logger.debug(
                config.accountId,
                "Push notification ID Tag is null, not processing Notification Clicked event for:  $extras"
            )
            return
        }

        // Check for dupe notification views; if same notficationdId within specified time interval (5 secs) don't process
        val isDuplicate = checkDuplicateNotificationIds(
            dedupeCheckKey(extras),
            notificationIdTagMap,
            Constants.NOTIFICATION_ID_TAG_INTERVAL
        )
        if (isDuplicate) {
            config.logger.debug(
                config.accountId,
                "Already processed Notification Clicked event for " + extras
                        + ", dropping duplicate."
            )
            return
        }

        try {
            // convert bundle to json
            val notif = wzrkBundleToJson(extras)
            val event = AnalyticsManagerBundler.notificationClickedJson(notif)

            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT, getFlattenedEventProperties(notif))
            coreMetaData.wzrkParams = notif
        } catch (t: Throwable) {
            // We won't get here
        }
        if (callbackManager.pushNotificationListener != null) {
            callbackManager.pushNotificationListener
                ?.onNotificationClickedPayloadReceived(Utils.convertBundleObjectToHashMap(extras))
        } else {
            Logger.d("CTPushNotificationListener is not set")
        }
    }

    /**
     * Looks the tapped preview up in the trusted store and renders the stored copy.
     * Runs on the serial executor, the same queue the store writes on, so a copy
     * saved at push arrival is always visible here.
     *
     * Example: a launch intent that carries its own wzrk_inapp but was never received
     * as a push finds no stored copy and renders nothing.
     */
    private fun renderTrustedPushPreview(extras: Bundle) {
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("renderTrustedPushPreview", Callable<Void?> {
            try {
                val trusted = pushPreviewStore.find(extras)
                    ?: return@Callable null // the store already logged why
                if (trusted.containsKey(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY)) {
                    inAppPreviewHandler.handleInAppPreview(trusted)
                } else if (trusted.containsKey(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY)) {
                    handleInboxPreview(trusted)
                } else if (trusted.containsKey(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY)) {
                    handleSendTestForDisplayUnits(trusted)
                }
            } catch (t: Throwable) {
                // A tap must never crash the app, whatever the extras contain.
                config.logger.verbose(config.accountId, "Failed to process push preview", t)
            }
            null
        })
    }

    private fun handleInboxPreview(extras: Bundle) {
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("testInboxNotification", Callable<Void?> {
            try {
                Logger.v(
                    "Received inbox via push payload: " + extras
                        .getString(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY)
                )
                val r = JSONObject()
                val inboxNotifs = JSONArray()
                r.put(Constants.INBOX_JSON_RESPONSE_KEY, inboxNotifs)
                // A missing payload throws here (caught below), as in Java.
                val testPushObject = JSONObject(
                    extras.getString(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY)!!
                )
                testPushObject.put("_id", (System.currentTimeMillis() / 1000).toString())
                inboxNotifs.put(testPushObject)

                val cleverTapResponse: CleverTapResponse =
                    InboxResponse(config, ctLockManager, callbackManager, controllerManager)
                cleverTapResponse.processResponse(r, null, context)
            } catch (t: Throwable) {
                Logger.v("Failed to process inbox message from push notification payload", t)
            }
            null
        })
    }

    /**
     * Pushes the Notification Viewed event to CleverTap.
     *
     * @param extras The [Bundle] object that contains the
     * notification details
     */
    override fun pushNotificationViewedEvent(extras: Bundle?) {
        @Suppress("DEPRECATION")
        if (extras == null || extras.isEmpty || extras.get(Constants.NOTIFICATION_TAG) == null) {
            config.logger.debug(
                config.accountId,
                "Push notification: " + (extras?.toString() ?: "NULL")
                        + " not from CleverTap - will not process Notification Viewed event."
            )
            return
        }

        // SDK-6282: custom renderers call this after showing the notification. It is the
        // trusted sighting for apps that do not use the SDK's rendering. Must run before the
        // wzrk_id and dedupe checks below, because every Send Test carries wzrk_id "0_0".
        // An analytics-only instance never renders a preview, so it has nothing to remember.
        if (!config.isAnalyticsOnly) {
            pushPreviewStore.remember(extras)
        }

        if (!extras.containsKey(Constants.NOTIFICATION_ID_TAG)
            || (extras.getString(Constants.NOTIFICATION_ID_TAG) == null)
        ) {
            config.logger.debug(
                config.accountId,
                "Push notification ID Tag is null, not processing Notification Viewed event for:  $extras"
            )
            return
        }

        // Check for dupe notification views; if same notficationdId within specified time interval (2 secs) don't process
        val isDuplicate = checkDuplicateNotificationIds(
            dedupeCheckKey(extras),
            notificationViewedIdTagMap,
            Constants.NOTIFICATION_VIEWED_ID_TAG_INTERVAL
        )
        if (isDuplicate) {
            config.logger.debug(
                config.accountId,
                "Already processed Notification Viewed event for $extras, dropping duplicate."
            )
            return
        }

        config.logger.debug("Recording Notification Viewed event for notification:  $extras")

        try {
            val notif = wzrkBundleToJson(extras)
            val event = AnalyticsManagerBundler.notificationViewedJson(notif)
            baseEventQueueManager.queueEvent(context, event, Constants.NV_EVENT, getFlattenedEventProperties(notif))
        } catch (e: JSONException) {
            config.logger.debug("Failed to recording Notification Viewed $e")
        }
    }

    /**
     * Raises the single "Live Activity" lifecycle event (mirrors iOS' Live Activity event).
     * The wzrk_* keys from the push become the event data, plus a `state` field
     * ("Started"/"Updated"/"Ended"/"Dismissed"). Routed through the Notification Viewed
     * pipeline so it is attributed exactly like a push impression.
     *
     * Must be called on a worker thread — it queues the event synchronously via
     * `baseEventQueueManager.queueEvent(...)` ([WorkerThread]), mirroring
     * [pushNotificationViewedEvent]. Render-path callers are already on a worker;
     * the dismiss path (main-thread receiver) dispatches via `postAsyncSafelyTask`.
     *
     * @param extras the Live Update push payload.
     * @param state  one of [Constants.LIVE_UPDATE_STATE_STARTED],
     * [Constants.LIVE_UPDATE_STATE_UPDATED],
     * [Constants.LIVE_UPDATE_STATE_ENDED],
     * [Constants.LIVE_UPDATE_STATE_DISMISSED].
     */
    @WorkerThread
    fun raiseLiveUpdateLifecycleEvent(extras: Bundle?, state: String) {
        if (extras == null || extras.isEmpty) {
            return
        }
        try {
            val notif = wzrkBundleToJson(extras)
            val event = AnalyticsManagerBundler.liveUpdateEventJson(notif, state)
            baseEventQueueManager.queueEvent(context, event, Constants.NV_EVENT, getFlattenedEventProperties(notif))
            // Log only the state + the (non-sensitive) in-place activity id, not the whole payload —
            // the bundle can carry title/message/deep-link PII that shouldn't hit Logcat (CWE-532).
            config.logger.debug(
                config.accountId,
                "Recorded Live Activity event (" + state + ") for activityId: "
                        + extras.getString(Constants.WZRK_LIVE_UPDATE_ID)
            )
        } catch (e: JSONException) {
            config.logger.debug("Failed to record Live Activity event $e")
        }
    }

    override fun pushProfile(profile: Map<String, Any?>?) {
        config.logger.verbose(config.accountId, "pushProfile: profile=$profile")
        if (profile == null || profile.isEmpty()) {
            return
        }
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("profilePush", Callable<Void?> {
            // SDK-6254: check the device id on the serial queue, not on the caller's thread. The
            // device-id barrier resolves the id as the first job on this same per-account queue, so
            // by the time this job runs the id is present. Checking on the caller's thread instead
            // would silently drop a profile pushed right after start-up on a first launch — before
            // the id had resolved — which is exactly when the first profile data arrives.
            if (deviceInfo.deviceID == null) {
                config.logger.verbose(
                    config.accountId,
                    "pushProfile: device id not available, dropping profile"
                )
                return@Callable null
            }
            _push(profile)
            null
        })
    }

    override fun removeMultiValuesForKey(key: String?, values: ArrayList<String?>?) {
        config.logger.verbose(config.accountId, "removeMultiValuesForKey: key=$key, values=$values")
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("removeMultiValuesForKey", Callable<Void?> {
            _handleMultiValues(values, key, ProfileCommand.REMOVE)
            null
        })
    }

    override fun removeValueForKey(key: String?) {
        config.logger.verbose(config.accountId, "removeValueForKey: key=$key")
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("removeValueForKey", Callable<Void?> {
            _removeValueForKey(key)
            null
        })
    }

    override fun sendDataEvent(event: JSONObject?) {
        baseEventQueueManager.queueEvent(context, event, Constants.DATA_EVENT)
    }

    fun pushChargedEvent(chargeDetails: HashMap<String, Any>?, items: ArrayList<HashMap<String, Any>>?) {
        config.logger.verbose(config.accountId, "pushChargedEvent: chargeDetails=$chargeDetails, items=$items")
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("pushChargedEvent", Callable<Void?> {
            _pushChargedEvent(chargeDetails, items)
            null
        })
    }

    @WorkerThread
    private fun _pushChargedEvent(chargeDetails: HashMap<String, Any>?, items: ArrayList<HashMap<String, Any>>?) {
        if (chargeDetails == null || items == null) {
            config.logger.debug(config.accountId, "Invalid Charged event: details and or items is null")
            return
        }

        val validationResult =
            validationPipelineProvider.chargedEventItemsValidationPipeline.execute(items, validationConfig)
        if (validationResult.shouldDrop()) {
            return
        }

        val chargedEvent = JSONObject()
        try {
            // Validate charged event details
            val detailsResult = validationPipelineProvider.eventDataPipeline.execute(chargeDetails, validationConfig)

            if (detailsResult.shouldDrop()) {
                return
            }

            val evtData = detailsResult.cleanedData

            // Validate each item
            val jsonItemsArray = JSONArray()
            for (map in items) {
                val itemResult = validationPipelineProvider.eventDataPipeline.execute(map, validationConfig)

                if (!itemResult.shouldDrop()) {
                    val itemDetails = itemResult.cleanedData
                    jsonItemsArray.put(itemDetails)
                }
            }
            evtData.put("Items", jsonItemsArray)

            chargedEvent.put("evtName", Constants.CHARGED_EVENT)
            chargedEvent.put("evtData", evtData)

            val flattenedData = getFlattenedEventProperties(evtData)
            baseEventQueueManager.queueEvent(context, chargedEvent, Constants.RAISED_EVENT, flattenedData)
        } catch (t: Throwable) {
            // We won't get here
        }
    }

    @Synchronized
    fun pushDeepLink(uri: Uri?, install: Boolean) {
        if (uri == null) {
            return
        }

        try {
            val referrer = UriHelper.getUrchinFromUri(uri)
            if (referrer.has("us")) {
                coreMetaData.source = referrer.get("us").toString()
            }
            if (referrer.has("um")) {
                coreMetaData.medium = referrer.get("um").toString()
            }
            if (referrer.has("uc")) {
                coreMetaData.campaign = referrer.get("uc").toString()
            }

            referrer.put("referrer", uri.toString())
            if (install) {
                referrer.put("install", true)
            }
            recordPageEventWithExtras(referrer)
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "Failed to push deep link", t)
        }
    }

    @Deprecated("Signed Call events are raised through CleverTapAPI#pushSignedCallEvent, which is deprecated.")
    fun raiseEventForSignedCall(eventName: String?, dcEventProperties: JSONObject?): Future<*>? {
        var future: Future<*>? = null

        val event = JSONObject()
        try {
            event.put("evtName", eventName)
            event.put("evtData", dcEventProperties)

            future = baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT)
        } catch (e: JSONException) {
            config.logger.debug(
                config.accountId, Constants.LOG_TAG_SIGNED_CALL +
                        "JSON Exception when raising Signed Call event "
                        + eventName + " - " + e.localizedMessage
            )
        }

        return future
    }

    fun raiseEventForGeofences(eventName: String?, geofenceProperties: JSONObject): Future<*>? {
        var future: Future<*>? = null

        val event = JSONObject()
        try {
            event.put("evtName", eventName)
            event.put("evtData", geofenceProperties)

            val location = Location("")
            location.latitude = geofenceProperties.getDouble("triggered_lat")
            location.longitude = geofenceProperties.getDouble("triggered_lng")

            geofenceProperties.remove("triggered_lat")
            geofenceProperties.remove("triggered_lng")

            coreMetaData.locationFromUser = location

            future = baseEventQueueManager.queueEvent(
                context, event, Constants.RAISED_EVENT,
                getFlattenedEventProperties(geofenceProperties)
            )
        } catch (e: JSONException) {
            config.logger.debug(
                config.accountId, Constants.LOG_TAG_GEOFENCES +
                        "JSON Exception when raising GeoFence event "
                        + eventName + " - " + e.localizedMessage
            )
        }

        return future
    }

    fun recordPageEventWithExtras(extras: JSONObject?) {
        try {
            val jsonObject = JSONObject()
            // Add the extras
            if (extras != null && extras.length() > 0) {
                val keys: Iterator<*> = extras.keys()
                while (keys.hasNext()) {
                    try {
                        val key = keys.next() as String
                        jsonObject.put(key, extras.getString(key))
                    } catch (ignore: ClassCastException) {
                        // Really won't get here
                    }
                }
            }
            baseEventQueueManager.queueEvent(context, jsonObject, Constants.PAGE_EVENT)
        } catch (t: Throwable) {
            // We won't get here
        }
    }

    fun setMultiValuesForKey(key: String?, values: ArrayList<String?>?) {
        config.logger.verbose(config.accountId, "setMultiValuesForKey: key=$key, values=$values")
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("setMultiValuesForKey", Callable<Void?> {
            _handleMultiValues(values, key, ProfileCommand.SET)
            null
        })
    }

    private fun _handleMultiValues(values: ArrayList<String?>?, key: String?, command: ProfileCommand) {
        if (key == null) {
            return
        }

        val eventData: MutableMap<String, ArrayList<String?>?> = HashMap()
        eventData[key] = values

        val eventDataValidationResult =
            validationPipelineProvider.multiValueDataPipeline.execute(eventData, validationConfig)

        if (eventDataValidationResult.shouldDrop()) {
            return
        }

        try {
            _pushMultiValue(eventDataValidationResult.cleanedData.getJSONArray(key), key, command)
        } catch (e: JSONException) {
            config.logger.verbose(config.accountId, "Failed to handle Multi Values for key$key", e)
        }
    }

    private fun _constructIncrementDecrementValues(value: Number?, key: String?, command: ProfileCommand) {
        var key = key
        if (value == null) {
            return
        }
        try {
            val keyResult = validationPipelineProvider.propertyKeyPipeline.execute(key, validationConfig)

            if (keyResult.shouldDrop()) {
                return
            }

            key = keyResult.cleanedKey

            val profileCommand = JSONObject().put(command.commandString, value)
            val profileUpdate = JSONObject().put(key, profileCommand)

            val operation = command.operation

            baseEventQueueManager.pushBasicProfile(profileUpdate, false, mergeProfileChanges(key, value, operation))
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "Failed to update profile value for key $key", t)
        }
    }

    private fun _push(profile: Map<String, Any?>) {
        try {
            // Validate profile data
            val profileResult = validationPipelineProvider.eventDataPipeline.execute(profile, validationConfig)

            if (profileResult.shouldDrop()) {
                return
            }

            val cleanedProfile = profileResult.cleanedData
            config.logger
                .verbose(config.accountId, "Constructed custom profile: $cleanedProfile")

            baseEventQueueManager.pushBasicProfile(
                cleanedProfile, false,
                mergeProfileChanges(cleanedProfile, ProfileOperation.UPDATE)
            )
        } catch (t: Throwable) {
            // Will not happen
            config.logger.verbose(config.accountId, "Failed to push profile", t)
        }
    }

    private fun _removeValueForKey(key: String?) {
        var key = key
        try {
            val keyValidationResult = validationPipelineProvider.propertyKeyPipeline.execute(key, validationConfig)

            if (keyValidationResult.shouldDrop()) {
                return
            }

            key = keyValidationResult.cleanedKey

            // If key contains "Identity" then do not remove from SQLDb and shared prefs
            if (key.lowercase(Locale.getDefault()).contains("identity")) {
                config.logger.verbose(
                    config.accountId,
                    "Cannot remove value for key $key from user profile"
                )
                return
            }

            // Send the delete command
            val command = ProfileCommand.DELETE
            val profileCommand = JSONObject().put(command.commandString, true)
            val profileUpdate = JSONObject().put(key, profileCommand)

            //Set removeFromSharedPrefs to true to remove PII keys from shared prefs.
            baseEventQueueManager.pushBasicProfile(
                profileUpdate, true,
                mergeProfileChanges(key, Constants.DELETE_MARKER, command.operation)
            )

            config.logger.verbose(
                config.accountId,
                "removing value for key $key from user profile"
            )
        } catch (t: Throwable) {
            config.logger.verbose(
                config.accountId,
                "Failed to remove profile value for key $key", t
            )
        }
    }

    private fun _pushMultiValue(originalValues: JSONArray, key: String, command: ProfileCommand) {
        try {
            val profileCommand = JSONObject()
            profileCommand.put(command.commandString, originalValues)

            val profileUpdate = JSONObject()
            profileUpdate.put(key, profileCommand)

            val operation = command.operation
            baseEventQueueManager.pushBasicProfile(
                profileUpdate, false,
                mergeProfileChanges(key, originalValues, operation)
            )

            config.logger.verbose(
                config.accountId,
                "Constructed multi-value profile push: $profileUpdate"
            )
        } catch (t: Throwable) {
            config.logger.verbose(
                config.accountId,
                "Error pushing multiValue for key $key", t
            )
        }
    }

    fun dedupeCheckKey(extras: Bundle): String? {
        // This flag is used so that we can release in phased manner, eventually the check has to go away.
        @Suppress("DEPRECATION")
        val doDedupeCheck = extras.get(Constants.WZRK_DEDUPE)

        var check = false
        if (doDedupeCheck != null) {
            if (doDedupeCheck is String) {
                check = "true".equals(doDedupeCheck, ignoreCase = true)
            }
            if (doDedupeCheck is Boolean) {
                check = doDedupeCheck
            }
        }

        val notificationIdTag: String? = if (check) {
            extras.getString(Constants.WZRK_PUSH_ID)
        } else {
            extras.getString(Constants.NOTIFICATION_ID_TAG)
        }
        return notificationIdTag
    }

    private fun checkDuplicateNotificationIds(
        notificationIdTag: String?,
        notificationTagMap: HashMap<String?, Long>,
        interval: Int
    ): Boolean {
        synchronized(notificationMapLock) {
            // default to false; only return true if we are sure we've seen this one before
            var isDupe = false
            try {
                val now = currentTimeProvider.currentTimeMillis()
                if (notificationTagMap.containsKey(notificationIdTag)) {
                    val timestamp: Long = notificationTagMap[notificationIdTag]!!
                    // same notificationId within time internal treat as dupe
                    if (now - timestamp < interval) {
                        isDupe = true
                    }
                }
                notificationTagMap[notificationIdTag] = now
            } catch (ignored: Throwable) {
                // no-op
            }
            return isDupe
        }
    }

    fun sendPingEvent(eventObject: JSONObject?) {
        baseEventQueueManager
            .queueEvent(context, eventObject, Constants.PING_EVENT)
    }

    override fun sendFetchEvent(eventObject: JSONObject?) {
        baseEventQueueManager
            .queueEvent(context, eventObject, Constants.FETCH_EVENT)
    }

    /**
     * This method handles send Test flow for Display Units
     *
     * @param extras - bundled data of notification payload
     */
    private fun handleSendTestForDisplayUnits(extras: Bundle) {
        try {
            val r = CTJsonConverter.displayUnitFromExtras(extras)

            val cleverTapResponse: CleverTapResponse = DisplayUnitResponse(config, callbackManager, controllerManager)
            cleverTapResponse.processResponse(r, null, context)
        } catch (t: Throwable) {
            Logger.v("Failed to process Display Unit from push notification payload", t)
        }
    }

    /**
     * Raises the Notification Clicked event, if `clicked` is true,
     * otherwise the Notification Viewed event, if `clicked` is false.
     *
     * @param clicked    Whether or not this notification was clicked
     * @param data       The data to be attached as the event data
     * @param customData Additional data such as form input to to be added to the event data
     */
    fun pushInboxMessageStateEvent(clicked: Boolean, data: CTInboxMessage, customData: Bundle?) {
        val msgId = data.messageId

        if (!clicked && data.isRead && isV2InboxMessage(msgId)) {
            config.logger.verbose(
                config.accountId,
                "Inbox: Skipping Viewed for $msgId — already read on another device (V2)"
            )
            return
        }

        val gate = if (clicked) inboxClickedSuppressor else inboxViewedSuppressor
        if (msgId != null && gate.shouldSuppress(msgId)) {
            config.logger.verbose(
                config.accountId,
                "Inbox: " + (if (clicked) "Clicked" else "Viewed") + " suppressed for " + msgId
            )
            return
        }

        val event = JSONObject()
        try {
            val notif = getWzrkFields(data)
            if (isV2InboxMessage(msgId)) {
                notif.put(Constants.WZRK_MID, msgId)
            }

            if (customData != null) {
                for (x in customData.keySet()) {
                    @Suppress("DEPRECATION")
                    val value = customData.get(x)
                    if (value != null) {
                        notif.put(x, value)
                    }
                }
            }

            if (clicked) {
                try {
                    coreMetaData.wzrkParams = notif
                } catch (t: Throwable) {
                    // no-op
                }
                event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME)
            } else {
                event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)
            }

            event.put("evtData", notif)
            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT, getFlattenedEventProperties(notif))
        } catch (ignored: Throwable) {
            // We won't get here
        }
    }

    /**
     * V1 inbox messages must NOT carry `wzrk_mid` in Viewed/Clicked events —
     * the backend rejects them. Safe defaults: null id, null controller, or
     * an id the controller doesn't know about all fall through to V1 behavior.
     */
    private fun isV2InboxMessage(msgId: String?): Boolean {
        if (msgId == null) return false
        val controller = controllerManager.ctInboxController
        return controller != null && controller.isV2Message(msgId)
    }

    private fun getFlattenedEventProperties(properties: JSONObject): FlattenedEventData {
        return FlattenedEventData.EventProperties(JsonFlattener.flatten(properties))
    }

    private fun mergeProfileChanges(
        key: String,
        originalValues: Any?,
        operation: ProfileOperation
    ): FlattenedEventData.ProfileChanges {
        val profileChanges = localDataStore.processProfileTree(key, originalValues, operation)
        return FlattenedEventData.ProfileChanges(profileChanges)
    }

    private fun mergeProfileChanges(
        originalValues: JSONObject,
        operation: ProfileOperation
    ): FlattenedEventData.ProfileChanges {
        val profileChanges = localDataStore.processProfileTree(originalValues, operation)
        return FlattenedEventData.ProfileChanges(profileChanges)
    }
}
