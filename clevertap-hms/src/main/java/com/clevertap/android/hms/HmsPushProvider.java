package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.HMS_LOG_TAG;
import static com.clevertap.android.hms.HmsConstants.HMS_SDK_NAME;
import static com.clevertap.android.hms.HmsConstants.MIN_CT_ANDROID_SDK_VERSION;
import static com.clevertap.android.hms.HmsConstants.HPS;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.pushnotification.CTPushProvider;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushType;
import java.util.HashMap;

/**
 * Clevertap's Huawei Plugin Ref: {@link CTPushProvider}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HmsPushProvider implements CTPushProvider {

    private final @NonNull
    CTPushProviderListener ctPushListener;

    private @NonNull
    IHmsSdkHandler hmsSdkHandler;

    private final CleverTapInstanceConfig config;

    @SuppressLint(value = "unused")
    public HmsPushProvider(@NonNull CTPushProviderListener ctPushListener, Context context,
            CleverTapInstanceConfig config) {
        this.ctPushListener = ctPushListener;
        this.config = config;
        this.hmsSdkHandler = new HmsSdkHandler(context, config);
    }

    @NonNull
    @Override
    public PushType getPushType() {
        return HPS;
    }

    @Override
    public boolean isAvailable() {
        return hmsSdkHandler.isAvailable();
    }

    @Override
    public boolean isSupported() {
        // First call Core makes on every device, so non-Huawei devices can register too if the instance is ready
        registerSdkVersion();
        return hmsSdkHandler.isSupported();
    }

    @Override
    public int minSDKSupportVersionCode() {
        return MIN_CT_ANDROID_SDK_VERSION;
    }

    @Override
    public void requestToken() {
        // Runs on every app launch once the instance exists, so this covers the case where isSupported was too early
        registerSdkVersion();
        ctPushListener.onNewToken(hmsSdkHandler.onNewToken(), getPushType());
    }

    void setHmsSdkHandler(@NonNull final IHmsSdkHandler hmsSdkHandler) {
        this.hmsSdkHandler = hmsSdkHandler;
    }

    /**
     * Registers this module's name and version with the CleverTapAPI instance that owns this provider,
     * so Core sends it in the af header of every request.
     * <p>
     * Core creates push providers on a background thread before the owning instance is added to
     * {@link CleverTapAPI#getInstances()}. This method only looks up existing instances to avoid
     * creating a duplicate via instanceWithConfig.
     * Called from isSupported and requestToken. Repeated calls are safe because Core stores versions
     * in a map keyed by module name.
     */
    void registerSdkVersion() {
        if (config == null) {
            return;
        }
        try {
            HashMap<String, CleverTapAPI> instances = CleverTapAPI.getInstances();
            CleverTapAPI cleverTapAPI = instances != null ? instances.get(config.getAccountId()) : null;
            if (cleverTapAPI == null) {
                return;
            }
            cleverTapAPI.setCustomSdkVersion(HMS_SDK_NAME, BuildConfig.VERSION_CODE);
            config.log(LOG_TAG, HMS_LOG_TAG + " Registered " + HMS_SDK_NAME + " version " + BuildConfig.VERSION_CODE);
        } catch (Throwable t) {
            // An older Core at runtime may not have setCustomSdkVersion. Registration is best effort.
            config.log(LOG_TAG, HMS_LOG_TAG + " Unable to register SDK version: " + t.getMessage());
        }
    }
}