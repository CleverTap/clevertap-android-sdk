package com.clevertap.android.sdk.network

import android.util.Base64
import com.clevertap.android.sdk.cryption.AESGCMCrypt
import com.clevertap.android.sdk.cryption.CTKeyGenerator
import com.clevertap.android.sdk.network.api.EncryptedResponseBody
import com.clevertap.android.sdk.network.api.EncryptionFailure
import com.clevertap.android.sdk.network.api.EncryptionResult
import com.clevertap.android.sdk.network.api.EncryptionSuccess
import javax.crypto.Cipher
import javax.crypto.SecretKey

internal class NetworkEncryptionManager(
    private val keyGenerator: CTKeyGenerator,
    private val aesgcm: AESGCMCrypt
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

    private fun sessionKeyBytes() : ByteArray {
        return sessionKeyForEncryption().encoded
    }

    fun sessionEncryptionKey() = Base64.encodeToString(sessionKeyBytes(), Base64.NO_WRAP)

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
        bodyString: String
    ): EncryptionResult {

        try {
            val responseBody = EncryptedResponseBody.fromJsonString(bodyString)
            val response = responseBody.encryptedPayload
            val iv = responseBody.iv

            val decodedResponse = Base64.decode(response, Base64.NO_WRAP)
            val decodedIv = Base64.decode(iv, Base64.NO_WRAP)
            val result =
                aesgcm.performCryptOperation(
                    mode = Cipher.DECRYPT_MODE,
                    data = decodedResponse,
                    iv = decodedIv,
                    secretKey = sessionKeyForEncryption()
                )

            return if (result != null) {
                EncryptionSuccess(
                    data = String(result.encryptedBytes),
                    iv = String(result.iv)
                )
            } else {
                EncryptionFailure
            }
        } catch (e: Exception) {
            return EncryptionFailure
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