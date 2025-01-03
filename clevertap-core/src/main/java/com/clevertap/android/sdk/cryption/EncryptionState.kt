package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_MIGRATION
import com.clevertap.android.sdk.cryption.CryptHandler.EncryptionAlgorithm

/**
 * Enum representing encryption states and their transition logic.
 */
internal enum class EncryptionState {
    ENCRYPTED_AES {
        override fun transitionTo(
            targetState: EncryptionState,
            data: String,
            cryptHandler: CryptHandler
        ): MigrationResult {
            val decrypted = cryptHandler.decrypt(data, KEY_ENCRYPTION_MIGRATION, EncryptionAlgorithm.AES)
            return when (targetState) {
                ENCRYPTED_AES_GCM -> {
                    val encrypted = decrypted?.let {
                        cryptHandler.encrypt(
                            it,
                            KEY_ENCRYPTION_MIGRATION,
                            EncryptionAlgorithm.AES_GCM
                        )
                    }
                    MigrationResult(encrypted ?: decrypted ?: data, encrypted != null)
                }

                PLAIN_TEXT -> MigrationResult(decrypted ?: data, decrypted != null)
                else -> throw IllegalArgumentException("Invalid transition from ENCRYPTED_AES to $targetState")
            }
        }
    },
    ENCRYPTED_AES_GCM {
        override fun transitionTo(
            targetState: EncryptionState,
            data: String,
            cryptHandler: CryptHandler
        ): MigrationResult {
            val decrypted = cryptHandler.decrypt(
                data,
                KEY_ENCRYPTION_MIGRATION,
                EncryptionAlgorithm.AES_GCM
            )
            return when (targetState) {
                PLAIN_TEXT -> MigrationResult(decrypted ?: data, decrypted != null)
                else -> throw IllegalArgumentException("Invalid transition from ENCRYPTED_AES_GCM to $targetState")
            }
        }
    },
    PLAIN_TEXT {
        override fun transitionTo(
            targetState: EncryptionState,
            data: String,
            cryptHandler: CryptHandler
        ): MigrationResult {
            return when (targetState) {
                ENCRYPTED_AES_GCM -> {
                    val encrypted = cryptHandler.encrypt(
                        data,
                        KEY_ENCRYPTION_MIGRATION,
                        EncryptionAlgorithm.AES_GCM
                    )
                    MigrationResult(encrypted ?: data, encrypted != null)
                }
                else -> throw IllegalArgumentException("Invalid transition from PLAIN_TEXT to $targetState")
            }
        }
    };

    abstract fun transitionTo(
        targetState: EncryptionState,
        data: String,
        cryptHandler: CryptHandler
    ): MigrationResult
}