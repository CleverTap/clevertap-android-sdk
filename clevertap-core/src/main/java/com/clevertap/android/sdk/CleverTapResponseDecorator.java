package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONObject;

abstract class CleverTapResponseDecorator extends CleverTapResponse {

    abstract void processResponse(JSONObject jsonBody, String stringBody,
            Context context);
}
