package com.clevertap.android.sdk;

import android.app.Activity;
import android.location.Location;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import java.lang.ref.WeakReference;
import org.json.JSONObject;

/**
 * This class stores run time state of CleverTap's instance
 */
@RestrictTo(Scope.LIBRARY)
public class CoreMetaData extends CleverTapMetaData {

    private static boolean appForeground = false;

    private static WeakReference<Activity> currentActivity;

    private static int activityCount = 0;

    private long appInstallTime = 0;

    private boolean appLaunchPushed = false;

    private final Object appLaunchPushedLock = new Object();

    private String currentScreenName = null;

    private int currentSessionId = 0;

    private boolean currentUserOptedOut = false;

    private boolean firstRequestInSession = false;

    private boolean firstSession = false;

    private int geofenceSDKVersion = 0;

    private boolean installReferrerDataSent = false;

    private boolean isBgPing = false;

    private boolean isLocationForGeofence = false;

    private boolean isProductConfigRequested;

    private int lastSessionLength = 0;

    private Location locationFromUser = null;

    private boolean offline;

    private final Object optOutFlagLock = new Object();

    private long referrerClickTime = 0;

    private String source = null, medium = null, campaign = null;

    private JSONObject wzrkParams = null;

    private static int initialAppEnteredForegroundTime = 0;

    public static Activity getCurrentActivity() {
        return (currentActivity == null) ? null : currentActivity.get();
    }

    static int getInitialAppEnteredForegroundTime() {
        return initialAppEnteredForegroundTime;
    }

    public static void setCurrentActivity(@Nullable Activity activity) {
        if (activity == null) {
            currentActivity = null;
            return;
        }
        if (!activity.getLocalClassName().contains("InAppNotificationActivity")) {
            currentActivity = new WeakReference<>(activity);
        }
    }

    public static String getCurrentActivityName() {
        Activity current = getCurrentActivity();
        return (current != null) ? current.getLocalClassName() : null;
    }

    public static boolean isAppForeground() {
        return appForeground;
    }

    public static void setAppForeground(boolean isForeground) {
        appForeground = isForeground;
    }

    static void setInitialAppEnteredForegroundTime(final int initialAppEnteredForegroundTime) {
        CoreMetaData.initialAppEnteredForegroundTime = initialAppEnteredForegroundTime;
    }

    public long getAppInstallTime() {
        return appInstallTime;
    }

    public void setAppInstallTime(final long appInstallTime) {
        this.appInstallTime = appInstallTime;
    }

    public Location getLocationFromUser() {
        return locationFromUser;
    }

    public void setLocationFromUser(final Location locationFromUser) {
        this.locationFromUser = locationFromUser;
    }

    public boolean isProductConfigRequested() {
        return isProductConfigRequested;
    }

    public void setProductConfigRequested(final boolean productConfigRequested) {
        isProductConfigRequested = productConfigRequested;
    }

    public void setCurrentScreenName(final String currentScreenName) {
        this.currentScreenName = currentScreenName;
    }

    synchronized void clearCampaign() {
        campaign = null;
    }

    synchronized void clearMedium() {
        medium = null;
    }

    synchronized void clearSource() {
        source = null;
    }

    synchronized void clearWzrkParams() {
        wzrkParams = null;
    }

    public static int getActivityCount() {
        return activityCount;
    }

    synchronized void setCampaign(String campaign) {
        if (this.campaign == null) {
            this.campaign = campaign;
        }
    }

    public synchronized String getCampaign() {
        return campaign;
    }

    public int getCurrentSessionId() {
        return currentSessionId;
    }

    public int getGeofenceSDKVersion() {
        return geofenceSDKVersion;
    }

    public void setGeofenceSDKVersion(int geofenceSDKVersion) {
        this.geofenceSDKVersion = geofenceSDKVersion;
    }

    void setLastSessionLength(final int lastSessionLength) {
        this.lastSessionLength = lastSessionLength;
    }

    //Session
    public int getLastSessionLength() {
        return lastSessionLength;
    }

    // only set if not already set during the session
    synchronized void setMedium(String medium) {
        if (this.medium == null) {
            this.medium = medium;
        }
    }

    public synchronized String getMedium() {
        return medium;
    }

    void setReferrerClickTime(final long referrerClickTime) {
        this.referrerClickTime = referrerClickTime;
    }

    public long getReferrerClickTime() {
        return referrerClickTime;
    }

    public synchronized String getSource() {
        return source;
    }

    //UTM
    // only set if not already set during the session
    synchronized void setSource(String source) {
        if (this.source == null) {
            this.source = source;
        }
    }

    public String getScreenName() {
        return currentScreenName;
    }

    public synchronized void setWzrkParams(JSONObject wzrkParams) {
        if (this.wzrkParams == null) {
            this.wzrkParams = wzrkParams;
        }
    }

    public synchronized JSONObject getWzrkParams() {
        return wzrkParams;
    }

    public boolean inCurrentSession() {
        return currentSessionId > 0;
    }

    void setAppLaunchPushed(boolean pushed) {
        synchronized (appLaunchPushedLock) {
            appLaunchPushed = pushed;
        }
    }

    public boolean isAppLaunchPushed() {
        synchronized (appLaunchPushedLock) {
            return appLaunchPushed;
        }
    }

    public boolean isBgPing() {
        return isBgPing;
    }

    public void setBgPing(final boolean bgPing) {
        isBgPing = bgPing;
    }

    public void setCurrentUserOptedOut(boolean enable) {
        synchronized (optOutFlagLock) {
            currentUserOptedOut = enable;
        }
    }

    public boolean isCurrentUserOptedOut() {
        synchronized (optOutFlagLock) {
            return currentUserOptedOut;
        }
    }

    public boolean isFirstRequestInSession() {
        return firstRequestInSession;
    }

    public void setFirstRequestInSession(boolean firstRequestInSession) {
        this.firstRequestInSession = firstRequestInSession;
    }

    void setFirstSession(final boolean firstSession) {
        this.firstSession = firstSession;
    }

    //Session
    public boolean isFirstSession() {
        return firstSession;
    }

    void setInstallReferrerDataSent(final boolean installReferrerDataSent) {
        this.installReferrerDataSent = installReferrerDataSent;
    }

    public boolean isInstallReferrerDataSent() {
        return installReferrerDataSent;
    }

    public boolean isLocationForGeofence() {
        return isLocationForGeofence;
    }

    public void setLocationForGeofence(boolean locationForGeofence) {
        isLocationForGeofence = locationForGeofence;
    }

    void setOffline(boolean value) {
        offline = value;
    }

    void setCurrentSessionId(int sessionId) {
        this.currentSessionId = sessionId;
    }

    public boolean isOffline() {
        return offline;
    }

    public static void setActivityCount(final int count) {
        activityCount = count;
    }

    static void incrementActivityCount() {
        activityCount++;
    }
}