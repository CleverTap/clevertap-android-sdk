package com.clevertap.android.sdk.response;

import android.content.Context;
import android.util.Log;
import androidx.annotation.WorkerThread;
import org.json.JSONObject;

/**
 * Abstract Response that will be wrapped by {@link CleverTapResponseDecorator} objects
 */
public abstract class CleverTapResponse {

    public boolean isFullResponse = false; // todo this is volatile, could not fix using current infra

    // Which endpoint this response came from. Set per-response by ClevertapResponseHandler before
    // each processResponse call (same mechanism as isFullResponse). Defaults to A1 so any path that
    // invokes processResponse directly behaves as an /a1 response.
    public CTResponseSource responseSource = CTResponseSource.A1;

    @WorkerThread
    public void processResponse(
            final JSONObject jsonBody,
            final String stringBody,
            final Context context
    ) {
        Log.i("CleverTapResponse", "Done processing response!");
    }
}