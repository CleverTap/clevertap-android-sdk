package com.clevertap.android.directcall.interfaces;

import com.clevertap.android.directcall.init.DirectCallAPI;

public interface OnMissedCallHostSetReceiver {

    void onSetMissedCallReceiver(DirectCallAPI.MissedCallNotificationOpenedHandler missedCallNotificationOpenedHandler);
}
