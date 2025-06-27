package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.cryption.CryptHandler.EncryptionAlgorithm

/**
 * This class is a factory class to generate a Crypt object based on the EncryptionAlgorithm
 */
internal class CryptFactory(
    private val accountId: String,
    private val ctKeyGenerator: CTKeyGenerator
) {

    // Cache to hold instances of Crypt for different encryption algorithms.
    private val cryptInstances: MutableMap<EncryptionAlgorithm, Crypt> = mutableMapOf()

    companion object {
        @JvmStatic
        fun getCrypt(
            type: EncryptionAlgorithm,
            accountID: String,
            ctKeyGenerator: CTKeyGenerator
        ): Crypt {
            return when (type) {
                EncryptionAlgorithm.AES -> AESCrypt(accountID)
                EncryptionAlgorithm.AES_GCM -> AESGCMCrypt(ctKeyGenerator = ctKeyGenerator)
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
        return cryptInstances.getOrPut(algorithm) { getCrypt(algorithm, accountId, ctKeyGenerator) }
    }

    fun getAesGcmCrypt() : AESGCMCrypt {
        return cryptInstances.getOrPut(EncryptionAlgorithm.AES_GCM) { getCrypt(EncryptionAlgorithm.AES_GCM, accountId, ctKeyGenerator) } as AESGCMCrypt
    }
}