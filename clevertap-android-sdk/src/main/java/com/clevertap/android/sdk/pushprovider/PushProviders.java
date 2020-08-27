package com.clevertap.android.sdk.pushprovider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.clevertap.android.sdk.BuildConfig;
import com.clevertap.android.sdk.CleverTapInstanceConfig;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * loads providers
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PushProviders {

    private final ArrayList<PushProvider> supportedProviders = new ArrayList<>();
    private final ArrayList<PushProvider> availableProviders = new ArrayList<>();
    private final CleverTapInstanceConfig config;
    private final Context context;

    private PushProviders(Context context, CleverTapInstanceConfig config) {
        this.config = config;
        this.context = context;
    }

    /**
     * Factory method to load push providers.
     *
     * @return A PushProviders class with the loaded providers.
     */
    @NonNull
    public static PushProviders load(Context context, CleverTapInstanceConfig config) {
        PushProviders providers = new PushProviders(context, config);
        providers.init();
        return providers;
    }

    /**
     * Loads all the plugins that are currently supported by the device.
     */
    private void init() {
        List<PushProvider> providers = createProviders();

        if (providers.isEmpty()) {
            config.getLogger().verbose("No push providers found!. Make sure to install at least one push provider");
            return;
        }

        for (PushProvider provider : providers) {
            if (!isValid(provider)) {
                config.getLogger().verbose("Invalid Provider: " + provider.getClass());
                continue;
            }

            if (!provider.isSupported()) {
                config.getLogger().verbose("Unsupported Provider: " + provider.getClass());
                continue;
            }

            supportedProviders.add(provider);
            if (provider.isAvailable()) {
                config.getLogger().verbose("Available Provider: " + provider.getClass());
                availableProviders.add(provider);
            } else {
                config.getLogger().verbose("Unavailable Provider: " + provider.getClass());
            }
        }
    }

    private boolean isValid(PushProvider provider) {

        if (BuildConfig.VERSION_CODE < provider.minSDKSupportVersionCode()) {
            config.getLogger().verbose("Provider: %s version %s does not match the SDK version %s. Make sure all Airship dependencies are the same version.");
            return false;
        }
        switch (provider.getPushType()) {
            case FCM:
            case HPS:
            case XPS:
            case BPS:
                if (provider.getPlatform() != PushConstants.ANDROID_PLATFORM) {
                    config.getLogger().verbose(config.getAccountId(), "Invalid Provider: " + provider.getClass() +
                            " delivery is only available for Android platforms." + provider.getPushType());
                    return false;
                }
                break;
            case ADM:
                if (provider.getPlatform() != PushConstants.AMAZON_PLATFORM) {
                    config.getLogger().verbose(config.getAccountId(), "Invalid Provider: " +
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
    private List<PushProvider> createProviders() {
        List<PushProvider> providers = new ArrayList<>();

        for (PushConstants.PushType pushType : config.getAllowedPushTypes()) {
            PushProvider pushProvider = null;
            try {
                Class<?> providerClass = Class.forName(pushType.getClassName());
                Constructor<?> constructor = providerClass.getConstructor(Context.class, CleverTapInstanceConfig.class);
                pushProvider = (PushProvider) constructor.newInstance(context, config);
                config.getLogger().verbose(config.getAccountId(), "Found provider:" + providerClass);
            } catch (InstantiationException e) {
                config.getLogger().verbose(config.getAccountId(), "Unable to create provider " + pushType.getClassName());
            } catch (IllegalAccessException e) {
                config.getLogger().verbose(config.getAccountId(), "Unable to create provider " + pushType.getClassName());
            } catch (ClassNotFoundException e) {
                config.getLogger().verbose(config.getAccountId(), "Unable to create provider " + pushType.getClassName());
            } catch (NoSuchMethodException e) {
                config.getLogger().verbose(config.getAccountId(), "Unable to create provider " + pushType.getClassName());
            } catch (Exception e) {
                config.getLogger().verbose(config.getAccountId(), "Unable to create provider " + pushType.getClassName());
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
        for (PushProvider pushProvider : availableProviders) {
            pushTypes.add(pushProvider.getPushType());
        }
        return pushTypes;
    }

    public ArrayList<PushProvider> availableProviders() {
        return availableProviders;
    }
}