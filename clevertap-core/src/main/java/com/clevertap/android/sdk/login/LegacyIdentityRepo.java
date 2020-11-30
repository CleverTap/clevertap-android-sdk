package com.clevertap.android.sdk.login;

import static com.clevertap.android.sdk.LogConstants.LOG_TAG_ON_USER_LOGIN;

import androidx.annotation.NonNull;
import com.clevertap.android.sdk.BaseCTApiListener;

/**
 * Legacy class which handles old static identity logic.
 * Here the profile set was fixed as < Email, Identity >
 */
public class LegacyIdentityRepo implements IdentityRepo {

    private final IdentitySet identities;

    private final BaseCTApiListener mCTApiListener;

    public LegacyIdentityRepo(final BaseCTApiListener ctApiListener) {
        this.mCTApiListener = ctApiListener;
        this.identities = IdentitySet.getDefault();
    }

    @Override
    public IdentitySet getIdentitySet() {
        return identities;
    }

    @Override
    public boolean hasIdentity(@NonNull final String Key) {
        boolean hasIdentity = identities.contains(Key);
        mCTApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                "isIdentity [Key: " + Key + " , Value: " + hasIdentity + "]");
        return hasIdentity;
    }
}