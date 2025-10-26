package com.clevertap.android.sdk;

import androidx.annotation.NonNull;

import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener;
import com.clevertap.android.sdk.interfaces.SCDomainListener;
import com.clevertap.android.sdk.login.ChangeUserCallback;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener;

import java.util.List;

public abstract class BaseCallbackManager {
    public abstract FailureFlushListener getFailureFlushListener();

    public abstract GeofenceCallback getGeofenceCallback();

    public abstract SCDomainListener getSCDomainListener();

    public abstract List<PushPermissionResponseListener> getPushPermissionResponseListenerList();

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public abstract CTProductConfigListener getProductConfigListener();

    public abstract CTPushNotificationListener getPushNotificationListener();

    public abstract SyncListener getSyncListener();

    //Profile
    public abstract void notifyUserProfileInitialized(String deviceID);

    abstract void notifyUserProfileInitialized();

    public abstract void setFailureFlushListener(FailureFlushListener failureFlushListener);

    public abstract void setSCDomainListener(SCDomainListener scDomainListener);

    public abstract void unregisterPushPermissionResponseListener(PushPermissionResponseListener pushPermissionResponseListener);

    public abstract void registerPushPermissionResponseListener(PushPermissionResponseListener pushPermissionResponseListener);

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public abstract void setProductConfigListener(
            CTProductConfigListener productConfigListener);

    public abstract void setPushNotificationListener(
            CTPushNotificationListener pushNotificationListener);

    public abstract void setSyncListener(SyncListener syncListener);

    public abstract void addOnInitCleverTapIDListener(@NonNull OnInitCleverTapIDListener listener);

    public abstract void removeOnInitCleverTapIDListener(@NonNull OnInitCleverTapIDListener listener);

    public abstract void notifyCleverTapIDChanged(String id);

    public abstract List<ChangeUserCallback> getChangeUserCallbackList();

    public abstract void addChangeUserCallback(ChangeUserCallback callback);

    public abstract void removeChangeUserCallback(ChangeUserCallback callback);

}
