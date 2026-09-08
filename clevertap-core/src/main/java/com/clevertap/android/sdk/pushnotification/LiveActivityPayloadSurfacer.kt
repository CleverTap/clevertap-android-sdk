package com.clevertap.android.sdk.pushnotification

import androidx.annotation.RestrictTo
import org.json.JSONObject

/**
 * Pure flattening of the nested Live Update `data` object into the flat key/value pairs to add at
 * the top level, extracted from `PushNotificationHandler.surfaceLiveActivityPayload` so the merge
 * semantics are unit-testable without a real [android.os.Bundle].
 *
 * Rules (see TAN §5.3):
 * - **Root-wins:** a key already present at the top level ([existingKeys]) is never overwritten, so
 *   wrapper/identity/analytics keys (`wzrk_pid`, `wzrk_activityId`, `wzrk_id`, …) can't be corrupted.
 * - **String coercion:** values are emitted as strings — parity with a flat FCM Push Template
 *   payload (`Map<String,String>`) that the renderers read via `getString(...)` and parse.
 * - **Nested arrays/objects** (e.g. `pt_progress_segments`, `wzrk_acts`) are serialized to compact
 *   JSON, exactly what the renderers re-parse.
 * - **JSON null is skipped** (never written as the literal "null").
 * - Null/empty/malformed input yields an empty map (never throws).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LiveActivityPayloadSurfacer {

    @JvmStatic
    fun flatten(dataJson: String?, existingKeys: Set<String>): Map<String, String> {
        if (dataJson.isNullOrEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        try {
            val json = JSONObject(dataJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (existingKeys.contains(key)) continue      // root-wins
                val value = json.get(key)
                if (value === JSONObject.NULL) continue        // skip JSON null
                out[key] = if (value is String) value else value.toString()
            }
        } catch (t: Throwable) {
            // Malformed data -> nothing to surface.
        }
        return out
    }
}
