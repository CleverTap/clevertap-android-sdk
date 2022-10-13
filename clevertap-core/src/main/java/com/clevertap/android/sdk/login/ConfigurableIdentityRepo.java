package com.clevertap.android.sdk.login;

import static com.clevertap.android.sdk.login.LoginConstants.LOG_TAG_ON_USER_LOGIN;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.validation.ValidationResult;
import com.clevertap.android.sdk.validation.ValidationResultFactory;
import com.clevertap.android.sdk.validation.ValidationResultStack;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ConfigurableIdentityRepo implements IdentityRepo {

    private static final String TAG = "ConfigurableIdentityRepo";

    private IdentitySet identitySet;

    private final LoginInfoProvider infoProvider;

    private final CleverTapInstanceConfig config;

    private final ValidationResultStack validationResultStack;

    public ConfigurableIdentityRepo(Context context, CleverTapInstanceConfig config, DeviceInfo deviceInfo, ValidationResultStack mValidationResultStack) {
       this(config,new LoginInfoProvider(context, config, deviceInfo),mValidationResultStack);
    }

    public ConfigurableIdentityRepo( CleverTapInstanceConfig config,LoginInfoProvider loginInfoProvider, ValidationResultStack mValidationResultStack) {
        this.config = config;
        this.infoProvider = loginInfoProvider;
        this.validationResultStack = mValidationResultStack;
        loadIdentitySet();
    }

    /**
     * Returns the loaded identity set. checkout the loadIdentitySet() for implementation
     * */
    @Override
    public IdentitySet getIdentitySet() {
        return identitySet;
    }

    @Override
    public boolean hasIdentity(@NonNull String Key) {
        boolean hasIdentity = identitySet.contains(Key);
        config.log(LOG_TAG_ON_USER_LOGIN,
                TAG + "isIdentity [Key: " + Key + " , Value: " + hasIdentity + "]");
        return hasIdentity;
    }

    /**
     * Loads the identity set . It executes 5 steps:
     *
     * 1. creating : 'prefKeySet'
     *    1.1. A string is provided by loginInfoProvider.getCachedIdentityKeysForAccount()
     *       1.1.1  the above function gets either string or null from storage based on 5 scenarios:
     *              whether keyvalue are coming from sp of default/nondefault config, etc
     *   1.2 This string is of format "__,__,__etc" and is split by ',' .the new list is filtered for
     *      wrong keys and finally used to create 'prefKeySet'
     *
     * 2. creating : 'configKeySet'
     *   2.1 A string array is provided by config.getIdentityKeys() . note that config is a
     *      dependency passed to ConfigurableIdentityRepo . it can be default or non default
     *      2.1.1 for default config instance identityKeys =manifest.getProfileKeys()
     *      2.1.2 for nondefault config instance , identityKeys = either null keys array or array
     *            of strings that are set post creation via config.setIdentityKeys(
     *            Constants.TYPE_EMAIL,Constants.TYPE_PHONE,..etc)
     *   2.2 this array is filtered for wrong keys and finally used to create 'configKeySet'
     *
     * 3. validate sets  : handleError(prefKeySet, configKeySet)
     *   3.1 note that : the validation criteria is simply that KeySet must not be empty
     *   3.2 if prefKeySet.isValid() AND configKeySet.isValid() AND !prefKeySet.equals(configKeySet),
     *      it will generate a validation error on vr stack passed via external dependency
     *
     * 4. setting identity set
     *   4.1  if prefKeySet is Valid, identitySet will be set as prefKeySet
     *   4.2  if above doesn't apply and configKeySet is Valid, identitySet will be set as configKeySet
     *   4.3  if above 2 doesn't apply identitySet will be set as defaultSet = ['Identity','Email']
     *
     * 5. Saving Identity Keys For Account
     *   5.1 if prefKeySet was not valid, loginInfoProvider.saveIdentityKeysForAccount(identitySet)
     *       is also called on the newly initialised identitySet
     */
    void loadIdentitySet() {
        // Read from Pref
        IdentitySet prefKeySet = IdentitySet.from(infoProvider.getCachedIdentityKeysForAccount());

        config.log(LOG_TAG_ON_USER_LOGIN, TAG + "PrefIdentitySet [" + prefKeySet + "]");

        /* ----------------------------------------------------------------
         *   For Default Instance - Get Identity Set configured via Manifest
         *   For Multi Instance - Get Identity Set configured via the setter
         * ---------------------------------------------------------------- */
        IdentitySet configKeySet = IdentitySet.from(config.getIdentityKeys());

        config.log(LOG_TAG_ON_USER_LOGIN, TAG + "ConfigIdentitySet [" + configKeySet + "]");

        /* ---------------------------------------------------
         *    Push error to LC in-case the data available
         *   in the config & preferences are not matching
         * --------------------------------------------------- */
        handleError(prefKeySet, configKeySet);

        /* ---------------------------------------------------
         *      If data is available from Pref, use
         *      else data is available via from config use
         *      else use legacy key set
         * --------------------------------------------------- */
        if (prefKeySet.isValid()) {
            identitySet = prefKeySet;
            config.log(LOG_TAG_ON_USER_LOGIN, TAG + "Identity Set activated from Pref[" + identitySet + "]");
        } else if (configKeySet.isValid()) {
            identitySet = configKeySet;
            config.log(LOG_TAG_ON_USER_LOGIN, TAG + "Identity Set activated from Config[" + identitySet + "]");
        } else {
            identitySet = IdentitySet.getDefault();
            config.log(LOG_TAG_ON_USER_LOGIN, TAG + "Identity Set activated from Default[" + identitySet + "]");
        }
        boolean isSavedInPref = prefKeySet.isValid();
        if (!isSavedInPref) {
            /* -------------------------------------------------------------------------
             *  If Config data is available, save in the preference if not saved already.
             * ------------------------------------------------------------------------ */
            String storedValue = identitySet.toString();
            infoProvider.saveIdentityKeysForAccount(storedValue);
            config.log(LOG_TAG_ON_USER_LOGIN, TAG + "Saving Identity Keys in Pref[" + storedValue + "]");
        }
    }

    /**
     * Push error to LC in-case the data available
     * in the config & preferences are not matching
     *
     * @param prefKeySet   - key set in the preference
     * @param configKeySet - key set in the config
     */
    private void handleError(final IdentitySet prefKeySet, final IdentitySet configKeySet) {
        if (prefKeySet.isValid() && configKeySet.isValid() && !prefKeySet.equals(configKeySet)) {
            ValidationResult error = ValidationResultFactory.create(531);
            validationResultStack.pushValidationResult(error);
            config.log(LOG_TAG_ON_USER_LOGIN,
                    TAG + "pushing error due to mismatch [Pref:" + prefKeySet + "], [Config:" + configKeySet + "]");
        } else {
            config.log(LOG_TAG_ON_USER_LOGIN,
                    TAG + "No error found while comparing [Pref:" + prefKeySet + "], [Config:" + configKeySet + "]");
        }
    }
}