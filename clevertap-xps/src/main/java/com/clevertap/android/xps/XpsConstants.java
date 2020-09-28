package com.clevertap.android.xps;

import androidx.annotation.IntDef;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface XpsConstants {

    @IntDef({TOKEN_SUCCESS, TOKEN_FAILURE, INVALID_TOKEN, OTHER_COMMAND, FAILED_WITH_EXCEPTION})
    @Retention(RetentionPolicy.SOURCE)
    @interface CommandResult {

    }

    String XIAOMI_LOG_TAG = PushType.XPS.toString();
    int MIN_CT_ANDROID_SDK_VERSION = 30800;
    int TOKEN_SUCCESS = 0;
    int TOKEN_FAILURE = 1;
    int INVALID_TOKEN = 2;
    int OTHER_COMMAND = 3;
    int FAILED_WITH_EXCEPTION = 4;
}