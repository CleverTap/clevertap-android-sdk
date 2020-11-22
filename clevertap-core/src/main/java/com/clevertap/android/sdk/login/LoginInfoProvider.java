package com.clevertap.android.sdk.login;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.BaseCTApiListener;
import com.clevertap.android.sdk.CTJsonConverter;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.StorageHelper;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY)
public class LoginInfoProvider {

    private final Context context;

    private final CleverTapInstanceConfig config;

    private final DeviceInfo mDeviceInfo;

    public LoginInfoProvider(BaseCTApiListener ctApiListener) {
        context = ctApiListener.context();
        config = ctApiListener.config();
        mDeviceInfo = ctApiListener.deviceInfo();
    }

    //Profile
    public JSONObject getCachedGUIDs() {
        String json = StorageHelper.getStringFromPrefs(context, config, Constants.CACHED_GUIDS_KEY, null);
        return CTJsonConverter.toJsonObject(json, config.getLogger(), config.getAccountId());
    }

    public void setCachedGUIDs(JSONObject cachedGUIDs) {
        if (cachedGUIDs == null) {
            return;
        }
        try {
            StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(config, Constants.CACHED_GUIDS_KEY),
                    cachedGUIDs.toString());
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Error persisting guid cache: " + t.toString());
        }
    }

    public boolean isAnonymousDevice() {
        JSONObject cachedGUIDs = getCachedGUIDs();
        return cachedGUIDs.length() <= 0;
    }

    public boolean deviceIsMultiUser() {
        JSONObject cachedGUIDs = getCachedGUIDs();
        return cachedGUIDs.length() > 1;
    }

    public void cacheGUIDForIdentifier(String guid, String key, String identifier) {
        if (isErrorDeviceId() || guid == null || key == null || identifier == null) {
            return;
        }

        String cacheKey = key + "_" + identifier;
        JSONObject cache = getCachedGUIDs();
        try {
            cache.put(cacheKey, guid);
            setCachedGUIDs(cache);
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Error caching guid: " + t.toString());
        }
    }

    private boolean isErrorDeviceId() {
        return mDeviceInfo.isErrorDeviceId();
    }

    public String getGUIDForIdentifier(String key, String identifier) {
        if (key == null || identifier == null) {
            return null;
        }

        String cacheKey = key + "_" + identifier;
        JSONObject cache = getCachedGUIDs();
        try {
            return cache.getString(cacheKey);
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Error reading guid cache: " + t.toString());
            return null;
        }
    }

    public boolean isLegacyProfileLoggedIn() {
        return getCachedGUIDs() != null && TextUtils.isEmpty(getCachedIdentityKeysForAccount());
    }

    public String getCachedIdentityKeysForAccount() {
        return StorageHelper
                .getStringFromPrefs(context, config, Constants.SP_KEY_PROFILE_IDENTITIES, "");
    }

    public void saveIdentityKeysForAccount(final String keyCommaSeparated) {
        StorageHelper.putString(context, config, keyCommaSeparated,
                Constants.EMPTY_STRING);
    }
}