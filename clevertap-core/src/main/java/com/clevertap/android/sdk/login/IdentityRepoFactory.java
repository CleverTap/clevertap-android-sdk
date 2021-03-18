package com.clevertap.android.sdk.login;

import static com.clevertap.android.sdk.login.LoginConstants.LOG_TAG_ON_USER_LOGIN;

import android.content.Context;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.validation.ValidationResultStack;

/**
 * Provides Repo instance for an account
 */
@RestrictTo(Scope.LIBRARY)
public class IdentityRepoFactory {

    /**
     * Creates repo provider based on login state & config details.
     *
     * @return - repo provider
     */
    public static IdentityRepo getRepo(Context context, CleverTapInstanceConfig config, DeviceInfo deviceInfo,
            ValidationResultStack validationResultStack) {
        final LoginInfoProvider infoProvider = new LoginInfoProvider(context, config, deviceInfo);
        final IdentityRepo repo;
        if (infoProvider.isLegacyProfileLoggedIn()) {
            repo = new LegacyIdentityRepo(
                    config);// case 1: Migration (cached guid but no newly saved profile pref)
        } else {
            /* ----------------------------------------------------
             * case 2: Not logged in & using default config
             * case 3: Not logged in & using multi instance config
             * -----------------------------------------------------*/
            repo = new ConfigurableIdentityRepo(context, config, deviceInfo, validationResultStack);
        }
        config.log(LOG_TAG_ON_USER_LOGIN,
                "Repo provider: " + repo.getClass().getSimpleName());
        return repo;
    }

    private IdentityRepoFactory() {
        // private constructor
    }
}
