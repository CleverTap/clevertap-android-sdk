package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONObject;

//TODO make singleton

/**
 * This class stores run time state of CleverTap's instance
 */
class CoreMetaData extends CleverTapMetaData {

    private static int activityCount = 0;

    private boolean appLaunchPushed = false;

    private final Object appLaunchPushedLock = new Object();

    private String currentScreenName = "";

    private int currentSessionId = 0;

    private boolean currentUserOptedOut = false;

    private boolean firstRequestInSession = false;

    private boolean firstSession = false;

    private int geofenceSDKVersion = 0;

    private boolean isBgPing = false;

    private boolean isLocationForGeofence = false;

    private int lastSessionLength = 0;

    private boolean offline;

    private final Object optOutFlagLock = new Object();

    private String source = null, medium = null, campaign = null;

    private JSONObject wzrkParams = null;

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

    synchronized String getCampaign() {
        return campaign;
    }

    synchronized void setCampaign(String campaign) {
        if (this.campaign == null) {
            this.campaign = campaign;
        }
    }

    int getCurrentSession() {
        return currentSessionId;
    }

    int getGeofenceSDKVersion() {
        return geofenceSDKVersion;
    }

    void setGeofenceSDKVersion(int geofenceSDKVersion) {
        this.geofenceSDKVersion = geofenceSDKVersion;
    }

    //Session
    int getLastSessionLength() {
        return lastSessionLength;
    }

    void setLastSessionLength(final int lastSessionLength) {
        this.lastSessionLength = lastSessionLength;
    }

    synchronized String getMedium() {
        return medium;
    }

    // only set if not already set during the session
    synchronized void setMedium(String medium) {
        if (this.medium == null) {
            this.medium = medium;
        }
    }

    String getScreenName() {
        return currentScreenName.equals("") ? null : currentScreenName;
    }

    synchronized String getSource() {
        return source;
    }

    //UTM
    // only set if not already set during the session
    synchronized void setSource(String source) {
        if (this.source == null) {
            this.source = source;
        }
    }

    synchronized JSONObject getWzrkParams() {
        return wzrkParams;
    }

    synchronized void setWzrkParams(JSONObject wzrkParams) {
        if (this.wzrkParams == null) {
            this.wzrkParams = wzrkParams;
        }
    }

    boolean inCurrentSession() {
        return currentSessionId > 0;
    }

    boolean isAppLaunchPushed() {
        synchronized (appLaunchPushedLock) {
            return appLaunchPushed;
        }
    }

    void setAppLaunchPushed(boolean pushed) {
        synchronized (appLaunchPushedLock) {
            appLaunchPushed = pushed;
        }
    }

    boolean isBgPing() {
        return isBgPing;
    }

    void setBgPing(final boolean bgPing) {
        isBgPing = bgPing;
    }

    boolean isCurrentUserOptedOut() {
        synchronized (optOutFlagLock) {
            return currentUserOptedOut;
        }
    }

    void setCurrentUserOptedOut(boolean enable) {
        synchronized (optOutFlagLock) {
            currentUserOptedOut = enable;
        }
    }

    boolean isFirstRequestInSession() {
        return firstRequestInSession;
    }

    void setFirstRequestInSession(boolean firstRequestInSession) {
        this.firstRequestInSession = firstRequestInSession;
    }

    //Session
    boolean isFirstSession() {
        return firstSession;
    }

    void setFirstSession(final boolean firstSession) {
        this.firstSession = firstSession;
    }

    boolean isLocationForGeofence() {
        return isLocationForGeofence;
    }

    void setLocationForGeofence(boolean locationForGeofence) {
        isLocationForGeofence = locationForGeofence;
    }

    boolean isMuted() {
        //TODO
        return false;
    }

    boolean isOffline() {
        return offline;
    }

    void setOffline(boolean value) {
        offline = value;
    }

    void setMuted(final Context context, boolean mute) {
        //TODO
    }

    void setSessionId(int sessionId) {
        this.currentSessionId = sessionId;
    }

    static int getActivityCount() {
        return activityCount;
    }

    static void setActivityCount(final int count) {
        activityCount = activityCount;
    }

    static void incrementActivityCount() {
        activityCount++;
    }
}