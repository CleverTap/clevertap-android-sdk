package com.clevertap.android.geofence.interfaces;

/**
 * Represents an asynchronous operation.
 */
public interface CTGeofenceTask {

    /**
     * Listener called when a {@link CTGeofenceTask} completes.
     */
    interface OnCompleteListener {

        /**
         * Called when the {@link CTGeofenceTask completes.
         */
        void onComplete();
    }

    /**
     * initiates execution
     */
    void execute();

    /**
     * Sets a listener that is called when the Task completes. The listener will be called
     * on background thread
     *
     * @param onCompleteListener an instance of {@link OnCompleteListener}
     */
    void setOnCompleteListener(OnCompleteListener onCompleteListener);
}
