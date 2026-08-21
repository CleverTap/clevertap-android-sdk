package com.clevertap.android.sdk.displayunits

import android.location.Location
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.inapp.evaluation.NdEvaluationManager
import com.clevertap.android.sdk.variables.JsonUtil

/**
 * Owns Native Display (Display Units) evaluation on the event stream — the ND analog of the
 * `InAppController.onQueue*` hooks (SDK-6055).
 *
 * ND evaluation used to piggyback on `InAppController`; that coupled a separate channel to the in-app
 * feature controller. This gives ND its own entry point: `EventQueueManager.initInAppEvaluation` calls
 * both controllers for a queued event. Each hook merges the app-launched system fields with the event
 * properties (same shape the in-app hooks use) and delegates to [NdEvaluationManager], guarded so an
 * ND fault can never break in-app evaluation or queue-flush scheduling.
 */
internal class NativeDisplayController(
    private val config: CleverTapInstanceConfig,
    private val deviceInfo: DeviceInfo,
    private val ndEvaluationManager: NdEvaluationManager,
) {

    @WorkerThread
    fun onQueueEvent(eventName: String, eventProperties: Map<String, Any>, userLocation: Location?) {
        runGuarded {
            ndEvaluationManager.evaluateOnEvent(eventName, mergedAppFields(eventProperties), userLocation)
        }
    }

    @WorkerThread
    fun onQueueChargedEvent(
        chargeDetails: Map<String, Any>,
        items: List<Map<String, Any>>,
        userLocation: Location?
    ) {
        runGuarded {
            ndEvaluationManager.evaluateOnChargedEvent(mergedAppFields(chargeDetails), items, userLocation)
        }
    }

    @WorkerThread
    fun onQueueProfileEvent(
        userAttributeChangedProperties: Map<String, Map<String, Any?>>,
        location: Location?
    ) {
        runGuarded {
            ndEvaluationManager.evaluateOnUserAttributeChange(userAttributeChangedProperties, location, mergedAppFields())
        }
    }

    private fun runGuarded(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            config.logger.debug(config.accountId, "ND evaluation failed", t)
        }
    }

    private fun mergedAppFields(extra: Map<String, Any> = emptyMap()): MutableMap<String, Any> =
        JsonUtil.mapFromJson<Any>(deviceInfo.appLaunchedFields).apply { putAll(extra) }
}
