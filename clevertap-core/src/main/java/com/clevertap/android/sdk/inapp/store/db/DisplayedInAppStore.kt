package com.clevertap.android.sdk.inapp.store.db

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.db.DelayedLegacyInAppDAO
import com.clevertap.android.sdk.db.DelayedLegacyInAppData
import org.json.JSONException
import org.json.JSONObject

/**
 * Keeps the full-screen In-App that is on screen in the database, encrypted, so it can be shown
 * again after the app process is killed. Only one In-App is kept: [save] replaces the old one.
 *
 * Uses the same DAO and table columns as the delayed In-Apps, on its own table.
 *
 * @property dao The DAO for the displayed In-App table.
 * @property cryptHandler Encrypts and decrypts the saved In-App.
 * @property logger The logger for verbose logging.
 * @property accountId The account ID for logging context.
 */
internal class DisplayedInAppStore(
    private val dao: DelayedLegacyInAppDAO,
    private val cryptHandler: CryptHandler,
    private val logger: ILogger,
    private val accountId: String
) {

    /**
     * Saves [data] for [inAppId] and removes any other saved In-App.
     *
     * @return true if it was saved.
     */
    @WorkerThread
    fun save(inAppId: String, data: JSONObject): Boolean {
        val encryptedData = cryptHandler.encrypt(data.toString())
        if (encryptedData == null) {
            logger.verbose(accountId, "Failed to encrypt displayed in-app: $inAppId")
            return false
        }
        dao.clearAll()
        return dao.insertBatch(listOf(DelayedLegacyInAppData(inAppId = inAppId, delay = 0, inAppData = encryptedData)))
    }

    /**
     * @return the data saved for [inAppId], or null if nothing is saved or it cannot be read.
     * A row that is read but cannot be decrypted or parsed is removed, as it can never be used.
     */
    @WorkerThread
    fun get(inAppId: String): JSONObject? {
        val encryptedData = dao.fetchSingleInApp(inAppId) ?: return null
        val data = try {
            cryptHandler.decrypt(encryptedData)?.let { JSONObject(it) }
        } catch (e: JSONException) {
            logger.verbose(accountId, "Error parsing displayed in-app: $inAppId", e)
            null
        }
        if (data == null) {
            logger.verbose(accountId, "Removing displayed in-app $inAppId: it cannot be decrypted or parsed")
            dao.remove(inAppId)
        }
        return data
    }

    /**
     * @return true if a saved In-App was removed.
     */
    @WorkerThread
    fun remove(inAppId: String): Boolean = dao.remove(inAppId)

    /**
     * Removes any saved In-App.
     */
    @WorkerThread
    fun clear(): Boolean = dao.clearAll()
}
