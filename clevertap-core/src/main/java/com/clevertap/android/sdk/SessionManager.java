package com.clevertap.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager extends BaseSessionManager{

    private final CoreMetaData mCleverTapMetaData;

    private final CleverTapInstanceConfig mConfig;

    private final Validator mValidator;

    public SessionManager(final CoreState coreState) {
        mCleverTapMetaData = coreState.getCoreMetaData();
        mValidator = coreState.getValidator();
        mConfig = coreState.getConfig();
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