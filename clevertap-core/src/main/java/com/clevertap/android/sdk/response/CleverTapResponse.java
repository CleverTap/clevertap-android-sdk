package com.clevertap.android.sdk.response;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;

public abstract class CleverTapResponse {

    public void processResponse(final JSONObject jsonBody, final String stringBody,
            final Context context) {
        Log.i("CleverTapResponse", "Done processing response!");
    }
}