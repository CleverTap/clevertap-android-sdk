package com.clevertap.android.sdk.network

import android.util.Base64
import com.clevertap.android.sdk.Logger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class RSAEncryption {

    companion object {
        private const val RSA_ALGORITHM = "RSA"
        private const val RSA_CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    }

    /**
     * Encrypts data using RSA with OAEP padding.
     *
     * @param data The data to encrypt.
     * @param publicKey The public key to use for encryption.
     * @return The encrypted data as a Base64 encoded string, or null if an error occurs.
     */
    fun encrypt(data: ByteArray, publicKey: PublicKey): String? {
        return try {
            val cipher = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION)
            val oaepParameterSpec = OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
            )
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParameterSpec)
            val encryptedBytes = cipher.doFinal(data)
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Logger.v("Error encrypting data with RSA", e)
            null
        }
    }

    /**
     * Converts a Base64 encoded string to a PublicKey object.
     *
     * @param publicKeyString The Base64 encoded public key string.
     * @return The PublicKey object, or null if an error occurs.
     */
    fun getPublicKeyFromString(publicKeyString: String): PublicKey? {
        return try {
            val publicKeyBytes = Base64.decode(publicKeyString, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(publicKeyBytes)
            val keyFactory = KeyFactory.getInstance(RSA_ALGORITHM)
            keyFactory.generatePublic(keySpec)
        } catch (e: Exception) {
            Logger.v("Error converting string to PublicKey", e)
            null
        }
    }
}