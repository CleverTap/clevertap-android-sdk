package com.clevertap.android.sdk.pushnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;

/**
 * Receives the delete-intent fired when a Live Activity (live update) notification is swiped away.
 * Raises the "Live Activity" lifecycle event with state {@code Dismissed}, keeping Android on par
 * with the iOS Live Activity dismissal callback (which the OS provides via ActivityKit).
 *
 * <p>The SDK attaches this receiver's intent only when the client-supplied factory did not set its
 * own delete intent, so client dismiss handling is never clobbered.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CTLiveActivityDismissReceiver extends BroadcastReceiver {

    public static final String TYPE_DISMISS = "com.clevertap.LIVE_ACTIVITY_DISMISS";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                return;
            }
            Logger.v("CTLiveActivityDismissReceiver: live activity dismissed for "
                    + extras.getString(Constants.LIVE_ACTIVITY_ID));
            CleverTapAPI.handleLiveActivityDismissed(context.getApplicationContext(), extras);
        } catch (Throwable t) {
            Logger.v("CTLiveActivityDismissReceiver: failed to handle dismissal", t);
        }
    }
}
