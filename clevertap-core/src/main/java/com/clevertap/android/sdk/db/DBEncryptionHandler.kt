package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.cryption.CryptHandler

internal class DBEncryptionHandler(
    private val crypt: CryptHandler
) {

    companion object {
        private const val TAG = "DBEncryptionHandler"
        private const val DEFAULT_KEY = "DefaultKey"
    }

    // todo graceful handling for nulls?
    fun unwrapDbData(data: String) : String? {
        return crypt.decrypt(data, DEFAULT_KEY)
    }

    /**
     * Wraps database data as per encryption level and returns original data in case of failure.
     */
    fun wrapDbData(data: String) : String {
        return crypt.encrypt(data, DEFAULT_KEY) ?: data
    }
}