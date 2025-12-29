package com.clevertap.android.sdk.inapp.delay

import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DEFAULT_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MAX_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MIN_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_DEFAULT_INACTION_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_INACTION_DURATION
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_MAX_INACTION_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_MIN_INACTION_SECONDS
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds

/**
 * Interface for extracting type-specific data from in-apps
 */
internal interface InAppDataExtractor<T> {
    fun extractDelay(inApp: JSONObject): Long
    fun createSuccessResult(id: String, data: JSONObject): T
    fun createErrorResult(id: String, message: String): T
    fun createDiscardedResult(id: String): T
}

/**
 * Data extractor for delayed in-apps
 */
internal class DelayedInAppDataExtractor : InAppDataExtractor<DelayedInAppResult> {
    override fun extractDelay(inApp: JSONObject): Long {
        val delaySeconds = inApp.optInt(INAPP_DELAY_AFTER_TRIGGER, INAPP_DEFAULT_DELAY_SECONDS)
        return if (delaySeconds in INAPP_MIN_DELAY_SECONDS..INAPP_MAX_DELAY_SECONDS) {
            delaySeconds.seconds.inWholeMilliseconds
        } else {
            0
        }
    }

    override fun createSuccessResult(id: String, data: JSONObject): DelayedInAppResult {
        return DelayedInAppResult.Success(data, id)
    }

    override fun createErrorResult(id: String, message: String): DelayedInAppResult {
        return DelayedInAppResult.Error(
            DelayedInAppResult.Error.ErrorReason.UNKNOWN,
            id,
            Exception(message)
        )
    }
    override fun createDiscardedResult(id: String): DelayedInAppResult {
        return DelayedInAppResult.Discarded(
            id,
           "Timer expired while app was backgrounded"
        )
    }
}

/**
 * Data extractor for in-action in-apps
 */
internal class InActionDataExtractor : InAppDataExtractor<InActionResult> {
    override fun extractDelay(inApp: JSONObject): Long {
        val inactionSeconds = inApp.optInt(INAPP_INACTION_DURATION, INAPP_DEFAULT_INACTION_SECONDS)
        return if (inactionSeconds in INAPP_MIN_INACTION_SECONDS..INAPP_MAX_INACTION_SECONDS) {
            inactionSeconds.seconds.inWholeMilliseconds
        } else {
            0
        }
    }

    override fun createSuccessResult(id: String, data: JSONObject): InActionResult {
        return InActionResult.ReadyToFetch(id.toLong(), data)
    }

    override fun createErrorResult(id: String, message: String): InActionResult {
        return InActionResult.Error(id.toLong(), message)
    }
    override fun createDiscardedResult(id: String): InActionResult {
        return InActionResult.Discarded(
            id,
            "Timer expired while app was backgrounded"
        )
    }
}