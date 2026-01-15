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
class EventNameValidator : Validator<EventNameNormalizationResult> {

    override fun validate(input: EventNameNormalizationResult, config: ValidationConfig): ValidationOutcome {
        val errors = mutableListOf<ValidationResult>()
        if (input.originalName == null) {
            val error = ValidationResultFactory.create(ValidationError.EVENT_NAME_NULL)
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.NULL_EVENT_NAME
            )
        }

        // Check for modifications during normalization before empty check so that modifications are recorded
        validateModifications(
            modifications = input.modifications,
            originalName = input.originalName,
            cleanedName = input.cleanedName,
            maxEventNameLength = config.maxEventNameLength,
            errors = errors
        )

        // Check for empty cleaned name (became empty after normalization)
        if (input.cleanedName.isEmpty()) {
            val error = ValidationResultFactory.create(ValidationError.EVENT_NAME_NULL)
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.NULL_EVENT_NAME
            )
        }

        // Check restricted event names
        if (isRestricted(input.cleanedName, config.restrictedEventNames)) {
            val error = ValidationResultFactory.create(
                ValidationError.RESTRICTED_EVENT_NAME,
                input.cleanedName
            )
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.RESTRICTED_EVENT_NAME
            )
        }

        // Check discarded event names
        if (isDiscarded(input.cleanedName, config.discardedEventNames)) {
            val error = ValidationResultFactory.create(
                ValidationError.DISCARDED_EVENT_NAME,
                input.cleanedName
            )
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.DISCARDED_EVENT_NAME
            )
        }

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
        originalName: String,
        cleanedName: String,
        maxEventNameLength: Int?,
        errors: MutableList<ValidationResult>
    ) {
        modifications.forEach { modification ->
            when (modification) {
                ModificationReason.TRUNCATED_TO_MAX_LENGTH -> {
                    val error = ValidationResultFactory.create(
                        ValidationError.EVENT_NAME_TOO_LONG,
                        originalName,
                        maxEventNameLength?.toString() ?: "unknown",
                        cleanedName
                    )
                    errors.add(error)
                }
                ModificationReason.INVALID_CHARACTERS_REMOVED -> {
                    val error = ValidationResultFactory.create(
                        ValidationError.EVENT_NAME_INVALID_CHARACTERS,
                        originalName,
                        cleanedName
                    )
                    errors.add(error)
                }
            }
        }
    }

    /**
     * Checks if the event name is in the restricted list.
     */
    private fun isRestricted(
        cleanedName: String,
        restrictedEventNames: Set<String>?
    ): Boolean {
        return restrictedEventNames?.any { restrictedName ->
            Utils.areNamesNormalizedEqual(cleanedName, restrictedName)
        } ?: false
    }

    /**
     * Checks if the event name is in the discarded list.
     */
    private fun isDiscarded(
        cleanedName: String,
        discardedEventNames: Set<String>?
    ): Boolean {
        return discardedEventNames?.any { discardedName ->
            Utils.areNamesNormalizedEqual(cleanedName, discardedName)
        } ?: false
    }
}