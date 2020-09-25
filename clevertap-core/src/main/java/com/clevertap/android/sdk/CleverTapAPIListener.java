package com.clevertap.android.sdk;

import com.clevertap.android.sdk.ab_testing.CTABTestListener;
import com.clevertap.android.sdk.featureFlags.FeatureFlagListener;
import com.clevertap.android.sdk.product_config.CTProductConfigControllerListener;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import com.clevertap.android.sdk.pushnotification.CTApiPushListener;

public interface CleverTapAPIListener extends
        CTInAppNotification.CTInAppNotificationListener,
        InAppNotificationActivity.InAppActivityListener,
        CTInAppBaseFragment.InAppListener,
        CTInboxActivity.InboxActivityListener,
        CTABTestListener,
        FeatureFlagListener,
        CTProductConfigControllerListener,
        CTProductConfigListener,
        CTApiPushListener {

}