package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.BuildConfig
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.cryption.ICryptHandler
import kotlin.system.measureTimeMillis

internal class DBEncryptionHandler(
    private val crypt: ICryptHandler,
    private val logger: ILogger,
    private val encryptionLevel: EncryptionLevel = EncryptionLevel.NONE
) {

    companion object {
        private const val TAG = "DBEncryptionHandler"
    }

    fun unwrapDbData(data: String?) : String? {
        return measureTimeInMillisAndLog(TAG, "unwrapDbData") {
            if (data == null) {
                data
            } else {
                val op = crypt.decryptSafe(data)
                if (op == null) {
                    logger.verbose(TAG, "unwrapDbData: Decryption failed for $data")
                }
                op
            }
        }
    }

    /**
     * Wraps database data as per encryption level and returns original data in case of failure.
     */
    fun wrapDbData(data: String) : String {
        return measureTimeInMillisAndLog(TAG, "wrapDbData") {
            if (encryptionLevel == EncryptionLevel.FULL_DATA) {
                val op = crypt.encryptSafe(data)
                if (op == null) {
                    logger.verbose(TAG, "wrapDbData: Encryption failed for $data")
                }
                op ?: data
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