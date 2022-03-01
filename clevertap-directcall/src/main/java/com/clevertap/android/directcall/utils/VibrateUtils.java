package com.clevertap.android.directcall.utils;

import static android.Manifest.permission.VIBRATE;

import android.content.Context;
import android.os.Vibrator;

import androidx.annotation.RequiresPermission;

public final class VibrateUtils {

    private static Vibrator vibrator;

    private VibrateUtils() {
        throw new UnsupportedOperationException(
                "Should not create instance of VibrateUtils class. Please use as static..");
    }

    public static Vibrator getVibrator(Context context) {
        if (vibrator == null) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        return vibrator;
    }

    /**
     * Vibrate.
     *
     * @param milliseconds The number of milliseconds to vibrate.
     */
    @RequiresPermission(VIBRATE)
    public void vibrate(final long milliseconds) {
        if (vibrator == null) return;
        vibrator.vibrate(milliseconds);
    }

    /**
     * Vibrate.
     *
     * @param pattern An array of longs of times for which to turn the vibrator on or off.
     * @param repeat  The index into pattern at which to repeat, or -1 if you don't want to repeat.
     */
    @RequiresPermission(VIBRATE)
    public void vibrate(final long[] pattern, final int repeat) {
        if (vibrator == null) return;
        vibrator.vibrate(pattern, repeat);
    }

    /**
     * Cancel vibrate.
     */
    @RequiresPermission(VIBRATE)
    public void cancel() {
        if (vibrator == null) return;
        vibrator.cancel();
    }

    public void free() {
        if (vibrator != null) {
            vibrator = null;
        }
    }
}