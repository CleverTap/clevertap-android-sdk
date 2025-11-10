package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.LocationManager
import com.clevertap.android.sdk.ProfileValueHandler
import com.clevertap.android.sdk.login.LoginInfoProvider

/**
 * User profile and login management
 * Handles user identity, login operations, and profile updates
 */
internal class ProfileFeature(
    val loginInfoProvider: LoginInfoProvider,
    val profileValueHandler: ProfileValueHandler,
) {
    lateinit var coreContract: CoreContract
    val locationManager by lazy {
        LocationManager(
            mContext = coreContract.context(),
            mConfig = coreContract.config(),
            mCoreMetaData = coreContract.coreMetaData(),
            analyticsManager = coreContract.analytics()
        )
    }
}
