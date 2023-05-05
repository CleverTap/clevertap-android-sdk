package com.clevertap.android.sdk.product_config;

import android.content.Context;
import com.clevertap.android.sdk.BaseAnalyticsManager;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.utils.FileUtils;

/**
 * Factory class to get {@link CTProductConfigController} instance for a particular configuration

 * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
 *      Note: This class has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
 * </p>
 */
@Deprecated
public class CTProductConfigFactory {

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public static CTProductConfigController getInstance(Context context, DeviceInfo deviceInfo,
            CleverTapInstanceConfig config, BaseAnalyticsManager baseAnalyticsManager, CoreMetaData coreMetaData,
            BaseCallbackManager callbackManager) {
        final String guid = deviceInfo.getDeviceID();
        FileUtils fileUtils = new FileUtils(context, config);
        ProductConfigSettings configSettings = new ProductConfigSettings(guid, config, fileUtils);
        return new CTProductConfigController(context, config, baseAnalyticsManager, coreMetaData, callbackManager,
                configSettings, fileUtils);
    }
}