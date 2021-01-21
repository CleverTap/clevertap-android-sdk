package com.clevertap.android.sdk;

import com.clevertap.android.sdk.featureFlags.FeatureFlagListener;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;

public interface CleverTapAPIListener extends
        CTInAppNotification.CTInAppNotificationListener,
        InAppNotificationActivity.InAppActivityListener,
        CTInAppBaseFragment.InAppListener,
        CTInboxActivity.InboxActivityListener,
        FeatureFlagListener{
}