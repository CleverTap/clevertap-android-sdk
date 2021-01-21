package com.clevertap.android.sdk;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;

abstract class CleverTapResponse {

    protected CoreState mCorestate;

    CoreState getCoreState() {
        return mCorestate;
    }

    void setCoreState(CoreState coreState) {
        mCorestate = coreState;
    }

    void processResponse(final JSONObject jsonBody, final String stringBody,
            final Context context) {
        Log.i("CleverTapResponse", "Done processing response!");
    }
}
