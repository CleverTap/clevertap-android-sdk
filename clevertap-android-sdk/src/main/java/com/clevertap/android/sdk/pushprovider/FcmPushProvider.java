package com.clevertap.android.sdk.pushprovider;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.PackageUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import static com.clevertap.android.sdk.pushprovider.PushConstants.ANDROID_PLATFORM;
import static com.clevertap.android.sdk.pushprovider.PushConstants.PushType.FCM;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FcmPushProvider extends PushProvider {

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

            String senderId = getSenderId();

            if (senderId != null) {
                config.getLogger().verbose(config.getAccountId(), "FcmManager: Requesting a FCM token with Sender Id - " + senderId);
                token = FirebaseInstanceId.getInstance().getToken(senderId, FirebaseMessaging.INSTANCE_ID_SCOPE);
            } else {
                config.getLogger().verbose(config.getAccountId(), "FcmManager: Requesting a FCM token");
                //noinspection deprecation
                token = FirebaseInstanceId.getInstance().getToken();
            }
        } catch (Throwable t) {
            config.getLogger().verbose("FcmManager: Error requesting FCM token", t);
        }

        return token;
    }

    /**
     * App supports FCM
     * @return
     */
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
        return app != null ? app.getOptions().getGcmSenderId() : null;
    }

    /**
     * Device supports FCM
     * @return
     */
    @Override
    public boolean isSupported() {
        return PackageUtils.isGooglePlayStoreAvailable(context);
    }

    @Override
    public int minSDKSupportVersionCode() {
        return 0;// supporting FCM from base version
    }
}