package com.clevertap.android.sdk.pushnotification.preview

import android.os.Bundle
import com.clevertap.android.sdk.Constants
import org.json.JSONObject

/**
 * The five push fields that decide which preview a Send Test shows.
 *
 * Only these fields are ever stored or handed to a renderer. Title, body,
 * account id, collapse key and everything else in the push are left out on
 * purpose, so nothing from a tapped intent can ride along into a render.
 *
 * Example: a push with
 *   wzrk_inapp_type = "advanced-builder"
 *   wzrk_inapp_s3_url = "https://.../inapp-preview-campaign/1699358338/8bd4..."
 * gives a payload with exactly those two fields.
 */
internal class PushPreviewPayload private constructor(
    private val fields: Map<String, String>
) {

    operator fun get(key: String): String? = fields[key]

    /** A bundle holding only the preview fields, safe to hand to a renderer. */
    fun toBundle(): Bundle = Bundle().apply {
        fields.forEach { (key, value) -> putString(key, value) }
    }

    /** JSON text for storage. Keys are the push field names. */
    fun toJson(): JSONObject = JSONObject().apply {
        fields.forEach { (key, value) -> put(key, value) }
    }

    companion object {

        /** Fixed order. The key algorithm depends on it, so never reorder. */
        val KEYS: List<String> = listOf(
            Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY,   // wzrk_inapp_type
            Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY,        // wzrk_inapp
            Constants.INAPP_PREVIEW_S3_URL_KEY,              // wzrk_inapp_s3_url
            Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY,        // wzrk_inbox
            Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY, // wzrk_adunit
        )

        /**
         * True when the bundle carries at least one preview field with a value, which is
         * exactly when [fromBundle] returns non-null. A key present with a null value does
         * not count, so such a bundle still takes the ordinary click path.
         */
        @JvmStatic
        fun hasPreviewFields(extras: Bundle): Boolean = KEYS.any { extras.get(it) != null }

        /** Null when the bundle carries no preview field at all. */
        fun fromBundle(extras: Bundle): PushPreviewPayload? =
            fromLookup { key -> extras.get(key)?.toString() }

        /** Null when the stored JSON carries no preview field (corrupt row). */
        fun fromJson(json: JSONObject): PushPreviewPayload? =
            fromLookup { key -> if (json.has(key)) json.optString(key) else null }

        private fun fromLookup(valueOf: (String) -> String?): PushPreviewPayload? {
            val fields = LinkedHashMap<String, String>(KEYS.size)
            for (key in KEYS) {
                valueOf(key)?.let { fields[key] = it }
            }
            return if (fields.isEmpty()) null else PushPreviewPayload(fields)
        }
    }
}
