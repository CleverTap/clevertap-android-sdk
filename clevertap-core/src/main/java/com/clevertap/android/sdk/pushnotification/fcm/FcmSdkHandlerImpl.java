package com.clevertap.android.sdk.pushnotification.fcm;

import static com.clevertap.android.sdk.PackageUtils.isGooglePlayServicesAvailable;
import static com.clevertap.android.sdk.PackageUtils.isGooglePlayStoreAvailable;
import static com.clevertap.android.sdk.pushnotification.PushConstants.FCM_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.FCM;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

/**
 * implementation of {@link IFcmMessageHandler}
 */
public class FcmSdkHandlerImpl implements IFcmSdkHandler {

    private final CTPushProviderListener listener;

    private ManifestInfo mManifestInfo;

    public FcmSdkHandlerImpl(final CTPushProviderListener listener) {
        this.listener = listener;
        this.mManifestInfo = ManifestInfo.getInstance(listener.context());
    }

    public PushType getPushType() {
        return FCM;
    }

    @Override
    public boolean isAvailable() {
        try {
            if (!isGooglePlayServicesAvailable(listener.context())) {
                listener.config().log(LOG_TAG, FCM_LOG_TAG + "Google Play services is currently unavailable.");
                return false;
            }

            String senderId = getSenderId();
            if (TextUtils.isEmpty(senderId)) {
                listener.config()
                        .log(LOG_TAG, FCM_LOG_TAG + "The FCM sender ID is not set. Unable to register for FCM.");
                return false;
            }
        } catch (Throwable t) {
            listener.config().log(LOG_TAG, FCM_LOG_TAG + "Unable to register with FCM.", t);
            return false;
        }
        return true;
    }

    @Override
    public boolean isSupported() {
        return isGooglePlayStoreAvailable(listener.context());
    }

    @Override
    public void requestToken() {
        try {
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                listener.config()
                                        .log(LOG_TAG, FCM_LOG_TAG + "getInstanceId failed", task.getException());
                                listener.onNewToken(null, getPushType());
                                return;
                            }

                            // Get new Instance ID token
                            String token = task.getResult() != null ? task.getResult().getToken() : null;
                            listener.config().log(LOG_TAG, FCM_LOG_TAG + "FCM token - " + token);
                            listener.onNewToken(token, getPushType());
                        }
                    });

        } catch (Throwable t) {
            listener.config().log(LOG_TAG, FCM_LOG_TAG + "Error requesting FCM token", t);
            listener.onNewToken(null, getPushType());
        }
    }

    String getFCMSenderID() {
        return mManifestInfo.getFCMSenderId();
    }

    String getSenderId() {
        String senderId = getFCMSenderID();
        if (!TextUtils.isEmpty(senderId)) {
            return senderId;
        }
        FirebaseApp app = FirebaseApp.getInstance();
        return app.getOptions().getGcmSenderId();
    }

    void setManifestInfo(final ManifestInfo manifestInfo) {
        mManifestInfo = manifestInfo;
    }
}