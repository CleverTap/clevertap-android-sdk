package com.clevertap.android.sdk.login;

import static com.clevertap.android.sdk.LogConstants.LOG_TAG_ON_USER_LOGIN;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.BaseCTApiListener;

@RestrictTo(Scope.LIBRARY)
public class ProfileHandlerFactory {

    private ProfileHandlerFactory() {

    }

    public static IProfileHandler getProfileHandler(@NonNull BaseCTApiListener ctApiListener) {
        LoginInfoProvider cacheHandler = new LoginInfoProvider(ctApiListener);
        IProfileHandler profileHandler = null;
        if (cacheHandler.isLegacyProfileLoggedIn()) {

            // case 1: Migration( cached guid but no newly saved profile pref)
            profileHandler = new LegacyProfileHandlerImpl(ctApiListener);
        } else {
            // case 2: Not logged in but default config
            // case 3: Not logged in but non-default config
            profileHandler = new ConfigurableProfileHandlerImpl(ctApiListener);
        }
        ctApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                "getProfileHandler Returns: " + profileHandler.getClass().getSimpleName());
        return profileHandler;
    }
}
