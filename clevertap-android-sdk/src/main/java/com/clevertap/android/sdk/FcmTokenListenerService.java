package com.clevertap.android.sdk;

import com.google.firebase.iid.FirebaseInstanceIdService;

public class FcmTokenListenerService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        Logger.d("FcmTokenListenerService: onTokenRefresh");
        CleverTapAPI.tokenRefresh(this);
    }
}
