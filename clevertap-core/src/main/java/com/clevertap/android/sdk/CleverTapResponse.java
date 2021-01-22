package com.clevertap.android.sdk;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;

abstract class CleverTapResponse {

    void processResponse(final JSONObject jsonBody, final String stringBody,
            final Context context) {
        Log.i("CleverTapResponse", "Done processing response!");
    }
}