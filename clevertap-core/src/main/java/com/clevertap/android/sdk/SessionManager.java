package com.clevertap.android.sdk;

import android.content.Context;

import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import com.clevertap.android.sdk.events.EventDetail;
import com.clevertap.android.sdk.usereventlogs.UserEventLog;
import com.clevertap.android.sdk.validation.ValidationConfig;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SessionManager extends BaseSessionManager {

    private long appLastSeen = 0;

    private int lastVisitTime;
    private long userLastVisitTs;

    private final CoreMetaData cleverTapMetaData;

    private final CleverTapInstanceConfig config;

    private final LocalDataStore localDataStore;

    private final ValidationConfig validationConfig;

    public SessionManager(CleverTapInstanceConfig config, CoreMetaData coreMetaData, ValidationConfig validationConfig,
            LocalDataStore localDataStore) {
        this.config = config;
        cleverTapMetaData = coreMetaData;
        this.validationConfig = validationConfig;
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
        }
    }

    @Override
    public void destroySession() {
        cleverTapMetaData.setCurrentSessionId(0);
        cleverTapMetaData.setRelaxNetwork(false);
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
            if (validationConfig != null) {
                validationConfig.updateDiscardedEventNames(null);
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
    @WorkerThread
    void setUserLastVisitTs() {
        UserEventLog appLaunchedEventLog = localDataStore.readUserEventLog(Constants.APP_LAUNCHED_EVENT);
        userLastVisitTs = appLaunchedEventLog != null ? appLaunchedEventLog.getLastTs() : -1;
    }

    private void createSession(final Context context) {
        int sessionId = getNow();
        cleverTapMetaData.setCurrentSessionId(sessionId);

        config.getLogger().verbose(config.getAccountId(),
                "Session created with ID: " + cleverTapMetaData.getCurrentSessionId());

        final int lastSessionID = StorageHelper.getIntFromPrefs(context, config.getAccountId(), Constants.SESSION_ID_LAST, 0);
        final int lastSessionTime = StorageHelper.getIntFromPrefs(context, config.getAccountId(), Constants.LAST_SESSION_EPOCH, 0);
        if (lastSessionTime > 0) {
            cleverTapMetaData.setLastSessionLength(lastSessionTime - lastSessionID);
        }

        config.getLogger().verbose(config.getAccountId(),
                "Last session length: " + cleverTapMetaData.getLastSessionLength() + " seconds");

        if (lastSessionID == 0) {
            cleverTapMetaData.setFirstSession(true);
        }
        StorageHelper.putInt(
                context,
                config.getAccountId(),
                Constants.SESSION_ID_LAST,
                cleverTapMetaData.getCurrentSessionId()
        );
    }

    int getNow() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public long getUserLastVisitTs() {
        return userLastVisitTs;
    }
}