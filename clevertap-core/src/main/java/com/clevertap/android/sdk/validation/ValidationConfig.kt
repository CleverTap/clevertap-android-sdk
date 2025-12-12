package com.clevertap.android.sdk.validation

import com.clevertap.android.sdk.Constants

/**
 * Builder for configuring validation rules.
 * Use this to specify which validations should be performed and their limits.
 */
class ValidationConfig private constructor(
    val maxKeyLength: Int?,
    val maxValueLength: Int?,
    val maxDepth: Int?,
    val maxArrayKeyCount: Int?,
    val maxObjectKeyCount: Int?,
    val maxArrayLength: Int?,
    val maxKVPairCount: Int?,
    val keyCharsNotAllowed: Set<Char>?,
    val valueCharsNotAllowed: Set<Char>?,
    val maxEventNameLength: Int?,
    val eventNameCharsNotAllowed: Set<Char>?,
    val restrictedEventNames: Set<String>?,
    val restrictedMultiValueFields: Set<String>?,
    val deviceCountryCodeProvider: () -> String?
) {
    /**
     * Mutable list of discarded event names from Dashboard.
     * Can be updated at runtime without rebuilding the config.
     */
    var discardedEventNames: Set<String>? = null
        private set

    /**
     * Updates the list of discarded event names.
     * This can be called at runtime to sync with Dashboard settings.
     */
    fun updateDiscardedEventNames(names: Set<String>?) {
        this.discardedEventNames = names
    }

    class Builder {
        private var maxKeyLength: Int? = null
        private var maxValueLength: Int? = null
        private var maxDepth: Int? = null
        private var maxArrayKeyCount: Int? = null
        private var maxObjectKeyCount: Int? = null
        private var maxArrayLength: Int? = null
        private var maxKVPairCount: Int? = null
        private var keyCharsNotAllowed: Set<Char>? = null
        private var valueCharsNotAllowed: Set<Char>? = null
        private var maxEventNameLength: Int? = null
        private var eventNameCharsNotAllowed: Set<Char>? = null
        private var restrictedEventNames: Set<String>? = null
        private var restrictedMultiValueFields: Set<String>? = null
        private var discardedEventNames: Set<String>? = null
        private var deviceCountryCodeProvider: (() -> String?)? = null

        /**
         * Add validation for maximum key length.
         * Keys longer than this will be truncated.
         */
        fun addKeySizeValidation(maxLength: Int) = apply {
            this.maxKeyLength = maxLength
        }

        /**
         * Add validation for maximum value length.
         * String values longer than this will be truncated.
         */
        fun addValueSizeValidation(maxLength: Int) = apply {
            this.maxValueLength = maxLength
        }

        /**
         * Add validation for maximum nesting depth.
         */
        fun addDepthValidation(maxDepth: Int) = apply {
            this.maxDepth = maxDepth
        }

        /**
         * Add validation for maximum number of keys with array values at any level.
         */
        fun addArrayKeyCountValidation(maxCount: Int) = apply {
            this.maxArrayKeyCount = maxCount
        }

        /**
         * Add validation for maximum number of keys with object values at any level.
         */
        fun addObjectKeyCountValidation(maxCount: Int) = apply {
            this.maxObjectKeyCount = maxCount
        }

        /**
         * Add validation for maximum array length.
         */
        fun addArrayLengthValidation(maxLength: Int) = apply {
            this.maxArrayLength = maxLength
        }

        /**
         * Add validation for maximum key-value pairs at any level.
         */
        fun addKVPairCountValidation(maxCount: Int) = apply {
            this.maxKVPairCount = maxCount
        }

        /**
         * Add validation for disallowed characters in keys.
         * These characters will be removed from keys.
         */
        fun addKeyCharacterValidation(charsNotAllowed: Set<Char>) = apply {
            this.keyCharsNotAllowed = charsNotAllowed
        }

        /**
         * Add validation for disallowed characters in values.
         * These characters will be removed from string values.
         */
        fun addValueCharacterValidation(charsNotAllowed: Set<Char>) = apply {
            this.valueCharsNotAllowed = charsNotAllowed
        }

        /**
         * Add validation for maximum event name length.
         * Event names longer than this will be truncated.
         */
        fun addEventNameLengthValidation(maxLength: Int) = apply {
            this.maxEventNameLength = maxLength
        }

        /**
         * Add validation for disallowed characters in event names.
         * These characters will be removed from event names.
         */
        fun addEventNameCharacterValidation(charsNotAllowed: Set<Char>) = apply {
            this.eventNameCharsNotAllowed = charsNotAllowed
        }

        /**
         * Set the list of restricted event names that cannot be used.
         */
        fun setRestrictedEventNames(restrictedNames: Set<String>) = apply {
            this.restrictedEventNames = restrictedNames
        }

        /**
         * Set the list of restricted multi-value field names that cannot be used.
         */
        fun setRestrictedMultiValueFields(restrictedFields: Set<String>) = apply {
            this.restrictedMultiValueFields = restrictedFields
        }

        /**
         * Set the list of discarded event names from Dashboard.
         * Note: This can also be updated later using updateDiscardedEventNames().
         */
        fun setDiscardedEventNames(discardedNames: Set<String>) = apply {
            this.discardedEventNames = discardedNames
        }

        /**
         * Set the device country code provider.
         * This function will be called whenever the country code is needed.
         */
        fun setDeviceCountryCodeProvider(provider: () -> String?) = apply {
            this.deviceCountryCodeProvider = provider
        }

        /**
         * Build the validation configuration.
         */
        fun build(): ValidationConfig {
            val config = ValidationConfig(
                maxKeyLength = maxKeyLength,
                maxValueLength = maxValueLength,
                maxDepth = maxDepth,
                maxArrayKeyCount = maxArrayKeyCount,
                maxObjectKeyCount = maxObjectKeyCount,
                maxArrayLength = maxArrayLength,
                maxKVPairCount = maxKVPairCount,
                keyCharsNotAllowed = keyCharsNotAllowed,
                valueCharsNotAllowed = valueCharsNotAllowed,
                maxEventNameLength = maxEventNameLength,
                eventNameCharsNotAllowed = eventNameCharsNotAllowed,
                restrictedEventNames = restrictedEventNames,
                restrictedMultiValueFields = restrictedMultiValueFields,
                deviceCountryCodeProvider = deviceCountryCodeProvider ?: { null }
            )
            // Set discarded names after construction
            config.discardedEventNames = discardedEventNames
            return config
        }
    }

    companion object {

        @JvmField
        val DEFAULT_RESTRICTED_EVENT_NAMES = setOf(
            "Stayed",
            "Notification Clicked",
            "Notification Viewed",
            "UTM Visited",
            "Notification Sent",
            "App Launched",
            "wzrk_d",
            "App Uninstalled",
            "Notification Bounced",
            Constants.GEOFENCE_ENTERED_EVENT_NAME,
            Constants.GEOFENCE_EXITED_EVENT_NAME,
            Constants.SC_OUTGOING_EVENT_NAME,
            Constants.SC_INCOMING_EVENT_NAME,
            Constants.SC_END_EVENT_NAME,
            Constants.SC_CAMPAIGN_OPT_OUT_EVENT_NAME
        )

        /**
         * Default validation configuration with common CleverTap limits.
         */
        fun default(countryCodeProvider: (() -> String?)? = null): ValidationConfig {
            return Builder()
                .addKeySizeValidation(Constants.MAX_KEY_LENGTH)
                .addValueSizeValidation(512)
                .addDepthValidation(3)
                .addArrayKeyCountValidation(5)
                .addObjectKeyCountValidation(5)
                .addArrayLengthValidation(100)
                .addKVPairCountValidation(100)
                .addKeyCharacterValidation(setOf(':', '$', '\'', '"', '\\'))
                .addValueCharacterValidation(setOf('\'', '"', '\\'))
                .addEventNameLengthValidation(Constants.MAX_VALUE_LENGTH)
                .addEventNameCharacterValidation(setOf('.', ':', '$', '\'', '"', '\\'))
                .setRestrictedEventNames(DEFAULT_RESTRICTED_EVENT_NAMES)
                .setRestrictedMultiValueFields(
                    setOf(
                        "Name", "Email", "Education", "Married", "DOB",
                        "Gender", "Phone", "Age", "FBID", "GPID", "Birthday",
                        "Identity"
                    )
                )
                .apply {
                    if (countryCodeProvider != null) {
                        setDeviceCountryCodeProvider(countryCodeProvider)
                    }
                }
                .build()
        }
    }
}