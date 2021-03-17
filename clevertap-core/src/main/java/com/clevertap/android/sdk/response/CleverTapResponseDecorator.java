package com.clevertap.android.sdk.response;

import android.content.Context;
import org.json.JSONObject;

/**
 * Abstract Decorator that will be used to decorate {@link CleverTapResponseHelper}
 * <br>Extend this class to create different kind of response
 */
abstract class CleverTapResponseDecorator extends CleverTapResponse {

    public abstract void processResponse(JSONObject jsonBody, String stringBody,
            Context context);
}
