package com.clevertap.android.sdk.cryption

/**
 * This class is a factory class to generate a Crypt object based on the EncryptionAlgorithm
 */
class CryptFactory {
    companion object {
        @JvmStatic
        fun getCrypt(type: CryptHandler.EncryptionAlgorithm, accountID: String): Crypt {
            return when (type) {
                CryptHandler.EncryptionAlgorithm.AES -> AESCrypt(accountID)
                CryptHandler.EncryptionAlgorithm.AES_GCM -> AESGCMCrypt()
            }
        }
    }
}