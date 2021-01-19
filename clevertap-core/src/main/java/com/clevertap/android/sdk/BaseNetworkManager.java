package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONArray;

abstract class BaseNetworkManager {

    abstract void initHandshake(final EventGroup eventGroup,
            final Runnable handshakeSuccessCallback);

    abstract boolean needsHandshakeForDomain(final EventGroup eventGroup);

    abstract boolean sendQueue(final Context context, final EventGroup eventGroup, final JSONArray queue);

}