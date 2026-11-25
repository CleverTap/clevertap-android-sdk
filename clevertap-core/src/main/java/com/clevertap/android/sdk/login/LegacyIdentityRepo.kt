package com.clevertap.android.sdk.login

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.login.LoginConstants.LOG_TAG_ON_USER_LOGIN

/**
 * Legacy class which handles old static identity logic.
 * Here the profile set was fixed as < Email, Identity >
 */
internal open class LegacyIdentityRepo(private val config: CleverTapInstanceConfig) : IdentityRepo {

    private lateinit var identities: IdentitySet

    init {
        loadIdentitySet()
    }

    override val identitySet: IdentitySet
        get() = identities

    override fun hasIdentity(Key: String): Boolean {
        val hasIdentity = identities.contains(Key)
        config.log(
            LOG_TAG_ON_USER_LOGIN,
            "isIdentity [Key: $Key , Value: $hasIdentity]"
        )
        return hasIdentity
    }

    private fun loadIdentitySet() {
        this.identities = IdentitySet.getDefault()
        config.log(
            LOG_TAG_ON_USER_LOGIN,
            "$TAG Setting the default IdentitySet[$identities]"
        )
    }

    private companion object {
        private const val TAG = "LegacyIdentityRepo"
    }
}
