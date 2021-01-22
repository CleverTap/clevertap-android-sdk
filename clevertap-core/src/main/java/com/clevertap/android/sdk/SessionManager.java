package com.clevertap.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager extends BaseSessionManager {

    private long appLastSeen = 0;

    private int lastVisitTime;

    private final CoreMetaData mCleverTapMetaData;

    private final CleverTapInstanceConfig mConfig;

    private final LocalDataStore mLocalDataStore;

    private final Validator mValidator;

    public SessionManager(CleverTapInstanceConfig config, CoreMetaData coreMetaData, Validator validator,
            LocalDataStore localDataStore) {
        mConfig = config;
        mCleverTapMetaData = coreMetaData;
        mValidator = validator;
        mLocalDataStore = localDataStore;
    }

    // SessionManager/session management
    public void checkTimeoutSession() {
        if (appLastSeen <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((now - appLastSeen) > Constants.SESSION_LENGTH_MINS * 60 * 1000) {
            mConfig.getLogger().verbose(mConfig.getAccountId(), "Session Timed Out");
            destroySession();
            CoreMetaData.setCurrentActivity(null);
        }
    }

    @Override
    public void destroySession() {
        mCleverTapMetaData.setSessionId(0);
        mCleverTapMetaData.setAppLaunchPushed(false);
        if (mCleverTapMetaData.isFirstSession()) {
            mCleverTapMetaData.setFirstSession(false);
        }
        mConfig.getLogger().verbose(mConfig.getAccountId(), "Session destroyed; Session ID is now 0");
        mCleverTapMetaData.clearSource();
        mCleverTapMetaData.clearMedium();
        mCleverTapMetaData.clearCampaign();
        mCleverTapMetaData.clearWzrkParams();
    }

    public long getAppLastSeen() {
        return appLastSeen;
    }

    public void setAppLastSeen(final long appLastSeen) {
        this.appLastSeen = appLastSeen;
    }

    public int getLastVisitTime() {
        return lastVisitTime;
    }

    @Override
    void lazyCreateSession(Context context) {
        if (!mCleverTapMetaData.inCurrentSession()) {
            mCleverTapMetaData.setFirstRequestInSession(true);
            if (mValidator != null) {
                mValidator.setDiscardedEvents(null);
            }
            createSession(context);
        }
    }

    //Session
    void setLastVisitTime() {
        EventDetail ed = mLocalDataStore.getEventDetail(Constants.APP_LAUNCHED_EVENT);
        if (ed == null) {
            lastVisitTime = -1;
        } else {
            lastVisitTime = ed.getLastTime();
        }
    }

    private void createSession(final Context context) {
        int sessionId = (int) (System.currentTimeMillis() / 1000);
        mCleverTapMetaData.setSessionId(sessionId);

        mConfig.getLogger().verbose(mConfig.getAccountId(),
                "Session created with ID: " + mCleverTapMetaData.getCurrentSession());

        SharedPreferences prefs = StorageHelper.getPreferences(context);

        final int lastSessionID = StorageHelper.getIntFromPrefs(context, mConfig, Constants.SESSION_ID_LAST, 0);
        final int lastSessionTime = StorageHelper.getIntFromPrefs(context, mConfig, Constants.LAST_SESSION_EPOCH, 0);
        if (lastSessionTime > 0) {
            mCleverTapMetaData.setLastSessionLength(lastSessionTime - lastSessionID);
        }

        mConfig.getLogger().verbose(mConfig.getAccountId(),
                "Last session length: " + mCleverTapMetaData.getLastSessionLength() + " seconds");

        if (lastSessionID == 0) {
            mCleverTapMetaData.setFirstSession(true);
        }

        final SharedPreferences.Editor editor = prefs.edit()
                .putInt(StorageHelper.storageKeyWithSuffix(mConfig, Constants.SESSION_ID_LAST),
                        mCleverTapMetaData.getCurrentSession());
        StorageHelper.persist(editor);
    }

}