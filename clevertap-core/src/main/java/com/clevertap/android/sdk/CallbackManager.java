package com.clevertap.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener;
import com.clevertap.android.sdk.interfaces.SCDomainListener;
import com.clevertap.android.sdk.login.ChangeUserCallback;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestrictTo(Scope.LIBRARY)
public class CallbackManager extends BaseCallbackManager {

    private SCDomainListener scDomainListener;

    private final List<PushPermissionResponseListener> pushPermissionResponseListenerList = new ArrayList<>();

    private final DeviceInfo deviceInfo;

    private FailureFlushListener failureFlushListener;

    @Deprecated
    private final List<OnInitCleverTapIDListener> onInitCleverTapIDListeners =  Collections.synchronizedList(new ArrayList<>());

    @Deprecated
    private WeakReference<CTProductConfigListener> productConfigListener;

    private CTPushNotificationListener pushNotificationListener = null;

    private SyncListener syncListener = null;

    private final List<ChangeUserCallback> changeUserCallbackList = Collections.synchronizedList(new ArrayList<>());
    private GeofenceCallback geofenceCallback;

    public CallbackManager(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    @Override
    public List<ChangeUserCallback> getChangeUserCallbackList() {
        return changeUserCallbackList;
    }

    @Override
    public void addChangeUserCallback(ChangeUserCallback callback) {
        changeUserCallbackList.add(callback);
    }

    @Override
    public void removeChangeUserCallback(ChangeUserCallback callback) {
        changeUserCallbackList.remove(callback);
    }

    @Override
    public FailureFlushListener getFailureFlushListener() {
        return failureFlushListener;
    }

    @Override
    public void setFailureFlushListener(final FailureFlushListener failureFlushListener) {
        this.failureFlushListener = failureFlushListener;
    }

    @Override
    public GeofenceCallback getGeofenceCallback() {
        return geofenceCallback;
    }

    @Override
    public SCDomainListener getSCDomainListener() {
        return scDomainListener;
    }

    @Override
    public void setSCDomainListener(SCDomainListener scDomainListener) {
        this.scDomainListener = scDomainListener;
    }

    @Override
    public List<PushPermissionResponseListener> getPushPermissionResponseListenerList() {
        return pushPermissionResponseListenerList;
    }

    @Override
    public void registerPushPermissionResponseListener(PushPermissionResponseListener pushPermissionResponseListener) {
        this.pushPermissionResponseListenerList.add(pushPermissionResponseListener);
    }

    @Override
    public void unregisterPushPermissionResponseListener(PushPermissionResponseListener pushPermissionResponseListener) {
        this.pushPermissionResponseListenerList.remove(pushPermissionResponseListener);
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    @Override
    public CTProductConfigListener getProductConfigListener() {
        if (productConfigListener != null && productConfigListener.get() != null) {
            return productConfigListener.get();
        }
        return null;
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    @Override
    public void setProductConfigListener(final CTProductConfigListener productConfigListener) {
        if (productConfigListener != null) {
            this.productConfigListener = new WeakReference<>(productConfigListener);
        }
    }

    @Override
    public CTPushNotificationListener getPushNotificationListener() {
        return pushNotificationListener;
    }

    @Override
    public void setPushNotificationListener(
            final CTPushNotificationListener pushNotificationListener) {
        this.pushNotificationListener = pushNotificationListener;
    }

    @Override
    public SyncListener getSyncListener() {
        return syncListener;
    }

    @Override
    public void setSyncListener(final SyncListener syncListener) {
        this.syncListener = syncListener;
    }

    @Override
    public void addOnInitCleverTapIDListener(@NonNull final OnInitCleverTapIDListener onInitCleverTapIDListener) {
        onInitCleverTapIDListeners.add(onInitCleverTapIDListener);
    }

    @Override
    public void removeOnInitCleverTapIDListener(@NonNull final OnInitCleverTapIDListener listener) {
        onInitCleverTapIDListeners.remove(listener);
    }

    @Override
    public void notifyCleverTapIDChanged(final String id) {
        synchronized (onInitCleverTapIDListeners) {
            for (final OnInitCleverTapIDListener listener : onInitCleverTapIDListeners) {
                if (listener != null) {
                    listener.onInitCleverTapID(id);
                }
            }
        }
    }

    //Profile
    @Override
    public void notifyUserProfileInitialized(String deviceID) {
        deviceID = (deviceID != null) ? deviceID : deviceInfo.getDeviceID();

        if (deviceID == null) {
            return;
        }

        final SyncListener sl;
        try {
            sl = getSyncListener();
            if (sl != null) {
                sl.profileDidInitialize(deviceID);
            }
        } catch (Throwable t) {
            // Ignore
        }
    }

    void notifyUserProfileInitialized() {
        notifyUserProfileInitialized(deviceInfo.getDeviceID());
    }
}