package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.Constants

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
            val decrypted = cryptHandler.decrypt(
                cipherText = data,
                key = Constants.KEY_ENCRYPTION_MIGRATION,
                algorithm = CryptHandler.EncryptionAlgorithm.AES
            )
            return when (targetState) {
                ENCRYPTED_AES_GCM -> {
                    val encrypted = decrypted?.let {
                        cryptHandler.encrypt(
                            plainText = it,
                            key = Constants.KEY_ENCRYPTION_MIGRATION,
                            algorithm = CryptHandler.EncryptionAlgorithm.AES_GCM
                        )
                    }
                    MigrationResult(
                        data = encrypted ?: decrypted ?: data,
                        migrationSuccessful = encrypted != null
                    )
                }
                PLAIN_TEXT -> MigrationResult(
                    data = decrypted ?: data,
                    migrationSuccessful = decrypted != null
                )
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
                cipherText = data,
                key = Constants.KEY_ENCRYPTION_MIGRATION,
                algorithm = CryptHandler.EncryptionAlgorithm.AES_GCM
            )
            return when (targetState) {
                PLAIN_TEXT -> MigrationResult(
                    data = decrypted ?: data,
                    migrationSuccessful = decrypted != null
                )
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
                        plainText = data,
                        key = Constants.KEY_ENCRYPTION_MIGRATION,
                        algorithm = CryptHandler.EncryptionAlgorithm.AES_GCM
                    )
                    MigrationResult(
                        data = encrypted ?: data,
                        migrationSuccessful = encrypted != null
                    )
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