package com.clevertap.android.sdk.cryption

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.clevertap.android.sdk.Constants.AES_GCM_PREFIX
import com.clevertap.android.sdk.Constants.AES_GCM_SUFFIX
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.StorageHelper
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val ENCRYPTION_KEY = "EncryptionKey"

class AESGCMCrypt(private val context: Context) : Crypt() {

    override fun encryptInternal(plainText: String): String? {
        return performCryptOperation(
            mode = Cipher.ENCRYPT_MODE,
            data = plainText.toByteArray(StandardCharsets.UTF_8)
        )?.let { (iv, encryptedBytes) ->
            // Concatenate IV and encrypted text and surround with <>
            "$AES_GCM_PREFIX${iv.toBase64()}${encryptedBytes.toBase64()}$AES_GCM_SUFFIX"
        }
    }

    override fun decryptInternal(cipherText: String): String? {
        return parseCipherText(cipherText)?.let { (iv, encryptedBytes) ->
            performCryptOperation(
                mode = Cipher.DECRYPT_MODE,
                data = encryptedBytes,
                iv = iv
            )
        }?.let { (_, decryptedBytes) ->
            String(decryptedBytes, StandardCharsets.UTF_8)
        }
    }

    private fun parseCipherText(cipherText: String): AESGCMCryptResult? {
        return try {
            // removes the postfix and prefix
            val content = cipherText.removePrefix(AES_GCM_PREFIX).removeSuffix(AES_GCM_SUFFIX)
            val ivLength = 16  // Base64 length for 12-byte IV (encoded length is 16)
            val iv = content.substring(0, ivLength).fromBase64()
            val encryptedBytes = content.substring(ivLength).fromBase64()
            AESGCMCryptResult(iv, encryptedBytes)
        } catch (e: Exception) {
            Logger.v("Error parsing cipherText", e)
            null
        }
    }

    private fun performCryptOperation(
        mode: Int,
        data: ByteArray,
        iv: ByteArray? = null
    ): AESGCMCryptResult? {
        return try {
            val secretKey = generateOrGetKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            when (mode) {
                Cipher.ENCRYPT_MODE -> {
                    cipher.init(mode, secretKey)
                    val generatedIv = cipher.iv // Automatically generates 12-byte IV for GCM
                    val encryptedBytes = cipher.doFinal(data)
                    AESGCMCryptResult(generatedIv, encryptedBytes)
                }
                Cipher.DECRYPT_MODE -> {
                    if (iv != null) {
                        val gcmParameterSpec =
                            GCMParameterSpec(128, iv) // 128-bit authentication tag length
                        cipher.init(mode, secretKey, gcmParameterSpec)
                        val decryptedBytes = cipher.doFinal(data)
                        AESGCMCryptResult(iv, decryptedBytes)
                    } else {
                        Logger.v("IV is required for decryption")
                        null
                    }
                }
                else -> {
                    Logger.v("Invalid mode used")
                    null
                }
            }
        } catch (e: Exception) {
            Logger.v("Error performing crypt operation", e)
            null
        }
    }

    private fun generateOrGetKey(): SecretKey? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // TODO add support for API 21 and 22
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)

                if (keyStore.containsAlias(ENCRYPTION_KEY)) {
                    keyStore.getKey(ENCRYPTION_KEY, null) as SecretKey
                } else {
                    val keyGenerator =
                        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                    val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                        ENCRYPTION_KEY,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                    keyGenerator.init(keyGenParameterSpec)
                    keyGenerator.generateKey()
                }
            } catch (e: Exception) {
                Logger.v("Error generating or retrieving key", e)
                null
            }
        } else {
            Logger.v("KeyStore is not supported on API levels below 23")

            val encodedKey = StorageHelper.getString(context, ENCRYPTION_KEY, null)
            if (encodedKey != null) {
                // If the key exists, decode it and return as SecretKey
                val decodedKey = Base64.decode(encodedKey, Base64.NO_WRAP)
                SecretKeySpec(decodedKey, "AES")
            } else {
                // If key doesn't exist, generate a new one and store it
                val keyGenerator = KeyGenerator.getInstance("AES")
                keyGenerator.init(256) // 256-bit AES key
                val secretKey = keyGenerator.generateKey()

                // Store the key in SharedPreferences
                val encodedNewKey = Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
                StorageHelper.putString(context, ENCRYPTION_KEY, encodedNewKey)
                secretKey
            }
        }
    }

    private data class AESGCMCryptResult(val iv: ByteArray, val encryptedBytes: ByteArray)

    // Utility extension functions for Base64 encoding/decoding
    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
