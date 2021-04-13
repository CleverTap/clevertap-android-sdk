package com.clevertap.android.sdk;

import android.content.Context;

abstract class BaseSessionManager {

    /**
     * Destroys the current session and resets <i>firstSession</i> flag, if first session lasts more than 20 minutes
     * <br><br>For an app like Music Player <li>user installs an app and plays music and then moves to background.
     * <li>User then re-launches an App after listening music in background for more than 20 minutes, in this case
     * since an app is not yet killed due to background music <i>app installed</i> event must not be raised by SDK
     */
    abstract void destroySession();

    abstract void lazyCreateSession(Context context);
}
