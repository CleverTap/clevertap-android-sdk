package com.clevertap.android.sdk.cryption

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.clevertap.android.sdk.Logger
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AESGCMCrypt : Crypt() {

    override fun encryptInternal(plainText: String): String? {
        return performCryptOperation(
            mode = Cipher.ENCRYPT_MODE,
            data = plainText.toByteArray(StandardCharsets.UTF_8)
        )?.let { (iv, encryptedBytes) ->
            // Concatenate IV and encrypted text and surround with <>
            "<${iv.toBase64()}${encryptedBytes.toBase64()}>"
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

    private fun parseCipherText(cipherText: String): Pair<ByteArray, ByteArray>? {
        return try {
            val content = cipherText.trim('<', '>')
            val ivLength = 16  // Base64 length for 12-byte IV (encoded length is 16)
            val iv = content.substring(0, ivLength).fromBase64()
            val encryptedBytes = content.substring(ivLength).fromBase64()
            Pair(iv, encryptedBytes)
        } catch (e: Exception) {
            Logger.v("Error parsing cipherText", e)
            null
        }
    }

    private fun performCryptOperation(
        mode: Int,
        data: ByteArray,
        iv: ByteArray? = null
    ): Pair<ByteArray, ByteArray>? {
        return try {
            val secretKey = generateOrGetKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            when (mode) {
                Cipher.ENCRYPT_MODE -> {
                    cipher.init(mode, secretKey)
                    val generatedIv = cipher.iv // Automatically generates 12-byte IV for GCM
                    val encryptedBytes = cipher.doFinal(data)
                    Pair(generatedIv, encryptedBytes)
                }
                Cipher.DECRYPT_MODE -> {
                    if (iv != null) {
                        val gcmParameterSpec =
                            GCMParameterSpec(128, iv) // 128-bit authentication tag length
                        cipher.init(mode, secretKey, gcmParameterSpec)
                        val decryptedBytes = cipher.doFinal(data)
                        Pair(iv, decryptedBytes)
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
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)

                if (keyStore.containsAlias("EncryptionKey")) {
                    keyStore.getKey("EncryptionKey", null) as SecretKey
                } else {
                    val keyGenerator =
                        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                    val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                        "EncryptionKey",
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
            null
        }
    }

    // Utility extension functions for Base64 encoding/decoding
    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
