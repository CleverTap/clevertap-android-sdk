package com.clevertap.android.geofence.interfaces

import androidx.annotation.RestrictTo

/**
 * Represents an asynchronous operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface CTGeofenceTask {

    /**
     * Listener called when a [CTGeofenceTask] completes.
     */
    fun interface OnCompleteListener {

        /**
         * Called when the [CTGeofenceTask] completes.
         */
        fun onComplete()
    }

    /**
     * initiates execution
     */
    fun execute()

    /**
     * Sets a listener that is called when the Task completes. The listener will be called
     * on background thread
     *
     * @param onCompleteListener an instance of [OnCompleteListener]
     */
    fun setOnCompleteListener(onCompleteListener: OnCompleteListener)
}
