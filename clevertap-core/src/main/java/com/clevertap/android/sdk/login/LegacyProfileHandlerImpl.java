package com.clevertap.android.sdk.login;

import com.clevertap.android.sdk.BaseCTApiListener;
import java.util.HashSet;

public class LegacyProfileHandlerImpl extends BaseProfilerHandler {

    LegacyProfileHandlerImpl(final BaseCTApiListener ctApiListener) {
        super(ctApiListener);
    }

    @Override
    HashSet<String> loadIdentitySet() {
        return defaultIdentitySet();
    }
}