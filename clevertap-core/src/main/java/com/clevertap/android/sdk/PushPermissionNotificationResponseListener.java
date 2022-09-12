package com.clevertap.android.sdk;

/**
 * A listener for notification permission.
 */
public interface PushPermissionNotificationResponseListener {

    /**
     * This is called when user either grants allow/dismiss permission for notifications for Android 13+
     *
     * @param accepted This boolean will return true if notification permission is granted and will retrun
     *                 false if permission is denied.
     */
    void response(boolean accepted);//Change method name here, sounds ambiguous
}
