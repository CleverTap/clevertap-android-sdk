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
    private val aesgcm: AESGCMCrypt
) {

    companion object {
        private var sessionKey: SecretKey? = null
    }

    /**
     * Returns session key for encryption
     */
    fun sessionKeyForEncryption(): String {
        if (sessionKey == null) {
            sessionKey = keyGenerator.generateSecretKey()
        }
        return convertByteArrayToString(sessionKey!!.encoded)
    }

    /**
     * Returns EncryptionResult which contains encrypted response, iv
     */
    fun encryptResponse(response: String): EncryptionResult {
        val result =
            aesgcm.performCryptOperation(Cipher.ENCRYPT_MODE, data = response.toByteArray())

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
        return Base64.encodeToString(arr, Base64.DEFAULT)
    }

}