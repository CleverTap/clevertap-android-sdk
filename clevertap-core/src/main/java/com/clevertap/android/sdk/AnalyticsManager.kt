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
import java.util.Locale
import java.util.concurrent.Future
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

    /** Runs [block] on the serial post-async-safely queue. */
    private inline fun postAsyncSafely(logTag: String, crossinline block: () -> Unit) {
        executors.postAsyncSafelyTask<Void?>().execute(logTag) {
            block()
            null
        }
    }

    override fun addMultiValuesForKey(key: String?, values: List<String?>?) {
        config.logger.verbose(config.accountId, "addMultiValuesForKey: key=$key, values=$values")
        postAsyncSafely("addMultiValuesForKey") { handleMultiValues(values, key, ProfileCommand.ADD) }
    }

    override fun incrementValue(key: String?, value: Number?) {
        config.logger.verbose(config.accountId, "incrementValue: key=$key, value=$value")
        postAsyncSafely("incrementValue") {
            constructIncrementDecrementValues(value, key, ProfileCommand.INCREMENT)
        }
    }

    override fun decrementValue(key: String?, value: Number?) {
        config.logger.verbose(config.accountId, "decrementValue: key=$key, value=$value")
        postAsyncSafely("decrementValue") {
            constructIncrementDecrementValues(value, key, ProfileCommand.DECREMENT)
        }
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
        try {
            event.put("evtName", Constants.WZRK_FETCH)
            event.put("evtData", JSONObject().put("t", Constants.FETCH_TYPE_FF))
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
        }
        config.logger.verbose(config.accountId, "Firing App Launched event")
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

    override fun pushDefineVarsEvent(data: JSONObject) {
        baseEventQueueManager.queueEvent(context, data, Constants.DEFINE_VARS_EVENT)
    }

    override fun pushDisplayUnitClickedEventForID(unitID: String?) {
        val event = JSONObject()

        try {
            event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME)

            //wzrk fields
            val displayUnit = controllerManager.displayUnitCache?.getDisplayUnitForID(unitID) ?: return
            val eventExtraData = displayUnit.wzrkFields ?: return
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
        } catch (t: Throwable) {
            // We won't get here
            config.logger.verbose(
                config.accountId,
                "${Constants.FEATURE_DISPLAY_UNIT}Failed to push Display Unit clicked event$t"
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
        additionalProperties: Map<String, Any>?
    ) {
        val event = JSONObject()
        try {
            event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME)

            val cache = controllerManager.displayUnitCache
            if (cache == null) {
                config.logger.verbose(
                    config.accountId,
                    "${Constants.FEATURE_DISPLAY_UNIT}Element click dropped — no display-unit cache installed"
                )
                return
            }
            val displayUnit = cache.getDisplayUnitForID(unitID)
            if (displayUnit == null) {
                config.logger.verbose(
                    config.accountId,
                    "${Constants.FEATURE_DISPLAY_UNIT}Element click dropped — no unit found for id: $unitID"
                )
                return
            }

            val eventExtraData = JSONObject()
            mergeAdditionalProperties(eventExtraData, additionalProperties)
            copyFields(from = displayUnit.wzrkFields, into = eventExtraData)

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
                "${Constants.FEATURE_DISPLAY_UNIT}Failed to push Display Unit element clicked event$t"
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
        extras: Map<String, Any>?
    ) {
        if (extras.isNullOrEmpty()) {
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
        merged.keys().forEach { k ->
            if (k.startsWith(Constants.WZRK_PREFIX)) {
                try {
                    out.put(k, merged.get(k))
                } catch (ignored: JSONException) {
                }
            }
        }
        return out
    }

    /** Copies every key of [from] (when present) into [into], skipping values JSON rejects. */
    private fun copyFields(from: JSONObject?, into: JSONObject) {
        from?.keys()?.forEach { k ->
            try {
                into.put(k, from.get(k))
            } catch (ignored: JSONException) {
            }
        }
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
        additionalProperties: Map<String, Any>?
    ) {
        val event = JSONObject()
        try {
            event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)

            val cache = controllerManager.displayUnitCache
            if (cache == null) {
                config.logger.verbose(
                    config.accountId,
                    "${Constants.FEATURE_DISPLAY_UNIT}Element view dropped — no display-unit cache installed"
                )
                return
            }
            val displayUnit = cache.getDisplayUnitForID(unitID)
            if (displayUnit == null) {
                config.logger.verbose(
                    config.accountId,
                    "${Constants.FEATURE_DISPLAY_UNIT}Element view dropped — no unit found for id: $unitID"
                )
                return
            }

            val eventExtraData = JSONObject()
            mergeAdditionalProperties(eventExtraData, additionalProperties)
            copyFields(from = displayUnit.wzrkFields, into = eventExtraData)

            event.put("evtData", eventExtraData)
            baseEventQueueManager.queueEvent(
                context, event, Constants.RAISED_EVENT,
                getFlattenedEventProperties(eventExtraData)
            )
        } catch (t: Throwable) {
            config.logger.verbose(
                config.accountId,
                "${Constants.FEATURE_DISPLAY_UNIT}Failed to push Display Unit element viewed event$t"
            )
        }
    }

    override fun pushDisplayUnitViewedEventForID(unitID: String?) {
        val event = JSONObject()

        try {
            event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)

            //wzrk fields
            val displayUnit = controllerManager.displayUnitCache?.getDisplayUnitForID(unitID) ?: return
            // The viewed event is the ND impression hook.
            controllerManager.ndFCManager?.let { ndFCManager ->
                // Java dereferenced the unit json without a null check; that failure is caught below.
                val unitJson = checkNotNull(displayUnit.jsonObject) { "Display Unit $unitID has no json" }
                // Key on the stable campaign id (ti), not unitID (= wzrk_id, which rotates daily);
                // the evaluator's whenLimits are keyed by ti. Mirrors in-app's InAppFCManager, which
                // records by inapp.getId() (ti). didShow no-ops on a null/empty id.
                val ndCampaignId = unitJson.optString(Constants.INAPP_ID_IN_PAYLOAD)
                // Count toward the global ndmp/ndtlc budget only for units inside the ND fcap regime
                // and not globally excluded (shared predicate — the gate and App-Launched trim read the
                // same NdFCManager helpers, so the three can never disagree). Exempt units still record
                // a session impression inside didShow, but must not spend a global slot — the server
                // caps against these counts and would otherwise starve ceiling-bound campaigns.
                ndFCManager.didShow(ndCampaignId, NdFCManager.countsTowardCaps(unitJson))
            }

            val eventExtras = displayUnit.wzrkFields ?: return
            event.put("evtData", eventExtras)
            baseEventQueueManager.queueEvent(
                context, event, Constants.RAISED_EVENT,
                getFlattenedEventProperties(eventExtras)
            )
        } catch (t: Throwable) {
            // We won't get here
            config.logger.verbose(
                config.accountId,
                "${Constants.FEATURE_DISPLAY_UNIT}Failed to push Display Unit viewed event$t"
            )
        }
    }

    override fun pushError(errorMessage: String?, errorCode: Int) {
        val location = try {
            CoreMetaData.getCurrentActivityName() ?: "Unknown"
        } catch (t: Throwable) {
            // Ignore
            "Unknown"
        }
        val props = hashMapOf<String, Any?>(
            "Error Message" to errorMessage,
            "Error Code" to errorCode,
            "Location" to location
        )
        pushEvent("Error Occurred", props)
    }

    override fun pushEvent(eventName: String?, eventActions: Map<String, Any?>?) {
        config.logger.verbose(config.accountId, "pushEvent: eventName=$eventName, eventActions=$eventActions")
        postAsyncSafely("pushEvent") { pushEventInternal(eventName, eventActions) }
    }

    @WorkerThread
    private fun pushEventInternal(eventName: String?, eventActions: Map<String, Any?>?) {
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

            customData?.keySet()?.forEach { x ->
                @Suppress("DEPRECATION")
                when (val value = customData.get(x)) {
                    // Nested payloads (e.g. wzrk_data for a key-values action) are carried as a
                    // Map and emitted as a nested JSON object.
                    is Map<*, *> -> notif.put(x, JSONObject(value))
                    null -> Unit
                    else -> notif.put(x, value)
                }
            }

            putStateEventName(event, clicked, notif)
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

            val lastSeen = installReferrerMap[url]
            if (lastSeen != null && now - lastSeen < 10) {
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

            val uriStr = buildString {
                append("wzrk://track?install=true")
                source?.let { append("&utm_source=${Uri.encode(it)}") }
                medium?.let { append("&utm_medium=${Uri.encode(it)}") }
                campaign?.let { append("&utm_campaign=${Uri.encode(it)}") }
            }
            pushDeepLink(Uri.parse(uriStr), true)
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

        val accountId = try {
            extras.getString(Constants.WZRK_ACCT_ID_KEY)
        } catch (t: Throwable) {
            null
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

        if (!extras.containsKey(Constants.NOTIFICATION_ID_TAG) ||
            (extras.getString(Constants.NOTIFICATION_ID_TAG) == null)
        ) {
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
                "Already processed Notification Clicked event for $extras, dropping duplicate."
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
        val pushNotificationListener = callbackManager.pushNotificationListener
        if (pushNotificationListener == null) {
            Logger.d("CTPushNotificationListener is not set")
        } else {
            pushNotificationListener.onNotificationClickedPayloadReceived(Utils.convertBundleObjectToHashMap(extras))
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
        postAsyncSafely("renderTrustedPushPreview") {
            try {
                val trusted = pushPreviewStore.find(extras)
                    ?: return@postAsyncSafely // the store already logged why
                when {
                    trusted.containsKey(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY) ->
                        inAppPreviewHandler.handleInAppPreview(trusted)

                    trusted.containsKey(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY) -> handleInboxPreview(trusted)

                    trusted.containsKey(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY) ->
                        handleSendTestForDisplayUnits(trusted)
                }
            } catch (t: Throwable) {
                // A tap must never crash the app, whatever the extras contain.
                config.logger.verbose(config.accountId, "Failed to process push preview", t)
            }
        }
    }

    private fun handleInboxPreview(extras: Bundle) {
        postAsyncSafely("testInboxNotification") {
            try {
                val payload = extras.getString(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY)
                Logger.v("Received inbox via push payload: $payload")
                val inboxNotifs = JSONArray()
                val r = JSONObject().put(Constants.INBOX_JSON_RESPONSE_KEY, inboxNotifs)
                // A missing payload throws here (caught below), as in Java.
                val testPushObject = JSONObject(requireNotNull(payload))
                testPushObject.put("_id", (System.currentTimeMillis() / 1000).toString())
                inboxNotifs.put(testPushObject)

                val cleverTapResponse: CleverTapResponse =
                    InboxResponse(config, ctLockManager, callbackManager, controllerManager)
                cleverTapResponse.processResponse(r, null, context)
            } catch (t: Throwable) {
                Logger.v("Failed to process inbox message from push notification payload", t)
            }
        }
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
                "Push notification: ${extras ?: "NULL"} not from CleverTap - will not process Notification Viewed event."
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
                "Recorded Live Activity event ($state) for activityId: ${extras.getString(Constants.WZRK_LIVE_UPDATE_ID)}"
            )
        } catch (e: JSONException) {
            config.logger.debug("Failed to record Live Activity event $e")
        }
    }

    override fun pushProfile(profile: Map<String, Any?>?) {
        config.logger.verbose(config.accountId, "pushProfile: profile=$profile")
        if (profile.isNullOrEmpty()) {
            return
        }
        postAsyncSafely("profilePush") {
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
                return@postAsyncSafely
            }
            pushProfileInternal(profile)
        }
    }

    override fun removeMultiValuesForKey(key: String?, values: List<String?>?) {
        config.logger.verbose(config.accountId, "removeMultiValuesForKey: key=$key, values=$values")
        postAsyncSafely("removeMultiValuesForKey") { handleMultiValues(values, key, ProfileCommand.REMOVE) }
    }

    override fun removeValueForKey(key: String?) {
        config.logger.verbose(config.accountId, "removeValueForKey: key=$key")
        postAsyncSafely("removeValueForKey") { removeValueForKeyInternal(key) }
    }

    override fun sendDataEvent(event: JSONObject) {
        baseEventQueueManager.queueEvent(context, event, Constants.DATA_EVENT)
    }

    fun pushChargedEvent(chargeDetails: Map<String, Any>?, items: List<Map<String, Any>>?) {
        config.logger.verbose(config.accountId, "pushChargedEvent: chargeDetails=$chargeDetails, items=$items")
        postAsyncSafely("pushChargedEvent") { pushChargedEventInternal(chargeDetails, items) }
    }

    @WorkerThread
    private fun pushChargedEventInternal(chargeDetails: Map<String, Any>?, items: List<Map<String, Any>>?) {
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
                    jsonItemsArray.put(itemResult.cleanedData)
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
    fun raiseEventForSignedCall(eventName: String?, dcEventProperties: JSONObject?): Future<*>? = try {
        val event = JSONObject()
            .put("evtName", eventName)
            .put("evtData", dcEventProperties)
        baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT)
    } catch (e: JSONException) {
        config.logger.debug(
            config.accountId,
            "${Constants.LOG_TAG_SIGNED_CALL}JSON Exception when raising Signed Call event $eventName - ${e.localizedMessage}"
        )
        null
    }

    fun raiseEventForGeofences(eventName: String?, geofenceProperties: JSONObject): Future<*>? = try {
        val event = JSONObject()
            .put("evtName", eventName)
            .put("evtData", geofenceProperties)

        val location = Location("").apply {
            latitude = geofenceProperties.getDouble("triggered_lat")
            longitude = geofenceProperties.getDouble("triggered_lng")
        }
        geofenceProperties.remove("triggered_lat")
        geofenceProperties.remove("triggered_lng")

        coreMetaData.locationFromUser = location

        baseEventQueueManager.queueEvent(
            context, event, Constants.RAISED_EVENT,
            getFlattenedEventProperties(geofenceProperties)
        )
    } catch (e: JSONException) {
        config.logger.debug(
            config.accountId,
            "${Constants.LOG_TAG_GEOFENCES}JSON Exception when raising GeoFence event $eventName - ${e.localizedMessage}"
        )
        null
    }

    fun recordPageEventWithExtras(extras: JSONObject?) {
        try {
            val jsonObject = JSONObject()
            // Add the extras
            extras?.keys()?.forEach { key -> jsonObject.put(key, extras.getString(key)) }
            baseEventQueueManager.queueEvent(context, jsonObject, Constants.PAGE_EVENT)
        } catch (t: Throwable) {
            // We won't get here
        }
    }

    fun setMultiValuesForKey(key: String?, values: List<String?>?) {
        config.logger.verbose(config.accountId, "setMultiValuesForKey: key=$key, values=$values")
        postAsyncSafely("setMultiValuesForKey") { handleMultiValues(values, key, ProfileCommand.SET) }
    }

    private fun handleMultiValues(values: List<String?>?, key: String?, command: ProfileCommand) {
        if (key == null) {
            return
        }

        val eventData = hashMapOf(key to values)

        val eventDataValidationResult =
            validationPipelineProvider.multiValueDataPipeline.execute(eventData, validationConfig)

        if (eventDataValidationResult.shouldDrop()) {
            return
        }

        try {
            pushMultiValue(eventDataValidationResult.cleanedData.getJSONArray(key), key, command)
        } catch (e: JSONException) {
            config.logger.verbose(config.accountId, "Failed to handle Multi Values for key$key", e)
        }
    }

    private fun constructIncrementDecrementValues(value: Number?, key: String?, command: ProfileCommand) {
        if (value == null) {
            return
        }
        // The failure log names the cleaned key once validation has produced one.
        var logKey = key
        try {
            val keyResult = validationPipelineProvider.propertyKeyPipeline.execute(key, validationConfig)

            if (keyResult.shouldDrop()) {
                return
            }

            val cleanedKey = keyResult.cleanedKey
            logKey = cleanedKey

            val profileUpdate = JSONObject().put(cleanedKey, JSONObject().put(command.commandString, value))
            baseEventQueueManager.pushBasicProfile(
                profileUpdate, false,
                mergeProfileChanges(cleanedKey, value, command.operation)
            )
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "Failed to update profile value for key $logKey", t)
        }
    }

    private fun pushProfileInternal(profile: Map<String, Any?>) {
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

    private fun removeValueForKeyInternal(key: String?) {
        // The failure log names the cleaned key once validation has produced one.
        var logKey = key
        try {
            val keyValidationResult = validationPipelineProvider.propertyKeyPipeline.execute(key, validationConfig)

            if (keyValidationResult.shouldDrop()) {
                return
            }

            val cleanedKey = keyValidationResult.cleanedKey
            logKey = cleanedKey

            // If key contains "Identity" then do not remove from SQLDb and shared prefs
            if (cleanedKey.lowercase(Locale.getDefault()).contains("identity")) {
                config.logger.verbose(
                    config.accountId,
                    "Cannot remove value for key $cleanedKey from user profile"
                )
                return
            }

            // Send the delete command
            val command = ProfileCommand.DELETE
            val profileUpdate = JSONObject().put(cleanedKey, JSONObject().put(command.commandString, true))

            //Set removeFromSharedPrefs to true to remove PII keys from shared prefs.
            baseEventQueueManager.pushBasicProfile(
                profileUpdate, true,
                mergeProfileChanges(cleanedKey, Constants.DELETE_MARKER, command.operation)
            )

            config.logger.verbose(
                config.accountId,
                "removing value for key $cleanedKey from user profile"
            )
        } catch (t: Throwable) {
            config.logger.verbose(
                config.accountId,
                "Failed to remove profile value for key $logKey", t
            )
        }
    }

    private fun pushMultiValue(originalValues: JSONArray, key: String, command: ProfileCommand) {
        try {
            val profileUpdate = JSONObject().put(key, JSONObject().put(command.commandString, originalValues))
            baseEventQueueManager.pushBasicProfile(
                profileUpdate, false,
                mergeProfileChanges(key, originalValues, command.operation)
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
        val check = when (val doDedupeCheck = extras.get(Constants.WZRK_DEDUPE)) {
            is String -> "true".equals(doDedupeCheck, ignoreCase = true)
            is Boolean -> doDedupeCheck
            else -> false
        }
        return extras.getString(if (check) { Constants.WZRK_PUSH_ID } else { Constants.NOTIFICATION_ID_TAG })
    }

    private fun checkDuplicateNotificationIds(
        notificationIdTag: String?,
        notificationTagMap: MutableMap<String?, Long>,
        interval: Int
    ): Boolean = synchronized(notificationMapLock) {
        // default to false; only return true if we are sure we've seen this one before
        var isDupe = false
        try {
            val now = currentTimeProvider.currentTimeMillis()
            // same notificationId within time internal treat as dupe
            val lastSeen = notificationTagMap[notificationIdTag]
            isDupe = lastSeen != null && now - lastSeen < interval
            notificationTagMap[notificationIdTag] = now
        } catch (ignored: Throwable) {
            // no-op
        }
        isDupe
    }

    fun sendPingEvent(eventObject: JSONObject) {
        baseEventQueueManager.queueEvent(context, eventObject, Constants.PING_EVENT)
    }

    override fun sendFetchEvent(eventObject: JSONObject) {
        baseEventQueueManager.queueEvent(context, eventObject, Constants.FETCH_EVENT)
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

        val gate = if (clicked) { inboxClickedSuppressor } else { inboxViewedSuppressor }
        if (msgId != null && gate.shouldSuppress(msgId)) {
            config.logger.verbose(
                config.accountId,
                "Inbox: ${if (clicked) { "Clicked" } else { "Viewed" }} suppressed for $msgId"
            )
            return
        }

        val event = JSONObject()
        try {
            val notif = getWzrkFields(data)
            if (isV2InboxMessage(msgId)) {
                notif.put(Constants.WZRK_MID, msgId)
            }

            customData?.keySet()?.forEach { x ->
                @Suppress("DEPRECATION")
                customData.get(x)?.let { notif.put(x, it) }
            }

            putStateEventName(event, clicked, notif)
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
    private fun isV2InboxMessage(msgId: String?): Boolean =
        msgId != null && controllerManager.ctInboxController?.isV2Message(msgId) == true

    /**
     * Sets the Notification Clicked / Viewed event name; a click also becomes the current
     * [CoreMetaData.wzrkParams] attribution.
     */
    private fun putStateEventName(event: JSONObject, clicked: Boolean, notif: JSONObject) {
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
    }

    private fun getFlattenedEventProperties(properties: JSONObject): FlattenedEventData =
        FlattenedEventData.EventProperties(JsonFlattener.flatten(properties))

    private fun mergeProfileChanges(
        key: String,
        originalValues: Any?,
        operation: ProfileOperation
    ): FlattenedEventData.ProfileChanges =
        FlattenedEventData.ProfileChanges(localDataStore.processProfileTree(key, originalValues, operation))

    private fun mergeProfileChanges(
        originalValues: JSONObject,
        operation: ProfileOperation
    ): FlattenedEventData.ProfileChanges =
        FlattenedEventData.ProfileChanges(localDataStore.processProfileTree(originalValues, operation))
}
