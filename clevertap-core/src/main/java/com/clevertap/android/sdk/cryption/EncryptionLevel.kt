package com.clevertap.android.sdk.cryption

/**
 * Encryption levels indicating the degree of security.
 */
enum class EncryptionLevel(private val value: Int) {
    NONE(0),    // No encryption
    MEDIUM(1);  // Medium level encryption

    fun intValue(): Int = value

    companion object {
        @JvmStatic
        fun fromInt(value: Int): EncryptionLevel {
            return EncryptionLevel.values().firstOrNull { it.value == value } ?: NONE
        }
    }
}