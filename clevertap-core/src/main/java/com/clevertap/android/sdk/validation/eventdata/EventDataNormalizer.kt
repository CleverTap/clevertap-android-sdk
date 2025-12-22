package com.clevertap.android.sdk.validation.eventdata

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.pipeline.EventDataMetrics
import com.clevertap.android.sdk.validation.pipeline.EventDataNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.KeyModification
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import com.clevertap.android.sdk.validation.pipeline.Normalizer
import com.clevertap.android.sdk.validation.pipeline.RemovalReason
import com.clevertap.android.sdk.validation.pipeline.RemovedItem
import com.clevertap.android.sdk.validation.pipeline.ValueModification
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Date
import kotlin.collections.iterator
import kotlin.math.max

/**
 * Normalizes event data (property key-value pairs) according to ValidationConfig.
 * Only performs normalization - does not validate.
 *
 * Normalization includes:
 * - Cleaning all keys and values
 * - Removing null values
 * - Removing empty keys/values
 * - Converting dates to standard format
 * - Validating phone numbers
 * - Tracking structural metrics
 */
class EventDataNormalizer(
    private val config: ValidationConfig
) : Normalizer<Map<*, *>?, EventDataNormalizationResult> {

    companion object {
        private const val DATE_PREFIX = "\$D_"
    }

    // Metrics tracking
    private var maxDepth = 0
    private var maxArrayKeyCount = 0
    private var maxObjectKeyCount = 0
    private var maxArrayLength = 0
    private var maxKVPairCount = 0

    // Modification tracking
    private val keysModified = mutableListOf<KeyModification>()
    private val valuesModified = mutableListOf<ValueModification>()
    private val itemsRemoved = mutableListOf<RemovedItem>()

    override fun normalize(input: Map<*, *>?): EventDataNormalizationResult {
        resetTracking()

        val cleanedData = when {
            input == null -> JSONObject()
            else -> {
                try {
                    cleanMapInternal(input, depth = 0)
                } catch (_: JSONException) {
                    JSONObject()
                }
            }
        }

        return EventDataNormalizationResult(
            cleanedData = cleanedData,
            metrics = buildMetrics()
        )
    }

    /**
     * Builds the metrics object from current tracking state.
     */
    private fun buildMetrics(): EventDataMetrics {
        return EventDataMetrics(
            maxDepth = maxDepth,
            maxArrayKeyCount = maxArrayKeyCount,
            maxObjectKeyCount = maxObjectKeyCount,
            maxArrayLength = maxArrayLength,
            maxKVPairCount = maxKVPairCount,
            keysModified = keysModified.toList(),
            valuesModified = valuesModified.toList(),
            itemsRemoved = itemsRemoved.toList()
        )
    }

    private fun resetTracking() {
        maxDepth = 0
        maxArrayKeyCount = 0
        maxObjectKeyCount = 0
        maxArrayLength = 0
        maxKVPairCount = 0
        keysModified.clear()
        valuesModified.clear()
        itemsRemoved.clear()
    }

    @Throws(JSONException::class)
    private fun cleanMapInternal(map: Map<*, *>, depth: Int): JSONObject {
        maxDepth = max(maxDepth, depth)

        val cleaned = JSONObject()
        var arrayKeyCount = 0
        var objectKeyCount = 0
        var kvPairCount = 0

        for ((key, value) in map) {
            if (key == null) {
                recordRemoval("null", RemovalReason.EMPTY_KEY, "")
                continue
            }

            val keyStr = key.toString()
            val cleanedKey = cleanKey(keyStr)

            if (cleanedKey.isEmpty()) {
                recordRemoval(keyStr, RemovalReason.EMPTY_KEY, "")
                continue
            }

            if (value == null) {
                recordRemoval(cleanedKey, RemovalReason.NULL_VALUE, null)
                continue
            }

            // Drop restricted multi-value fields at 0th level if value is object or array
            if (depth == 0 && config.restrictedMultiValueFields?.contains(cleanedKey) == true) {
                val isObjectOrArray = when (value) {
                    is Map<*, *>, is JSONObject, is List<*>, is Array<*>, is JSONArray -> true
                    else -> false
                }
                if (isObjectOrArray) {
                    recordRemoval(cleanedKey, RemovalReason.RESTRICTED_KEY_NESTED_VALUE, value)
                    continue
                }
            }

            // Special validation for Phone key
            if (cleanedKey.equals("Phone", ignoreCase = true)) {
                // only validate and record error, don't remove
                validatePhoneNumber(cleanedKey, value)
            }

            when (value) {
                is Map<*, *>, is JSONObject -> objectKeyCount++
                is List<*>, is Array<*>, is JSONArray -> arrayKeyCount++
            }

            val cleanedValue = cleanAnyValue(value, cleanedKey, depth + 1)

            if (cleanedValue != null) {
                cleaned.put(cleanedKey, cleanedValue)
                kvPairCount++
            }
        }

        maxArrayKeyCount = max(maxArrayKeyCount, arrayKeyCount)
        maxObjectKeyCount = max(maxObjectKeyCount, objectKeyCount)
        maxKVPairCount = max(maxKVPairCount, kvPairCount)

        return cleaned
    }

    private fun validatePhoneNumber(key: String, value: Any?) {
        if (value !is String) {
            recordRemoval(key, RemovalReason.INVALID_PHONE_NUMBER, value)
            return
        }

        val phoneValue = value.trim()

        // If no country code available, require phone to start with '+'
        if (config.deviceCountryCodeProvider().isNullOrEmpty()) {
            if (!phoneValue.startsWith("+")) {
                recordRemoval(key, RemovalReason.INVALID_COUNTRY_CODE, phoneValue)
            }
        }
    }

    @Throws(JSONException::class)
    private fun cleanAnyValue(value: Any?, parentKey: String, depth: Int): Any? {
        if (value == null || value == JSONObject.NULL) {
            return null
        }

        val cleaned = when (value) {
            is Map<*, *> -> cleanMapInternal(value, depth)
            is JSONObject -> cleanJSONObject(value, depth)
            is List<*> -> cleanList(value, parentKey, depth)
            is Array<*> -> cleanList(value.toList(), parentKey, depth)
            is JSONArray -> cleanJSONArray(value, parentKey, depth)
            else -> return cleanPrimitiveValue(value, parentKey)
        }

        return if (isEmpty(cleaned)) {
            recordRemoval(parentKey, RemovalReason.EMPTY_VALUE, "")
            null
        } else {
            cleaned
        }
    }

    private fun isEmpty(value: Any): Boolean {
        return when (value) {
            is JSONObject -> value.length() == 0
            is JSONArray -> value.length() == 0
            else -> false
        }
    }

    @Throws(JSONException::class)
    private fun cleanList(list: List<*>, parentKey: String, depth: Int): JSONArray {
        maxDepth = max(maxDepth, depth)
        maxArrayLength = max(maxArrayLength, list.size)

        val cleaned = JSONArray()

        for (value in list) {
            if (value == null) {
                recordRemoval(parentKey, RemovalReason.NULL_VALUE, null)
                continue
            }

            val cleanedValue = cleanAnyValue(value, parentKey, depth)
            if (cleanedValue != null) {
                cleaned.put(cleanedValue)
            }
        }

        return cleaned
    }

    @Throws(JSONException::class)
    private fun cleanJSONObject(json: JSONObject, depth: Int): JSONObject {
        maxDepth = max(maxDepth, depth)

        val cleaned = JSONObject()
        var arrayKeyCount = 0
        var objectKeyCount = 0
        var kvPairCount = 0

        val it = json.keys()
        while (it.hasNext()) {
            val key = it.next()
            val cleanedKey = cleanKey(key)

            if (cleanedKey.isEmpty()) {
                recordRemoval(key, RemovalReason.EMPTY_VALUE, "")
                continue
            }

            val value = json.get(key)

            if (value == null || value == JSONObject.NULL) {
                recordRemoval(cleanedKey, RemovalReason.NULL_VALUE, "")
                continue
            }

            when (value) {
                is JSONObject -> objectKeyCount++
                is JSONArray -> arrayKeyCount++
            }

            val cleanedValue = cleanAnyValue(value, cleanedKey, depth + 1)

            if (cleanedValue != null) {
                cleaned.put(cleanedKey, cleanedValue)
                kvPairCount++
            }
        }

        maxArrayKeyCount = max(maxArrayKeyCount, arrayKeyCount)
        maxObjectKeyCount = max(maxObjectKeyCount, objectKeyCount)
        maxKVPairCount = max(maxKVPairCount, kvPairCount)

        return cleaned
    }

    @Throws(JSONException::class)
    private fun cleanJSONArray(array: JSONArray, parentKey: String, depth: Int): JSONArray {
        maxDepth = max(maxDepth, depth)
        maxArrayLength = max(maxArrayLength, array.length())

        val cleaned = JSONArray()

        for (i in 0 until array.length()) {
            val value = array.get(i)

            if (value == null || value == JSONObject.NULL) {
                recordRemoval(parentKey, RemovalReason.NULL_VALUE, null)
                continue
            }

            val cleanedValue = cleanAnyValue(value, parentKey, depth)
            if (cleanedValue != null) {
                cleaned.put(cleanedValue)
            }
        }

        return cleaned
    }

    private fun cleanKey(key: String): String {
        val original = key
        var cleaned = key.trim()

        if (cleaned.isEmpty()) {
            return ""
        }

        val reasons = mutableListOf<ModificationReason>()

        // Remove disallowed characters
        config.keyCharsNotAllowed?.let { notAllowed ->
            val filtered = cleaned.filterNot { it in notAllowed }
            if (filtered != cleaned) {
                cleaned = filtered
                reasons.add(ModificationReason.INVALID_CHARACTERS_REMOVED)
            }
        }

        // Truncate if exceeds max length
        config.maxKeyLength?.let { maxLength ->
            if (cleaned.length > maxLength) {
                reasons.add(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
                cleaned = cleaned.substring(0, maxLength)
            }
        }

        val result = cleaned.trim()

        // Record modification if key changed
        if (result != original && reasons.isNotEmpty()) {
            keysModified.add(
                KeyModification(
                    originalKey = original,
                    cleanedKey = result,
                    reasons = reasons
                )
            )
        }

        return result
    }

    private fun cleanPrimitiveValue(value: Any?, key: String): Any? {
        return when (value) {
            is Int, is Long, is Float, is Double, is Boolean -> value
            is String -> cleanStringValue(value, key)
            is Char -> cleanPrimitiveValue(value.toString(), key)
            is Date -> "$DATE_PREFIX${value.time / 1000}"
            else -> {
                recordRemoval(key, RemovalReason.NON_PRIMITIVE_VALUE, value)
                null
            }
        }
    }

    private fun cleanStringValue(value: String, key: String): String? {
        val original = value
        var cleaned = value.trim()

        if (cleaned.isEmpty()) {
            recordRemoval(key, RemovalReason.EMPTY_VALUE, "")
            return null
        }

        val reasons = mutableListOf<ModificationReason>()

        // Remove disallowed characters
        config.valueCharsNotAllowed?.let { notAllowed ->
            val filtered = cleaned.filterNot { it in notAllowed }
            if (filtered != cleaned) {
                cleaned = filtered
                reasons.add(ModificationReason.INVALID_CHARACTERS_REMOVED)
            }
        }

        // Truncate if exceeds max length
        config.maxValueLength?.let { maxLength ->
            if (cleaned.length > maxLength) {
                reasons.add(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
                cleaned = cleaned.substring(0, maxLength)
            }
        }

        val result = cleaned.trim()

        // Record modification
        if (result != original && reasons.isNotEmpty()) {
            valuesModified.add(
                ValueModification(
                    key = key,
                    originalValue = original,
                    cleanedValue = result,
                    reasons = reasons
                )
            )
        }

        if (result.isEmpty()) {
            recordRemoval(key, RemovalReason.EMPTY_VALUE, "")
            return null
        }

        return result
    }

    private fun recordRemoval(key: String, reason: RemovalReason, originalValue: Any?) {
        itemsRemoved.add(RemovedItem(key, reason, originalValue))
    }
}