package com.clevertap.android.sdk.events

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.network.NetworkRepo
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONException
import org.json.JSONObject

internal open class EventMediator(
    private val config: CleverTapInstanceConfig,
    private val cleverTapMetaData: CoreMetaData,
    private val networkRepo: NetworkRepo
) {

    open fun shouldDeferProcessingEvent(event: JSONObject, eventType: Int): Boolean {
        if (eventType == Constants.DEFINE_VARS_EVENT) {
            return false
        }
        if (config.isCreatedPostAppLaunch) {
            return false
        }
        if (event.has("evtName")) {
            try {
                if (event.getString("evtName") in Constants.SYSTEM_EVENTS) {
                    return false
                }
            } catch (e: JSONException) {
                //no-op
            }
        }
        return (eventType == Constants.RAISED_EVENT && !cleverTapMetaData.isAppLaunchPushed)
    }

    open fun shouldDropEvent(event: JSONObject?, eventType: Int): Boolean {
        if (eventType == Constants.FETCH_EVENT || eventType == Constants.DEFINE_VARS_EVENT) {
            return false
        }

        if (networkRepo.isMuted()) {
            config.logger
                .verbose(config.accountId, "CleverTap is muted, dropping event - " + event!!.toString())
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
        val eName = if (event != null) getEventName(event) else null
        val isSystemEvent = ValidationConfig.DEFAULT_RESTRICTED_EVENT_NAMES.contains(eName)
        val dropEvent = !isSystemEvent
        if (dropEvent) {
            config.logger.debug(config.accountId, "Current user is opted out dropping event: $event")
        } else {
            config.logger.debug(config.accountId, "This is a system event, not dropping event: $event")
        }
        return dropEvent
    }

    open fun isAppLaunchedEvent(event: JSONObject): Boolean {
        return try {
            event.has(Constants.KEY_EVT_NAME) &&
                    event.getString(Constants.KEY_EVT_NAME) == Constants.APP_LAUNCHED_EVENT
        } catch (e: JSONException) {
            false
        }
    }

    open fun isEvent(event: JSONObject): Boolean {
        return event.has(Constants.KEY_EVT_NAME)
    }

    open fun getEventName(event: JSONObject): String? {
        return try {
            event.getString(Constants.KEY_EVT_NAME)
        } catch (e: JSONException) {
            null
        }
    }

    open fun isChargedEvent(event: JSONObject): Boolean {
        return try {
            event.has(Constants.KEY_EVT_NAME) &&
                    event.getString(Constants.KEY_EVT_NAME) == Constants.CHARGED_EVENT
        } catch (e: JSONException) {
            false
        }
    }

    open fun getChargedEventItemDetails(event: JSONObject): List<Map<String, Any>> {
        return try {
            JsonUtil.listFromJson<Map<String, Any>>(
                event.getJSONObject(Constants.KEY_EVT_DATA).getJSONArray(Constants.KEY_ITEMS)
            )!!
        } catch (e: JSONException) {
            ArrayList()
        }
    }

    open fun getChargedEventDetails(event: JSONObject): Map<String, Any> {
        return try {
            val items = event.getJSONObject(Constants.KEY_EVT_DATA).remove(Constants.KEY_ITEMS)
            val chargedDetails: Map<String, Any> = JsonUtil.mapFromJson<Any>(
                event.getJSONObject(Constants.KEY_EVT_DATA)
            )
            event.getJSONObject(Constants.KEY_EVT_DATA).put(Constants.KEY_ITEMS, items)
            chargedDetails
        } catch (e: JSONException) {
            HashMap()
        }
    }
}
