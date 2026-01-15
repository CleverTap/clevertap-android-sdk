package com.clevertap.android.sdk.validation

/**
 * Sealed interface representing the result of validation.
 * Provides a clear indication of whether the event should be dropped and why.
 */
sealed interface ValidationOutcome {
    /**
     * List of all validation errors found.
     */
    val errors: List<ValidationResult>
    
    /**
     * Validation passed with no critical errors.
     * Event can proceed with optional warnings.
     */
    data class Success(
        override val errors: List<ValidationResult> = emptyList()
    ) : ValidationOutcome
    
    /**
     * Validation failed with errors but event can still proceed.
     * Errors are logged but event is not dropped.
     */
    data class Warning(
        override val errors: List<ValidationResult>
    ) : ValidationOutcome
    
    /**
     * Validation failed critically and event should be dropped.
     * Contains the reason(s) why the event must be dropped.
     */
    data class Drop(
        override val errors: List<ValidationResult>,
        val reason: DropReason
    ) : ValidationOutcome
}

/**
 * Reasons why an event should be dropped.
 */
enum class DropReason {
    NULL_EVENT_NAME,
    RESTRICTED_EVENT_NAME,
    DISCARDED_EVENT_NAME,
    EMPTY_KEY,
    EMPTY_EVENT_DATA,
}
