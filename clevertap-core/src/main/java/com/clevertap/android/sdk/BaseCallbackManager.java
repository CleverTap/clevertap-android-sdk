package com.clevertap.android.sdk;

import com.clevertap.android.sdk.displayunits.DisplayUnitListener;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener;
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener;
import java.util.ArrayList;

public abstract class BaseCallbackManager {

    abstract void _notifyInboxInitialized();

    public abstract void _notifyInboxMessagesDidUpdate();

    public abstract FailureFlushListener getFailureFlushListener();

    public abstract CTFeatureFlagsListener getFeatureFlagListener();

    public abstract GeofenceCallback getGeofenceCallback();

    public abstract InAppNotificationButtonListener getInAppNotificationButtonListener();

    public abstract InAppNotificationListener getInAppNotificationListener();

    public abstract CTInboxListener getInboxListener();

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

    public abstract void setFeatureFlagListener(CTFeatureFlagsListener listener);

    public abstract void setGeofenceCallback(GeofenceCallback geofenceCallback);

    public abstract void setInAppNotificationButtonListener(
            InAppNotificationButtonListener inAppNotificationButtonListener);

    public abstract void setInAppNotificationListener(InAppNotificationListener inAppNotificationListener);

    public abstract void setInboxListener(CTInboxListener inboxListener);

    public abstract void setProductConfigListener(
            CTProductConfigListener productConfigListener);

    public abstract void setPushAmpListener(CTPushAmpListener pushAmpListener);

    public abstract void setPushNotificationListener(
            CTPushNotificationListener pushNotificationListener);

    public abstract void setSyncListener(SyncListener syncListener);

    public abstract OnInitCleverTapIDListener getOnInitCleverTapIDListener();

    public abstract void setOnInitCleverTapIDListener(OnInitCleverTapIDListener onInitCleverTapIDListener);
}