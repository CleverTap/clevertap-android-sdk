package com.clevertap.android.sdk.cryption

import android.content.Context
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.StorageHelper

/**
 * Handles encryption and decryption for various encryption algorithms and levels.
 *
 * @param encryptionLevel - The encryption level to use.
 * @param accountID - The account ID for which the cryptographic operations are performed.
 */
class CryptHandler(
    encryptionLevel: Int,
    private val accountID: String
) {
    private val encryptionLevel: EncryptionLevel

    // Cache to hold instances of Crypt for different encryption algorithms.
    private val cryptInstances: MutableMap<EncryptionAlgorithm, Crypt> = mutableMapOf()

    lateinit var currentEncryptionState: MutableMap<String, EncryptionDataState>

    /**
     * Supported encryption algorithms.
     */
    enum class EncryptionAlgorithm(val value: Int) {
        AES(0),
        AES_GCM(1);
    }

    /**
     * Encryption levels indicating the degree of security.
     */
    enum class EncryptionLevel(private val value: Int) {
        NONE(0),    // No encryption
        MEDIUM(1);  // Medium level encryption

        fun intValue(): Int = value
    }

    /** Enum for encryption states */
    enum class EncryptionDataState(val state: Int) {
        ENCRYPTED_AES(0b00),
        PLAIN_TEXT(0b01),
        ENCRYPTED_AES_GCM(0b11);

        companion object {
            fun fromState(stateValue: Int): EncryptionDataState {
                return values().first { it.state == stateValue }
            }
        }
    }


    init {
        this.encryptionLevel = EncryptionLevel.values()[encryptionLevel]
    }

    /**
     * Encrypts the given plain text using a specific key and the AES_GCM algorithm by default.
     *
     * @param plainText - The text to encrypt.
     * @param key - The key used for encryption.
     * @return The encrypted text, or the original plain text if encryption is not required.
     */
    fun encrypt(
        plainText: String,
        key: String,
    ): String? {
        // Use AES_GCM algorithm by default.
        val crypt = getCryptInstance(EncryptionAlgorithm.AES_GCM)
        when (encryptionLevel) {
            EncryptionLevel.MEDIUM -> {
                // Encrypt only if the key is valid and the text is not already encrypted.
                if (key in Constants.MEDIUM_CRYPT_KEYS && !isTextEncrypted(plainText)) {
                    return crypt.encryptInternal(plainText)
                }
            }
            else -> return plainText
        }
        return plainText
    }

    /**
     * Decrypts the given cipher text using the specified algorithm.
     *
     * @param cipherText - The text to decrypt.
     * @param key - The key used for decryption.
     * @param algorithm - The encryption algorithm to use (default is AES_GCM).
     * @return The decrypted text, or the original cipher text if decryption fails.
     */
    @JvmOverloads
    fun decrypt(
        cipherText: String,
        key: String,
        algorithm: EncryptionAlgorithm = EncryptionAlgorithm.AES_GCM
    ): String? {
        // Determine the appropriate Crypt instance based on the cipher text's format.
        val crypt = if (isTextAESEncrypted(cipherText)) {
            getCryptInstance(EncryptionAlgorithm.AES)
        } else if (isTextAESGCMEncrypted(cipherText)) {
            getCryptInstance(EncryptionAlgorithm.AES_GCM)
        } else {
            return cipherText
        }

        when (encryptionLevel) {
            EncryptionLevel.MEDIUM -> {
                // Decrypt only if the key is valid.
                if (key in Constants.MEDIUM_CRYPT_KEYS) {
                    return crypt.decryptInternal(cipherText)
                }
            }
            else -> {
                return crypt.decryptInternal(cipherText)
            }
        }
        return cipherText
    }

    /**
     * Encrypts the given plain text using the default AES_GCM algorithm.
     *
     * @param plainText - The text to encrypt.
     * @return The encrypted text, or null if encryption fails.
     */
    fun encrypt(plainText: String): String? {
        val crypt = getCryptInstance(EncryptionAlgorithm.AES_GCM)
        return crypt.encryptInternal(plainText)
    }

    /**
     * Decrypts the given cipher text using the default AES_GCM algorithm.
     *
     * @param cipherText - The text to decrypt.
     * @return The decrypted text, or null if decryption fails.
     */
    fun decrypt(cipherText: String): String? {
        val crypt = getCryptInstance(EncryptionAlgorithm.AES_GCM)
        return crypt.decryptInternal(cipherText)
    }

    /**
     * Retrieves or creates a Crypt instance for the specified algorithm.
     *
     * @param algorithm - The encryption algorithm to use.
     * @return The Crypt instance for the specified algorithm.
     */
    private fun getCryptInstance(algorithm: EncryptionAlgorithm): Crypt {
        return cryptInstances.getOrPut(algorithm) { CryptFactory.getCrypt(algorithm, accountID) }
    }

    /**
     * Updates the encryption state in case of failure while processing new data.
     *
     * @param context - The application context.
     * @param failedKey - The key for which encryption failed.
     */
    fun updateEncryptionStateOnFailure(
        context: Context,
        failedKey: String,
    ) {
        val currentState = currentEncryptionState[failedKey]
        val updatedState = (0b10 xor currentState!!.state) and currentState.state

        currentEncryptionState[failedKey] = EncryptionDataState.fromState(updatedState)
        Logger.v(
            accountID,
            "Updating encryption flag status after error for $failedKey to $updatedState"
        )
        val serializedMap = currentEncryptionState.entries.joinToString(",") { "${it.key}:${it.value}" }

        StorageHelper.putString(
            context,
            StorageHelper.storageKeyWithSuffix(accountID, "currentEncryptionState"),
            serializedMap
        )
    }

    companion object {
        /**
         * Checks if the given text is encrypted (either using AES or AES_GCM).
         *
         * @param plainText - The text to check.
         * @return True if the text is encrypted; false otherwise.
         */
        @JvmStatic
        fun isTextEncrypted(plainText: String): Boolean {
            return isTextAESEncrypted(plainText) || isTextAESGCMEncrypted(plainText)
        }

        // Determines if the text is AES encrypted.
        private fun isTextAESEncrypted(plainText: String): Boolean {
            return plainText.startsWith('[') && plainText.endsWith(']')
        }

        // Determines if the text is AES_GCM encrypted.
        private fun isTextAESGCMEncrypted(plainText: String): Boolean {
            return plainText.startsWith('<') && plainText.endsWith('>')
        }
    }
}
