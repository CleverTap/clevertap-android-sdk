package com.clevertap.android.sdk.login;

import android.content.Context;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.BaseCTApiListener;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.ValidationResult;
import com.clevertap.android.sdk.ValidationResultFactory;
import com.clevertap.android.sdk.ValidationResultStack;

public class ConfigurableProfileHandlerImpl implements IProfileHandler {

    protected final Context context;

    protected final CleverTapInstanceConfig mConfig;

    protected final ValidationResultStack errorLogger;

    private final LoginInfoProvider mInfoProvider;

    private ProfileKeysSet mProfileKeysSet;

    public ConfigurableProfileHandlerImpl(BaseCTApiListener ctApiListener) {
        this.context = ctApiListener.context();
        this.mConfig = ctApiListener.config();
        this.mInfoProvider = new LoginInfoProvider(ctApiListener);
        this.errorLogger = ctApiListener.remoteErrorLogger();
        loadIdentitySet();
    }

    @Override
    public boolean isProfileKey(@NonNull String Key) {
        return mProfileKeysSet.containsKey(Key);
    }

    void loadIdentitySet() {
        // Logic :
        // First Read from pref
        // If not present in the pref, set the default

        ProfileKeysSet prefKeySet = ProfileKeysSet.from(mInfoProvider.getCachedIdentityKeysForAccount());

        ProfileKeysSet configKeySet = ProfileKeysSet.from(mConfig.getProfileKeys(context));

        handleError(prefKeySet, configKeySet);

        if (prefKeySet.isValid()) {
            mProfileKeysSet = prefKeySet;
        } else if (configKeySet.isValid()) {
            mProfileKeysSet = configKeySet;
        } else {
            mProfileKeysSet = ProfileKeysSet.getDefault();
        }
        boolean isSavedInPref = prefKeySet.isValid();
        if (!isSavedInPref) {
            // phone,email,identity,name -> [phone][email][identity]
            // [phone][email][identity] -> phone,email,identity
            mInfoProvider.saveIdentityKeysForAccount(mProfileKeysSet.toString());
        }
    }

    private void handleError(final ProfileKeysSet prefKeySet, final ProfileKeysSet configKeySet) {
        if (prefKeySet.isValid() && configKeySet.isValid() && !prefKeySet.equals(configKeySet)) {
            ValidationResult error = ValidationResultFactory.create(531);
            errorLogger.pushValidationResult(error);
        }
    }
}