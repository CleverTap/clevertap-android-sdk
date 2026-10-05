package com.clevertap.android.sdk.response;

import android.content.Context;
import android.util.Log;
import androidx.annotation.WorkerThread;
import org.json.JSONObject;

/**
 * Abstract Response that will be wrapped by {@link CleverTapResponseDecorator} objects
 */
public abstract class CleverTapResponse {

    /**
     * Whether this processor should run while a user switch is in progress. Defaults to {@code true};
     * processors that must be skipped during a user switch override this to return {@code false}.
     * Lets {@link ClevertapResponseHandler} decide inclusion polymorphically instead of checking
     * concrete types.
     */
    public boolean runsDuringUserSwitch() {
        return true;
    }

    @WorkerThread
    public void processResponse(
            final JSONObject jsonBody,
            final String stringBody,
            final Context context
    ) {
        Log.i("CleverTapResponse", "Done processing response!");
    }

    /**
     * Context-aware entry point. Decorators that need request-scoped flags (source / isFullResponse /
     * isUserSwitching) override this; the default ignores the context and runs the plain overload.
     */
    @WorkerThread
    public void processResponse(
            final JSONObject jsonBody,
            final String stringBody,
            final Context context,
            final ResponseContext responseContext
    ) {
        processResponse(jsonBody, stringBody, context);
    }
}