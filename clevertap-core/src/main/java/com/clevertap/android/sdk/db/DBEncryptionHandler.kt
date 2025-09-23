package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.BuildConfig
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.cryption.CryptHandler
import kotlin.system.measureTimeMillis

internal class DBEncryptionHandler(
    private val crypt: CryptHandler,
    private val logger: ILogger
) {

    companion object {
        private const val TAG = "DBEncryptionHandler"
        private const val DEFAULT_KEY = "DefaultKey"
    }

    // todo graceful handling for nulls?
    fun unwrapDbData(data: String) : String? {
        return measureTimeInMillisAndLog(TAG, "unwrapDbData") {
            crypt.decrypt(data, DEFAULT_KEY)
        }
    }

    /**
     * Wraps database data as per encryption level and returns original data in case of failure.
     */
    fun wrapDbData(data: String) : String {
        return measureTimeInMillisAndLog(TAG, "wrapDbData") {
            crypt.encrypt(data, DEFAULT_KEY)
                ?: data.also { logger.verbose(TAG, "Failed to encrypt data, so saving plain text: $data") }
        }
    }

    private inline fun <T> measureTimeInMillisAndLog(
        tag: String = "TimeMeasurement",
        message: String = "Execution took",
        block: () -> T
    ): T {
        var result: T
        val timeMillis = measureTimeMillis {
            result = block()
        }

        if (BuildConfig.DEBUG) {
            logger.verbose(tag, "$message took $timeMillis ms")
        }
        return result
    }
}