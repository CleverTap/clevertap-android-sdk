package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.AES_GCM_SUFFIX
import com.clevertap.android.sdk.Constants.AES_GCM_PREFIX
import com.clevertap.android.sdk.Constants.AES_SUFFIX
import com.clevertap.android.sdk.Constants.AES_PREFIX

/**
 * Handles encryption and decryption for various encryption algorithms and levels.
 *
 * @param encryptionLevel - The encryption level to use.
 * @param accountID - The account ID for which the cryptographic operations are performed.
 */
internal class CryptHandler constructor(
    private val encryptionLevel: EncryptionLevel,
    private val accountID: String,
    private val repository: CryptRepository,
    private val cryptFactory: CryptFactory
) {

    /**
     * Supported encryption algorithms.
     */
    enum class EncryptionAlgorithm(val value: Int) {
        AES(0),
        AES_GCM(1);
    }

    /**
     * Encrypts the given plain text using a specific key and the AES_GCM algorithm by default.
     *
     * @param plainText - The text to encrypt.
     * @param key - The key used for encryption.
     * @return The encrypted text, or the original plain text if encryption is not required.
     */
    @JvmOverloads
    fun encrypt(
        plainText: String,
        key: String,
        algorithm: EncryptionAlgorithm = EncryptionAlgorithm.AES_GCM
    ): String? {

        if (isTextEncrypted(plainText)) {
            return plainText
        }

        // Use AES_GCM algorithm by default.
        val crypt = cryptFactory.getCryptInstance(algorithm)
        when (encryptionLevel) {
            EncryptionLevel.MEDIUM -> {
                // Encrypt only if the key is valid
                if (key in Constants.MEDIUM_CRYPT_KEYS) {
                    return crypt.encryptInternal(plainText)
                }
            }
            else -> {
                return plainText
            }
        }
        return plainText
    }

    /**
     * Decrypts the given cipher text using the specified algorithm.
     *
     * @param cipherText - The text to decrypt.
     * @param key - The key used for decryption.
     * @param algorithm - The encryption algorithm to use (default is AES_GCM).
     * @return The decrypted text, or the original cipher text if decryption is not required.
     */
    @JvmOverloads
    fun decrypt(
        cipherText: String,
        key: String,
        algorithm: EncryptionAlgorithm = EncryptionAlgorithm.AES_GCM
    ): String? {
        if (!isTextEncrypted(cipherText)) {
            return cipherText
        }

        val crypt = cryptFactory.getCryptInstance(algorithm)
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
     * Encrypts the given plain text without any checks
     *
     * @param plainText - The text to encrypt.
     * @return The encrypted text, or null if encryption fails.
     */
    fun encrypt(
        plainText: String,
        algorithm: EncryptionAlgorithm = EncryptionAlgorithm.AES_GCM
    ): String? {
        val crypt = cryptFactory.getCryptInstance(algorithm)
        return crypt.encryptInternal(plainText)
    }

    /**
     * Decrypts the given cipher text without any checks.
     *
     * @param cipherText - The text to decrypt.
     * @return The decrypted text, or null if decryption fails.
     */
    @JvmOverloads
    fun decrypt(
        cipherText: String,
        algorithm: EncryptionAlgorithm = EncryptionAlgorithm.AES_GCM
    ): String? {
        val crypt = cryptFactory.getCryptInstance(algorithm)
        return crypt.decryptInternal(cipherText)
    }

    /**
     * Updates the encryption state in case of failure while processing new data.
     *
     * @param migrationSuccessful - Indicates if migration was successful
     */
    fun updateMigrationFailureCount(migrationSuccessful: Boolean) {
        repository.updateMigrationFailureCount(migrationSuccessful)
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
        fun isTextAESEncrypted(plainText: String): Boolean {
            return plainText.startsWith(AES_PREFIX) && plainText.endsWith(AES_SUFFIX)
        }

        // Determines if the text is AES_GCM encrypted.
        fun isTextAESGCMEncrypted(plainText: String): Boolean {
            return plainText.startsWith(AES_GCM_PREFIX) && plainText.endsWith(AES_GCM_SUFFIX)
        }
    }
}
