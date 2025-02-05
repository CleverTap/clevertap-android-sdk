package com.clevertap.android.sdk.network

import android.util.Base64
import com.clevertap.android.sdk.Logger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

// TODO trim class to contain only encryption bit
class RSAEncryption {

    companion object {
        private const val RSA_ALGORITHM = "RSA"
        private const val RSA_CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        private const val KEY_SIZE = 2048
    }

    /**
     * Generates a new RSA key pair.
     *
     * @return A KeyPair containing the public and private keys.
     */
    fun generateKeyPair(): KeyPair? {
        return try {
            val keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM)
            keyPairGenerator.initialize(KEY_SIZE)
            val generateKeyPair = keyPairGenerator.generateKeyPair()
            generateKeyPair
        } catch (e: Exception) {
            Logger.v("Error generating RSA key pair", e)
            null
        }
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
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Logger.v("Error encrypting data with RSA", e)
            null
        }
    }

    /**
     * Decrypts data using RSA with OAEP padding.
     *
     * @param encryptedData The Base64 encoded encrypted data.
     * @param privateKey The private key to use for decryption.
     * @return The decrypted data as a byte array, or null if an error occurs.
     */
    fun decrypt(encryptedData: String, privateKey: PrivateKey): ByteArray? {
        return try {
            val cipher = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION)
            val oaepParameterSpec = OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
            )
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParameterSpec)
            val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
            cipher.doFinal(encryptedBytes)
        } catch (e: Exception) {
            Logger.v("Error decrypting data with RSA", e)
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

    /**
     * Converts a Base64 encoded string to a PrivateKey object.
     *
     * @param privateKeyString The Base64 encoded private key string.
     * @return The PrivateKey object, or null if an error occurs.
     */
    fun getPrivateKeyFromString(privateKeyString: String): PrivateKey? {
        return try {
            val privateKeyBytes = Base64.decode(privateKeyString, Base64.DEFAULT)
            val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val keyFactory = KeyFactory.getInstance(RSA_ALGORITHM)
            keyFactory.generatePrivate(keySpec)
        } catch (e: Exception) {
            Logger.v("Error converting string to PrivateKey", e)
            null
        }
    }

    /**
     * Converts a PublicKey object to a Base64 encoded string.
     *
     * @param publicKey The PublicKey object.
     * @return The Base64 encoded public key string, or null if an error occurs.
     */
    fun getStringFromPublicKey(publicKey: PublicKey): String? {
        return try {
            val publicKeyBytes = publicKey.encoded
            Base64.encodeToString(publicKeyBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Logger.v("Error converting PublicKey to string", e)
            null
        }
    }

    /**
     * Converts a PrivateKey object to a Base64 encoded string.
     *
     * @param privateKey The PrivateKey object.
     * @return The Base64 encoded private key string, or null if an error occurs.
     */
    fun getStringFromPrivateKey(privateKey: PrivateKey): String? {
        return try {
            val privateKeyBytes = privateKey.encoded
            Base64.encodeToString(privateKeyBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Logger.v("Error converting PrivateKey to string", e)
            null
        }
    }

}