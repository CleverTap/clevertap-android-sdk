package com.clevertap.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import com.clevertap.android.sdk.events.EventDetail;
import com.clevertap.android.sdk.validation.Validator;

public class SessionManager extends BaseSessionManager {

    private long appLastSeen = 0;

    private int lastVisitTime;

    private final CoreMetaData cleverTapMetaData;

    private final CleverTapInstanceConfig config;

    private final LocalDataStore localDataStore;

    private final Validator validator;

    public SessionManager(CleverTapInstanceConfig config, CoreMetaData coreMetaData, Validator validator,
            LocalDataStore localDataStore) {
        this.config = config;
        cleverTapMetaData = coreMetaData;
        this.validator = validator;
        this.localDataStore = localDataStore;
    }

    // SessionManager/session management
    public void checkTimeoutSession() {
        if (appLastSeen <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((now - appLastSeen) > Constants.SESSION_LENGTH_MINS * 60 * 1000) {
            config.getLogger().verbose(config.getAccountId(), "Session Timed Out");
            destroySession();
            CoreMetaData.setCurrentActivity(null);
        }
    }

    @Override
    public void destroySession() {
        cleverTapMetaData.setCurrentSessionId(0);
        cleverTapMetaData.setAppLaunchPushed(false);
        if (cleverTapMetaData.isFirstSession()) {
            cleverTapMetaData.setFirstSession(false);
        }
        config.getLogger().verbose(config.getAccountId(), "Session destroyed; Session ID is now 0");
        cleverTapMetaData.clearSource();
        cleverTapMetaData.clearMedium();
        cleverTapMetaData.clearCampaign();
        cleverTapMetaData.clearWzrkParams();
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
    public void lazyCreateSession(Context context) {
        if (!cleverTapMetaData.inCurrentSession()) {
            cleverTapMetaData.setFirstRequestInSession(true);
            if (validator != null) {
                validator.setDiscardedEvents(null);
            }
            createSession(context);
        }
    }

    //Session
    void setLastVisitTime() {
        EventDetail ed = localDataStore.getEventDetail(Constants.APP_LAUNCHED_EVENT);
        if (ed == null) {
            lastVisitTime = -1;
        } else {
            lastVisitTime = ed.getLastTime();
        }
    }

    private void createSession(final Context context) {
        int sessionId = (int) (System.currentTimeMillis() / 1000);
        cleverTapMetaData.setCurrentSessionId(sessionId);

        config.getLogger().verbose(config.getAccountId(),
                "Session created with ID: " + cleverTapMetaData.getCurrentSessionId());

        SharedPreferences prefs = StorageHelper.getPreferences(context);

        final int lastSessionID = StorageHelper.getIntFromPrefs(context, config, Constants.SESSION_ID_LAST, 0);
        final int lastSessionTime = StorageHelper.getIntFromPrefs(context, config, Constants.LAST_SESSION_EPOCH, 0);
        if (lastSessionTime > 0) {
            cleverTapMetaData.setLastSessionLength(lastSessionTime - lastSessionID);
        }

        config.getLogger().verbose(config.getAccountId(),
                "Last session length: " + cleverTapMetaData.getLastSessionLength() + " seconds");

        if (lastSessionID == 0) {
            cleverTapMetaData.setFirstSession(true);
        }

        final SharedPreferences.Editor editor = prefs.edit()
                .putInt(StorageHelper.storageKeyWithSuffix(config, Constants.SESSION_ID_LAST),
                        cleverTapMetaData.getCurrentSessionId());
        StorageHelper.persist(editor);
    }

}