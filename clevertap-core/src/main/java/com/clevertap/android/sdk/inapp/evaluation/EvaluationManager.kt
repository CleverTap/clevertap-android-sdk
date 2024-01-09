package com.clevertap.android.sdk.inapp.evaluation

import android.location.Location
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.isNotNullAndEmpty
import com.clevertap.android.sdk.network.EndpointId
import com.clevertap.android.sdk.network.EndpointId.ENDPOINT_A1
import com.clevertap.android.sdk.network.NetworkHeadersListener
import com.clevertap.android.sdk.orEmptyArray
import com.clevertap.android.sdk.toList
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Manages the evaluation of in-app notifications for the client and server sides.
 *
 * This class, `EvaluationManager`, is responsible for coordinating the evaluation of in-app notifications
 * in both server-side and client-side contexts. It employs a combination of trigger matching, limit checking,
 * and priority sorting to determine which in-app notifications should be displayed or suppressed. The class
 * interfaces with various components such as [TriggersMatcher], [TriggerManager], [LimitsMatcher], and
 * [StoreRegistry] to access necessary data and storage.
 *
 * The class implements the [NetworkHeadersListener] interface to handle the attachment and removal of headers
 * related to evaluated server-side campaign IDs and suppressed client-side in-app notifications. Additionally,
 * it provides methods for evaluating in-app notifications on events, charged events, and app launches, both
 * on the client and server sides.
 *
 * @property triggersMatcher An instance of [TriggersMatcher] to match triggers for in-app notifications.
 * @property triggersManager An instance of [TriggerManager] to manage triggers for in-app notifications.
 * @property limitsMatcher An instance of [LimitsMatcher] to match limits for in-app notifications.
 * @property storeRegistry An instance of [StoreRegistry] to access storage for in-app notifications.
 */
