package com.clevertap.android.sdk;

import android.content.Context;
import com.clevertap.android.sdk.validation.ValidationResultStack;

public interface BaseCTApiListener {

    CleverTapInstanceConfig config();

    Context context();

    DeviceInfo deviceInfo();

    ValidationResultStack remoteErrorLogger();
}