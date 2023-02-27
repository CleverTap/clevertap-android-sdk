package com.clevertap.android.sdk.network;

import android.content.Context;
import com.clevertap.android.sdk.events.EventGroup;
import org.json.JSONArray;

public abstract class BaseNetworkManager {

    public abstract void flushDBQueue(final Context context, final EventGroup eventGroup);

    public abstract int getDelayFrequency();

    public abstract void initHandshake(final EventGroup eventGroup,
            final Runnable handshakeSuccessCallback);

    public abstract boolean needsHandshakeForDomain(final EventGroup eventGroup);

    public abstract boolean sendQueue(final Context context, final EventGroup eventGroup, final JSONArray queue);

}