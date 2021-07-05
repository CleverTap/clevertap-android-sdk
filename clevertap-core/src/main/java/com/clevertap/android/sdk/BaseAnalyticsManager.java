package com.clevertap.android.sdk;

import android.os.Bundle;
import com.clevertap.android.sdk.inapp.CTInAppNotification;
import java.util.ArrayList;
import java.util.Map;
import org.json.JSONObject;

public abstract class BaseAnalyticsManager {

    public abstract void addMultiValuesForKey(String key, ArrayList<String> values);

    public abstract void incrementValue(String key, Number value);

    public abstract void decrementValue(String key, Number value);

    public abstract void fetchFeatureFlags();

    //Event
    public abstract void forcePushAppLaunchedEvent();

    public abstract void pushAppLaunchedEvent();

    public abstract void pushDisplayUnitClickedEventForID(String unitID);

    public abstract void pushDisplayUnitViewedEventForID(String unitID);

    @SuppressWarnings({"unused"})
    public abstract void pushError(String errorMessage, int errorCode);

    public abstract void pushEvent(String eventName, Map<String, Object> eventActions);

    @SuppressWarnings({"unused", "WeakerAccess"})
    public abstract void pushInAppNotificationStateEvent(boolean clicked, CTInAppNotification data,
            Bundle customData);

    public abstract void pushInstallReferrer(String url);

    public abstract void pushInstallReferrer(String source, String medium, String campaign);

    public abstract void pushNotificationClickedEvent(Bundle extras);

    @SuppressWarnings({"unused", "WeakerAccess"})
    public abstract void pushNotificationViewedEvent(Bundle extras);

    public abstract void pushProfile(Map<String, Object> profile);

    public abstract void removeMultiValuesForKey(String key, ArrayList<String> values);

    public abstract void removeValueForKey(String key);

    public abstract void sendDataEvent(JSONObject event);

    public abstract void sendFetchEvent(final JSONObject eventObject);
}