package com.clevertap.android.sdk

import android.content.Context

internal abstract class BaseSessionManager {

    /**
     * Destroys the current session and resets *firstSession* flag, if first session lasts more than 20 minutes
     *
     * For an app like Music Player
     * - user installs an app and plays music and then moves to background.
     * - User then re-launches an App after listening music in background for more than 20 minutes, in this case
     * since an app is not yet killed due to background music *app installed* event must not be raised by SDK
     */
    abstract fun destroySession()

    abstract fun lazyCreateSession(context: Context)
}
