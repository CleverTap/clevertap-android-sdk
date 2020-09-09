package com.clevertap.android.sdk.pushnotification;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.CTExecutors;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.StorageHelper;

import java.util.ArrayList;
import java.util.List;

import static com.clevertap.android.sdk.BuildConfig.VERSION_CODE;

/**
 * loads providers
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PushProviders implements CTPushProviderListener {

    private final ArrayList<CTPushProvider> availableProviders = new ArrayList<>();
    private final CTApiPushListener ctApiPushListener;

    private PushProviders(CTApiPushListener ctApiPushListener) {
        this.ctApiPushListener = ctApiPushListener;
    }

    /**
     * Factory method to load push providers.
     *
     * @return A PushProviders class with the loaded providers.
     */
    @NonNull
    public static PushProviders load(CTApiPushListener ctApiPushListener) {
        PushProviders providers = new PushProviders(ctApiPushListener);
        providers.init();
        return providers;
    }

    /**
     * Loads all the plugins that are currently supported by the device.
     */
    private void init() {
        List<CTPushProvider> providers = createProviders();

        if (providers.isEmpty()) {
            log("No push providers found!. Make sure to install at least one push provider");
            return;
        }

        for (CTPushProvider provider : providers) {
            if (!isValid(provider)) {
                log("Invalid Provider: " + provider.getClass());
                continue;
            }

            if (!provider.isSupported()) {
                log("Unsupported Provider: " + provider.getClass());
                continue;
            }

            if (provider.isAvailable()) {
                log("Available Provider: " + provider.getClass());
                availableProviders.add(provider);
            } else {
                log("Unavailable Provider: " + provider.getClass());
            }
        }
    }

    private boolean isValid(CTPushProvider provider) {

        if (VERSION_CODE < provider.minSDKSupportVersionCode()) {
            log("Provider: %s version %s does not match the SDK version %s. Make sure all Airship dependencies are the same version.");
            return false;
        }
        switch (provider.getPushType()) {
            case FCM:
            case HPS:
            case XPS:
            case BPS:
                if (provider.getPlatform() != PushConstants.ANDROID_PLATFORM) {
                    log("Invalid Provider: " + provider.getClass() +
                            " delivery is only available for Android platforms." + provider.getPushType());
                    return false;
                }
                break;
            case ADM:
                if (provider.getPlatform() != PushConstants.AMAZON_PLATFORM) {
                    log("Invalid Provider: " +
                            provider.getClass() +
                            " ADM delivery is only available for Amazon platforms." + provider.getPushType());
                    return false;
                }
                break;
        }

        return true;
    }

    /**
     * Creates the list of push providers.
     *
     * @return The list of push providers.
     */
    @NonNull
    private List<CTPushProvider> createProviders() {
        List<CTPushProvider> providers = new ArrayList<>();

        for (PushConstants.PushType pushType : config().getAllowedPushTypes()) {
            CTPushProvider pushProvider = null;
            try {
                Class<?> providerClass = Class.forName(pushType.getClassName());
                pushProvider = (CTPushProvider) providerClass.newInstance();
                pushProvider.setCTPushListener(this);
                log("Found provider:" + providerClass);
            } catch (InstantiationException e) {
                log("Unable to create provider InstantiationException" + pushType.getClassName());
            } catch (IllegalAccessException e) {
                log("Unable to create provider IllegalAccessException" + pushType.getClassName());
            } catch (ClassNotFoundException e) {
                log("Unable to create provider ClassNotFoundException" + pushType.getClassName());
            } catch (Exception e) {
                log("Unable to create provider Exception" + pushType.getClassName(), e);
            }

            if (pushProvider == null) {
                continue;
            }

            providers.add(pushProvider);
        }

        return providers;
    }

    @NonNull
    public ArrayList<PushConstants.PushType> getAvailablePushTypes() {
        ArrayList<PushConstants.PushType> pushTypes = new ArrayList<>();
        for (CTPushProvider pushProvider : availableProviders) {
            pushTypes.add(pushProvider.getPushType());
        }
        return pushTypes;
    }

    public ArrayList<CTPushProvider> availableProviders() {
        return availableProviders;
    }

    public boolean isNotificationSupported() {
        for (PushConstants.PushType pushType : getAvailablePushTypes()) {
            if (getCachedToken(pushType) != null)
                return true;
        }
        return false;
    }

    @Override
    public Context context() {
        return ctApiPushListener.context();
    }

    @Override
    public CleverTapInstanceConfig config() {
        return ctApiPushListener.config();
    }

    private void log(String message) {
        log("", message);
    }

    public void log(String message, Throwable throwable) {
        ctApiPushListener.config().getLogger().verbose(getDefaultSuffix(""), message, throwable);
    }

    @Override
    public void log(String tag, String message) {
        ctApiPushListener.config().getLogger().verbose(getDefaultSuffix(tag), message);
    }

    @Override
    public void log(String tag, String message, Throwable throwable) {
        ctApiPushListener.config().getLogger().verbose(getDefaultSuffix(tag), message, throwable);
    }

    @Override
    public void onNewToken(String token, PushConstants.PushType pushType) {
        ctApiPushListener.onNewToken(token, pushType);
    }

    private String getDefaultSuffix(String tag) {
        return "[" + PushConstants.LOG_TAG + ":" + ctApiPushListener.config().getAccountId() + "]" + (!TextUtils.isEmpty(tag) ? ": " + tag : "");
    }

    public void cacheToken(final String token, final PushConstants.PushType pushType) {
        if (TextUtils.isEmpty(token) || pushType == null) return;

        try {
            CTExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    if (alreadyHaveToken(token, pushType)) return;
                    @PushConstants.RegKeyType String key = pushType.getTokenPrefKey();
                    if (TextUtils.isEmpty(key)) return;
                    StorageHelper.putStringImmediate(context(), StorageHelper.storageKeyWithSuffix(config(), key), token);
                    log(pushType + "Cached New Token successfully " + token);
                }
            });

        } catch (Throwable t) {
            log( pushType + "Unable to cache token " + token, t);
        }
    }

    private boolean alreadyHaveToken(String newToken, PushConstants.PushType pushType) {
        boolean alreadyAvailable = !TextUtils.isEmpty(newToken) && pushType != null && newToken.equalsIgnoreCase(getCachedToken(pushType));
        if (pushType != null)
            log( pushType + "Token Already available value: " + alreadyAvailable);
        return alreadyAvailable;
    }

    public String getCachedToken(PushConstants.PushType pushType) {
        if (pushType != null) {
            @PushConstants.RegKeyType String key = pushType.getTokenPrefKey();
            if (!TextUtils.isEmpty(key)) {
                String cachedToken = StorageHelper.getStringFromPrefs(context(), config(), key, null);
                log( pushType + "getting Cached Token - " + cachedToken);
                return cachedToken;
            }
        }
        if (pushType != null) {
            log( pushType + " Unable to find cached Token for type ");
        }
        return null;
    }

    public void handleToken(String token, PushConstants.PushType pushType, boolean register) {
        if (register) {
            registerToken(token, pushType);
        } else {
            unregisterToken(token, pushType);
        }
    }

    public void unregisterToken(String token, PushConstants.PushType pushType) {
        ctApiPushListener.pushDeviceTokenEvent(token, false, pushType);
        removeCachedToken(pushType);
    }

    private void removeCachedToken(final PushConstants.PushType pushType) {
        if (pushType != null) {

            try {
                CTExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        @PushConstants.RegKeyType String key = pushType.getTokenPrefKey();
                        if (TextUtils.isEmpty(key)) return;
                        StorageHelper.removeImmediate(context(), StorageHelper.storageKeyWithSuffix(config(), key));
                        log( pushType + "Removed Cached Token successfully ");
                    }
                });

            } catch (Throwable t) {
                log(pushType + "Unable to remove cached token ", t);
            }
        }
    }

    private void registerToken(String token, PushConstants.PushType pushType) {
        ctApiPushListener.pushDeviceTokenEvent(token, true, pushType);
        cacheToken(token, pushType);
    }

    public void refreshAllTokens() {
        CTExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                for (final CTPushProvider pushProvider : availableProviders()) {
                    try {
                        pushProvider.requestToken();
                    } catch (Throwable t) {
                        //no-op
                        log("Token Refresh error " + pushProvider, t);
                    }
                }
            }
        });
    }
}