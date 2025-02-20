package com.clevertap.android.sdk.cryption

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import com.clevertap.android.sdk.Logger
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

internal class CTKeyGenerator(val cryptRepository: CryptRepository) {

    /**
     * Generates or retrieves a secret key for encryption/decryption.
     *
     * This method uses the Android Keystore system on devices running API 23 (Marshmallow) or higher
     * to securely store the key. If the Android Keystore is not available (on older API levels),
     * it falls back to generating a key and storing it in SharedPreferences, encoded in Base64.
     *
     * @return The secret key for encryption/decryption, or null if an error occurs during key generation/retrieval.
     */
    fun generateOrGetKey(): SecretKey? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fromAndroidKeystore()
        } else {
            Logger.v("KeyStore is not supported on API levels below 23")

            val encodedKey = cryptRepository.localEncryptionKey()
            if (encodedKey != null) {
                // If the key exists, decode it and return as SecretKey
                val decodedKey = encodedKey.fromBase64()
                SecretKeySpec(decodedKey, "AES")
            } else {
                val secretKey = generateSecretKey()

                // Store the key in SharedPreferences
                val encodedNewKey = secretKey.encoded.toBase64()
                cryptRepository.updateLocalEncryptionKey(encodedNewKey)
                secretKey
            }
        }
    }

    fun generateSecretKey(): SecretKey {
        // If key doesn't exist, generate a new one and store it
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256) // 256-bit AES key
        val secretKey = keyGenerator.generateKey()
        return secretKey
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun fromAndroidKeystore() = try {
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
}