package com.clevertap.android.sdk.inapp.store.db

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.db.DelayedLegacyInAppDAO
import com.clevertap.android.sdk.db.DelayedLegacyInAppData
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import com.clevertap.android.sdk.iterator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The `DelayedLegacyInAppStore` class manages the storage and retrieval of delayed In-App messages
 * in a SQLite database with encryption support.
 *
 * <p>
 * It stores delayed in-apps in the database table with encryption applied to the inAppData
 * for security, similar to how InAppStore encrypts client-side in-apps.
 * </p>
 *
 * @property delayedLegacyInAppDAO The DAO for database operations.
 * @property logger The logger for verbose logging.
 * @property accountId The account ID for logging context.
 * @property cryptHandler The handler used for encryption and decryption of In-App messages.
 */
internal class DelayedLegacyInAppStore(
    private val delayedLegacyInAppDAO: DelayedLegacyInAppDAO,
    private val cryptHandler: CryptHandler,
    private val logger: ILogger,
    private val accountId: String
) {

    @WorkerThread
    fun saveDelayedInApp(inAppId: String, delay: Int, inAppJson: JSONObject): Boolean {
        // Encrypt the inAppData before saving to database
        val encryptedInAppData = cryptHandler.encrypt(inAppJson.toString())

        if (encryptedInAppData == null) {
            logger.verbose(
                accountId,
                "Failed to encrypt delayed in-app: $inAppId. Data will not be stored."
            )
            return false
        }

        val data = DelayedLegacyInAppData(
            inAppId = inAppId,
            delay = delay,
            inAppData = encryptedInAppData
        )

        val result = delayedLegacyInAppDAO.insert(data)
        return result > 0
    }

    @WorkerThread
    fun saveDelayedInAppsBatch(delayedInApps: JSONArray): Boolean {
        if (delayedInApps.length() == 0) return true

        val dataList = mutableListOf<DelayedLegacyInAppData>()
        var encryptionFailureCount = 0

        delayedInApps.iterator<JSONObject> { inAppJson ->
            try {
                val inAppId = inAppJson.optString(Constants.INAPP_ID_IN_PAYLOAD)
                val delay = inAppJson.optInt(INAPP_DELAY_AFTER_TRIGGER)

                // Encrypt the inAppData before saving to database
                val encryptedInAppData = cryptHandler.encrypt(inAppJson.toString())

                if (encryptedInAppData == null) {
                    logger.verbose(
                        accountId,
                        "Failed to encrypt delayed in-app: $inAppId. Skipping this item."
                    )
                    encryptionFailureCount++
                } else {
                    dataList.add(
                        DelayedLegacyInAppData(
                            inAppId = inAppId,
                            delay = delay,
                            inAppData = encryptedInAppData
                        )
                    )
                }
            } catch (e: JSONException) {
                logger.verbose(accountId, "Error parsing delayed in-app", e)
            }
        }

        if (dataList.isEmpty()) {
            logger.verbose(
                accountId,
                "No delayed in-apps to save. All items failed encryption or parsing."
            )
            return false
        }

        if (encryptionFailureCount > 0) {
            logger.verbose(
                accountId,
                "Skipped $encryptionFailureCount delayed in-apps due to encryption failure"
            )
        }

        return delayedLegacyInAppDAO.insertBatch(dataList)
    }

    @WorkerThread
    fun getDelayedInApp(inAppId: String): JSONObject? {
        val encryptedInAppDataString = delayedLegacyInAppDAO.fetchSingleInApp(inAppId)

        return encryptedInAppDataString?.let {
            try {
                // Decrypt the data before parsing to JSONObject
                val decryptedInAppData = cryptHandler.decrypt(it)

                if (decryptedInAppData == null) {
                    logger.verbose(accountId, "Failed to decrypt delayed in-app: $inAppId")
                    return null
                }

                JSONObject(decryptedInAppData)
            } catch (e: Exception) {
                logger.verbose(accountId, "Error decrypting/parsing delayed in-app: $inAppId", e)
                null
            }
        }
    }

    @WorkerThread
    fun removeDelayedInApp(inAppId: String): Boolean {
        return delayedLegacyInAppDAO.remove(inAppId)
    }

    @WorkerThread
    fun removeDelayedInAppsBatch(inAppIds: List<String>): Int {
        var removedCount = 0
        inAppIds.forEach { inAppId ->
            if (delayedLegacyInAppDAO.remove(inAppId)) {
                removedCount++
            }
        }
        return removedCount
    }

    @WorkerThread
    fun hasDelayedInApp(inAppId: String): Boolean {
        return delayedLegacyInAppDAO.fetchSingleInApp(inAppId) != null
    }
}