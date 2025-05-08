package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.Constants.AES_GCM_PREFIX
import com.clevertap.android.sdk.Constants.AES_GCM_SUFFIX
import com.clevertap.android.sdk.Logger
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * This class implements the AES-GCM Crypt algorithm
 *
 */
internal class AESGCMCrypt(
    private val ctKeyGenerator: CTKeyGenerator
) : Crypt() {

    /**
     * This method is used internally to encrypt the plain text
     *
     * @param plainText - plainText to be encrypted
     * @return encrypted text appended with iv, prefix and suffix
     */
    override fun encryptInternal(plainText: String): String? {
        return performCryptOperation(
            mode = Cipher.ENCRYPT_MODE,
            data = plainText.toByteArray(StandardCharsets.UTF_8)
        )?.let { (iv, encryptedBytes) ->
            // Concatenate IV and encrypted text with a ":" delimiter
            "$AES_GCM_PREFIX${iv.toBase64()}:${encryptedBytes.toBase64()}$AES_GCM_SUFFIX"
        }
    }


    /**
     * This method is used internally to decrypt the cipher text
     *
     * @param cipherText - cipherText to be decrypted
     * @return decrypted text
     */
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

    /**
     * This method is used to parse the cipher text (i.e remove the prefix and suffix, extract out the IV and encryptedBytes) and convert it to a byte array
     *
     * @param cipherText - cipher text to be parsed
     * @return AESGCMCryptResult
     */
    private fun parseCipherText(cipherText: String): AESGCMCryptResult? {
        return try {
            // Remove the prefix and suffix
            val content = cipherText.removePrefix(AES_GCM_PREFIX).removeSuffix(AES_GCM_SUFFIX)

            // Split IV and encrypted bytes using a delimiter
            val parts = content.split(":") // Use ":" as a delimiter
            val iv = parts[0].fromBase64()
            val encryptedBytes = parts[1].fromBase64()
            AESGCMCryptResult(iv, encryptedBytes)
        } catch (e: Exception) {
            Logger.v("Error parsing cipherText", e)
            null
        }
    }

    /**
     * This method actually performs both the encryption and decryption crypt task.
     *
     * @param mode - mode to determine encryption/decryption
     * @param data - data to be crypted
     * @param iv - iv required for decryption
     * @return AESGCMCryptResult
     */
    fun performCryptOperation(
        mode: Int,
        data: ByteArray,
        iv: ByteArray? = null,
        secretKey: SecretKey? = ctKeyGenerator.generateOrGetKey()
    ): AESGCMCryptResult? {
        return try {

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            when (mode) {
                Cipher.ENCRYPT_MODE -> {
                    // 128-bit authentication tag length
                    val gcmParameterSpec = GCMParameterSpec(128, iv)
                    cipher.init(mode, secretKey, gcmParameterSpec)
                    val generatedIv = cipher.iv // Automatically generates 12-byte IV for GCM
                    val encryptedBytes = cipher.doFinal(data)
                    AESGCMCryptResult(generatedIv, encryptedBytes)
                }
                Cipher.DECRYPT_MODE -> {
                    if (iv != null) {
                        // 128-bit authentication tag length
                        val gcmParameterSpec = GCMParameterSpec(128, iv)
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

    internal data class AESGCMCryptResult(
        val iv: ByteArray,
        val encryptedBytes: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AESGCMCryptResult

            if (!iv.contentEquals(other.iv)) return false
            if (!encryptedBytes.contentEquals(other.encryptedBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = iv.contentHashCode()
            result = 31 * result + encryptedBytes.contentHashCode()
            return result
        }
    }
}