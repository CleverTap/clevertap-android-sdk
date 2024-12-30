package com.clevertap.android.sdk.cryption

import android.content.Context

/**
 * This class is a factory class to generate a Crypt object based on the EncryptionAlgorithm
 */
class CryptFactory {
    companion object {
        @JvmStatic
        fun getCrypt(type: CryptHandler.EncryptionAlgorithm, accountID: String, context: Context): Crypt {
            return when (type) {
                CryptHandler.EncryptionAlgorithm.AES -> AESCrypt(accountID)
                CryptHandler.EncryptionAlgorithm.AES_GCM -> AESGCMCrypt(context)
            }
        }
    }
}