@RestrictTo(LIBRARY)
class EvaluationManager constructor(
    private val triggersMatcher: TriggersMatcher,
    private val triggersManager: TriggerManager,
    private val limitsMatcher: LimitsMatcher,
    private val storeRegistry: StoreRegistry,
) : NetworkHeadersListener {

    // Internal list to track server-side evaluated campaign IDs.
    @VisibleForTesting
    internal var evaluatedServerSideCampaignIds: MutableList<Long> = ArrayList()

    // Internal list to track client-side suppressed in-app notifications.
    @VisibleForTesting
    internal var suppressedClientSideInApps: MutableList<Map<String, Any?>> = ArrayList()

    private val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    /**
     * Evaluates in-app notifications based on a specific event, incorporating the event name,
     * additional properties associated with the event, and the user's location.
     *
     * This method creates an [EventAdapter] instance representing the specified event with the provided details,
     * evaluates the event against server-side, and then proceeds to evaluate it client-side.
     *
     * @param eventName The name of the event triggering the in-app notification evaluation.
     * @param eventProperties Additional properties associated with the event, provided as a map.
     * @param userLocation The location of the user triggering the event, if available.
     *
     * @return A JSONArray containing the evaluated in-app notifications for client-side rendering.
     *         This array includes in-app notifications that meet the criteria for display.
     */
    fun evaluateOnEvent(eventName: String, eventProperties: Map<String, Any>, userLocation: Location?): JSONArray {
        val event = EventAdapter(eventName, eventProperties, userLocation = userLocation)
        evaluateServerSide(event)
        return evaluateClientSide(event)
    }

    /**
     * Evaluates in-app notifications for a charged event, incorporating details about the event,
     * a list of items associated with the event, and the user's location.
     *
     * This method creates an [EventAdapter] instance representing the charged event with the provided details,
     * and then proceeds to evaluate the event against both the server-side and client-side.
     *
     * @param details A map containing additional details about the charged event.
     * @param items A list of maps representing items associated with the charged event.
     * @param userLocation The location of the user triggering the charged event, if available.
     *
     * @return A JSONArray containing the evaluated in-app notifications for client-side rendering.
     */
    fun evaluateOnChargedEvent(
        details: Map<String, Any>,
        items: List<Map<String, Any>>,
        userLocation: Location?
    ): JSONArray {
        val event = EventAdapter(Constants.CHARGED_EVENT, details, items, userLocation = userLocation)
        evaluateServerSide(event)
        return evaluateClientSide(event)
    }

    /**
     * Evaluates client-side in-app notifications for the "App Launched" event,
     * incorporating properties associated with the event and the user's location.
     *
     * This method creates an [EventAdapter] instance representing the "App Launched" event
     * with the provided event properties, and then proceeds to evaluate the event against the client-side.
     *
     * @param eventProperties Additional properties associated with the "App Launched" event, provided as a map.
     * @param userLocation The location of the user during the app launch, if available.
     *
     * @return A JSONArray containing the evaluated in-app notifications for client-side rendering.
     *         This array includes in-app notifications that meet the criteria for display.
     */
    // onBatchSent with App Launched event in batch
    fun evaluateOnAppLaunchedClientSide(eventProperties: Map<String, Any>, userLocation: Location?): JSONArray {
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, eventProperties, userLocation = userLocation)
        return evaluateClientSide(event)
    }

    /**
     * Evaluates server-side in-app notifications for the "App Launched" event,
     * considering the provided list of in-app notifications, event properties, and user location.
     *
     * This method creates an [EventAdapter] instance representing the "App Launched" event
     * with the provided event properties and user location. It then evaluates the eligible in-app notifications
     * from the given list, sorts them by priority, and suppresses or saves them accordingly.
     *
     * @param appLaunchedNotifs A list of JSONObjects representing in-app notifications.
     * @param eventProperties Additional properties associated with the "App Launched" event, provided as a map.
     * @param userLocation The location of the user during the app launch, if available.
     *
     * @return A JSONArray containing the evaluated and prioritized in-app notifications for server-side rendering.
     *         This array includes in-app notifications that meet the criteria for display.
     */
    fun evaluateOnAppLaunchedServerSide(
        appLaunchedNotifs: List<JSONObject>,
        eventProperties: Map<String, Any>,
        userLocation: Location?
    ): JSONArray {
        val event = EventAdapter(Constants.APP_LAUNCHED_EVENT, eventProperties, userLocation = userLocation)

        val eligibleInApps = evaluate(event, appLaunchedNotifs)

        var updated = false
        sortByPriority(eligibleInApps).forEach { inApp ->
            if (!shouldSuppress(inApp)) {
                if (updated) {
                    saveSuppressedClientSideInAppIds()
                } // save before returning
                return JSONArray().also { it.put(inApp) }
            } else {
                updated = true
                suppress(inApp)
            }
        }
        // save before returning
        if (updated) {
            saveSuppressedClientSideInAppIds()
        }
        return JSONArray()
    }

    fun matchWhenLimitsBeforeDisplay(listOfLimitAdapter: List<LimitAdapter>, campaignId: String): Boolean {
        return limitsMatcher.matchWhenLimits(listOfLimitAdapter, campaignId)
    }

    /**
     * Evaluates server side in-app notifications based on the provided event.
     *
     * This method retrieves server-side in-app notifications metadata from the storage, evaluates them against the provided event,
     * and updates the list of evaluated server-side campaign IDs. The updated list is then saved back to storage.
     *
     * @param event The [EventAdapter] representing the event triggering the server-side in-app notification evaluation.
     */
    @VisibleForTesting
    internal fun evaluateServerSide(event: EventAdapter) {
        // Flag to track if the list of evaluated server-side campaign IDs has been updated.
        var updated = false
        // Access the in-app store from the store registry.
        storeRegistry.inAppStore?.let { store ->
            // Retrieve server-side in-app notifications metadata from storage and evaluate them against the event.
            val eligibleInApps = evaluate(event, store.readServerSideInAppsMetaData().toList())

            // Iterate through eligible server-side in-app notifications.
            eligibleInApps.forEach { inApp ->
                // Extract the campaign ID from the in-app notification.
                val campaignId = inApp.optLong(Constants.INAPP_ID_IN_PAYLOAD)
                // Add the campaign ID to the list of evaluated server-side campaign IDs if it's not zero.
                if (campaignId != 0L) {
                    updated = true
                    evaluatedServerSideCampaignIds.add(campaignId)
                }
            }
            // Save the updated list of evaluated server-side campaign IDs to storage if there were updates.
            if (updated) {
                saveEvaluatedServerSideInAppIds()
            }
        }
    }

    /**
     * Evaluates client side in-app notifications based on the provided event.
     *
     * This method retrieves client-side in-app notifications from the storage, evaluates them against the provided event.
     * The resulting eligible in-app notifications are sorted by priority, and the method handles the suppression
     * and updating of TTLs (Time to Live).
     *
     * @param event The [EventAdapter] representing the event triggering the client-side in-app notification evaluation.
     *
     * @return A JSONArray containing the evaluated and prioritized in-app notifications for client-side rendering.
     *         This array includes in-app notifications that meet the criteria for display.
     */
    @VisibleForTesting
    internal fun evaluateClientSide(event: EventAdapter): JSONArray {
        // Flag to track if the list of suppressed client-side in-app IDs has been updated.
        var updated = false
        // Access the in-app store from the store registry.
        storeRegistry.inAppStore?.let { store ->
            // Retrieve client-side in-app notifications from storage and evaluate them against the event.
            val eligibleInApps = evaluate(event, store.readClientSideInApps().toList())

            // Sort eligible client-side in-app notifications by priority.
            sortByPriority(eligibleInApps).forEach { inApp ->
                // Check if the in-app notification should not be suppressed.
                if (!shouldSuppress(inApp)) {
                    // Save suppressed client-side in-app IDs before returning if there were updates.
                    if (updated) {
                        saveSuppressedClientSideInAppIds()
                    } // save before returning

                    // Update the Time to Live (TTL) for the in-app notification.
                    updateTTL(inApp)
                    // Return a JSONArray containing the current in-app notification.
                    return JSONArray().also { it.put(inApp) }
                } else {
                    // Update the flag, suppress the in-app, and continue processing.
                    updated = true
                    suppress(inApp)
                }
            }
            // Save suppressed client-side in-app IDs before returning if there were updates.
            if (updated) {
                saveSuppressedClientSideInAppIds()
            } // save before returning

            // Return an empty JSONArray if no eligible in-app notifications are displayed.
        }.run { return JSONArray() }
    }

    /**
     * Evaluates a list of in-app notifications against the provided event, considering triggers and limits.
     *
     * This method iterates through the given list of in-app notifications and checks for matches with the event's triggers.
     * If a match is found, the method increments the associated triggers, checks against limits, and processes accordingly.
     * Eligible in-app notifications are added to a list for further consideration.
     *
     * @param event The [EventAdapter] representing the event triggering the in-app notification evaluation.
     * @param inappNotifs A list of JSONObjects representing in-app notifications to be evaluated.
     * @param clearResource A function to clear resources, invoked when a limit indicates data should be discarded.
     *                     Accepts a URL parameter
     *
     * @return A list of JSONObjects representing in-app notifications that meet the criteria for display.
     */
    @VisibleForTesting
    internal fun evaluate(
        event: EventAdapter,
        inappNotifs: List<JSONObject>,
        clearResource: (url: String) -> Unit = {}
    ): List<JSONObject> {
        val eligibleInApps: MutableList<JSONObject> = mutableListOf()

        for (inApp in inappNotifs) {
            val campaignId = inApp.optString(Constants.INAPP_ID_IN_PAYLOAD)

            val matchesTrigger =
                triggersMatcher.matchEvent(getWhenTriggers(inApp), event)
            if (matchesTrigger) {
                Logger.v("INAPP", "Triggers matched for event ${event.eventName} against inApp $campaignId")
                triggersManager.increment(campaignId)

                val matchesLimits = limitsMatcher.matchWhenLimits(getWhenLimits(inApp), campaignId)
                val discardData = limitsMatcher.shouldDiscard(getWhenLimits(inApp), campaignId)

                if (discardData) {
                    clearResource.invoke("") // todo pass correct url
                }
                if (matchesLimits) {
                    Logger.v("INAPP", "Limits matched for event ${event.eventName} against inApp $campaignId")
                    eligibleInApps.add(inApp)
                } else {
                    Logger.v("INAPP", "Limits did not matched for event ${event.eventName} against inApp $campaignId")
                }
            } else {
                Logger.v("INAPP", "Triggers did not matched for event ${event.eventName} against inApp $campaignId")
            }
        }
        return eligibleInApps
    }

    @VisibleForTesting
    internal fun getWhenTriggers(triggerJson: JSONObject): List<TriggerAdapter> {
        val whenTriggers = triggerJson.optJSONArray(Constants.INAPP_WHEN_TRIGGERS).orEmptyArray()
        return (0 until whenTriggers.length()).mapNotNull {
            val jsonObject = whenTriggers[it] as? JSONObject
            jsonObject?.let { nonNullJsonObject -> TriggerAdapter(nonNullJsonObject) }
        }
    }

    internal fun getWhenLimits(limitJSON: JSONObject): List<LimitAdapter> {
        val frequencyLimits = limitJSON.optJSONArray(Constants.INAPP_FC_LIMITS).orEmptyArray()
        val occurrenceLimits = limitJSON.optJSONArray(Constants.INAPP_OCCURRENCE_LIMITS).orEmptyArray()

        return (frequencyLimits.toList<JSONObject>() + occurrenceLimits.toList()).mapNotNull {
            if (it.isNotNullAndEmpty()) {
                LimitAdapter(it)
            } else null
        }
    }

    /**
     * Sorts list of InApp objects with priority(100 highest - 1 lowest) and if equal priority
     * then then the one created earliest
     */
    internal fun sortByPriority(inApps: List<JSONObject>): List<JSONObject> {
        val priority: (JSONObject) -> Int = { inApp ->
            inApp.optInt(Constants.INAPP_PRIORITY, 1)
        }

        val ti: (JSONObject) -> String = { inApp ->
            inApp.optString(Constants.INAPP_ID_IN_PAYLOAD, (Clock.SYSTEM.newDate().time / 1000).toString())
        }
        // Sort by priority descending and then by timestamp ascending
        return inApps.sortedWith(compareByDescending<JSONObject> { priority(it) }.thenBy { ti(it) })
    }

    private fun shouldSuppress(inApp: JSONObject): Boolean {
        return inApp.optBoolean(Constants.INAPP_SUPPRESSED)
    }

    @VisibleForTesting
    internal fun suppress(inApp: JSONObject) {
        val campaignId = inApp.optString(Constants.INAPP_ID_IN_PAYLOAD)
        val wzrkId = generateWzrkId(campaignId)
        val wzrkPivot = inApp.optString(Constants.INAPP_WZRK_PIVOT, "wzrk_default")
        val wzrkCgId = inApp.optInt(Constants.INAPP_WZRK_CGID)

        suppressedClientSideInApps.add(
            mapOf(
                Constants.NOTIFICATION_ID_TAG to wzrkId,
                Constants.INAPP_WZRK_PIVOT to wzrkPivot,
                Constants.INAPP_WZRK_CGID to wzrkCgId
            )
        )
    }

    /**
     * Generates a unique WizRocket (Wzrk) identifier based on the provided input and current date.
     *
     * This method concatenates the provided identifier (`ti`) with the current date in the "yyyyMMdd" format
     * to create a distinctive identifier for use in in-app notifications and tracking.
     *
     * @param ti The input identifier, often representing a campaign ID or similar.
     * @param clock The Clock instance used for obtaining the current date. Defaults to the system clock.
     *
     * @return A unique WizRocket identifier combining the input identifier and the current date.
     */

    @VisibleForTesting
    internal fun generateWzrkId(ti: String, clock: Clock = Clock.SYSTEM): String {
        val date = dateFormatter.format(clock.newDate())
        return "${ti}_$date"
    }

    @VisibleForTesting
    internal fun updateTTL(inApp: JSONObject, clock: Clock = Clock.SYSTEM) {
        val offset = inApp.opt(Constants.WZRK_TIME_TO_LIVE_OFFSET) as? Long
        if (offset != null) {
            val now = clock.currentTimeSeconds()
            val ttl = now + offset
            inApp.put(Constants.WZRK_TIME_TO_LIVE, ttl)
        } else {
            // return TTL as null since it cannot be calculated due to null offset value
            // The default TTL will be set in the CTInAppNotification
            inApp.remove(Constants.WZRK_TIME_TO_LIVE)
        }
    }

    private fun removeSentEvaluatedServerSideCampaignIds(header: JSONObject) {
        var updated = false
        val inAppsEval = header.optJSONArray(Constants.INAPP_SS_EVAL_META)
        inAppsEval?.let {
            for (i in 0 until it.length()) {
                val campaignId = it.optLong(i)

                if (campaignId != 0L) {
                    updated = true
                    evaluatedServerSideCampaignIds.remove(campaignId)
                }
            }
        }
        if (updated) {
            saveEvaluatedServerSideInAppIds()
        }
    }

    private fun removeSentSuppressedClientSideInApps(header: JSONObject) {
        var updated = false
        val inAppsEval = header.optJSONArray(Constants.INAPP_SUPPRESSED_META)
        inAppsEval?.let {
            val iterator = suppressedClientSideInApps.iterator()
            while (iterator.hasNext()) {
                val suppressedInApp = iterator.next()
                val inAppId = suppressedInApp[Constants.NOTIFICATION_ID_TAG] as? String
                if (inAppId != null && inAppsEval.toString().contains(inAppId)) {
                    updated = true
                    iterator.remove()
                }
            }
        }

        if (updated) {
            saveSuppressedClientSideInAppIds()
        }
    }

    /**
     * Attaches additional headers to the network request based on the provided endpoint ID.
     *
     * This method is responsible for attaching headers, such as evaluated server-side in-app campaign IDs
     * and suppressed client-side in-app notifications, to the network request based on the specified endpoint ID.
     *
     * @param endpointId The endpoint ID representing the target of the network request.
     * @return A JSONObject containing additional headers, or null if no headers need to be attached.
     */
    override fun onAttachHeaders(endpointId: EndpointId): JSONObject? {
        // Initialize a JSONObject to hold additional headers.
        val header = JSONObject()
        // Check if the network request is targeting a specific endpoint (e.g., ENDPOINT_A1).
        if (endpointId == ENDPOINT_A1) {
            // Attach evaluated server-side in-app campaign IDs if available.
            if (evaluatedServerSideCampaignIds.isNotEmpty()) {
                header.put(Constants.INAPP_SS_EVAL_META, JsonUtil.listToJsonArray(evaluatedServerSideCampaignIds))
            }
            // Attach suppressed client-side in-app notifications if available.
            if (suppressedClientSideInApps.isNotEmpty()) {
                header.put(Constants.INAPP_SUPPRESSED_META, JsonUtil.listToJsonArray(suppressedClientSideInApps))
            }
        }
        // Return the header JSONObject if it is not empty; otherwise, return null.
        if (header.isNotNullAndEmpty())
            return header

        return null
    }

    /**
     * Handles actions to be performed after headers are successfully sent in the network request.
     *
     * This method is responsible for processing actions specific to the provided endpoint ID
     * after the headers have been successfully sent in the network request.
     *
     * @param allHeaders The JSONObject containing all headers that were sent in the network request.
     * @param endpointId The endpoint ID representing the target of the network request.
     */
    override fun onSentHeaders(allHeaders: JSONObject, endpointId: EndpointId) {
        // Check if the network request is targeting a specific endpoint (e.g., ENDPOINT_A1).
        if (endpointId == ENDPOINT_A1) {
            // Remove evaluated server-side campaign IDs that have been sent successfully.
            removeSentEvaluatedServerSideCampaignIds(allHeaders)
            // Remove suppressed client-side in-app notifications that have been sent successfully.
            removeSentSuppressedClientSideInApps(allHeaders)
        }
    }

    @WorkerThread
    fun loadSuppressedCSAndEvaluatedSSInAppsIds() {
        storeRegistry.inAppStore?.let { store ->
            evaluatedServerSideCampaignIds =
                store.readEvaluatedServerSideInAppIds().toList<Number>().map { it.toLong() } as MutableList<Long>
            suppressedClientSideInApps = JsonUtil.listFromJson(store.readSuppressedClientSideInAppIds())
        }
    }

    @VisibleForTesting
    internal fun saveEvaluatedServerSideInAppIds() {
        storeRegistry.inAppStore?.storeEvaluatedServerSideInAppIds(
            JsonUtil.listToJsonArray(
                evaluatedServerSideCampaignIds
            )
        )
    }

    @VisibleForTesting
    internal fun saveSuppressedClientSideInAppIds() {
        storeRegistry.inAppStore?.storeSuppressedClientSideInAppIds(
            JsonUtil.listToJsonArray(
                suppressedClientSideInApps
            )
        )
    }
}
