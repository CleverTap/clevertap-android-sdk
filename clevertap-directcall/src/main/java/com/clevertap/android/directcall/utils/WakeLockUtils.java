package com.clevertap.android.directcall.utils;

import static com.clevertap.android.directcall.Constants.WAKE_LOCK_TIMEOUT;

import android.content.Context;
import android.os.PowerManager;

public class WakeLockUtils {

    /**
     * The Wake lock.
     */
    private static PowerManager.WakeLock wakeLock;

    private WakeLockUtils() {
        throw new UnsupportedOperationException(
                "Should not create instance of WakeLockUtils class. Please use as static..");
    }

    public static void holdWakeLock(Context context, int levelAndFlags) {
        holdWakeLockTimed(context, levelAndFlags);
    }

    /**
     * Hold wake lock.
     *
     * @param context the context
     */
    private static void holdWakeLockTimed(Context context, int levelAndFlags) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(/*PowerManager.PARTIAL_WAKE_LOCK*/ levelAndFlags, "found: DirectCallWakeLock");
        wakeLock.acquire(WAKE_LOCK_TIMEOUT);
    }

    /**
     * Release wake lock.
     */
    public static void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    /**
     * Deallocates static ref. to avoid OOM
     */
    public static void deallocate() {
        if (wakeLock != null) {
            wakeLock = null;
        }
    }
}