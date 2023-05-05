package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.Utils.runOnUiThread;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.displayunits.DisplayUnitListener;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.interfaces.SCDomainListener;
import com.clevertap.android.sdk.interfaces.NotificationRenderedListener;
import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener;
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener;
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

@RestrictTo(Scope.LIBRARY)
public class CallbackManager extends BaseCallbackManager {

    private WeakReference<DisplayUnitListener> displayUnitListenerWeakReference;

    private GeofenceCallback geofenceCallback;

    private SCDomainListener scDomainListener;

    private WeakReference<InAppNotificationButtonListener> inAppNotificationButtonListener;

    private InAppNotificationListener inAppNotificationListener;

    private final List<PushPermissionResponseListener> pushPermissionResponseListenerList = new ArrayList<>();

    private CTInboxListener inboxListener;

    private final CleverTapInstanceConfig config;

    private final DeviceInfo deviceInfo;

    private FailureFlushListener failureFlushListener;

    @Deprecated
    private WeakReference<CTFeatureFlagsListener> featureFlagListenerWeakReference;

    private NotificationRenderedListener notificationRenderedListener;

    private OnInitCleverTapIDListener onInitCleverTapIDListener;

    @Deprecated
    private WeakReference<CTProductConfigListener> productConfigListener;

    private CTPushAmpListener pushAmpListener = null;

    private CTPushNotificationListener pushNotificationListener = null;

    private SyncListener syncListener = null;

    public CallbackManager(CleverTapInstanceConfig config, DeviceInfo deviceInfo) {
        this.config = config;
        this.deviceInfo = deviceInfo;
    }

    private FetchVariablesCallback fetchVariablesCallback;

    @Override
    public void _notifyInboxMessagesDidUpdate() {
        if (this.inboxListener != null) {
            Utils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (inboxListener != null) {
                        inboxListener.inboxMessagesDidUpdate();
                    }
                }
            });
        }
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

    @Override
    public CTInboxListener getInboxListener() {
        return inboxListener;
    }

    @Override
    public void setInboxListener(final CTInboxListener inboxListener) {
        this.inboxListener = inboxListener;
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
    public CTPushAmpListener getPushAmpListener() {
        return pushAmpListener;
    }

    @Override
    public void setPushAmpListener(final CTPushAmpListener pushAmpListener) {
        this.pushAmpListener = pushAmpListener;
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
    public OnInitCleverTapIDListener getOnInitCleverTapIDListener() {
        return onInitCleverTapIDListener;
    }

    @Override
    public void setOnInitCleverTapIDListener(final OnInitCleverTapIDListener onInitCleverTapIDListener) {
        this.onInitCleverTapIDListener = onInitCleverTapIDListener;
    }

    @Override
    public void setNotificationRenderedListener(final NotificationRenderedListener notificationRenderedListener) {
        this.notificationRenderedListener = notificationRenderedListener;
    }

    @Override
    public NotificationRenderedListener getNotificationRenderedListener() {
        return notificationRenderedListener;
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

    @Override
    public void setDisplayUnitListener(DisplayUnitListener listener) {
        if (listener != null) {
            displayUnitListenerWeakReference = new WeakReference<>(listener);
        } else {
            config.getLogger().verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Failed to set - DisplayUnitListener can't be null");
        }
    }

    void _notifyInboxInitialized() {
        if (this.inboxListener != null) {
            this.inboxListener.inboxDidInitialize();
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

    @Override @Nullable
    public FetchVariablesCallback getFetchVariablesCallback() {
        return fetchVariablesCallback;
    }

    @Override
    public void setFetchVariablesCallback(
        FetchVariablesCallback fetchVariablesCallback) {
        this.fetchVariablesCallback = fetchVariablesCallback;
    }
}