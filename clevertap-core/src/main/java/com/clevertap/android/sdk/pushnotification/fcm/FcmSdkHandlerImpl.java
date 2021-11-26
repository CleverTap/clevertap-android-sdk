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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * implementation of {@link IFcmMessageHandler}
 */
public class FcmSdkHandlerImpl implements IFcmSdkHandler {

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final CTPushProviderListener listener;

    private ManifestInfo manifestInfo;

    public FcmSdkHandlerImpl(final CTPushProviderListener listener, final Context context,
            final CleverTapInstanceConfig config) {
        this.context = context;
        this.config = config;
        this.listener = listener;
        this.manifestInfo = ManifestInfo.getInstance(context);
    }

    public PushType getPushType() {
        return FCM;
    }

    @Override
    public boolean isAvailable() {
        try {
            if (!isGooglePlayServicesAvailable(context)) {
                config.log(LOG_TAG, FCM_LOG_TAG + "Google Play services is currently unavailable.");
                return false;
            }

            String senderId = getSenderId();
            if (TextUtils.isEmpty(senderId)) {
                config
                        .log(LOG_TAG, FCM_LOG_TAG + "The FCM sender ID is not set. Unable to register for FCM.");
                return false;
            }
        } catch (Throwable t) {
            config.log(LOG_TAG, FCM_LOG_TAG + "Unable to register with FCM.", t);
            return false;
        }
        return true;
    }

    @Override
    public boolean isSupported() {
        return isGooglePlayStoreAvailable(context);
    }

    @Override
    public void requestToken() {
        try {
            config.log(LOG_TAG, FCM_LOG_TAG + "Requesting FCM token using googleservices.json");
            FirebaseMessaging
                    .getInstance()
                    .getToken()
                    .addOnCompleteListener
                            (new OnCompleteListener<String>() {
                                 @Override
                                 public void onComplete(@NonNull final Task<String> task) {
                                     if (!task.isSuccessful()) {
                                         config.log(LOG_TAG,
                                                 FCM_LOG_TAG + "FCM token using googleservices.json failed",
                                                 task.getException());
                                         listener.onNewToken(null, getPushType());
                                         return;
                                     }
                                     String token = task.getResult() != null ? task.getResult() : null;
                                     config.log(LOG_TAG,
                                             FCM_LOG_TAG + "FCM token using googleservices.json - " + token);
                                     listener.onNewToken(token, getPushType());
                                 }
                             }
                            );
        } catch (Throwable t) {
            config.log(LOG_TAG, FCM_LOG_TAG + "Error requesting FCM token", t);
            listener.onNewToken(null, getPushType());
        }
    }

    String getSenderId() {
        FirebaseApp app = FirebaseApp.getInstance();
        return app.getOptions().getGcmSenderId();
    }

    void setManifestInfo(final ManifestInfo manifestInfo) {
        this.manifestInfo = manifestInfo;
    }
}