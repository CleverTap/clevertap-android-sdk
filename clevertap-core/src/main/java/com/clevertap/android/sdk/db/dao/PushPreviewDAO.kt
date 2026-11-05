package com.clevertap.android.sdk.db.dao

import androidx.annotation.WorkerThread

/**
 * Table access for trusted copies of Send Test previews (SDK-6282).
 *
 * A row is written when a preview push is seen at arrival and read when a
 * notification is tapped. Rows are never deleted on read; they age out.
 *
 * Example: a Send Test arrives at 10:00 with a 24 hour `wzrk_ttl`. [save] writes
 * one row. A tap at 10:05 finds it through [find]. A tap the next day at 10:05
 * finds nothing because the row has expired.
 */
internal interface PushPreviewDAO {

    /**
     * Saves one preview. A second call with the same [previewKey] replaces the
     * row and refreshes its expiry. Expired rows are removed and the table is
     * trimmed to its cap on every call.
     *
     * @param previewKey 64 hex characters, see PushPreviewKey
     * @param data the preview fields as JSON text (encrypted before storage)
     * @param expiresAtSeconds epoch seconds after which the row is ignored
     * @param collapseKey the push's wzrk_ck, kept for diagnostics only
     */
    @WorkerThread
    fun save(previewKey: String, data: String, expiresAtSeconds: Long, collapseKey: String?)

    /**
     * Returns the stored JSON text for [previewKey] if it exists and has not
     * expired at [nowSeconds], otherwise null.
     */
    @WorkerThread
    fun find(previewKey: String, nowSeconds: Long): String?
}
