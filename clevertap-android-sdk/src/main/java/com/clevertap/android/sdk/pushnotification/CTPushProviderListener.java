package com.clevertap.android.sdk.pushnotification;

import android.content.Context;

public interface CTPushProviderListener {

    Context context();

    void log(String tag, String message);

    void log(String tag, String message, Throwable throwable);
}