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

public class ConfigurableIdentityRepo implements IdentityRepo {

    private static final String TAG = "ConfigurableIdentityRepo";

    private IdentitySet identitySet;

    private final LoginInfoProvider infoProvider;

    private final CleverTapInstanceConfig config;

    private final ValidationResultStack validationResultStack;

    public ConfigurableIdentityRepo(Context context, CleverTapInstanceConfig config, DeviceInfo deviceInfo, ValidationResultStack mValidationResultStack) {
        this.config = config;
        this.infoProvider = new LoginInfoProvider(context, config, deviceInfo);
        this.validationResultStack = mValidationResultStack;
        loadIdentitySet(); // <--1
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ConfigurableIdentityRepo(Context context, CleverTapInstanceConfig config,LoginInfoProvider loginInfoProvider, ValidationResultStack mValidationResultStack) {
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
     * Loads the identity set:
     *
     * 1. create IdentitySet1 : 'prefKeySet'
     * |-- 1.1. A string is provided by loginInfoProvider.getCachedIdentityKeysForAccount()
     * |    |-- 1.1.1-5 the above function gets either string or null from storage based on 5 scenarios: whether keyvalue are coming from sp of default/nondefault config, etc
     * |-- 1.2 This string is of format "__,__,__etc" and is split by ',' .the new list is filtered for wrong keys and finally used to create 'prefKeySet'
     *
     * 2. create IdentitySet2 : 'configKeySet'
     * |-- 2.1 A string array is provided by config.getIdentityKeys()
     *      |-- 2.1.0 config is a dependency passed to ConfigurableIdentityRepo . it can be default or non default
     *      |-- 2.1.1 for default config instance identitiyKeys =manifest.getProfileKeys()
     *      |-- 2.1.2 for nondefault config instance , identitiyKeys = either null keys array or array of strings that are set post creation via config.setIdentityKeys(Constants.TYPE_EMAIL,Constants.TYPE_PHONE,..etc)
     * |-- 2.2 this array is filtered for wrong keys and finally used to create 'configKeySet'
     *
     * <note>: the validation critieria is that keyset must not be empty</note>
     *
     * 3. validate sets  : handleError(prefKeySet, configKeySet);
     * |-- 3.1 : if prefKeySet.isValid() && configKeySet.isValid() && !prefKeySet.equals(configKeySet), it will generate a validation error on vr stack passed via external dependency
     *
     *
     * 4. setting identity set
     * |-- 4.1  identitySet = prefkeyset(if prefKeySet.isValid())
     * |-- 4.2  identitySet = configkeyset(if configKeySet.isValid()) or
     * |-- 4.3  (if above 2 doesn't apply) identitySet =IdentitySet.getDefault()  = ['Identity','Email']
     *
     * 5. if (!prefKeySet.isValid() ) loginInfoProvider.saveIdentityKeysForAccount(identitySet) is also called on the newly initialised identitySet
     */
    void loadIdentitySet() {
        System.out.println("loadIdentitySet===called");
        // Read from Pref
        IdentitySet prefKeySet = IdentitySet.from(infoProvider.getCachedIdentityKeysForAccount());
        System.out.println("ConfigurableIdentityRepo|loadIdentitySet : prefKeySet='"+prefKeySet.toString()+"'");

        config.log(LOG_TAG_ON_USER_LOGIN, TAG + "PrefIdentitySet [" + prefKeySet + "]");

        /* ----------------------------------------------------------------
         *   For Default Instance - Get Identity Set configured via Manifest
         *   For Multi Instance - Get Identity Set configured via the setter
         * ---------------------------------------------------------------- */
        IdentitySet configKeySet = IdentitySet.from(config.getIdentityKeys());

        System.out.println("ConfigurableIdentityRepo|loadIdentitySet : configKeySet='"+configKeySet.toString()+"'");
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