package com.clevertap.android.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener;
import com.clevertap.android.sdk.interfaces.SCDomainListener;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestrictTo(Scope.LIBRARY)
public class CallbackManager extends BaseCallbackManager {

    private SCDomainListener scDomainListener;

    private final List<PushPermissionResponseListener> pushPermissionResponseListenerList = new ArrayList<>();

    private final DeviceInfo deviceInfo;

    @Deprecated
    private final List<OnInitCleverTapIDListener> onInitCleverTapIDListeners =  Collections.synchronizedList(new ArrayList<>());

    @Deprecated
    private WeakReference<CTProductConfigListener> productConfigListener;

    private SyncListener syncListener = null;

    public CallbackManager(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
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