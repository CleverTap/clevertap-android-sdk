package com.clevertap.android.sdk.network

import android.util.Base64
import com.clevertap.android.sdk.cryption.AESGCMCrypt
import com.clevertap.android.sdk.cryption.CTKeyGenerator
import com.clevertap.android.sdk.network.api.EncryptionFailure
import com.clevertap.android.sdk.network.api.EncryptionResult
import com.clevertap.android.sdk.network.api.EncryptionSuccess
import javax.crypto.Cipher
import javax.crypto.SecretKey

internal class NetworkEncryptionManager(
    private val keyGenerator: CTKeyGenerator,
    private val aesgcm: AESGCMCrypt,
    private val rsaCrypt: RSAEncryption,
    private val publicKeyForRsa: () -> String
) {

    companion object {
        private var sessionKey: SecretKey? = null
    }

    /**
     * Returns session key for encryption
     */
    private fun sessionKeyForEncryption(): SecretKey {
        return sessionKey ?: keyGenerator.generateSecretKey().also { sessionKey = it }
    }

    private fun sessionKeyBytes() : ByteArray? {
        return sessionKeyForEncryption().encoded
    }

    fun encryptedSessionKey() : String? {
        val symmetricKey = sessionKeyBytes() ?: return null
        val cryptKey = rsaCrypt.getPublicKeyFromString(publicKeyForRsa())
        if (cryptKey != null) {
            return rsaCrypt.encrypt(symmetricKey, cryptKey)
        }
        return null
    }

    /**
     * Returns EncryptionResult which contains encrypted response, iv
     */
    fun encryptResponse(response: String): EncryptionResult {
        val result =
            aesgcm.performCryptOperation(
                mode = Cipher.ENCRYPT_MODE,
                data = response.toByteArray(),
                iv = null,
                secretKey = sessionKeyForEncryption()
            )

        return if (result != null) {
            EncryptionSuccess(
                data = convertByteArrayToString(result.encryptedBytes),
                iv = convertByteArrayToString(result.iv)
            )
        } else {
            EncryptionFailure
        }
    }

    /**
     * Returns EncryptionResult which contains encrypted response, iv
     */
    fun decryptResponse(
        response: String,
        iv: String // base64 encoded from BE
    ): EncryptionResult {

        val decodedIv = Base64.decode(response.toByteArray(), Base64.DEFAULT)
        val decodedResponse = Base64.decode(iv.toByteArray(), Base64.DEFAULT)
        val result =
            aesgcm.performCryptOperation(
                mode = Cipher.DECRYPT_MODE,
                data = decodedResponse,
                iv = decodedIv,
                secretKey = sessionKeyForEncryption()
            )

        return if (result != null) {
            EncryptionSuccess(
                data = convertByteArrayToString(result.encryptedBytes),
                iv = convertByteArrayToString(result.iv)
            )
        } else {
            EncryptionFailure
        }
    }

    /**
     * Converts byte array to base64 string
     */
    private fun convertByteArrayToString(arr: ByteArray) : String {
        //return arr.toString(Charsets.UTF_8) // might have some restricted chars
        // return java.util.Base64.getEncoder().encodeToString(arr) // Requires min api 26
        return Base64.encodeToString(arr, Base64.NO_WRAP)
    }

}