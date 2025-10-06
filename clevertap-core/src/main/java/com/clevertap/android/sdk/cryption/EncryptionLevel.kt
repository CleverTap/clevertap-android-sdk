package com.clevertap.android.sdk.cryption

/**
 * Encryption levels indicating the degree of security.
 */
enum class EncryptionLevel(private val value: Int) {
    NONE(0),    // No encryption
    MEDIUM(1),  // Medium level encryption
    FULL_DATA(2); // Encrypts full data rather than partial PII keys

    fun intValue(): Int = value

    fun shouldEncrypt() : Boolean = value > 0

    companion object {
        @JvmStatic
        fun fromInt(value: Int): EncryptionLevel {
            return EncryptionLevel.values().firstOrNull { it.value == value } ?: NONE
        }
    }
}