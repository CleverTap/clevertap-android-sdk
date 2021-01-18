package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONObject;

//TODO make singleton

/**
 * This class stores run time state of CleverTap's instance
 */
class CoreMetaData extends CleverTapMetaData {

    private boolean appLaunchPushed = false;

    private final Object appLaunchPushedLock = new Object();

    private int currentSessionId = 0;

    private boolean currentUserOptedOut = false;

    private boolean firstRequestInSession = false;

    private boolean firstSession = false;

    private int lastSessionLength = 0;

    private boolean offline;

    private final Object optOutFlagLock = new Object();

    private String source = null, medium = null, campaign = null;

    private JSONObject wzrkParams = null;

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
}