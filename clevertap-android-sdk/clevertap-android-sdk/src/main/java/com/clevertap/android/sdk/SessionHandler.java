package com.clevertap.android.sdk;

import java.lang.ref.WeakReference;

@Deprecated
public class SessionHandler {

    private WeakReference<CleverTapAPI> weakReference;

    SessionHandler(CleverTapAPI cleverTapAPI){
        this.weakReference = new WeakReference<>(cleverTapAPI);
    }

    /**
     * @deprecated use {@link CleverTapAPI#getTotalVisits()}
     */
    @Deprecated
    public int getTotalVisits() {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
            return 0;
        } else {
            return cleverTapAPI.getTotalVisits();
        }
    }

    /**
     * @deprecated use {@link CleverTapAPI#getScreenCount()}
     */
    @Deprecated
    public int getScreenCount() {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
            return 0;
        } else {
            return cleverTapAPI.getScreenCount();
        }
    }

    /**
     * @deprecated use {@link CleverTapAPI#getTimeElapsed()}
     */
    @Deprecated
    public int getTimeElapsed() {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
            return 0;
        } else {
            return cleverTapAPI.getTimeElapsed();
        }
    }

    /**
     * @deprecated use {@link CleverTapAPI#getPreviousVisitTime()}
     */
    @Deprecated
    public int getPreviousVisitTime() {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
            return 0;
        } else {
            return cleverTapAPI.getPreviousVisitTime();
        }
    }

    /**
     * @deprecated use {@link CleverTapAPI#getUTMDetails()}
     */
    @Deprecated
    public UTMDetail getUTMDetails() {
        CleverTapAPI cleverTapAPI = weakReference.get();
        if (cleverTapAPI == null) {
            Logger.d("CleverTap Instance is null.");
            return null;
        } else {
            return cleverTapAPI.getUTMDetails();
        }
    }
}
