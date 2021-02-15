package com.clevertap.android.sdk.response;

import android.content.Context;
import org.json.JSONObject;

abstract class CleverTapResponseDecorator extends CleverTapResponse {

    public abstract void processResponse(JSONObject jsonBody, String stringBody,
            Context context);
}
