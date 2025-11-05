package com.clevertap.android.sdk

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Bundle
import com.clevertap.android.sdk.AnalyticsManagerBundler.notificationClickedJson
import com.clevertap.android.sdk.AnalyticsManagerBundler.notificationViewedJson
import com.clevertap.android.sdk.AnalyticsManagerBundler.wzrkBundleToJson
import com.clevertap.android.sdk.StorageHelper.getInt
import com.clevertap.android.sdk.StorageHelper.putInt
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.utils.UriHelper
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultFactory
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.Validator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Future

internal class AnalyticsManager internal constructor(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val baseEventQueueManager: BaseEventQueueManager,
    private val validator: Validator,
    private val validationResultStack: ValidationResultStack,
    private val coreMetaData: CoreMetaData,
    private val deviceInfo: DeviceInfo,
    private val currentTimeProvider: Clock,
    private val executors: CTExecutors
) : BaseAnalyticsManager() {
    private val installReferrerMap = HashMap<String?, Int?>(8)
    private val notificationMapLock = Any()

    private val notificationIdTagMap = HashMap<String?, Long?>()
    private val notificationViewedIdTagMap = HashMap<String?, Long?>()

    override fun addMultiValuesForKey(key: String?, values: ArrayList<String?>?) {
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("addMultiValuesForKey") {
            val command = Constants.COMMAND_ADD
            _handleMultiValues(values, key, command)
            null
        }
    }

    override fun incrementValue(key: String?, value: Number?) {
        _constructIncrementDecrementValues(value, key, Constants.COMMAND_INCREMENT)
    }

    override fun decrementValue(key: String?, value: Number?) {
        _constructIncrementDecrementValues(value, key, Constants.COMMAND_DECREMENT)
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
        } catch (_: JSONException) {
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
            config.getLogger()
                .debug(
                    config.accountId,
                    "App Launched Events disabled in the Android Manifest file"
                )
            return
        }
        if (coreMetaData.isAppLaunchPushed) {
            config.getLogger()
                .verbose(
                    config.accountId,
                    "App Launched has already been triggered. Will not trigger it "
                )
            return
        } else {
            config.getLogger().verbose(config.accountId, "Firing App Launched event")
        }
        coreMetaData.isAppLaunchPushed = true
        val event = JSONObject()
        try {
            event.put("evtName", Constants.APP_LAUNCHED_EVENT)

            event.put("evtData", deviceInfo.appLaunchedFields)
        } catch (_: Throwable) {
            // We won't get here
        }
        baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT)
    }

    override fun pushDefineVarsEvent(data: JSONObject?) {
        baseEventQueueManager.queueEvent(context, data, Constants.DEFINE_VARS_EVENT)
    }

    override fun pushDisplayUnitClickedEvent(data: JSONObject?) {
        val event = JSONObject()

        try {
            event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME)
            event.put("evtData", data)
            try {
                coreMetaData.wzrkParams = data
            } catch (_: Throwable) {
                // no-op
            }
            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT)
        } catch (t: Throwable) {
            // We won't get here
            config.getLogger().verbose(
                config.accountId,
                Constants.FEATURE_DISPLAY_UNIT + "Failed to push Display Unit clicked event" + t
            )
        }
    }

    override fun pushDisplayUnitViewedEvent(data: JSONObject?) {
        val event = JSONObject()

        try {
            event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)
            event.put("evtData", data) // wzrk fields

            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT)
        } catch (t: Throwable) {
            // We won't get here
            config.getLogger().verbose(
                config.accountId,
                Constants.FEATURE_DISPLAY_UNIT + "Failed to push Display Unit viewed event" + t
            )
        }
    }

    @Suppress("unused")
    override fun pushError(errorMessage: String?, errorCode: Int) {
        val props = HashMap<String?, Any?>()
        props.put("Error Message", errorMessage)
        props.put("Error Code", errorCode)

        try {
            val activityName = CoreMetaData.getCurrentActivityName()
            if (activityName != null) {
                props.put("Location", activityName)
            } else {
                props.put("Location", "Unknown")
            }
        } catch (t: Throwable) {
            // Ignore
            props.put("Location", "Unknown")
        }

        pushEvent("Error Occurred", props)
    }

    override fun pushEvent(eventName: String?, eventActions: MutableMap<String?, Any?>?) {
        var eventName = eventName
        var eventActions = eventActions
        if (eventName == null || eventName == "") {
            return
        }

        val validationResult = validator.isRestrictedEventName(eventName)
        // Check for a restricted event name
        if (validationResult.errorCode > 0) {
            validationResultStack.pushValidationResult(validationResult)
            return
        }

        val discardedResult = coreMetaData.isEventDiscarded(eventName)
        // Check for a discarded event name
        if (discardedResult.errorCode > 0) {
            validationResultStack.pushValidationResult(discardedResult)
            return
        }

        if (eventActions == null) {
            eventActions = HashMap()
        }

        val event = JSONObject()
        try {
            // Validate
            var vr = validator.cleanEventName(eventName)

            // Check for an error
            if (vr.errorCode != 0) {
                event.put(Constants.ERROR_KEY, CTJsonConverter.getErrorObject(vr))
            }

            eventName = vr.getObject().toString()
            val actions = JSONObject()
            for (key in eventActions.keys) {
                var key = key
                var value = eventActions.get(key)
                vr = validator.cleanObjectKey(key)
                key = vr.getObject().toString()
                // Check for an error
                if (vr.errorCode != 0) {
                    event.put(Constants.ERROR_KEY, CTJsonConverter.getErrorObject(vr))
                }
                try {
                    vr = validator.cleanObjectValue(value, Validator.ValidationContext.Event)
                } catch (_: IllegalArgumentException) {
                    // The object was neither a String, Boolean, or any number primitives
                    val error = ValidationResultFactory
                        .create(
                            512, Constants.PROP_VALUE_NOT_PRIMITIVE, eventName, key,
                            value?.toString() ?: ""
                        )
                    config.getLogger().debug(config.accountId, error.errorDesc)
                    validationResultStack.pushValidationResult(error)
                    // Skip this record
                    continue
                }
                value = vr.getObject()
                // Check for an error
                if (vr.errorCode != 0) {
                    event.put(Constants.ERROR_KEY, CTJsonConverter.getErrorObject(vr))
                }
                actions.put(key, value)
            }
            event.put("evtName", eventName)
            event.put("evtData", actions)

            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT)
        } catch (_: Throwable) {
            // We won't get here
        }
    }

    /**
     * Raises the Notification Clicked event, if {@param clicked} is true,
     * otherwise the Notification Viewed event, if {@param clicked} is false.
     *
     * @param clicked    Whether or not this notification was clicked
     * @param data       The data to be attached as the event data
     * @param customData Additional data such as form input to to be added to the event data
     */
    @Suppress("unused")
    override fun pushInAppNotificationStateEvent(
        clicked: Boolean,
        data: CTInAppNotification,
        customData: Bundle?
    ) {
        val event = JSONObject()
        try {
            val notif = CTJsonConverter.getWzrkFields(data)

            if (customData != null) {
                for (x in customData.keySet()) {
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
            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT)
        } catch (ignored: Throwable) {
            // We won't get here
        }
    }

    override fun pushInstallReferrer(url: String?) {
        try {
            config.getLogger().verbose(config.accountId, "Referrer received: $url")

            if (url == null) {
                return
            }
            val now = (System.currentTimeMillis() / 1000).toInt()

            if (installReferrerMap.containsKey(url) && now - installReferrerMap.get(url)!! < 10) {
                config.getLogger()
                    .verbose(
                        config.accountId,
                        "Skipping install referrer due to duplicate within 10 seconds"
                    )
                return
            }

            installReferrerMap.put(url, now)

            val uri = Uri.parse("wzrk://track?install=true&$url")

            pushDeepLink(uri, true)
        } catch (_: Throwable) {
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
            val status = getInt(context, "app_install_status", 0)
            if (status != 0) {
                Logger.d("Install referrer has already been set. Will not override it")
                return
            }
            putInt(context, "app_install_status", 1)

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

    override fun pushNotificationClickedEvent(extras: Bundle?): Boolean {
        if (config.isAnalyticsOnly) {
            config.getLogger()
                .debug(
                    config.accountId,
                    "is Analytics Only - will not process Notification Clicked event."
                )
            return false
        }

        if (extras == null || extras.isEmpty() || extras.get(Constants.NOTIFICATION_TAG) == null) {
            config.getLogger().debug(
                config.accountId,
                "Push notification not from CleverTap - will not process Notification Clicked event."
            )
            return false
        }

        var accountId: String? = null
        try {
            accountId = extras.getString(Constants.WZRK_ACCT_ID_KEY)
        } catch (_: Throwable) {
            // no-op
        }

        val shouldProcess = (accountId == null && config.isDefaultInstance)
                || config.accountId == accountId

        if (!shouldProcess) {
            config.getLogger().debug(
                config.accountId,
                "Push notification not targeted at this instance, not processing Notification Clicked Event"
            )
            return false
        }

        if (!extras.containsKey(Constants.NOTIFICATION_ID_TAG) || (extras.getString(Constants.NOTIFICATION_ID_TAG) == null)) {
            config.getLogger().debug(
                config.accountId,
                "Push notification ID Tag is null, not processing Notification Clicked event for:  $extras"
            )
            return false
        }

        // Check for dupe notification views; if same notficationdId within specified time interval (5 secs) don't process
        val isDuplicate = checkDuplicateNotificationIds(
            dedupeCheckKey(extras),
            notificationIdTagMap,
            Constants.NOTIFICATION_ID_TAG_INTERVAL
        )
        if (isDuplicate) {
            config.getLogger().debug(
                config.accountId,
                ("Already processed Notification Clicked event for " + extras
                        + ", dropping duplicate.")
            )
            return false
        }

        try {
            // convert bundle to json
            val event = notificationClickedJson(extras)

            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT)
            coreMetaData.wzrkParams = wzrkBundleToJson(extras)
        } catch (_: Throwable) {
            // We won't get here
        }
        return true
    }

    /**
     * Pushes the Notification Viewed event to CleverTap.
     *
     * @param extras The [Bundle] object that contains the
     * notification details
     */
    @Suppress("unused")
    override fun pushNotificationViewedEvent(extras: Bundle?) {
        if (extras == null || extras.isEmpty() || extras.get(Constants.NOTIFICATION_TAG) == null) {
            config.getLogger().debug(
                config.accountId,
                ("Push notification: " + (extras?.toString() ?: "NULL")
                        + " not from CleverTap - will not process Notification Viewed event.")
            )
            return
        }

        if (!extras.containsKey(Constants.NOTIFICATION_ID_TAG)
            || (extras.getString(Constants.NOTIFICATION_ID_TAG) == null)
        ) {
            config.getLogger().debug(
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
            config.getLogger().debug(
                config.accountId,
                "Already processed Notification Viewed event for $extras, dropping duplicate."
            )
            return
        }

        config.getLogger().debug("Recording Notification Viewed event for notification:  $extras")

        val event = notificationViewedJson(extras)
        baseEventQueueManager.queueEvent(context, event, Constants.NV_EVENT)
    }

    override fun pushProfile(profile: Map<String?, Any?>?) {
        if (profile == null || profile.isEmpty() || deviceInfo.getDeviceID() == null) {
            return
        }
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("profilePush") {
            _push(profile)
            null
        }
    }

    override fun removeMultiValuesForKey(key: String?, values: ArrayList<String?>?) {
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("removeMultiValuesForKey") {
            _handleMultiValues(values, key, Constants.COMMAND_REMOVE)
            null
        }
    }

    override fun removeValueForKey(key: String?) {
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("removeValueForKey") {
            _removeValueForKey(key)
            null
        }
    }

    override fun sendDataEvent(event: JSONObject?) {
        baseEventQueueManager.queueEvent(context, event, Constants.DATA_EVENT)
    }

    fun _generateEmptyMultiValueError(key: String?) {
        val error = ValidationResultFactory.create(512, Constants.INVALID_MULTI_VALUE, key)
        validationResultStack.pushValidationResult(error)
        config.getLogger().debug(config.accountId, error.errorDesc)
    }

    fun pushChargedEvent(
        chargeDetails: HashMap<String?, Any?>?,
        items: ArrayList<HashMap<String?, Any?>>?
    ) {
        if (chargeDetails == null || items == null) {
            config.getLogger()
                .debug(config.accountId, "Invalid Charged event: details and or items is null")
            return
        }

        if (items.size > 50) {
            val error = ValidationResultFactory.create(522)
            config.getLogger().debug(config.accountId, error.errorDesc)
            validationResultStack.pushValidationResult(error)
        }

        val evtData = JSONObject()
        val chargedEvent = JSONObject()
        var vr: ValidationResult?
        try {
            for (key in chargeDetails.keys) {
                var key = key
                var value = chargeDetails.get(key)
                vr = validator.cleanObjectKey(key)
                key = vr.getObject().toString()
                // Check for an error
                if (vr.errorCode != 0) {
                    chargedEvent.put(Constants.ERROR_KEY, CTJsonConverter.getErrorObject(vr))
                }

                try {
                    vr = validator.cleanObjectValue(value, Validator.ValidationContext.Event)
                } catch (_: IllegalArgumentException) {
                    // The object was neither a String, Boolean, or any number primitives
                    val error = ValidationResultFactory.create(
                        511,
                        Constants.PROP_VALUE_NOT_PRIMITIVE, "Charged", key,
                        value?.toString() ?: ""
                    )
                    validationResultStack.pushValidationResult(error)
                    config.getLogger().debug(config.accountId, error.errorDesc)
                    // Skip this property
                    continue
                }
                value = vr.getObject()
                // Check for an error
                if (vr.errorCode != 0) {
                    chargedEvent.put(Constants.ERROR_KEY, CTJsonConverter.getErrorObject(vr))
                }

                evtData.put(key, value)
            }

            val jsonItemsArray = JSONArray()
            for (map in items) {
                val itemDetails = JSONObject()
                for (key in map.keys) {
                    var key = key
                    var value = map.get(key)
                    vr = validator.cleanObjectKey(key)
                    key = vr.getObject().toString()
                    // Check for an error
                    if (vr.errorCode != 0) {
                        chargedEvent.put(Constants.ERROR_KEY, CTJsonConverter.getErrorObject(vr))
                    }

                    try {
                        vr = validator.cleanObjectValue(value, Validator.ValidationContext.Event)
                    } catch (_: IllegalArgumentException) {
                        // The object was neither a String, Boolean, or any number primitives
                        val error = ValidationResultFactory
                            .create(
                                511, Constants.OBJECT_VALUE_NOT_PRIMITIVE, key,
                                value?.toString() ?: ""
                            )
                        config.getLogger().debug(config.accountId, error.errorDesc)
                        validationResultStack.pushValidationResult(error)
                        // Skip this property
                        continue
                    }
                    value = vr.getObject()
                    // Check for an error
                    if (vr.errorCode != 0) {
                        chargedEvent.put(Constants.ERROR_KEY, CTJsonConverter.getErrorObject(vr))
                    }
                    itemDetails.put(key, value)
                }
                jsonItemsArray.put(itemDetails)
            }
            evtData.put("Items", jsonItemsArray)

            chargedEvent.put("evtName", Constants.CHARGED_EVENT)
            chargedEvent.put("evtData", evtData)

            baseEventQueueManager.queueEvent(context, chargedEvent, Constants.RAISED_EVENT)
        } catch (_: Throwable) {
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
            config.getLogger().verbose(config.accountId, "Failed to push deep link", t)
        }
    }

    fun raiseEventForSignedCall(eventName: String?, dcEventProperties: JSONObject?): Future<*>? {
        var future: Future<*>? = null

        val event = JSONObject()
        try {
            event.put("evtName", eventName)
            event.put("evtData", dcEventProperties)

            future = baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT)
        } catch (e: JSONException) {
            config.getLogger().debug(
                config.accountId, (Constants.LOG_TAG_SIGNED_CALL +
                        "JSON Exception when raising Signed Call event "
                        + eventName + " - " + e.localizedMessage)
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

            future = baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT)
        } catch (e: JSONException) {
            config.getLogger().debug(
                config.accountId, (Constants.LOG_TAG_GEOFENCES +
                        "JSON Exception when raising GeoFence event "
                        + eventName + " - " + e.localizedMessage)
            )
        }

        return future
    }

    fun recordPageEventWithExtras(extras: JSONObject?) {
        try {
            val jsonObject = JSONObject()
            // Add the extras
            if (extras != null && extras.length() > 0) {
                val keys: MutableIterator<*> = extras.keys()
                while (keys.hasNext()) {
                    try {
                        val key = keys.next() as String
                        jsonObject.put(key, extras.getString(key))
                    } catch (_: ClassCastException) {
                        // Really won't get here
                    }
                }
            }
            baseEventQueueManager.queueEvent(context, jsonObject, Constants.PAGE_EVENT)
        } catch (_: Throwable) {
            // We won't get here
        }
    }

    fun setMultiValuesForKey(key: String?, values: ArrayList<String?>?) {
        val task = executors.postAsyncSafelyTask<Void?>()
        task.execute("setMultiValuesForKey") {
            _handleMultiValues(values, key, Constants.COMMAND_SET)
            null
        }
    }

    private fun _generateInvalidMultiValueKeyError(key: String?) {
        val error = ValidationResultFactory.create(523, Constants.INVALID_MULTI_VALUE_KEY, key)
        validationResultStack.pushValidationResult(error)
        config.getLogger().debug(
            config.accountId,
            "Invalid multi-value property key $key profile multi value operation aborted"
        )
    }

    private fun _handleMultiValues(values: ArrayList<String?>?, key: String?, command: String) {
        var key = key
        if (key == null) {
            return
        }

        if (values == null || values.isEmpty()) {
            _generateEmptyMultiValueError(key)
            return
        }

        val vr: ValidationResult?

        // validate the key
        vr = validator.cleanMultiValuePropertyKey(key)

        // Check for an error
        if (vr.errorCode != 0) {
            validationResultStack.pushValidationResult(vr)
        }

        // reset the key
        val _key = vr.getObject()
        val cleanKey = if (_key != null) vr.getObject().toString() else null

        // if key is empty generate an error and return
        if (cleanKey == null || cleanKey.isEmpty()) {
            _generateInvalidMultiValueKeyError(key)
            return
        }

        key = cleanKey
        _pushMultiValue(values, key, command)
    }

    private fun _constructIncrementDecrementValues(value: Number?, key: String?, command: String) {
        var key = key
        try {
            if (key == null || value == null) {
                return
            }

            // validate the key
            val vr = validator.cleanObjectKey(key)
            key = vr.getObject().toString()

            if (key.isEmpty()) {
                val error = ValidationResultFactory.create(
                    512,
                    Constants.PUSH_KEY_EMPTY, key
                )
                validationResultStack.pushValidationResult(error)
                config.getLogger().debug(config.accountId, error.errorDesc)
                // Abort
                return
            }

            if (value.toInt() < 0 || value.toDouble() < 0 || value.toFloat() < 0) {
                val error = ValidationResultFactory.create(
                    512,
                    Constants.INVALID_INCREMENT_DECREMENT_VALUE, key
                )
                validationResultStack.pushValidationResult(error)
                config.getLogger().debug(config.accountId, error.errorDesc)
                // Abort
                return
            }


            // Check for an error
            if (vr.errorCode != 0) {
                validationResultStack.pushValidationResult(vr)
            }

            // push to server
            val commandObj = JSONObject().put(command, value)
            val updateObj = JSONObject().put(key, commandObj)
            baseEventQueueManager.pushBasicProfile(updateObj, false)
        } catch (t: Throwable) {
            config.getLogger().verbose(
                config.accountId, "Failed to update profile value for key "
                        + key, t
            )
        }
    }

    private fun _push(profile: Map<String?, Any?>?) {
        if (profile == null || profile.isEmpty()) {
            return
        }

        try {
            var vr: ValidationResult?
            val customProfile = JSONObject()
            for (key in profile.keys) {
                var key = key
                var value = profile.get(key)

                vr = validator.cleanObjectKey(key)
                key = vr.getObject().toString()
                // Check for an error
                if (vr.errorCode != 0) {
                    validationResultStack.pushValidationResult(vr)
                }

                if (key.isEmpty()) {
                    val keyError = ValidationResultFactory.create(512, Constants.PUSH_KEY_EMPTY)
                    validationResultStack.pushValidationResult(keyError)
                    config.getLogger().debug(config.accountId, keyError.errorDesc)
                    // Skip this property
                    continue
                }

                try {
                    vr = validator.cleanObjectValue(value, Validator.ValidationContext.Profile)
                } catch (_: Throwable) {
                    // The object was neither a String, Boolean, or any number primitives
                    val error = ValidationResultFactory.create(
                        512,
                        Constants.OBJECT_VALUE_NOT_PRIMITIVE_PROFILE,
                        value?.toString() ?: "", key
                    )
                    validationResultStack.pushValidationResult(error)
                    config.getLogger().debug(config.accountId, error.errorDesc)
                    // Skip this property
                    continue
                }
                value = vr.getObject()
                // Check for an error
                if (vr.errorCode != 0) {
                    validationResultStack.pushValidationResult(vr)
                }

                // test Phone:  if no device country code, test if phone starts with +, log but always send
                if (key.equals("Phone", ignoreCase = true)) {
                    try {
                        value = value.toString()
                        val countryCode = deviceInfo.countryCode
                        if (countryCode == null || countryCode.isEmpty()) {
                            val _value = value
                            if (!_value.startsWith("+")) {
                                val error = ValidationResultFactory
                                    .create(512, Constants.INVALID_COUNTRY_CODE, _value)
                                validationResultStack.pushValidationResult(error)
                                config.getLogger()
                                    .debug(config.accountId, error.errorDesc)
                            }
                        }
                        config.getLogger().verbose(
                            config.accountId,
                            "Profile phone is: $value device country code is: " + (countryCode
                                ?: "null")
                        )
                    } catch (e: Exception) {
                        validationResultStack
                            .pushValidationResult(
                                ValidationResultFactory.create(
                                    512,
                                    Constants.INVALID_PHONE
                                )
                            )
                        config.getLogger()
                            .debug(
                                config.accountId,
                                "Invalid phone number: " + e.localizedMessage
                            )
                        continue
                    }
                }

                customProfile.put(key, value)
            }

            config.getLogger()
                .verbose(config.accountId, "Constructed custom profile: $customProfile")

            baseEventQueueManager.pushBasicProfile(customProfile, false)
        } catch (t: Throwable) {
            // Will not happen
            config.getLogger().verbose(config.accountId, "Failed to push profile", t)
        }
    }

    private fun _removeValueForKey(key: String?) {
        var key = key
        try {
            key = key ?: ""

            // so we will generate a validation error later on

            // validate the key
            val vr = validator.cleanObjectKey(key)
            key = vr.getObject().toString()

            if (key.isEmpty()) {
                val error = ValidationResultFactory.create(512, Constants.KEY_EMPTY)
                validationResultStack.pushValidationResult(error)
                config.getLogger().debug(config.accountId, error.errorDesc)
                // Abort
                return
            }
            // Check for an error
            if (vr.errorCode != 0) {
                validationResultStack.pushValidationResult(vr)
            }

            //If key contains "Identity" then do not remove from SQLDb and shared prefs
            if (key.lowercase(Locale.getDefault()).contains("identity")) {
                config.getLogger()
                    .verbose(
                        config.accountId, "Cannot remove value for key " +
                                key + " from user profile"
                    )
                return
            }

            // send the delete command
            val command = JSONObject().put(Constants.COMMAND_DELETE, true)
            val update = JSONObject().put(key, command)

            //Set removeFromSharedPrefs to true to remove PII keys from shared prefs.
            baseEventQueueManager.pushBasicProfile(update, true)

            config.getLogger()
                .verbose(
                    config.accountId,
                    "removing value for key $key from user profile"
                )
        } catch (t: Throwable) {
            config.getLogger()
                .verbose(config.accountId, "Failed to remove profile value for key $key", t)
        }
    }

    private fun _pushMultiValue(originalValues: ArrayList<String?>?, key: String, command: String) {
        try {
            // push to server
            val commandObj = JSONObject()
            commandObj.put(command, JSONArray(originalValues))

            val fields = JSONObject()
            fields.put(key, commandObj)

            baseEventQueueManager.pushBasicProfile(fields, false)

            config.getLogger()
                .verbose(config.accountId, "Constructed multi-value profile push: $fields")
        } catch (t: Throwable) {
            config.getLogger()
                .verbose(config.accountId, "Error pushing multiValue for key $key", t)
        }
    }

    fun dedupeCheckKey(extras: Bundle): String? {
        // This flag is used so that we can release in phased manner, eventually the check has to go away.
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
        notificationTagMap: HashMap<String?, Long?>,
        interval: Int
    ): Boolean {
        synchronized(notificationMapLock) {
            // default to false; only return true if we are sure we've seen this one before
            var isDupe = false
            try {
                val now = currentTimeProvider.currentTimeMillis()
                if (notificationTagMap.containsKey(notificationIdTag)) {
                    // noinspection ConstantConditions
                    val timestamp: Long = notificationTagMap.get(notificationIdTag)!!
                    // same notificationId within time internal treat as dupe
                    if (now - timestamp < interval) {
                        isDupe = true
                    }
                }
                notificationTagMap.put(notificationIdTag, now)
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
     * Raises the Notification Clicked event, if {@param clicked} is true,
     * otherwise the Notification Viewed event, if {@param clicked} is false.
     *
     * @param clicked    Whether or not this notification was clicked
     * @param data       The data to be attached as the event data
     * @param customData Additional data such as form input to to be added to the event data
     */
    @Suppress("unused")
    fun pushInboxMessageStateEvent(clicked: Boolean, data: CTInboxMessage, customData: Bundle?) {
        val event = JSONObject()
        try {
            val notif = CTJsonConverter.getWzrkFields(data)

            if (customData != null) {
                for (x in customData.keySet()) {
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
            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT)
        } catch (ignored: Throwable) {
            // We won't get here
        }
    }
}