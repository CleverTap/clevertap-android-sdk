package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import java.nio.charset.StandardCharsets
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * This class implements the AES Cryption algorithm
 */
class AESCrypt : Crypt() {
    /**
     * This method returns the key-password to be used for encryption/decryption
     *
     * @param accountID : accountId of the current instance
     * @return key-password
     */
    private fun generateKeyPassword(accountID: String): String {
        return APP_ID_KEY_PREFIX + accountID + APP_ID_KEY_SUFFIX
    }

    /**
     * This method is used internally to encrypt the plain text
     *
     * @param plainText - plainText to be encrypted
     * @param accountID - accountID used for password generation
     * @return encrypted text
     */
    override fun encryptInternal(plainText: String, accountID: String): String? {

        return performCryptOperation(
            Cipher.ENCRYPT_MODE, generateKeyPassword(accountID), plainText.toByteArray(
                StandardCharsets.UTF_8
            )
        )?.let { encryptedBytes ->
            encryptedBytes.contentToString()
        }

    }

    /**
     * This method is used internally to decrypt the cipher text
     *
     * @param cipherText - cipherText to be decrypted
     * @param accountID - accountID used for password generation
     * @return decrypted text
     */
    override fun decryptInternal(cipherText: String, accountID: String): String? {
        return parseCipherText(cipherText)?.let { bytes ->
            performCryptOperation(Cipher.DECRYPT_MODE, generateKeyPassword(accountID), bytes)
        }?.let { decryptedBytes ->
            String(decryptedBytes, StandardCharsets.UTF_8)
        }
    }

    /**
     * This method is used to parse the cipher text (i.e remove the [] and split the string around commas) and convert it to a byte array
     *
     * @param cipherText - cipher text to be parsed
     * @return Parsed string in the form of a byte array
     */
    override fun parseCipherText(cipherText: String): ByteArray? {
        return try {
            // Removes the enclosing brackets, trims any leading or trailing whitespace, and then splits the resulting string based on commas
            val byteStrings =
                cipherText.substring(1, cipherText.length - 1).trim().split("\\s*,\\s*".toRegex())
            val bytes = ByteArray(byteStrings.size)
            for (i in byteStrings.indices) {
                bytes[i] = byteStrings[i].toByte()
            }
            bytes
        } catch (e: Exception) {
            Logger.v("Unable to parse cipher text", e)
            null
        }
    }

    /**
     * This method actually performs both the encryption and decryption crypt task.
     *
     * @param mode     - mode to determine encryption/decryption
     * @param password - password for cryption
     * @param text     - text to be crypted
     * @return Crypted text in the form of a byte array
     */
    private fun performCryptOperation(mode: Int, password: String, text: ByteArray?): ByteArray? {
        return try {
            val salt = Constants.CRYPTION_SALT.toByteArray(StandardCharsets.UTF_8)
            val iv = Constants.CRYPTION_IV.toByteArray(StandardCharsets.UTF_8)
            val keySpec: KeySpec = PBEKeySpec(password.toCharArray(), salt, 1000, 256)
            val keyFactory = SecretKeyFactory.getInstance("PBEWithMD5And128BitAES-CBC-OpenSSL")
            val keyBytes = keyFactory.generateSecret(keySpec).encoded
            val key: SecretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val ivParams = IvParameterSpec(iv)
            cipher.init(mode, key, ivParams)
            cipher.doFinal(text)
        } catch (e: Exception) {
            Logger.v("Unable to perform crypt operation", e)
            null
        }
    }

    companion object {
        // Build prefix and suffix strings longhand, to obfuscate them slightly.
        // Probably doesn't matter.
        private val APP_ID_KEY_PREFIX = StringBuilder()
            .append("L").append("q").append(3).append("f").append("z").toString()
        private val APP_ID_KEY_SUFFIX = StringBuilder()
            .append("b").append("L").append("t").append("i").append(2).toString()
    }
}