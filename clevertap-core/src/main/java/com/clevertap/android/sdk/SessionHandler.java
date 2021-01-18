package com.clevertap.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionHandler {

    private final CoreMetaData mCleverTapMetaData;

    private final CleverTapInstanceConfig mConfig;

    private final Validator mValidator;

    public SessionHandler(final CoreState coreState, final Validator validator) {
        mCleverTapMetaData = coreState.getCoreMetaData();
        mValidator = validator;
        mConfig = coreState.getConfig();
    }

    /**
     * Destroys the current session and resets <i>firstSession</i> flag, if first session lasts more than 20 minutes
     * <br><br>For an app like Music Player <li>user installs an app and plays music and then moves to background.
     * <li>User then re-launches an App after listening music in background for more than 20 minutes, in this case
     * since an app is not yet killed due to background music <i>app installed</i> event must not be raised by SDK
     */
    void destroySession() {
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

    void lazyCreateSession(Context context) {
        if (!mCleverTapMetaData.inCurrentSession()) {
            mCleverTapMetaData.setFirstRequestInSession(true);
            if (mValidator != null) {
                mValidator.setDiscardedEvents(null);
            }
            createSession(context);
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