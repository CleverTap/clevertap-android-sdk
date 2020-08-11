package com.clevertap.android.sdk.pushprovider;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.PackageUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import static com.clevertap.android.sdk.pushprovider.PushConstants.ANDROID_PLATFORM;
import static com.clevertap.android.sdk.pushprovider.PushConstants.PushType.FCM;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FcmPushProvider extends PushProvider {

    private CleverTapInstanceConfig config;

    public FcmPushProvider(@NonNull Context context, @NonNull CleverTapInstanceConfig config) {
        super(context, config);
    }

    @Override
    public int getPlatform() {
        return ANDROID_PLATFORM;
    }

    @NonNull
    @Override
    public PushConstants.PushType getPushType() {
        return FCM;
    }


    @Nullable
    @Override
    public String getRegistrationToken() {
        String token = null;
        try {

            FirebaseApp app = FirebaseApp.getInstance();
            String senderId = getSenderId();
            if (senderId == null) {
                config.getLogger().verbose("The FCM sender ID is not set. Unable to register with FCM.");
                return null;
            }

            FirebaseInstanceId instanceId = FirebaseInstanceId.getInstance(app);
            token = instanceId.getToken(senderId, FirebaseMessaging.INSTANCE_ID_SCOPE);

        } catch (Exception e) {
            config.getLogger().verbose("FCM error " + e.getMessage(), e);
        }

        return token;
    }

    @Override
    public boolean isAvailable() {
        try {
            if (!PackageUtils.isGooglePlayServicesAvailable(context)) {
                config.getLogger().verbose("Google Play services is currently unavailable.");
                return false;
            }

            String senderId = getSenderId();
            if (senderId == null) {
                config.getLogger().verbose("The FCM sender ID is not set. Unable to register for FCM.");
                return false;
            }
        } catch (Exception e) {
            config.getLogger().verbose("Unable to register with FCM.", e);
            return false;
        }
        return true;
    }

    private String getSenderId() {
        String senderId = PushUtils.getFCMSenderID(context);
        if (!TextUtils.isEmpty(senderId)) {
            return senderId;
        }

        FirebaseApp app = FirebaseApp.getInstance();
        return app.getOptions().getGcmSenderId();
    }

    @Override
    public boolean isSupported() {
        return PackageUtils.isGooglePlayStoreAvailable(context);
    }

    @Override
    public int minSDKSupportVersionCode() {
        return 0;// supporting FCM from base version
    }
}