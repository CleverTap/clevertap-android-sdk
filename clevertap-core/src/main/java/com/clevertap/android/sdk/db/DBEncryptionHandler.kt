package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.BuildConfig
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.cryption.ICryptHandler
import kotlin.system.measureTimeMillis

/**
 * Handles the encryption and decryption of data before it is stored in or retrieved from the database.
 * The actual encryption/decryption logic is determined by the provided [ICryptHandler] and
 * is only performed if the [encryptionLevel] is set to [EncryptionLevel.FULL_DATA].
 *
 * This class acts as a wrapper around the cryptographic operations to ensure that database
 * interactions are handled consistently with respect to the configured encryption policy.
 *
 * @property crypt An instance of [ICryptHandler] that provides the core encryption and decryption functionalities.
 * @property logger An instance of [ILogger] for logging debug and error information.
 * @property encryptionLevel The level of encryption to be applied. If set to [EncryptionLevel.NONE],
 *                           no encryption or decryption will be performed.
 */
internal class DBEncryptionHandler(
    private val crypt: ICryptHandler,
    private val logger: ILogger,
    private val encryptionLevel: EncryptionLevel = EncryptionLevel.NONE
) {

    companion object {
        private const val TAG = "DBEncryptionHandler"
    }

    /**
     * Unwraps database data by decrypting it.
     * This function will always attempt to decrypt the data, regardless of the current encryption level,
     * to handle cases where the encryption level might have changed.
     *
     * @param data The encrypted string data retrieved from the database. Can be null.
     * @return The decrypted string, or null if the input was null or if decryption fails.
     */
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