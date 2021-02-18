package com.clevertap.android.sdk.login;

import static com.clevertap.android.sdk.LogConstants.LOG_TAG_ON_USER_LOGIN;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.BaseCTApiListener;

/**
 * Provides Repo instance for an account
 */
@RestrictTo(Scope.LIBRARY)
public class IdentityRepoFactory {

    /**
     * Creates repo provider based on login state & config details.
     *
     * @param ctApiListener - CleverTapAPI instance
     * @return - repo provider
     */
    public static IdentityRepo getRepo(@NonNull BaseCTApiListener ctApiListener) {
        final LoginInfoProvider infoProvider = new LoginInfoProvider(ctApiListener);
        final IdentityRepo repo;
        if (infoProvider.isLegacyProfileLoggedIn()) {
            repo = new LegacyIdentityRepo(
                    ctApiListener);// case 1: Migration (cached guid but no newly saved profile pref)
        } else {
            /* ----------------------------------------------------
             * case 2: Not logged in & using default config
             * case 3: Not logged in & using multi instance config
             * -----------------------------------------------------*/
            repo = new ConfigurableIdentityRepo(ctApiListener);
        }
        ctApiListener.config().log(LOG_TAG_ON_USER_LOGIN,
                "Repo provider: " + repo.getClass().getSimpleName());
        return repo;
    }

    private IdentityRepoFactory() {
        // private constructor
    }
}
