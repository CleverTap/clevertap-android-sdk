package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.BaseLocationManager
import com.clevertap.android.sdk.ProfileValueHandler
import com.clevertap.android.sdk.login.LoginInfoProvider

/**
 * User profile and login management
 * Handles user identity, login operations, and profile updates
 */
internal data class ProfileFeature(
    val loginInfoProvider: LoginInfoProvider,
    val profileValueHandler: ProfileValueHandler,
    val locationManager: BaseLocationManager
)
