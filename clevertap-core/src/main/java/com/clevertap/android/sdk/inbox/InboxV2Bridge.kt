package com.clevertap.android.sdk.inbox

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.FetchInboxCallback
import com.clevertap.android.sdk.network.fetch.CallResult
import com.clevertap.android.sdk.network.fetch.NetworkScope
import kotlinx.coroutines.launch

/**
 * Java-interop adapter over [InboxV2Fetcher]. Launches a coroutine on the
 * shared [NetworkScope] and delivers a boolean result to a Java callback.
 *
 * The only place where SDK-internal coroutines meet the public Java API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class InboxV2Bridge(
    private val fetcher: InboxV2Fetcher,
    private val networkScope: NetworkScope
) {

    /**
     * Fire-and-forget fetch. Returns immediately. The [callback] (if any) is
     * invoked on the network dispatcher thread once the fetch completes.
     *
     * A null [callback] is fine — used by app-launch and onUserLogin which
     * don't need to know the outcome.
     */
    fun submit(respectThrottle: Boolean, callback: FetchInboxCallback?) {
        networkScope.coroutineScope.launch {
            val result = fetcher.fetch(respectThrottle)
            callback?.onInboxFetched(result is CallResult.Success)
        }
    }
}
