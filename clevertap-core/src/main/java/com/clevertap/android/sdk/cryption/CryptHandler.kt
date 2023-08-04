package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.Constants

class CryptHandler(encryptionLevel: Int, encryptionType: EncryptionAlgorithm, accountID: String) {
    private var encryptionLevel: EncryptionLevel
    private var encryptionType: EncryptionAlgorithm
    private var crypt: Crypt
    private var accountID: String

    enum class EncryptionAlgorithm {
        AES
    }

    enum class EncryptionLevel(private val value: Int) {
        NONE(0), MEDIUM(1);

        fun intValue(): Int {
            return value
        }
    }

    init {
        this.encryptionLevel = EncryptionLevel.values()[encryptionLevel]
        this.encryptionType = encryptionType
        this.accountID = accountID
        this.crypt = CryptFactory.getCrypt(encryptionType)
    }

    /**
     * This method returns the encrypted text if the key is a part of the current encryption level
     *
     * @param plainText - plainText to be encrypted
     * @param key       - key of the plainText to be encrypted
     * @return encrypted text
     */
    fun encrypt(plainText: String, key: String): String {
        when (encryptionLevel) {
            EncryptionLevel.MEDIUM ->
                if (Constants.MEDIUM_CRYPT_KEYS.contains(key))
                    return crypt.encryptInternal(plainText, accountID) ?: plainText
            else -> return plainText
        }
        return plainText
    }

    /**
     * This method returns the decrypted text if the key is a part of the current encryption level
     *
     * @param cipherText - cipherText to be decrypted
     * @param key        - key of the cipherText that needs to be decrypted
     * @return decrypted text
     */
    fun decrypt(cipherText: String, key: String): String {
        return when (encryptionLevel) {
            EncryptionLevel.MEDIUM -> {
                if (key in Constants.MEDIUM_CRYPT_KEYS)
                    crypt.decryptInternal(cipherText, accountID) ?: cipherText
                else
                    cipherText
            }
            else -> {
                // None crypt keys is required in the case of migration only
                if (Constants.NONE_CRYPT_KEYS.contains(key))
                    crypt.decryptInternal(cipherText, accountID) ?: cipherText
                else
                    cipherText
            }
        }
    }
}