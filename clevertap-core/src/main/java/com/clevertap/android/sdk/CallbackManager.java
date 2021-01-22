package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.Utils.runOnUiThread;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.displayunits.DisplayUnitListener;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener;
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

@RestrictTo(Scope.LIBRARY)
public class CallbackManager {

    private WeakReference<DisplayUnitListener> displayUnitListenerWeakReference;

    private CTExperimentsListener experimentsListener = null;

    private GeofenceCallback geofenceCallback;

    private WeakReference<InAppNotificationButtonListener> inAppNotificationButtonListener;

    private InAppNotificationListener inAppNotificationListener;

    private CTInboxListener inboxListener;

    private final CleverTapInstanceConfig mConfig;

    private final DeviceInfo mDeviceInfo;

    private WeakReference<CTProductConfigListener> productConfigListener;

    private CTPushAmpListener pushAmpListener = null;

    private CTPushNotificationListener pushNotificationListener = null;

    private SyncListener syncListener = null;

    CallbackManager(CoreState coreState) {
        mConfig = coreState.getConfig();
        mDeviceInfo = coreState.getDeviceInfo();
    }

    public CTExperimentsListener getExperimentsListener() {
        return experimentsListener;
    }

    public void setExperimentsListener(final CTExperimentsListener experimentsListener) {
        this.experimentsListener = experimentsListener;
    }

    public GeofenceCallback getGeofenceCallback() {
        return geofenceCallback;
    }

    public void setGeofenceCallback(final GeofenceCallback geofenceCallback) {
        this.geofenceCallback = geofenceCallback;
    }

    public InAppNotificationButtonListener getInAppNotificationButtonListener() {
        if (inAppNotificationButtonListener != null && inAppNotificationButtonListener.get() != null) {
            return inAppNotificationButtonListener.get();
        }
        return null;
    }

    public void setInAppNotificationButtonListener(
            InAppNotificationButtonListener inAppNotificationButtonListener) {
        this.inAppNotificationButtonListener = new WeakReference<>(inAppNotificationButtonListener);
    }

    public InAppNotificationListener getInAppNotificationListener() {
        return inAppNotificationListener;
    }

    public void setInAppNotificationListener(final InAppNotificationListener inAppNotificationListener) {
        this.inAppNotificationListener = inAppNotificationListener;
    }

    public CTInboxListener getInboxListener() {
        return inboxListener;
    }

    public void setInboxListener(final CTInboxListener inboxListener) {
        this.inboxListener = inboxListener;
    }

    public WeakReference<CTProductConfigListener> getProductConfigListener() {
        return productConfigListener;
    }

    public void setProductConfigListener(
            final CTProductConfigListener productConfigListener) {
        if (productConfigListener != null) {
            this.productConfigListener = new WeakReference<>(productConfigListener);
        }
    }

    public CTPushAmpListener getPushAmpListener() {
        return pushAmpListener;
    }

    public void setPushAmpListener(final CTPushAmpListener pushAmpListener) {
        this.pushAmpListener = pushAmpListener;
    }

    public CTPushNotificationListener getPushNotificationListener() {
        return pushNotificationListener;
    }

    public void setPushNotificationListener(
            final CTPushNotificationListener pushNotificationListener) {
        this.pushNotificationListener = pushNotificationListener;
    }

    public SyncListener getSyncListener() {
        return syncListener;
    }

    public void setSyncListener(final SyncListener syncListener) {
        this.syncListener = syncListener;
    }

    //Profile
    public void notifyUserProfileInitialized(String deviceID) {
        deviceID = (deviceID != null) ? deviceID : mDeviceInfo.getDeviceID();

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

    public void setDisplayUnitListener(DisplayUnitListener listener) {
        if (listener != null) {
            displayUnitListenerWeakReference = new WeakReference<>(listener);
        } else {
            mConfig.getLogger().verbose(mConfig.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Failed to set - DisplayUnitListener can't be null");
        }
    }

    void _notifyInboxInitialized() {
        if (this.inboxListener != null) {
            this.inboxListener.inboxDidInitialize();
        }
    }

    void _notifyInboxMessagesDidUpdate() {
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

    /**
     * Notify the registered Display Unit listener about the running Display Unit campaigns
     *
     * @param displayUnits - Array of Display Units {@link CleverTapDisplayUnit}
     */
    void notifyDisplayUnitsLoaded(final ArrayList<CleverTapDisplayUnit> displayUnits) {
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
                mConfig.getLogger().verbose(mConfig.getAccountId(),
                        Constants.FEATURE_DISPLAY_UNIT + "No registered listener, failed to notify");
            }
        } else {
            mConfig.getLogger()
                    .verbose(mConfig.getAccountId(), Constants.FEATURE_DISPLAY_UNIT + "No Display Units found");
        }
    }

    void notifyUserProfileInitialized() {
        notifyUserProfileInitialized(mDeviceInfo.getDeviceID());
    }

}