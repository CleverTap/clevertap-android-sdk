package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.Utils.runOnUiThread;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.clevertap.android.sdk.displayunits.CTDisplayUnitController;
import com.clevertap.android.sdk.displayunits.DisplayUnitListener;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.inapp.callbacks.FetchInAppsCallback;
import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener;
import com.clevertap.android.sdk.interfaces.SCDomainListener;
import com.clevertap.android.sdk.login.ChangeUserCallback;
import com.clevertap.android.sdk.network.BatchListener;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestrictTo(Scope.LIBRARY)
public class CallbackManager extends BaseCallbackManager {

    private WeakReference<DisplayUnitListener> displayUnitListenerWeakReference;

    private GeofenceCallback geofenceCallback;

    private SCDomainListener scDomainListener;

    private WeakReference<InAppNotificationButtonListener> inAppNotificationButtonListener;

    private InAppNotificationListener inAppNotificationListener;

    private final List<PushPermissionResponseListener> pushPermissionResponseListenerList = new ArrayList<>();


    private final CleverTapInstanceConfig config;

    private final DeviceInfo deviceInfo;

    private FailureFlushListener failureFlushListener;

    @Deprecated
    private WeakReference<CTFeatureFlagsListener> featureFlagListenerWeakReference;

    private final List<OnInitCleverTapIDListener> onInitCleverTapIDListeners =  Collections.synchronizedList(new ArrayList<>());

    @Deprecated
    private WeakReference<CTProductConfigListener> productConfigListener;

    private CTPushNotificationListener pushNotificationListener = null;

    private SyncListener syncListener = null;

    private FetchInAppsCallback fetchInAppsCallback;

    private final List<ChangeUserCallback> changeUserCallbackList = Collections.synchronizedList(new ArrayList<>());

    public CallbackManager(CleverTapInstanceConfig config, DeviceInfo deviceInfo) {
        this.config = config;
        this.deviceInfo = deviceInfo;
    }

    private BatchListener batchListener;
    private CTFeatureFlagsController ctFeatureFlagsController;
    private CTDisplayUnitController ctDisplayUnitController;

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

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    @Override
    public CTFeatureFlagsListener getFeatureFlagListener() {
        if (featureFlagListenerWeakReference != null && featureFlagListenerWeakReference.get() != null) {
            return featureFlagListenerWeakReference.get();
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
    public void setFeatureFlagListener(final CTFeatureFlagsListener listener) {
        this.featureFlagListenerWeakReference = new WeakReference<>(listener);
    }

    @Override
    public GeofenceCallback getGeofenceCallback() {
        return geofenceCallback;
    }

    @Override
    public void setGeofenceCallback(final GeofenceCallback geofenceCallback) {
        this.geofenceCallback = geofenceCallback;
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
    public InAppNotificationButtonListener getInAppNotificationButtonListener() {
        if (inAppNotificationButtonListener != null && inAppNotificationButtonListener.get() != null) {
            return inAppNotificationButtonListener.get();
        }
        return null;
    }

    @Override
    public void setInAppNotificationButtonListener(
            InAppNotificationButtonListener inAppNotificationButtonListener) {
        this.inAppNotificationButtonListener = new WeakReference<>(inAppNotificationButtonListener);
    }

    @Override
    public InAppNotificationListener getInAppNotificationListener() {
        return inAppNotificationListener;
    }

    @Override
    public List<PushPermissionResponseListener> getPushPermissionResponseListenerList() {
        return pushPermissionResponseListenerList;
    }

    @Override
    public void setInAppNotificationListener(final InAppNotificationListener inAppNotificationListener) {
        this.inAppNotificationListener = inAppNotificationListener;
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

    /**
     * Notify the registered Display Unit listener about the running Display Unit campaigns
     *
     * @param displayUnits - Array of Display Units {@link CleverTapDisplayUnit}
     */
    public void notifyDisplayUnitsLoaded(final ArrayList<CleverTapDisplayUnit> displayUnits) {
        if (displayUnits != null && !displayUnits.isEmpty()) {
            if (displayUnitListenerWeakReference != null && displayUnitListenerWeakReference.get() != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //double check to ensure null safety
                        if (displayUnitListenerWeakReference != null
                                && displayUnitListenerWeakReference.get() != null) {
                            displayUnitListenerWeakReference.get().onDisplayUnitsLoaded(displayUnits);
                        }
                    }
                });
            } else {
                config.getLogger().verbose(config.getAccountId(),
                        Constants.FEATURE_DISPLAY_UNIT + "No registered listener, failed to notify");
            }
        } else {
            config.getLogger()
                    .verbose(config.getAccountId(), Constants.FEATURE_DISPLAY_UNIT + "No Display Units found");
        }
    }

    void notifyUserProfileInitialized() {
        notifyUserProfileInitialized(deviceInfo.getDeviceID());
    }

    @Override
    public FetchInAppsCallback getFetchInAppsCallback() {
        return fetchInAppsCallback;
    }

    @Override
    public void setFetchInAppsCallback(FetchInAppsCallback fetchInAppsCallback) {
        this.fetchInAppsCallback = fetchInAppsCallback;
    }

    public BatchListener getBatchListener() {
        return batchListener;
    }

    @Override
    public void setBatchListener(BatchListener batchListener) {
        this.batchListener = batchListener;
    }

    @Override
    public CTFeatureFlagsController getCTFeatureFlagsController() {
        return this.ctFeatureFlagsController;
    }

    @Override
    public void setCTFeatureFlagsController(CTFeatureFlagsController ctFeatureFlagsController) {
        this.ctFeatureFlagsController = ctFeatureFlagsController;
    }
}