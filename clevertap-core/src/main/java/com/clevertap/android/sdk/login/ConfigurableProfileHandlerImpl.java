package com.clevertap.android.sdk.login;

import static com.clevertap.android.sdk.LogConstants.LOG_TAG_ON_USER_LOGIN;

import androidx.annotation.NonNull;
import com.clevertap.android.sdk.BaseCTApiListener;
import com.clevertap.android.sdk.ValidationResult;
import com.clevertap.android.sdk.ValidationResultFactory;

public class ConfigurableProfileHandlerImpl implements IProfileHandler {

    private static final String TAG = "ConfigurableProfileHandlerImpl: ";

    private final BaseCTApiListener mCTApiListener;

    private final LoginInfoProvider mInfoProvider;

    private ProfileKeysSet mProfileKeysSet;

    public ConfigurableProfileHandlerImpl(BaseCTApiListener ctApiListener) {
        this.mCTApiListener = ctApiListener;
        this.mInfoProvider = new LoginInfoProvider(ctApiListener);
        loadIdentitySet();
    }

    @Override
    public boolean isProfileKey(@NonNull String Key) {
        boolean isProfileKey = mProfileKeysSet.containsKey(Key);
        mCTApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                TAG + "isProfileKey [Key: " + Key + " , Value: " + isProfileKey + "]");
        return isProfileKey;
    }

    void loadIdentitySet() {
        // Logic :
        // First Read from pref
        // If not present in the pref, set the default

        ProfileKeysSet prefKeySet = ProfileKeysSet.from(mInfoProvider.getCachedIdentityKeysForAccount());

        mCTApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                TAG + "PrefKeySet [" + prefKeySet + "]");
        ProfileKeysSet configKeySet = ProfileKeysSet
                .from(mCTApiListener.config().getProfileKeys(mCTApiListener.context()));

        mCTApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                TAG + "ConfigKeySet [" + configKeySet + "]");

        handleError(prefKeySet, configKeySet);

        if (prefKeySet.isValid()) {
            mProfileKeysSet = prefKeySet;
            mCTApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                    TAG + "Profile Set activated from Pref[" + mProfileKeysSet + "]");
        } else if (configKeySet.isValid()) {
            mProfileKeysSet = configKeySet;
            mCTApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                    TAG + "Profile Set activated from Config[" + mProfileKeysSet + "]");
        } else {
            mProfileKeysSet = ProfileKeysSet.getDefault();
            mCTApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                    TAG + "Profile Set activated from Default[" + mProfileKeysSet + "]");
        }
        boolean isSavedInPref = prefKeySet.isValid();
        if (!isSavedInPref) {
            // phone,email,identity,name -> [phone][email][identity]
            // [phone][email][identity] -> phone,email,identity
            String storedValue = mProfileKeysSet.toString();
            mInfoProvider.saveIdentityKeysForAccount(storedValue);
            mCTApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                    TAG + "Saving Profile Keys in Pref[" + storedValue + "]");
        }
    }

    private void handleError(final ProfileKeysSet prefKeySet, final ProfileKeysSet configKeySet) {
        if (prefKeySet.isValid() && configKeySet.isValid() && !prefKeySet.equals(configKeySet)) {
            ValidationResult error = ValidationResultFactory.create(531);
            mCTApiListener.remoteErrorLogger().pushValidationResult(error);
            mCTApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                    TAG + "pushing error due to mismatch [Pref:" + prefKeySet + "], [Config:" + configKeySet + "]");
        }else {
            mCTApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                    TAG + "No error found while comparing [Pref:" + prefKeySet + "], [Config:" + configKeySet + "]");
        }
    }
}