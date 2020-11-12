package com.clevertap.android.sdk.core.login;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.BaseCTApiListener;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Constants.IdentityType;
import com.clevertap.android.sdk.StorageHelper;
import java.util.HashSet;

@RestrictTo(Scope.LIBRARY)
public class CTProfileHandlerImpl implements IProfileHandler {

    private final IValidator iValidator;

    private final BaseCTApiListener mApiListener;

    public CTProfileHandlerImpl(BaseCTApiListener baseCTApiListener) {
        this.mApiListener = baseCTApiListener;
        this.iValidator = new ProfileValidatorImpl();
    }

    @Override
    public boolean isProfileKey(@NonNull final String Key) {
        return getProfileIdentitySet().contains(IdentityType.fromKey(Key));
    }

    private HashSet<Constants.IdentityType> getProfileIdentitySet() {

        // Logic :
        // First Read from pref
        // If not present in the pref, set the default

        String identifier = StorageHelper
                .getStringFromPrefs(mApiListener.context(), mApiListener.config(), Constants.SP_KEY_PROFILE_IDENTITIES, "");
        boolean isSavedInPref = !TextUtils.isEmpty(identifier);
        String[] profileIdentifierKeys = isSavedInPref ? identifier.split(Constants.SEPARATOR_COMMA)
                : mApiListener.config().getProfileKeys(mApiListener.context());
        HashSet<Constants.IdentityType> hashSet = iValidator.toIdentityType(profileIdentifierKeys);

        if (!hashSet.isEmpty()) {
            if (!isSavedInPref) {
                saveProfileKeysInPref(hashSet);
            }
        } else {
            hashSet.addAll(Constants.DEFAULT_PROFILE_IDENTIFIER_KEYS);
        }

        return hashSet;
    }

    private void saveProfileKeysInPref(final HashSet<IdentityType> hashSet) {
        StorageHelper.putString(mApiListener.context(), mApiListener.config(), iValidator.toIdentityString(hashSet),
                Constants.EMPTY_STRING);
    }
}