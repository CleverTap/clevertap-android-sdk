package com.clevertap.android.sdk.pushnotification.fcm;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.PackageUtils;
import com.clevertap.android.sdk.pushnotification.IPushCallback;
import com.clevertap.android.sdk.pushnotification.IPushProvider;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.clevertap.android.sdk.pushnotification.PushUtils;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import static com.clevertap.android.sdk.pushnotification.PushConstants.ANDROID_PLATFORM;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.FCM;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FcmPushProvider implements IPushProvider {
    private IPushCallback listener;
    private static String LOG_TAG = FcmPushProvider.class.getSimpleName();

    @Override
    public void setListener(IPushCallback listener) {
        this.listener = listener;
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
                token = FirebaseInstanceId.getInstance().getToken(senderId, FirebaseMessaging.INSTANCE_ID_SCOPE);
                listener.log(LOG_TAG, "FCM token for Sender Id - " + senderId + " is " + token);
            } else {

                //noinspection deprecation
                token = FirebaseInstanceId.getInstance().getToken();
                listener.log(LOG_TAG, "FCM token is " + token);
            }

        } catch (Throwable t) {
            listener.log(LOG_TAG, "Error requesting FCM token", t);
        }

        return token;
    }

    /**
     * App supports FCM
     *
     * @return boolean true if FCM services are available
     */
    @Override
    public boolean isAvailable() {
        try {
            if (!PackageUtils.isGooglePlayServicesAvailable(listener.context())) {
                listener.log(LOG_TAG, "Google Play services is currently unavailable.");
                return false;
            }

            String senderId = getSenderId();
            if (senderId == null) {
                listener.log(LOG_TAG, "The FCM sender ID is not set. Unable to register for FCM.");
                return false;
            }
        } catch (Exception e) {
            listener.log(LOG_TAG, "Unable to register with FCM.", e);
            return false;
        }
        return true;
    }

    private String getSenderId() {
        String senderId = PushUtils.getFCMSenderID(listener.context());
        if (!TextUtils.isEmpty(senderId)) {
            return senderId;
        }
        FirebaseApp app = FirebaseApp.getInstance();
        return app.getOptions().getGcmSenderId();
    }

    /**
     * Device supports FCM
     *
     * @return - true if FCM is supported in the platform
     */
    @Override
    public boolean isSupported() {
        return PackageUtils.isGooglePlayStoreAvailable(listener.context());
    }

    @Override
    public int minSDKSupportVersionCode() {
        return 0;// supporting FCM from base version
    }
}