package com.clevertap.android.sdk.pushnotification;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.CTExecutors;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.StorageHelper;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static com.clevertap.android.sdk.BuildConfig.VERSION_CODE;

/**
 * loads providers
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PushProviders implements CTPushProviderListener {
    private final ArrayList<PushConstants.PushType> allEnabledPushTypes = new ArrayList<>();
    private final ArrayList<PushConstants.PushType> customEnabledPushTypes = new ArrayList<>();
    private final ArrayList<CTPushProvider> availableCTPushProviders = new ArrayList<>();
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

        findEnabledPushTypes();

        List<CTPushProvider> providers = createProviders();

        findCTPushProviders(providers);

        findCustomEnabledPushTypes();
    }

    private void findCTPushProviders(List<CTPushProvider> providers) {
        if (providers.isEmpty()) {
            config().log(PushConstants.LOG_TAG, "No push providers found!. Make sure to install at least one push provider");
            return;
        }

        for (CTPushProvider provider : providers) {
            if (!isValid(provider)) {
                config().log(PushConstants.LOG_TAG, "Invalid Provider: " + provider.getClass());
                continue;
            }

            if (!provider.isSupported()) {
                config().log(PushConstants.LOG_TAG, "Unsupported Provider: " + provider.getClass());
                continue;
            }

            if (provider.isAvailable()) {
                config().log(PushConstants.LOG_TAG, "Available Provider: " + provider.getClass());
                availableCTPushProviders.add(provider);
            } else {
                config().log(PushConstants.LOG_TAG, "Unavailable Provider: " + provider.getClass());
            }
        }
    }

    private void findCustomEnabledPushTypes() {
        customEnabledPushTypes.addAll(allEnabledPushTypes);
        for (final CTPushProvider pushProvider : availableCTPushProviders) {
            customEnabledPushTypes.remove(pushProvider.getPushType());
        }
    }

    private void findEnabledPushTypes() {
        for (PushConstants.PushType pushType : config().getAllowedPushTypes()) {
            String className = pushType.getMessagingSDKClassName();
            try {
                Class.forName(className);
                allEnabledPushTypes.add(pushType);
                config().log(PushConstants.LOG_TAG, "SDK Class Available :" + className);
            } catch (Exception e) {
                config().log(PushConstants.LOG_TAG, "SDK class Not available " + className + " Exception:" + e.getClass().getName());
            }
        }
    }

    private boolean isValid(CTPushProvider provider) {

        if (VERSION_CODE < provider.minSDKSupportVersionCode()) {
            config().log(PushConstants.LOG_TAG, "Provider: %s version %s does not match the SDK version %s. Make sure all Airship dependencies are the same version.");
            return false;
        }
        switch (provider.getPushType()) {
            case FCM:
            case HPS:
            case XPS:
            case BPS:
                if (provider.getPlatform() != PushConstants.ANDROID_PLATFORM) {
                    config().log(PushConstants.LOG_TAG, "Invalid Provider: " + provider.getClass() +
                            " delivery is only available for Android platforms." + provider.getPushType());
                    return false;
                }
                break;
            case ADM:
                if (provider.getPlatform() != PushConstants.AMAZON_PLATFORM) {
                    config().log(PushConstants.LOG_TAG, "Invalid Provider: " +
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

        for (PushConstants.PushType pushType : allEnabledPushTypes) {
            String className = pushType.getCtProviderClassName();
            CTPushProvider pushProvider = null;
            try {
                Class<?> providerClass = Class.forName(className);
                Constructor<?> constructor = providerClass.getConstructor(CTPushProviderListener.class);
                pushProvider = (CTPushProvider) constructor.newInstance(this);
                config().log(PushConstants.LOG_TAG, "Found provider:" + className);
            } catch (InstantiationException e) {
                config().log(PushConstants.LOG_TAG, "Unable to create provider InstantiationException" + className);
            } catch (IllegalAccessException e) {
                config().log(PushConstants.LOG_TAG, "Unable to create provider IllegalAccessException" + className);
            } catch (ClassNotFoundException e) {
                config().log(PushConstants.LOG_TAG, "Unable to create provider ClassNotFoundException" + className);
            } catch (Exception e) {
                config().log(PushConstants.LOG_TAG, "Unable to create provider " + className + " Exception:" + e.getClass().getName());
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
        for (CTPushProvider pushProvider : availableCTPushProviders) {
            pushTypes.add(pushProvider.getPushType());
        }
        return pushTypes;
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


    @Override
    public void onNewToken(String token, PushConstants.PushType pushType) {
        ctApiPushListener.onNewToken(token, pushType);
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
                    config().log(PushConstants.LOG_TAG, pushType + "Cached New Token successfully " + token);
                }
            });

        } catch (Throwable t) {
            config().log(PushConstants.LOG_TAG, pushType + "Unable to cache token " + token, t);
        }
    }

    private boolean alreadyHaveToken(String newToken, PushConstants.PushType pushType) {
        boolean alreadyAvailable = !TextUtils.isEmpty(newToken) && pushType != null && newToken.equalsIgnoreCase(getCachedToken(pushType));
        if (pushType != null)
            config().log(PushConstants.LOG_TAG, pushType + "Token Already available value: " + alreadyAvailable);
        return alreadyAvailable;
    }

    public String getCachedToken(PushConstants.PushType pushType) {
        if (pushType != null) {
            @PushConstants.RegKeyType String key = pushType.getTokenPrefKey();
            if (!TextUtils.isEmpty(key)) {
                String cachedToken = StorageHelper.getStringFromPrefs(context(), config(), key, null);
                config().log(PushConstants.LOG_TAG, pushType + "getting Cached Token - " + cachedToken);
                return cachedToken;
            }
        }
        if (pushType != null) {
            config().log(PushConstants.LOG_TAG, pushType + " Unable to find cached Token for type ");
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
    }

    private void registerToken(String token, PushConstants.PushType pushType) {
        ctApiPushListener.pushDeviceTokenEvent(token, true, pushType);
        cacheToken(token, pushType);
    }

    public void refreshAllTokens() {
        CTExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                // refresh tokens of Push Providers
                refreshCTProviderTokens();

                // refresh tokens of custom Providers
                refreshCustomProviderTokens();
            }
        });
    }

    private void refreshCustomProviderTokens() {
        for (PushConstants.PushType pushType : customEnabledPushTypes) {
            try {
                ctApiPushListener.pushDeviceTokenEvent(getCachedToken(pushType), true, pushType);
            } catch (Throwable t) {
                config().log(PushConstants.LOG_TAG, "Token Refresh error " + pushType, t);
            }
        }
    }

    private void refreshCTProviderTokens() {
        for (final CTPushProvider pushProvider : availableCTPushProviders) {
            try {
                pushProvider.requestToken();
            } catch (Throwable t) {
                //no-op
                config().log(PushConstants.LOG_TAG, "Token Refresh error " + pushProvider, t);
            }
        }
    }
}