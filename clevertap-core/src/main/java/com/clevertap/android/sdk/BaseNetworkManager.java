package com.clevertap.android.sdk;

abstract class BaseNetworkManager {

    abstract void initHandshake(final EventGroup eventGroup,
            final Runnable handshakeSuccessCallback);

    abstract boolean needsHandshakeForDomain(final EventGroup eventGroup);

}
