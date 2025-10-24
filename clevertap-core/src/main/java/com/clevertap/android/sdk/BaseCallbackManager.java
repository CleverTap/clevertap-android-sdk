package com.clevertap.android.sdk;

import androidx.annotation.NonNull;

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

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCallbackManager {
    public abstract FailureFlushListener getFailureFlushListener();

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public abstract CTFeatureFlagsListener getFeatureFlagListener();

    public abstract GeofenceCallback getGeofenceCallback();

    public abstract SCDomainListener getSCDomainListener();

    public abstract InAppNotificationButtonListener getInAppNotificationButtonListener();

    public abstract InAppNotificationListener getInAppNotificationListener();

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

    public abstract void notifyDisplayUnitsLoaded(final ArrayList<CleverTapDisplayUnit> displayUnits);

    //Profile
    public abstract void notifyUserProfileInitialized(String deviceID);

    abstract void notifyUserProfileInitialized();

    public abstract void setDisplayUnitListener(DisplayUnitListener listener);

    public abstract void setFailureFlushListener(FailureFlushListener failureFlushListener);

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public abstract void setFeatureFlagListener(CTFeatureFlagsListener listener);

    public abstract void setGeofenceCallback(GeofenceCallback geofenceCallback);

    public abstract void setSCDomainListener(SCDomainListener scDomainListener);

    public abstract void setInAppNotificationButtonListener(
            InAppNotificationButtonListener inAppNotificationButtonListener);

    public abstract void setInAppNotificationListener(InAppNotificationListener inAppNotificationListener);

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

    public abstract FetchInAppsCallback getFetchInAppsCallback();

    public abstract void setFetchInAppsCallback(FetchInAppsCallback fetchInAppsCallback);

    public abstract BatchListener getBatchListener();

    public abstract void setBatchListener(BatchListener batchListener);

    public abstract CTFeatureFlagsController getCTFeatureFlagsController();

    public abstract void setCTFeatureFlagsController(CTFeatureFlagsController ctInboxController);

    public void invokeBatchListener(JSONArray array, boolean success) {
        BatchListener batchListener = getBatchListener();
        if (batchListener != null) {
            batchListener.onBatchSent(array, success);
        }
    }

    public abstract List<ChangeUserCallback> getChangeUserCallbackList();

    public abstract void addChangeUserCallback(ChangeUserCallback callback);

    public abstract void removeChangeUserCallback(ChangeUserCallback callback);

    public abstract CTDisplayUnitController getCTDisplayUnitController();

    public abstract void setCTDisplayUnitController(final CTDisplayUnitController CTDisplayUnitController);

}
