package com.clevertap.android.sdk.network;

import android.content.Context;
import androidx.annotation.WorkerThread;
import com.clevertap.android.sdk.events.EventGroup;
import org.json.JSONArray;

public abstract class BaseNetworkManager {

    @WorkerThread
    public abstract void flushDBQueue(final Context context, final EventGroup eventGroup,final String caller);

    public abstract int getDelayFrequency();

    @WorkerThread
    public abstract void initHandshake(final EventGroup eventGroup,
            final @WorkerThread Runnable handshakeSuccessCallback);

    public abstract boolean needsHandshakeForDomain(final EventGroup eventGroup);

    @WorkerThread
    public abstract boolean sendQueue(final Context context, final EventGroup eventGroup, final JSONArray queue, final String caller);

}