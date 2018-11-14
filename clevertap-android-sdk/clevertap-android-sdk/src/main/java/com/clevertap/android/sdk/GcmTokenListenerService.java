package com.clevertap.android.sdk;

import com.google.android.gms.iid.InstanceIDListenerService;

@Deprecated
public class GcmTokenListenerService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        Logger.d("GcmTokenListenerService: onTokenRefresh");
        CleverTapAPI.tokenRefresh(this);
    }
}
