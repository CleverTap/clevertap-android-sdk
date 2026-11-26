package com.clevertap.android.sdk.login

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.login.LoginConstants.LOG_TAG_ON_USER_LOGIN
import androidx.annotation.RestrictTo

/**
 * Legacy class which handles old static identity logic.
 * Here the profile set was fixed as < Email, Identity >
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class LegacyIdentityRepo(private val config: CleverTapInstanceConfig) : IdentityRepo {

    override val identitySet: IdentitySet = IdentitySet.default().also {
        config.log(LOG_TAG_ON_USER_LOGIN, "$TAG Setting the default IdentitySet[$it]")
    }

    override fun hasIdentity(key: String): Boolean {
        val hasIdentity = identitySet.contains(key)
        config.log(LOG_TAG_ON_USER_LOGIN, "isIdentity [Key: $key , Value: $hasIdentity]")
        return hasIdentity
    }

    private companion object {
        private const val TAG = "LegacyIdentityRepo"
    }
}
