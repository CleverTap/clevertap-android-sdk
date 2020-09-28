package com.clevertap.android.sdk.ab_testing.gesture;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import com.clevertap.android.sdk.Logger;

public class ConnectionGesture implements SensorEventListener {

    public interface OnGestureListener {

        void onGesture();
    }

    private static final float MINIMUM_GRAVITY = 9.8f - 2.0f;

    private static final float MAXIMUM_GRAVITY = 9.8f + 2.0f;

    private static final long MINIMUM_UP_DOWN_DURATION = 250000000;  // 1/4 second

    private static final long MINIMUM_CANCEL_DURATION = 1000000000;  // one second

    private static final int STATE_UP = -1;

    private static final int STATE_NONE = 0;

    private static final int STATE_DOWN = 1;

    private static final int TRIGGER_NONE = 0;

    private static final int TRIGGER_BEGIN = 1;

    private static final float SMOOTHING_FACTOR = 0.7f;

    private int gestureState = STATE_NONE;

    private long lastTime = -1;

    private final OnGestureListener listener;

    private final float[] smoothed = new float[3];

    private int triggerState = -1;

    public ConnectionGesture(OnGestureListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // no-op
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float[] smoothedValues = smoothSamples(event.values);
        final int oldState = gestureState;
        gestureState = STATE_NONE;

        final float totalGravitySquared = smoothedValues[0] * smoothedValues[0]
                + smoothedValues[1] * smoothedValues[1] + smoothedValues[2] * smoothedValues[2];

        final float minimumGravitySquared = MINIMUM_GRAVITY * MINIMUM_GRAVITY;
        final float maximumGravitySquared = MAXIMUM_GRAVITY * MAXIMUM_GRAVITY;

        if (smoothed[2] > MINIMUM_GRAVITY && smoothed[2] < MAXIMUM_GRAVITY) {
            gestureState = STATE_UP;
        }

        if (smoothed[2] < -MINIMUM_GRAVITY && smoothed[2] > -MAXIMUM_GRAVITY) {
            gestureState = STATE_DOWN;
        }

        if (totalGravitySquared < minimumGravitySquared || totalGravitySquared > maximumGravitySquared) {
            gestureState = STATE_NONE;
        }

        if (oldState != gestureState) {
            lastTime = event.timestamp;
        }

        final long durationNanos = event.timestamp - lastTime;

        switch (gestureState) {
            case STATE_DOWN:
                if (durationNanos > MINIMUM_UP_DOWN_DURATION && triggerState == TRIGGER_NONE) {
                    Logger.v("Connection gesture started");
                    triggerState = TRIGGER_BEGIN;
                }
                break;
            case STATE_UP:
                if (durationNanos > MINIMUM_UP_DOWN_DURATION && triggerState == TRIGGER_BEGIN) {
                    Logger.v("Connection gesture completed");
                    triggerState = TRIGGER_NONE;
                    listener.onGesture();
                }
                break;
            case STATE_NONE:
                if (durationNanos > MINIMUM_CANCEL_DURATION && triggerState != TRIGGER_NONE) {
                    Logger.v("Connection gesture canceled");
                    triggerState = TRIGGER_NONE;
                }
                break;
        }
    }

    private float[] smoothSamples(final float[] samples) {
        for (int i = 0; i < 3; i++) {
            final float old = smoothed[i];
            smoothed[i] = old + (SMOOTHING_FACTOR * (samples[i] - old));
        }
        return smoothed;
    }
}

