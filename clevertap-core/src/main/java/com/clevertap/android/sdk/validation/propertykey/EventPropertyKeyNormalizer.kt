package com.clevertap.android.sdk.validation.propertykey

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.pipeline.KeyModification
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import com.clevertap.android.sdk.validation.pipeline.Normalizer
import com.clevertap.android.sdk.validation.pipeline.PropertyKeyNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.RemovalReason

/**
 * Normalizes event property keys according to ValidationConfig.
 * Only performs normalization - does not validate.
 *
 * Normalization includes:
 * - Removing disallowed characters
 * - Truncating to maximum length
 * - Trimming whitespace
 * - Detecting if key becomes empty after cleaning
 */
class EventPropertyKeyNormalizer : Normalizer<String?, PropertyKeyNormalizationResult> {

    override fun normalize(input: String?, config: ValidationConfig): PropertyKeyNormalizationResult {
        val original = input?: ""
        var cleaned = input?.trim() ?: ""
        val modifications = mutableSetOf<KeyModification>()

        if (cleaned.isEmpty()) {
            return PropertyKeyNormalizationResult(
                originalKey = original,
                cleanedKey = "",
                modifications = emptySet(),
                wasRemoved = true,
                removalReason = RemovalReason.EMPTY_KEY
            )
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

        // Check if key became empty after cleaning
        if (result.isEmpty()) {
            return PropertyKeyNormalizationResult(
                originalKey = original,
                cleanedKey = "",
                modifications = emptySet(),
                wasRemoved = true,
                removalReason = RemovalReason.EMPTY_KEY
            )
        }

        // Record modifications
        if (result != original && reasons.isNotEmpty()) {
            modifications.add(
                KeyModification(
                    originalKey = original,
                    cleanedKey = result,
                    reasons = reasons
                )
            )
        }

        return PropertyKeyNormalizationResult(
            originalKey = original,
            cleanedKey = result,
            modifications = modifications,
            wasRemoved = false,
            removalReason = null
        )
    }
}