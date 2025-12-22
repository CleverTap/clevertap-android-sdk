package com.clevertap.android.sdk.validation.eventname

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.pipeline.EventNameNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import com.clevertap.android.sdk.validation.pipeline.Normalizer

/**
 * Normalizes event names according to ValidationConfig.
 * Only performs normalization - does not validate.
 *
 * Normalization includes:
 * - Removing disallowed characters
 * - Truncating to maximum length
 * - Trimming whitespace
 */
class EventNameNormalizer : Normalizer<String?, EventNameNormalizationResult> {

    override fun normalize(input: String?, config: ValidationConfig): EventNameNormalizationResult {
        if (input == null) {
            return EventNameNormalizationResult(
                originalName = null,
                cleanedName = "",
                modifications = emptySet()
            )
        }

        val original = input
        var cleaned = input.trim()
        val modifications = mutableSetOf<ModificationReason>()

        // Remove disallowed characters
        config.eventNameCharsNotAllowed?.let { notAllowed ->
            val filtered = cleaned.filterNot { it in notAllowed }
            if (filtered != cleaned) {
                cleaned = filtered
                modifications.add(ModificationReason.INVALID_CHARACTERS_REMOVED)
            }
        }

        // Truncate if exceeds max length
        config.maxEventNameLength?.let { maxLength ->
            if (cleaned.length > maxLength) {
                modifications.add(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
                cleaned = cleaned.substring(0, maxLength)
            }
        }

        val result = cleaned.trim()

        return EventNameNormalizationResult(
            originalName = original,
            cleanedName = result,
            modifications = modifications
        )
    }
}