package com.clevertap.android.sdk.response

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import org.json.JSONObject
import androidx.annotation.RestrictTo

/**
 * Abstract Response that will be wrapped by [CleverTapResponseDecorator] objects
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class CleverTapResponse {

    /**
     * Whether this processor should run while a user switch is in progress. Defaults to `true`;
     * processors that must be skipped during a user switch override this to return `false`.
     * Lets [ClevertapResponseHandler] decide inclusion polymorphically instead of checking
     * concrete types.
     */
    open fun runsDuringUserSwitch(): Boolean = true

    @WorkerThread
    open fun processResponse(
        jsonBody: JSONObject?,
        stringBody: String?,
        context: Context
    ) {
        Log.i("CleverTapResponse", "Done processing response!")
    }

    /**
     * Context-aware entry point. Decorators that need request-scoped flags (source / isFullResponse /
     * isUserSwitching) override this; the default ignores the context and runs the plain overload.
     */
    @WorkerThread
    open fun processResponse(
        jsonBody: JSONObject?,
        stringBody: String?,
        context: Context,
        responseContext: ResponseContext
    ) {
        processResponse(jsonBody, stringBody, context)
    }
}
