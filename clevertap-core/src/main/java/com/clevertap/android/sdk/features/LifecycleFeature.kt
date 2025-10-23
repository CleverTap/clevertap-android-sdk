package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.ActivityLifeCycleManager

/**
 * Activity lifecycle management feature
 * Manages app lifecycle events and activity tracking
 */
internal data class LifecycleFeature(
    val activityLifeCycleManager: ActivityLifeCycleManager
)
