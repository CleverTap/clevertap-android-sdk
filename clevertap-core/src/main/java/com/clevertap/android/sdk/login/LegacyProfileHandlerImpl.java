package com.clevertap.android.sdk.login;

import androidx.annotation.NonNull;

public class LegacyProfileHandlerImpl implements IProfileHandler {

    LegacyProfileHandlerImpl() {
    }

    @Override
    public boolean isProfileKey(@NonNull final String Key) {
        return ProfileKeysSet.getDefault().containsKey(Key);
    }
}