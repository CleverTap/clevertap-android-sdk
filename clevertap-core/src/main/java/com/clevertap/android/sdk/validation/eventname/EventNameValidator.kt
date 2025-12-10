package com.clevertap.android.sdk.validation.eventname

import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.validation.DropReason
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultFactory
import com.clevertap.android.sdk.validation.pipeline.EventNameNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import com.clevertap.android.sdk.validation.pipeline.Validator

/**
 * Validates event names after normalization.
 * Checks for null names, truncation, character modifications, and restrictions.
 */
class EventNameValidator(
    private val config: ValidationConfig
) : Validator<EventNameNormalizationResult> {

    override fun validate(input: EventNameNormalizationResult): ValidationOutcome {
        val errors = mutableListOf<ValidationResult>()
        if (input.originalName == null) {
            val error = ValidationResultFactory.create(ValidationError.EVENT_NAME_NULL)
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.NULL_EVENT_NAME
            )
        }

        // Check for empty cleaned name (became empty after normalization)
        if (input.cleanedName.isNullOrEmpty()) {
            val error = ValidationResultFactory.create(ValidationError.EVENT_NAME_NULL)
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.NULL_EVENT_NAME
            )
        }

        // Check restricted event names
        validateNotRestricted(input.cleanedName, errors)?.let { return it }

        // Check discarded event names
        validateNotDiscarded(input.cleanedName, errors)?.let { return it }

        // Check for modifications during normalization
        validateModifications(input.metrics.modifications, input.originalName, errors)

        return if (errors.isEmpty()) {
            ValidationOutcome.Success()
        } else {
            ValidationOutcome.Warning(errors)
        }
    }

    /**
     * Validates and reports any modifications made during normalization.
     * Adds warnings for truncation and character removal.
     */
    private fun validateModifications(
        modifications: Set<ModificationReason>,
        originalName : String,
        errors: MutableList<ValidationResult>
    ) {
        modifications.forEach { modification ->
            when (modification) {
                ModificationReason.TRUNCATED_TO_MAX_LENGTH -> {
                    val error = ValidationResultFactory.create(
                        ValidationError.EVENT_NAME_TOO_LONG,
                        originalName,
                        config.maxEventNameLength?.toString() ?: "unknown"
                    )
                    errors.add(error)
                }
                ModificationReason.INVALID_CHARACTERS_REMOVED -> {
                    val error = ValidationResultFactory.create(
                        ValidationError.EVENT_NAME_INVALID_CHARACTERS,
                        originalName
                    )
                    errors.add(error)
                }
            }
        }
    }

    /**
     * Validates that the event name is not in the restricted list.
     * Returns Drop outcome if name is restricted, null otherwise.
     */
    private fun validateNotRestricted(
        cleanedName: String,
        errors: MutableList<ValidationResult>
    ): ValidationOutcome.Drop? {
        val isRestricted = config.restrictedEventNames?.any { restrictedName ->
            Utils.areNamesNormalizedEqual(cleanedName, restrictedName)
        } ?: false

        if (isRestricted) {
            val error = ValidationResultFactory.create(
                ValidationError.RESTRICTED_EVENT_NAME,
                cleanedName
            )
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.RESTRICTED_EVENT_NAME
            )
        }

        return null
    }

    /**
     * Validates that the event name is not in the discarded list.
     * Returns Drop outcome if name is discarded, null otherwise.
     */
    private fun validateNotDiscarded(
        cleanedName: String,
        errors: MutableList<ValidationResult>
    ): ValidationOutcome.Drop? {
        val isDiscarded = config.discardedEventNames?.any { discardedName ->
            Utils.areNamesNormalizedEqual(cleanedName, discardedName)
        } ?: false

        if (isDiscarded) {
            val error = ValidationResultFactory.create(
                ValidationError.DISCARDED_EVENT_NAME,
                cleanedName
            )
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.DISCARDED_EVENT_NAME
            )
        }

        return null
    }
}