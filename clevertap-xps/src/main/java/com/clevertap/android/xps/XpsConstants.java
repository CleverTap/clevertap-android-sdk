package com.clevertap.android.xps;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface XpsConstants {

    String LOG_TAG = XiaomiPushProvider.class.getSimpleName();
    int MIN_CT_ANDROID_SDK_VERSION = 30800;

    int TOKEN_SUCCESS = 0;
    int TOKEN_FAILURE = 1;
    int INVALID_TOKEN = 2;
    int OTHER_COMMAND = 3;
    int FAILED_WITH_EXCEPTION = 4;

    @IntDef({TOKEN_SUCCESS, TOKEN_FAILURE, INVALID_TOKEN, OTHER_COMMAND, FAILED_WITH_EXCEPTION})
    @Retention(RetentionPolicy.SOURCE)
    @interface CommandResult {
    }
}