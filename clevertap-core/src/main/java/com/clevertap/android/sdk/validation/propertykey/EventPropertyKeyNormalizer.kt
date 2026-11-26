package com.clevertap.android.sdk.validation.propertykey

import androidx.annotation.RestrictTo
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class EventPropertyKeyNormalizer : Normalizer<String?, PropertyKeyNormalizationResult> {

    override fun normalize(input: String?, config: ValidationConfig): PropertyKeyNormalizationResult {
        val original = input.orEmpty()
        var cleaned = input?.trim().orEmpty()
        val modifications = mutableSetOf<KeyModification>()

        if (cleaned.isEmpty()) {
            return removedKeyResult(original)
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
            return removedKeyResult(original)
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

    /** Result for a key that is empty, either as given or after cleaning. */
    private fun removedKeyResult(original: String): PropertyKeyNormalizationResult {
        return PropertyKeyNormalizationResult(
            originalKey = original,
            cleanedKey = "",
            modifications = emptySet(),
            wasRemoved = true,
            removalReason = RemovalReason.EMPTY_KEY
        )
    }
}
