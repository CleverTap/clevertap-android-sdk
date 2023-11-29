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

    @WorkerThread
    public void processResponse(
            final JSONObject jsonBody,
            final String stringBody,
            final Context context
    ) {
        Log.i("CleverTapResponse", "Done processing response!");
    }
}