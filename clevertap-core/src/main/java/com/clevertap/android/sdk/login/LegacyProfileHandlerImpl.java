package com.clevertap.android.sdk.login;

import static com.clevertap.android.sdk.LogConstants.LOG_TAG_ON_USER_LOGIN;

import androidx.annotation.NonNull;
import com.clevertap.android.sdk.BaseCTApiListener;

public class LegacyProfileHandlerImpl implements IProfileHandler {

    private final BaseCTApiListener mCTApiListener;

    public LegacyProfileHandlerImpl(final BaseCTApiListener ctApiListener) {
        this.mCTApiListener = ctApiListener;
    }

    @Override
    public boolean isProfileKey(@NonNull final String Key) {
        boolean isProfileKey = ProfileKeysSet.getDefault().containsKey(Key);
        mCTApiListener.config().getLogger().verbose(LOG_TAG_ON_USER_LOGIN,
                "isProfileKey [Key: " + Key + " , Value: " + isProfileKey + "]");
        return isProfileKey;
    }
}