package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.Constants.AES_GCM_SUFFIX
import com.clevertap.android.sdk.Constants.AES_GCM_PREFIX
import com.clevertap.android.sdk.Constants.AES_SUFFIX
import com.clevertap.android.sdk.Constants.AES_PREFIX

internal interface ICryptHandler {
    fun encryptSafe(plainText: String): String?

    fun decryptSafe(cipherText: String): String?

    fun encrypt(plainText: String): String?

    fun decrypt(cipherText: String): String?

    fun decryptWithAlgorithm(cipherText: String, algorithm: CryptHandler.EncryptionAlgorithm): String?

    fun updateMigrationFailureCount(migrationSuccessful: Boolean)
}
/**
 * Handles encryption and decryption for various encryption algorithms
 */
internal class CryptHandler constructor(
    private val repository: CryptRepository,
    private val cryptFactory: CryptFactory
) : ICryptHandler {

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
     * @return The encrypted text, or the original plain text if encryption is not required.
     */
    override fun encryptSafe(plainText: String): String? {

        if (isTextAESGCMEncrypted(plainText)) {
            return plainText
        }

        // Use AES_GCM algorithm by default.
        val crypt = cryptFactory.getCryptInstance(DEFAULT_ALGORITHM)
        return crypt.encryptInternal(plainText)
    }

    /**
     * Decrypts the given cipher text using the specified algorithm.
     *
     * @param cipherText - The text to decrypt.
     * @return The decrypted text, or the original cipher text if decryption is not required.
     */
    override fun decryptSafe(cipherText: String): String? {
        if (!isTextAESGCMEncrypted(cipherText)) {
            return cipherText
        }
        return cryptFactory.getCryptInstance(DEFAULT_ALGORITHM).decryptInternal(cipherText)
    }

    /**
     * Encrypts the given plain text without any checks
     *
     * @param plainText - The text to encrypt.
     * @return The encrypted text, or null if encryption fails.
     */
    override fun encrypt(plainText: String): String? {
        val crypt = cryptFactory.getCryptInstance(DEFAULT_ALGORITHM)
        return crypt.encryptInternal(plainText)
    }

    /**
     * Decrypts the given cipher text without any checks.
     *
     * @param cipherText - The text to decrypt.
     * @return The decrypted text, or null if decryption fails.
     */
    override fun decrypt(cipherText: String): String? {
        val crypt = cryptFactory.getCryptInstance(DEFAULT_ALGORITHM)
        return crypt.decryptInternal(cipherText)
    }

    override fun decryptWithAlgorithm(cipherText: String, algorithm: EncryptionAlgorithm): String? {
        val crypt = cryptFactory.getCryptInstance(algorithm)
        return crypt.decryptInternal(cipherText)
    }

    /**
     * Updates the encryption state in case of failure while processing new data.
     *
     * @param migrationSuccessful - Indicates if migration was successful
     */
    // todo remove this method and use repo in use case
    override fun updateMigrationFailureCount(migrationSuccessful: Boolean) {
        repository.updateMigrationFailureCount(migrationSuccessful)
    }

    companion object {

        /**
         * Default Algorithm used for encryption on SDK
         */
        val DEFAULT_ALGORITHM = EncryptionAlgorithm.AES_GCM

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
