package com.clevertap.android.sdk;

import java.lang.ref.WeakReference;

@Deprecated
public class DataHandler {
    private WeakReference<CleverTapAPI> weakReference;

    DataHandler(CleverTapAPI cleverTapAPI){
        this.weakReference = new WeakReference<>(cleverTapAPI);
    }

    /**
     * Sends the GCM registration ID to CleverTap.
     *
     * @param gcmId    The GCM registration ID
     * @param register Boolean indicating whether to register
     *                 or not for receiving push messages from CleverTap.
     *                 Set this to true to receive push messages from CleverTap,
     *                 and false to not receive any messages from CleverTap.
     * @deprecated use {@link CleverTapAPI#pushGcmRegistrationId(String gcmId, boolean register)}
     */
    @Deprecated
    public void pushGcmRegistrationId(String gcmId, boolean register) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushGcmRegistrationId(gcmId, register);
        }
    }

    /**
     * Sends the FCM registration ID to CleverTap.
     *
     * @param fcmId    The FCM registration ID
     * @param register Boolean indicating whether to register
     *                 or not for receiving push messages from CleverTap.
     *                 Set this to true to receive push messages from CleverTap,
     *                 and false to not receive any messages from CleverTap.
     * @deprecated use {@link CleverTapAPI#pushFcmRegistrationId(String gcmId, boolean register)}
     */
    @Deprecated
    public void pushFcmRegistrationId(String fcmId, boolean register) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushFcmRegistrationId(fcmId, register);
        }
    }
}
