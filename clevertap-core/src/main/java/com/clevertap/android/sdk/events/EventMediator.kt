package com.clevertap.android.sdk.events

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.network.NetworkRepo
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONException
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class EventMediator(
    private val config: CleverTapInstanceConfig,
    private val cleverTapMetaData: CoreMetaData,
    private val networkRepo: NetworkRepo
) {

    open fun shouldDeferProcessingEvent(event: JSONObject, eventType: Int): Boolean {
        if (eventType == Constants.DEFINE_VARS_EVENT || config.isCreatedPostAppLaunch) {
            return false
        }
        if (event.has("evtName") && isSystemEvent(event)) {
            return false
        }
        return eventType == Constants.RAISED_EVENT && !cleverTapMetaData.isAppLaunchPushed
    }

    private fun isSystemEvent(event: JSONObject): Boolean {
        return try {
            event.getString("evtName") in Constants.SYSTEM_EVENTS
        } catch (e: JSONException) {
            //no-op
            false
        }
    }

    open fun shouldDropEvent(event: JSONObject?, eventType: Int): Boolean {
        if (eventType == Constants.FETCH_EVENT || eventType == Constants.DEFINE_VARS_EVENT) {
            return false
        }

        if (networkRepo.isMuted()) {
            // a null event throws here, as it did in Java
            config.logger.verbose(config.accountId, "CleverTap is muted, dropping event - ${checkNotNull(event)}")
            return true
        }

        if (!cleverTapMetaData.isCurrentUserOptedOut) {
            return false
        }

        if (!cleverTapMetaData.enabledSystemEvents) {
            config.logger.debug(config.accountId, "Current user is opted out dropping event: $event")
            // opted-out and system events disabled
            return true
        }

        if (eventType != Constants.RAISED_EVENT && eventType != Constants.NV_EVENT) {
            // opted-out and system events enabled
            config.logger.debug(
                config.accountId,
                "This is not RAISED_EVENT or NV_EVENT, not dropping event: $event"
            )
            return false
        }

        // opted-out and system events enabled
        // check for Constants.RAISED_EVENT and Constants.NV_EVENT event special cases
        return shouldDropOptedOutEvent(event)
    }

    /** For an opted-out user with system events enabled: drops everything except system events. */
    private fun shouldDropOptedOutEvent(event: JSONObject?): Boolean {
        val eName = event?.let { getEventName(it) }
        val dropEvent = eName !in ValidationConfig.DEFAULT_RESTRICTED_EVENT_NAMES
        if (dropEvent) {
            config.logger.debug(config.accountId, "Current user is opted out dropping event: $event")
        } else {
            config.logger.debug(config.accountId, "This is a system event, not dropping event: $event")
        }
        return dropEvent
    }

    open fun isAppLaunchedEvent(event: JSONObject): Boolean = hasEventName(event, Constants.APP_LAUNCHED_EVENT)

    open fun isEvent(event: JSONObject): Boolean = event.has(Constants.KEY_EVT_NAME)

    open fun getEventName(event: JSONObject): String? = try {
        event.getString(Constants.KEY_EVT_NAME)
    } catch (e: JSONException) {
        null
    }

    open fun isChargedEvent(event: JSONObject): Boolean = hasEventName(event, Constants.CHARGED_EVENT)

    private fun hasEventName(event: JSONObject, name: String): Boolean = try {
        event.has(Constants.KEY_EVT_NAME) && event.getString(Constants.KEY_EVT_NAME) == name
    } catch (e: JSONException) {
        false
    }

    open fun getChargedEventItemDetails(event: JSONObject): List<Map<String, Any>> = try {
        JsonUtil.listFromJsonSafe(event.getJSONObject(Constants.KEY_EVT_DATA).getJSONArray(Constants.KEY_ITEMS))
    } catch (e: JSONException) {
        emptyList()
    }

    open fun getChargedEventDetails(event: JSONObject): Map<String, Any> = try {
        val evtData = event.getJSONObject(Constants.KEY_EVT_DATA)
        val items = evtData.remove(Constants.KEY_ITEMS)
        val chargedDetails = JsonUtil.mapFromJson<Any>(evtData)
        evtData.put(Constants.KEY_ITEMS, items)
        chargedDetails
    } catch (e: JSONException) {
        emptyMap()
    }
}
