package com.clevertap.android.sdk;

import androidx.annotation.NonNull;
import com.clevertap.android.sdk.displayunits.DisplayUnitListener;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.inapp.callbacks.FetchInAppsCallback;
import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener;
import com.clevertap.android.sdk.interfaces.SCDomainListener;
import com.clevertap.android.sdk.login.ChangeUserCallback;
import com.clevertap.android.sdk.network.BatchListener;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener;
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener;
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseCallbackManager {

    abstract void _notifyInboxInitialized();

    public abstract void _notifyInboxMessagesDidUpdate();

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

    public abstract CTInboxListener getInboxListener();

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public abstract CTProductConfigListener getProductConfigListener();

    public abstract CTPushAmpListener getPushAmpListener();

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

    public abstract void setInboxListener(CTInboxListener inboxListener);

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    public abstract void setProductConfigListener(
            CTProductConfigListener productConfigListener);

    public abstract void setPushAmpListener(CTPushAmpListener pushAmpListener);

    public abstract void setPushNotificationListener(
            CTPushNotificationListener pushNotificationListener);

    public abstract void setSyncListener(SyncListener syncListener);

    public abstract void addOnInitCleverTapIDListener(@NonNull OnInitCleverTapIDListener listener);

    public abstract void removeOnInitCleverTapIDListener(@NonNull OnInitCleverTapIDListener listener);

    public abstract void notifyCleverTapIDChanged(String id);

    public abstract FetchVariablesCallback getFetchVariablesCallback();

    public abstract void setFetchVariablesCallback(FetchVariablesCallback fetchVariablesCallback);

    public abstract FetchInAppsCallback getFetchInAppsCallback();

    public abstract void setFetchInAppsCallback(FetchInAppsCallback fetchInAppsCallback);

    public abstract BatchListener getBatchListener();

    public abstract void setBatchListener(BatchListener batchListener);

    public abstract List<ChangeUserCallback> getChangeUserCallbackList();

    public abstract void addChangeUserCallback(ChangeUserCallback callback);

    public abstract void removeChangeUserCallback(ChangeUserCallback callback);

}
