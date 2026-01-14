package com.clevertap.android.sdk.inapp.delay

import com.clevertap.android.sdk.inapp.data.InAppDurationPartitioner
import org.json.JSONObject

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
    override fun extractDelay(inApp: JSONObject): Long =
        InAppDurationPartitioner.extractDelayMillis(inApp)

    override fun createSuccessResult(id: String, data: JSONObject): DelayedInAppResult {
        return DelayedInAppResult.Success(id, data)
    }

    override fun createErrorResult(id: String, message: String): DelayedInAppResult {
        return DelayedInAppResult.Error(
            id,
            DelayedInAppResult.Error.ErrorReason.UNKNOWN,
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
    override fun extractDelay(inApp: JSONObject): Long =
        InAppDurationPartitioner.extractInActionMillis(inApp)

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