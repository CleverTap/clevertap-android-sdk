package com.clevertap.android.sdk.featureFlags;

import android.content.Context;
import com.clevertap.android.sdk.BaseAnalyticsManager;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.FileUtils;

/**
 * Factory class to get {@link CTFeatureFlagsController} instance for a particular configuration
 */
public class CTFeatureFlagsFactory {

    public static CTFeatureFlagsController getInstance(Context context, String guid, CleverTapInstanceConfig config,
            BaseCallbackManager callbackManager, BaseAnalyticsManager analyticsManager) {
        FileUtils fileUtils = new FileUtils(context, config);
        return new CTFeatureFlagsController(guid, config, callbackManager, analyticsManager, fileUtils);
    }
}