package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.displayunits.CTDisplayUnitController
import com.clevertap.android.sdk.network.ContentFetchManager

/**
 * Display Unit feature
 * Manages display units and content fetching for display units
 */
internal data class DisplayUnitFeature(
    var displayUnitController: CTDisplayUnitController? = null,
    val contentFetchManager: ContentFetchManager
)
