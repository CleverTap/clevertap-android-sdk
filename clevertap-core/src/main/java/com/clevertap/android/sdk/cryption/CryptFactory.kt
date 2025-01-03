package com.clevertap.android.sdk.cryption

import android.content.Context
import com.clevertap.android.sdk.cryption.CryptHandler.EncryptionAlgorithm

/**
 * This class is a factory class to generate a Crypt object based on the EncryptionAlgorithm
 */
internal class CryptFactory(
    private val context: Context,
    private val accountId: String
) {

    // Cache to hold instances of Crypt for different encryption algorithms.
    private val cryptInstances: MutableMap<EncryptionAlgorithm, Crypt> = mutableMapOf()

    companion object {
        @JvmStatic
        fun getCrypt(type: EncryptionAlgorithm, accountID: String, context: Context): Crypt {
            return when (type) {
                EncryptionAlgorithm.AES -> AESCrypt(accountID)
                EncryptionAlgorithm.AES_GCM -> AESGCMCrypt(context)
            }
        }
    }

    /**
     * Retrieves or creates a Crypt instance for the specified algorithm.
     *
     * @param algorithm - The encryption algorithm to use.
     * @return The Crypt instance for the specified algorithm.
     */
    fun getCryptInstance(algorithm: EncryptionAlgorithm): Crypt {
        return cryptInstances.getOrPut(algorithm) { getCrypt(algorithm, accountId, context) }
    }
}