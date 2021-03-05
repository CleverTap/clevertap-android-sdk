package com.clevertap.android.sdk.pushnotification.fcm;

import static com.clevertap.android.sdk.pushnotification.PushConstants.FCM_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.FCM;
import static com.clevertap.android.sdk.utils.PackageUtils.isGooglePlayServicesAvailable;
import static com.clevertap.android.sdk.utils.PackageUtils.isGooglePlayStoreAvailable;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import com.clevertap.android.sdk.utils.Utils;
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

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private ManifestInfo mManifestInfo;

    public FcmSdkHandlerImpl(final CTPushProviderListener listener, final Context context,
            final CleverTapInstanceConfig config) {
        mContext = context;
        mConfig = config;
        this.listener = listener;
        this.mManifestInfo = ManifestInfo.getInstance(context);
    }

    public PushType getPushType() {
        return FCM;
    }

    @Override
    public boolean isAvailable() {
        try {
            if (!isGooglePlayServicesAvailable(mContext)) {
                mConfig.log(LOG_TAG, FCM_LOG_TAG + "Google Play services is currently unavailable.");
                return false;
            }

            String senderId = getSenderId();
            if (TextUtils.isEmpty(senderId)) {
                mConfig
                        .log(LOG_TAG, FCM_LOG_TAG + "The FCM sender ID is not set. Unable to register for FCM.");
                return false;
            }
        } catch (Throwable t) {
            mConfig.log(LOG_TAG, FCM_LOG_TAG + "Unable to register with FCM.", t);
            return false;
        }
        return true;
    }

    @Override
    public boolean isSupported() {
        return isGooglePlayStoreAvailable(mContext);
    }

    @Override
    public void requestToken() {
        try {
            String tokenUsingManifestMetaEntry = Utils
                    .getFcmTokenUsingManifestMetaEntry(mContext, mConfig);
            if (!TextUtils.isEmpty(tokenUsingManifestMetaEntry)) {
                mConfig.log(LOG_TAG, FCM_LOG_TAG + "FCM token - " + tokenUsingManifestMetaEntry);
                listener.onNewToken(tokenUsingManifestMetaEntry, getPushType());
            } else {
                mConfig
                        .log(LOG_TAG, FCM_LOG_TAG + "Requesting FCM token using googleservices.json");
                FirebaseInstanceId.getInstance().getInstanceId()
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                if (!task.isSuccessful()) {
                                    mConfig
                                            .log(LOG_TAG, FCM_LOG_TAG + "FCM token using googleservices.json failed",
                                                    task.getException());
                                    listener.onNewToken(null, getPushType());
                                    return;
                                }

                                // Get new Instance ID token
                                String token = task.getResult() != null ? task.getResult().getToken() : null;
                                mConfig
                                        .log(LOG_TAG, FCM_LOG_TAG + "FCM token using googleservices.json - " + token);
                                listener.onNewToken(token, getPushType());
                            }
                        });
            }
        } catch (Throwable t) {
            mConfig.log(LOG_TAG, FCM_LOG_TAG + "Error requesting FCM token", t);
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