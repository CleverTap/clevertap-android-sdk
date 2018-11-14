package com.clevertap.android.sdk;

import android.os.Bundle;
import com.clevertap.android.sdk.exceptions.InvalidEventNameException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class EventHandler {

    private WeakReference<CleverTapAPI> weakReference;

    EventHandler(CleverTapAPI cleverTapAPI){
        this.weakReference = new WeakReference<>(cleverTapAPI);
    }

    /**
     * Push an event with a set of attribute pairs.
     *
     * @param eventName    The name of the event
     * @param eventActions A {@link HashMap}, with keys as strings, and values as {@link String},
     *                     {@link Integer}, {@link Long}, {@link Boolean}, {@link Float}, {@link Double},
     *                     {@link java.util.Date}, or {@link Character}
     *
     * @deprecated use {@link CleverTapAPI#pushEvent(String eventName, Map eventActions)}
     */
    @Deprecated
    public void push(String eventName, Map<String, Object> eventActions) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushEvent(eventName, eventActions);
        }
    }

    /**
     * Pushes a basic event.
     *
     * @param eventName The name of the event
     * @deprecated use {@link CleverTapAPI#pushEvent(String eventName)}
     */
    @Deprecated
    public void push(String eventName) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if(cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushEvent(eventName);
        }
    }

    /**
     * Push an event which describes a purchase made.
     *
     * @param eventName     Has to be specified as "Charged". Anything other than this
     *                      will result in an {@link InvalidEventNameException} being thrown.
     * @param chargeDetails A {@link HashMap}, with keys as strings, and values as {@link String},
     *                      {@link Integer}, {@link Long}, {@link Boolean}, {@link Float}, {@link Double},
     *                      {@link java.util.Date}, or {@link Character}
     * @param items         An {@link ArrayList} which contains up to 15 {@link HashMap} objects,
     *                      where each HashMap object describes a particular item purchased
     * @throws InvalidEventNameException Thrown if the event name is not "Charged"
     * @deprecated use {@link CleverTapAPI#pushChargedEvent(HashMap chargeDetails, ArrayList items)}
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public void push(String eventName, HashMap<String, Object> chargeDetails,
                     ArrayList<HashMap<String, Object>> items)
            throws InvalidEventNameException {
        // This method is for only charged events
        if (!eventName.equals(Constants.CHARGED_EVENT)) {
            throw new InvalidEventNameException("Not a charged event");
        }
        CleverTapAPI cleverTapAPI = weakReference.get();
        if(cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushChargedEvent(chargeDetails, items);
        }
    }

    /**
     * Pushes the Notification Clicked event to CleverTap.
     *
     * @param extras The {@link Bundle} object that contains the
     *               notification details
     * @deprecated use {@link CleverTapAPI#pushNotificationClickedEvent(Bundle extras)}
     */
    @Deprecated
    public void pushNotificationClickedEvent(final Bundle extras) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if(cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushNotificationClickedEvent(extras);
        }
    }

    /**
     * Pushes the notification details to CleverTap.
     *
     * @param extras The {@link Bundle} object that contains the
     *               notification details
     * @deprecated use {@link CleverTapAPI#pushNotificationClickedEvent(Bundle extras)}
     */
    @Deprecated
    public void pushNotificationEvent(final Bundle extras) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if(cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushNotificationClickedEvent(extras);
        }
    }

    /**
     * Pushes the Notification Viewed event to CleverTap.
     *
     * @param extras The {@link Bundle} object that contains the
     *               notification details
     * @deprecated use {@link CleverTapAPI#pushNotificationViewedEvent(Bundle extras)}
     */
    @Deprecated
    public void pushNotificationViewedEvent(Bundle extras){
        CleverTapAPI cleverTapAPI = weakReference.get();
        if(cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushNotificationViewedEvent(extras);
        }
    }

    /**
     * @deprecated use {@link CleverTapAPI#getDetails(String event)}
     */
    @Deprecated
    public EventDetail getDetails(String event) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if(cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
            return null;
        } else {
            return cleverTapAPI.getDetails(event);
        }
    }

    /**
     * @deprecated use {@link CleverTapAPI#getHistory()}
     */
    @Deprecated
    public Map<String, EventDetail> getHistory() {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if(cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
            return null;
        }else {
            return cleverTapAPI.getHistory();
        }
    }

    /**
     * @deprecated use {@link CleverTapAPI#getFirstTime(String event)}
     */
    @Deprecated
    public int getFirstTime(String event) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if(cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
            return 0;
        }else {
            return cleverTapAPI.getFirstTime(event);
        }
    }

    /**
     * @deprecated use {@link CleverTapAPI#getLastTime(String event)}
     */
    @Deprecated
    public int getLastTime(String event) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if(cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
            return 0;
        }else {
            return cleverTapAPI.getLastTime(event);
        }
    }

    /**
     * @deprecated use {@link CleverTapAPI#getCount(String event)}
     */
    @Deprecated
    public int getCount(String event) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if(cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
            return 0;
        }else {
            return cleverTapAPI.getCount(event);
        }
    }

    /**
     * Internally records an "Error Occurred" event, which can be viewed in the dashboard.
     *
     * @param errorMessage The error message
     * @param errorCode    The error code
     * @deprecated use {@link CleverTapAPI#pushError(String errorMessage, int errorCode)}
     */
    @Deprecated
    public void pushError(final String errorMessage, final int errorCode) {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if(cleverTapAPI == null){
            Logger.d("CleverTap Instance is null.");
        } else {
            cleverTapAPI.pushError(errorMessage, errorCode);
        }
    }
}
