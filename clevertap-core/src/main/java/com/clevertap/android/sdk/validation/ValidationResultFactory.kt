package com.clevertap.android.sdk.validation

import androidx.annotation.RestrictTo

/**
 * Factory for creating validation results with proper error codes and messages.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object ValidationResultFactory {

    /**
     * Creates a ValidationResult for the given error type.
     */
    @JvmStatic
    fun create(error: ValidationError, vararg values: String): ValidationResult {
        val result = ValidationResult()
        result.errorCode = error.code
        result.errorDesc = error.getMessage(*values)
        return result
    }
}

/**
 * Enum representing all validation errors with their codes and message templates.
 */
enum class ValidationError(val code: Int, private val messageTemplate: String) {
    
    // Event name validation errors (510)
    EVENT_NAME_NULL(510, "Event Name is null"),
    EVENT_NAME_TOO_LONG(510, "%s... exceeds the limit of %s characters. Trimmed"),
    EVENT_NAME_INVALID_CHARACTERS(510, "Key '%s' contains invalid characters. Cleaned"),

    // Profile validation errors (512)
    INVALID_MULTI_VALUE(512, "Invalid multi value for key %s, profile multi value operation aborted."),
    INVALID_INCREMENT_DECREMENT_VALUE(512, "Increment/Decrement value for profile key %s, cannot be zero or negative"),
    INVALID_COUNTRY_CODE(512, "Device country code not available and profile phone: %s does not appear to start with country code"),
    INVALID_PHONE(512, "Invalid phone number"),
    EMPTY_KEY(512, "Found an empty key. Skipping and continuing"),
    EMPTY_KEY_ABORT(512, "Found an empty key. Aborting the operation"),
    PROP_VALUE_NOT_PRIMITIVE(512, "Property value for property %s wasn't a primitive (%s)"),
    CHANNEL_ID_MISSING_IN_PAYLOAD(512, "ChannelId is required for API 26+ but not provided in the notification payload. Falling to default channel: %s"),
    CHANNEL_ID_NOT_REGISTERED(512, "Unable to render notification on channelId: %s as it is not registered by the app. Falling to default channel: "),
    NOTIFICATION_VIEWED_DISABLED(512, "Recording of Notification Viewed is disabled in the CleverTap Dashboard for notification payload: %s"),
    
    // Event name restrictions (513)
    RESTRICTED_EVENT_NAME(513, "%s is a restricted event name. Last event aborted."),
    DISCARDED_EVENT_NAME(513, "%s is a discarded event name. Last event aborted."),
    
    // Custom ID errors (514)
    USE_CUSTOM_ID_FALLBACK(514, "CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml/Instance Configuration. CleverTap SDK will create a fallback device ID"),
    USE_CUSTOM_ID_MISSING_IN_MANIFEST(514, "CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml. Custom CleverTap ID passed will not be used."),
    UNABLE_TO_SET_CT_CUSTOM_ID(514, "CleverTap ID - %s already exists. Unable to set custom CleverTap ID - %s"),
    INVALID_CT_CUSTOM_ID(514, "Attempted to set invalid custom CleverTap ID - %s, falling back to default error CleverTap ID - %s"),
    
    // Key validation errors (520)
    KEY_INVALID_CHARACTERS(520, "Key '%s' contains invalid characters. Cleaned"),
    KEY_LENGTH_EXCEEDED(520, "Key '%s' exceeds the limit of %s characters. Trimmed"),
    
    // Value validation errors (521)
    VALUE_CHARS_LIMIT_EXCEEDED(521, "%s... exceeds the limit of %s characters. Trimmed"),
    VALUE_INVALID_CHARACTERS(521, "Value for key '%s' contains invalid characters. Cleaned"),

    // Charged event errors (522)
    CHARGED_EVENT_TOO_MANY_ITEMS(522, "Charged event contained more than 50 items."),
    
    // Multi-value key errors (523)
    RESTRICTED_MULTI_VALUE_KEY(523, "%s... is a restricted key for multi-value properties. Operation aborted."),
    
    // Profile identifier errors (531)
    PROFILE_IDENTIFIERS_MISMATCH(531, "Profile Identifiers mismatch with the previously saved ones"),
    
    // Structural limit errors (540-544)
    DEPTH_LIMIT_EXCEEDED(541, "Event data exceeded maximum nesting depth. Depth: %s, Limit: %s"),
    ARRAY_KEY_COUNT_LIMIT_EXCEEDED(542, "Event data exceeded maximum array key count. Count: %s, Limit: %s"),
    OBJECT_KEY_COUNT_LIMIT_EXCEEDED(543, "Event data exceeded maximum object key count. Count: %s, Limit: %s"),
    ARRAY_LENGTH_LIMIT_EXCEEDED(543, "Event data exceeded maximum array length. Length: %s, Limit: %s"),
    KV_PAIR_COUNT_LIMIT_EXCEEDED(544, "Event data exceeded maximum key-value pair count. Count: %s, Limit: %s"),
    
    // Data removal errors (545)
    NULL_VALUE_REMOVED(545, "Null value for key '%s' was removed"),
    EMPTY_VALUE_REMOVED(545, "Empty value for key '%s' was removed");
    
    /**
     * Formats the error message with the provided values.
     */
    fun getMessage(vararg values: String): String {
        return if (values.isEmpty()) {
            messageTemplate
        } else {
            String.format(messageTemplate, *values)
        }
    }
}
