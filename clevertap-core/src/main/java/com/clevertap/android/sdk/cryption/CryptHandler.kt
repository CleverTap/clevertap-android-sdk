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
    private val encryptionLevel: EncryptionLevel,
    private val accountID: String
) {

    // Cache to hold instances of Crypt for different encryption algorithms.
    private val cryptInstances: MutableMap<EncryptionAlgorithm, Crypt> = mutableMapOf()

    private var migrationFailureCount: Int = 0

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
        // Use AES_GCM algorithm by default.
        val crypt = getCryptInstance(algorithm)
        when (encryptionLevel) {
            EncryptionLevel.MEDIUM -> {
                // Encrypt only if the key is valid
                if (key in Constants.MEDIUM_CRYPT_KEYS) {
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
        val crypt = getCryptInstance(algorithm)
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
     * @param migrationSuccessful - Indicates if migration was successful
     */
    fun updateMigrationFailureCount(
        context: Context,
        migrationSuccessful: Boolean
    ) {
        if(!migrationSuccessful)
            migrationFailureCount += 1
        else
            migrationFailureCount = 0
        Logger.v(
            accountID,
            "Updating migrationFailureCount to $migrationFailureCount"
        )

        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(accountID, "encryptionMigrationFailureCount"),
            migrationFailureCount
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
        fun isTextAESEncrypted(plainText: String): Boolean {
            return plainText.startsWith('[') && plainText.endsWith(']')
        }

        // Determines if the text is AES_GCM encrypted.
        fun isTextAESGCMEncrypted(plainText: String): Boolean {
            return plainText.startsWith('<') && plainText.endsWith('>')
        }
    }
}
