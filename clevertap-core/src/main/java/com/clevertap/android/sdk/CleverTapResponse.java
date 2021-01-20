package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONObject;

abstract class CleverTapResponse {

    private CoreState mCorestate;

    CoreState getCoreState() {
        return mCorestate;
    }

    void setCoreState(CoreState coreState) {
        mCorestate = coreState;
    }

    abstract void processResponse(final JSONObject jsonBody, final String stringBody,
            final Context context);
}
