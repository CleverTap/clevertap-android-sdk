package com.clevertap.android.sdk.inapp.store.db

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.db.dao.DisplayedInAppDAO
import com.clevertap.android.sdk.db.dao.DisplayedInAppData
import org.json.JSONException
import org.json.JSONObject

/**
 * The saved full-screen In-App, read back from the database.
 *
 * @property inApp The In-App JSON as it was received.
 * @property timeToLive The In-App's time to live, in seconds since the epoch.
 */
internal data class DisplayedInApp(
    val inApp: JSONObject,
    val timeToLive: Long
)

/**
 * Keeps the full-screen In-App that is on screen in the database, encrypted, so it can be shown
 * again after the app process is killed. Only one In-App is kept: [save] replaces the old one.
 *
 * @property dao The DAO for the displayed In-App table.
 * @property cryptHandler Encrypts and decrypts the saved In-App.
 * @property logger The logger for verbose logging.
 * @property accountId The account ID for logging context.
 */
internal class DisplayedInAppStore(
    private val dao: DisplayedInAppDAO,
    private val cryptHandler: CryptHandler,
    private val logger: ILogger,
    private val accountId: String
) {

    /**
     * Saves [inApp] with its [timeToLive] for [inAppId] and removes any other saved In-App.
     *
     * @return true if it was saved.
     */
    @WorkerThread
    fun save(inAppId: String, inApp: JSONObject, timeToLive: Long): Boolean {
        val encryptedData = cryptHandler.encrypt(inApp.toString())
        if (encryptedData == null) {
            logger.verbose(accountId, "Failed to encrypt displayed in-app: $inAppId")
            return false
        }
        return dao.save(DisplayedInAppData(inAppId = inAppId, inAppData = encryptedData, timeToLive = timeToLive))
    }

    /**
     * @return the In-App saved for [inAppId], or null if nothing is saved or it cannot be read.
     * A row that is read but cannot be decrypted or parsed is removed, as it can never be used.
     */
    @WorkerThread
    fun get(inAppId: String): DisplayedInApp? {
        val saved = dao.fetch(inAppId) ?: return null
        val inApp = try {
            cryptHandler.decrypt(saved.inAppData)?.let { JSONObject(it) }
        } catch (e: JSONException) {
            logger.verbose(accountId, "Error parsing displayed in-app: $inAppId", e)
            null
        }
        if (inApp == null) {
            logger.verbose(accountId, "Removing displayed in-app $inAppId: it cannot be decrypted or parsed")
            dao.remove(inAppId)
            return null
        }
        return DisplayedInApp(inApp, saved.timeToLive)
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
