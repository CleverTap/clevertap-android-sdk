package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController

/**
 * Feature Flag feature
 * Manages feature flag evaluation and callbacks
 */
internal data class FeatureFlagFeature(
    val ctFeatureFlagsController: CTFeatureFlagsController?
)
