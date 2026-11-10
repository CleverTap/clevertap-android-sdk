package com.clevertap.android.sdk.db.dao

import androidx.annotation.WorkerThread

/**
 * A row of the displayed in-app table: the full-screen in-app that is on screen, kept so it can be
 * shown again after the app process is killed.
 *
 * @property inAppId the unique identifier for the in-app campaign
 * @property inAppData the encrypted JSON of the in-app notification
 * @property timeToLive the in-app's time to live, in seconds since the epoch
 */
internal data class DisplayedInAppData(
    val inAppId: String,
    val inAppData: String,
    val timeToLive: Long
)

internal interface DisplayedInAppDAO {
    /**
     * Save the displayed in-app. Only one is kept: any other row is removed in the same transaction.
     *
     * @return true if it was saved, false otherwise (the old row is then kept)
     */
    @WorkerThread
    fun save(displayedInApp: DisplayedInAppData): Boolean

    /**
     * Fetch the displayed in-app by its ID
     *
     * @return the row, or null if not found or it could not be read
     */
    @WorkerThread
    fun fetch(inAppId: String): DisplayedInAppData?

    /**
     * Remove the displayed in-app by its ID
     *
     * @return true if the record was deleted, false otherwise
     */
    @WorkerThread
    fun remove(inAppId: String): Boolean

    /**
     * Remove every row from the table
     *
     * @return true if the operation was successful, false otherwise
     */
    @WorkerThread
    fun clearAll(): Boolean
}
