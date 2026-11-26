package com.clevertap.android.sdk.cryption

/**
 * Encryption levels indicating the degree of security.
 */
public enum class EncryptionLevel(private val value: Int) {
    NONE(0),    // No encryption
    MEDIUM(1),  // Medium level encryption
    FULL_DATA(2); // Encrypts full data rather than partial PII keys

    /**
     * Returns the integer stored for this level: 0 for [NONE], 1 for [MEDIUM], 2 for [FULL_DATA].
     *
     * @return the integer value of this level
     * @see fromInt
     */
    public fun intValue(): Int = value

    /**
     * Returns whether this level encrypts anything at all.
     *
     * @return `true` for [MEDIUM] and [FULL_DATA], `false` for [NONE]
     */
    public fun shouldEncrypt() : Boolean = value > 0

    public companion object {
        /**
         * Maps an integer (as returned by [intValue]) back to its level.
         *
         * @param value the integer value of a level
         * @return the matching level, or [NONE] when [value] matches no level
         */
        @JvmStatic
        public fun fromInt(value: Int): EncryptionLevel {
            return EncryptionLevel.values().firstOrNull { it.value == value } ?: NONE
        }
    }
}
