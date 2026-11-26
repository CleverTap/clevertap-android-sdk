package com.clevertap.android.sdk.login

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.login.LoginConstants.LOG_TAG_ON_USER_LOGIN
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationResultFactory
import com.clevertap.android.sdk.validation.ValidationResultStack
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class ConfigurableIdentityRepo(
    private val config: CleverTapInstanceConfig,
    private val infoProvider: LoginInfoProvider,
    private val validationResultStack: ValidationResultStack
) : IdentityRepo {

    /**
     * The loaded identity set. checkout the loadIdentitySet() for implementation
     */
    override val identitySet: IdentitySet = loadIdentitySet()

    override fun hasIdentity(key: String): Boolean {
        val hasIdentity = identitySet.contains(key)
        config.log(LOG_TAG_ON_USER_LOGIN, "${TAG}isIdentity [Key: $key , Value: $hasIdentity]")
        return hasIdentity
    }

    /**
     * Loads the identity set . It executes 5 steps:
     * ---------------------------------------------------------------------------
     * 1. creating : 'prefKeySet'
     *    1.1. A string is provided by loginInfoProvider.getCachedIdentityKeysForAccount()
     *       1.1.1  the above function gets either string or null from storage based on 5 scenarios:
     *              whether keyvalue are coming from sp of default/nondefault config, etc
     *   1.2 This string is of format "__,__,__etc" and is split by ',' .the new list is filtered for
     *      wrong keys and finally used to create 'prefKeySet'
     * ---------------------------------------------------------------------------
     * 2. creating : 'configKeySet'
     *   2.1 A string array is provided by config.getIdentityKeys() . note that config is a
     *      dependency passed to ConfigurableIdentityRepo . it can be default or non default
     *      2.1.1 for default config instance identityKeys =manifest.getProfileKeys()
     *      2.1.2 for nondefault config instance , identityKeys = either null keys array or array
     *            of strings that are set post creation via config.setIdentityKeys(
     *            Constants.TYPE_EMAIL,Constants.TYPE_PHONE,..etc)
     *   2.2 this array is filtered for wrong keys and finally used to create 'configKeySet'
     * ---------------------------------------------------------------------------
     * 3. validate sets  : handleError(prefKeySet, configKeySet)
     *   3.1 note that : the validation criteria is simply that KeySet must not be empty
     *   3.2 if prefKeySet.isValid AND configKeySet.isValid AND !prefKeySet.equals(configKeySet),
     *      it will generate a validation error on vr stack passed via external dependency
     * ---------------------------------------------------------------------------
     * 4. setting identity set
     *   4.1  if prefKeySet is Valid, identitySet will be set as prefKeySet
     *   4.2  if above doesn't apply and configKeySet is Valid, identitySet will be set as configKeySet
     *   4.3  if above 2 doesn't apply identitySet will be set as defaultSet = ['Identity','Email']
     * ---------------------------------------------------------------------------
     * 5. Saving Identity Keys For Account
     *   5.1 if prefKeySet was not valid, loginInfoProvider.saveIdentityKeysForAccount(identitySet)
     *       is also called on the newly initialised identitySet
     */
    private fun loadIdentitySet(): IdentitySet {
        // Read from Pref
        val prefKeySet = IdentitySet.from(infoProvider.cachedIdentityKeysForAccount)

        config.log(LOG_TAG_ON_USER_LOGIN, "${TAG}PrefIdentitySet [$prefKeySet]")

        /* ----------------------------------------------------------------
         *   For Default Instance - Get Identity Set configured via Manifest
         *   For Multi Instance - Get Identity Set configured via the setter
         * ---------------------------------------------------------------- */
        val configKeySet = IdentitySet.from(config.identityKeys)

        config.log(LOG_TAG_ON_USER_LOGIN, "${TAG}ConfigIdentitySet [$configKeySet]")

        /* ---------------------------------------------------
         *    Push error to LC in-case the data available
         *   in the config & preferences are not matching
         * --------------------------------------------------- */
        handleError(prefKeySet, configKeySet)

        /* ---------------------------------------------------
         *      If data is available from Pref, use
         *      else data is available via from config use
         *      else use legacy key set
         * --------------------------------------------------- */
        val identitySet = when {
            prefKeySet.isValid -> prefKeySet.also {
                config.log(LOG_TAG_ON_USER_LOGIN, "${TAG}Identity Set activated from Pref[$it]")
            }

            configKeySet.isValid -> configKeySet.also {
                config.log(LOG_TAG_ON_USER_LOGIN, "${TAG}Identity Set activated from Config[$it]")
            }

            else -> IdentitySet.default().also {
                config.log(LOG_TAG_ON_USER_LOGIN, "${TAG}Identity Set activated from Default[$it]")
            }
        }
        if (!prefKeySet.isValid) {
            /* -------------------------------------------------------------------------
             *  If Config data is available, save in the preference if not saved already.
             * ------------------------------------------------------------------------ */
            val storedValue = identitySet.toString()
            infoProvider.saveIdentityKeysForAccount(storedValue)
            config.log(LOG_TAG_ON_USER_LOGIN, "${TAG}Saving Identity Keys in Pref[$storedValue]")
        }
        return identitySet
    }

    /**
     * Push error to LC in-case the data available
     * in the config & preferences are not matching
     *
     * @param prefKeySet   - key set in the preference
     * @param configKeySet - key set in the config
     */
    private fun handleError(prefKeySet: IdentitySet, configKeySet: IdentitySet) {
        if (prefKeySet.isValid && configKeySet.isValid && prefKeySet != configKeySet) {
            val error = ValidationResultFactory.create(ValidationError.PROFILE_IDENTIFIERS_MISMATCH)
            validationResultStack.pushValidationResult(error)
            config.log(
                LOG_TAG_ON_USER_LOGIN,
                "${TAG}pushing error due to mismatch [Pref:$prefKeySet], [Config:$configKeySet]"
            )
        } else {
            config.log(
                LOG_TAG_ON_USER_LOGIN,
                "${TAG}No error found while comparing [Pref:$prefKeySet], [Config:$configKeySet]"
            )
        }
    }

    private companion object {
        private const val TAG = "ConfigurableIdentityRepo"
    }
}
