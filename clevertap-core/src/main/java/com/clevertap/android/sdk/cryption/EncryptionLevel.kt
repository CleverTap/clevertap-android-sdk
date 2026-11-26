package com.clevertap.android.sdk.cryption

/**
 * Encryption levels indicating the degree of security.
 */
public enum class EncryptionLevel(private val value: Int) {
    NONE(0),    // No encryption
    MEDIUM(1),  // Medium level encryption
    FULL_DATA(2); // Encrypts full data rather than partial PII keys

    public fun intValue(): Int = value

    public fun shouldEncrypt() : Boolean = value > 0

    public companion object {
        @JvmStatic
        public fun fromInt(value: Int): EncryptionLevel {
            return EncryptionLevel.values().firstOrNull { it.value == value } ?: NONE
        }
    }
}
