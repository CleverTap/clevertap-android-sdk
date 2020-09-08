package com.clevertap.android.sdk.pushnotification.fcm;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.PackageUtils;
import com.clevertap.android.sdk.pushnotification.CTPushListener;
import com.clevertap.android.sdk.pushnotification.CTPushProvider;
import com.clevertap.android.sdk.pushnotification.CTRegistrationListener;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.clevertap.android.sdk.pushnotification.PushUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import static com.clevertap.android.sdk.pushnotification.PushConstants.ANDROID_PLATFORM;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.FCM;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FcmPushProvider implements CTPushProvider {
    private CTPushListener ctPushListener;
    private static String LOG_TAG = FcmPushProvider.class.getSimpleName();

    @Override
    public void setCTPushListener(CTPushListener listener) {
        this.ctPushListener = listener;
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
    public void getRegistrationToken(final CTRegistrationListener registrationListener) {
        try {
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                ctPushListener.log(LOG_TAG, "getInstanceId failed", task.getException());
                                if (registrationListener != null) {
                                    registrationListener.complete(null);
                                }
                                return;
                            }

                            // Get new Instance ID token
                            String token = task.getResult().getToken();
                            ctPushListener.log(LOG_TAG, "FCM token for Sender Id - " + token);
                            if (registrationListener != null) {
                                registrationListener.complete(token);
                            }
                        }
                    });

        } catch (Throwable t) {
            ctPushListener.log(LOG_TAG, "Error requesting FCM token", t);
            if (registrationListener != null) {
                registrationListener.complete(null);
            }
        }
    }

    /**
     * App supports FCM
     *
     * @return boolean true if FCM services are available
     */
    @Override
    public boolean isAvailable() {
        try {
            if (!PackageUtils.isGooglePlayServicesAvailable(ctPushListener.context())) {
                ctPushListener.log(LOG_TAG, "Google Play services is currently unavailable.");
                return false;
            }

            String senderId = getSenderId();
            if (senderId == null) {
                ctPushListener.log(LOG_TAG, "The FCM sender ID is not set. Unable to register for FCM.");
                return false;
            }
        } catch (Exception e) {
            ctPushListener.log(LOG_TAG, "Unable to register with FCM.", e);
            return false;
        }
        return true;
    }

    private String getSenderId() {
        String senderId = PushUtils.getFCMSenderID(ctPushListener.context());
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
        return PackageUtils.isGooglePlayStoreAvailable(ctPushListener.context());
    }

    @Override
    public int minSDKSupportVersionCode() {
        return 0;// supporting FCM from base version
    }
}