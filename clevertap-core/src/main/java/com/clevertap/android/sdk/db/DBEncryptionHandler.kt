package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.BuildConfig
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.cryption.EncryptionLevel
import kotlin.system.measureTimeMillis

internal class DBEncryptionHandler(
    private val crypt: CryptHandler,
    private val logger: ILogger,
    private val encryptionLevel: EncryptionLevel = EncryptionLevel.NONE
) {

    companion object {
        private const val TAG = "DBEncryptionHandler"
        private const val DEFAULT_KEY = "DefaultKey"
    }

    // todo graceful handling for nulls?
    fun unwrapDbData(data: String?) : String? {
        return measureTimeInMillisAndLog(TAG, "unwrapDbData") {
            if (data == null) {
                data
            } else {
                crypt.decryptSafe(data)
            }
        }
    }

    /**
     * Wraps database data as per encryption level and returns original data in case of failure.
     */
    fun wrapDbData(data: String) : String {
        return measureTimeInMillisAndLog(TAG, "wrapDbData") {
            if (encryptionLevel == EncryptionLevel.FULL_DATA) {
                crypt.encryptSafe(data) ?: data
            } else {
                data
            }
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