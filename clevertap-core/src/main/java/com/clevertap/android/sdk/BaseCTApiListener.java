package com.clevertap.android.sdk;

import android.content.Context;

public interface BaseCTApiListener {

    CleverTapInstanceConfig config();

    Context context();
}