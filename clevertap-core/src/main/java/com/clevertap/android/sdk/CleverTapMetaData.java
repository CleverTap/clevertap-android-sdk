package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONObject;

//TODO make singleton
class CleverTapMetaData {

    private boolean offline;

    private String source = null, medium = null, campaign = null;

    private boolean firstSession = false;

    private boolean firstRequestInSession = false;

    private int lastSessionLength = 0;

    private int currentSessionId = 0;

    private boolean appLaunchPushed = false;

    private boolean currentUserOptedOut = false;

    private final Object optOutFlagLock = new Object();

    private final Object appLaunchPushedLock = new Object();


    void setFirstSession(final boolean firstSession) {
        this.firstSession = firstSession;
    }

    void setLastSessionLength(final int lastSessionLength) {
        this.lastSessionLength = lastSessionLength;
    }

    boolean isFirstRequestInSession() {
        return firstRequestInSession;
    }

    void setFirstRequestInSession(boolean firstRequestInSession) {
        this.firstRequestInSession = firstRequestInSession;
    }

    void setSessionId(int sessionId){
        this.currentSessionId = sessionId;
    }

    //Session
    boolean isFirstSession() {
        return firstSession;
    }

    // only set if not already set during the session
    synchronized void setMedium(String medium) {
        if (this.medium == null) {
            this.medium = medium;
        }
    }
    synchronized String getMedium() {
        return medium;
    }

    int getCurrentSession() {
        return currentSessionId;
    }

    boolean inCurrentSession() {
        return currentSessionId > 0;
    }

    void setMuted(final Context context, boolean mute) {
        //TODO
    }
    boolean isMuted(){
        //TODO
        return false;
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

    //Session
    int getLastSessionLength() {
        return lastSessionLength;
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

    private JSONObject wzrkParams = null;

    synchronized void clearSource() {
        source = null;
    }

    synchronized void clearWzrkParams() {
        wzrkParams = null;
    }

    synchronized void setCampaign(String campaign) {
        if (this.campaign == null) {
            this.campaign = campaign;
        }
    }
    synchronized void clearCampaign() {
        campaign = null;
    }

    synchronized void clearMedium() {
        medium = null;
    }

    synchronized String getCampaign() {
        return campaign;
    }

    boolean isOffline() {
        return offline;
    }

    void setOffline(boolean value) {
        offline = value;
    }
}
