package com.clevertap.android.sdk;

import android.content.Context;

public interface BaseCTApiListener {

    Context context();

    CleverTapInstanceConfig config();
}