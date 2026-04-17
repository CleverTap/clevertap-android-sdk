package com.clevertap.android.sdk.cryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.clevertap.android.sdk.Logger
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal class CTKeyGenerator(val cryptRepository: CryptRepository) {

    /**
     * Generates or retrieves a secret key for encryption/decryption.
     *
     * This method uses Android Keystore to securely retrieve or create the key.
     * With minSdk 23+, no SharedPreferences fallback path is used.
     *
     * @return The secret key for encryption/decryption, or null if an error occurs.
     */

    fun generateOrGetKey(): SecretKey? {
        return fromAndroidKeystore()
    }

    fun generateSecretKey(): SecretKey {
        // If key doesn't exist, generate a new one and store it
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256) // 256-bit AES key
        val secretKey = keyGenerator.generateKey()
        return secretKey
    }

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