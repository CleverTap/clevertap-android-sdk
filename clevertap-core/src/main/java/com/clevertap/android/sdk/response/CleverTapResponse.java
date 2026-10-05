package com.clevertap.android.sdk.response;

import android.content.Context;
import android.util.Log;
import androidx.annotation.WorkerThread;
import org.json.JSONObject;

/**
 * Abstract Response that will be wrapped by {@link CleverTapResponseDecorator} objects
 */
public abstract class CleverTapResponse {

    // Which endpoint this response came from. Set per-response by ClevertapResponseHandler before
    // each processResponse call. Defaults to A1 so any path that invokes processResponse directly
    // behaves as an /a1 response. TODO: still a shared mutable field (read by InAppResponse,
    // ContentFetchResponse, DisplayUnitResponse) — fold into an immutable ResponseContext param.
    public CTResponseSource responseSource = CTResponseSource.A1;

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
}