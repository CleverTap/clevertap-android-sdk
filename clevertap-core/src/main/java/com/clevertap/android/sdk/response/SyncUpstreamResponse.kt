package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.LocalDataStore
import org.json.JSONObject

internal class SyncUpstreamResponse(
    private val localDataStore: LocalDataStore,
    private val logger: ILogger,
    private val accountId: String
): CleverTapResponseDecorator() {
    override fun processResponse(
        jsonBody: JSONObject?,
        stringBody: String?,
        context: Context?
    ) {
        try {
            localDataStore.syncWithUpstream(context, jsonBody)
        } catch (t: Throwable) {
            logger.verbose(accountId, "Failed to sync local cache with upstream", t)
        }
    }
}