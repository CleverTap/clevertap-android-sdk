package com.clevertap.android.sdk.login;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.clevertap.android.sdk.BaseCTApiListener;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ValidationResultStack;
import java.util.HashSet;

public abstract class BaseProfilerHandler {

    protected final Context context;

    protected final CleverTapInstanceConfig mConfig;

    protected final ValidationResultStack mResultStack;

    private final IValidator iValidator;

    private final LoginInfoProvider mInfoProvider;

    private final HashSet<String> profileIdentitySet;

    BaseProfilerHandler(BaseCTApiListener ctApiListener) {
        this.context = ctApiListener.context();
        this.mConfig = ctApiListener.config();
        this.mResultStack = ctApiListener.remoteErrorLogger();
        this.iValidator = new ProfileValidatorImpl(ctApiListener.remoteErrorLogger());
        this.mInfoProvider = new LoginInfoProvider(ctApiListener);
        this.profileIdentitySet = loadIdentitySet();
    }

    public boolean isProfileKey(@NonNull String Key) {
        for (String profileKeys : profileIdentitySet) {
            if (profileKeys.equalsIgnoreCase(Key)) {
                return true;
            }
        }
        return false;
    }

    HashSet<String> loadIdentitySet() {
        // Logic :
        // First Read from pref
        // If not present in the pref, set the default

        String prefProfileKeys = mInfoProvider.getCachedIdentityKeysForAccount();
        boolean isSavedInPref = !TextUtils.isEmpty(prefProfileKeys);

        /* For default instance, use manifest meta
         */

        String[] profileIdentifierKeys = isSavedInPref ? prefProfileKeys.split(Constants.SEPARATOR_COMMA)
                : fetchConfig();
        HashSet<String> hashSet = iValidator.toIdentityType(profileIdentifierKeys);
        if (isSavedInPref) {
            iValidator.sendErrorOnIdentityMismatch(prefProfileKeys.split(Constants.SEPARATOR_COMMA), fetchConfig());
        }
        if (hashSet.isEmpty()) {
            hashSet.addAll(defaultIdentitySet());
        }
        if (!isSavedInPref) {
            // phone,email,identity,name -> [phone][email][identity]
            // [phone][email][identity] -> phone,email,identity
            mInfoProvider.saveIdentityKeysForAccount(iValidator.toIdentityString(hashSet));
        }

        return hashSet;
    }

    HashSet<String> defaultIdentitySet() {
        return Constants.DEFAULT_PROFILE_IDENTIFIER_KEYS;
    }

    private String[] fetchConfig() {
        return mConfig.getProfileKeys(context);
    }
}