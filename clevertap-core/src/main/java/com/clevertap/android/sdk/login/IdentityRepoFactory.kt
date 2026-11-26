package com.clevertap.android.sdk.login

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.login.LoginConstants.LOG_TAG_ON_USER_LOGIN
import com.clevertap.android.sdk.validation.ValidationResultStack

/**
 * Provides Repo instance for an account
 */
internal object IdentityRepoFactory {

    /**
     * Creates repo provider based on login state & config details.
     *
     * @return - repo provider
     */
    fun getRepo(
        context: Context,
        config: CleverTapInstanceConfig,
        validationResultStack: ValidationResultStack
    ): IdentityRepo {
        val infoProvider = LoginInfoProvider(context, config)
        val repo: IdentityRepo = if (infoProvider.isLegacyProfileLoggedIn) {
            // case 1: Migration (cached guid but no newly saved profile pref)
            LegacyIdentityRepo(config)
        } else {
            /* ----------------------------------------------------
             * case 2: Not logged in & using default config
             * case 3: Not logged in & using multi instance config
             * -----------------------------------------------------*/
            ConfigurableIdentityRepo(config, infoProvider, validationResultStack)
        }
        config.log(LOG_TAG_ON_USER_LOGIN, "Repo provider: ${repo.javaClass.simpleName}")
        return repo
    }
}
