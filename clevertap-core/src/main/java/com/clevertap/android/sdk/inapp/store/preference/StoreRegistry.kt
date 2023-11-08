package com.clevertap.android.sdk.inapp.store.preference

data class StoreRegistry(
    var inAppStore: InAppStore? = null, var impressionStore: ImpressionStore? = null,
    var legacyInAppStore: LegacyInAppStore? = null
)