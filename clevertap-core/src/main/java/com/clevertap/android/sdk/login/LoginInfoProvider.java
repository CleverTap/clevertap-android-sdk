package com.clevertap.android.sdk.login;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.utils.CTJsonConverter;
import org.json.JSONObject;

/**
 * Handles saving and/or providing login related information.
 */
@RestrictTo(Scope.LIBRARY)
public class LoginInfoProvider {

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final DeviceInfo deviceInfo;

    public LoginInfoProvider(Context context, CleverTapInstanceConfig config, DeviceInfo deviceInfo) {
        this.context = context;
        this.config = config;
        this.deviceInfo = deviceInfo;
    }

    //Profile

    /**
     * Caches a single pair of <Identity_Value, Guid> for this account
     *
     * @param guid       - guid of the user
     * @param key        - Identity Key e.g Email
     * @param identifier - Value corresponding to the Key e.g abc@gmail.com
     *                   Format in which the entries are saved
     *                   "Email_abc@gmail.com:Guid"
     */
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

    public boolean deviceIsMultiUser() {
        JSONObject cachedGUIDs = getCachedGUIDs();
        boolean deviceIsMultiUser = cachedGUIDs.length() > 1;
        config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN,
                "deviceIsMultiUser:[" + deviceIsMultiUser + "]");
        return deviceIsMultiUser;
    }

    /**
     * @return - All pairs of cached <Identity_Value, Guid> for this account in json format.
     */
    public JSONObject getCachedGUIDs() {
        String json = StorageHelper.getStringFromPrefs(context, config, Constants.CACHED_GUIDS_KEY, null);
        config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN,
                "getCachedGUIDs:[" + json + "]");
        return CTJsonConverter.toJsonObject(json, config.getLogger(), config.getAccountId());
    }

    /**
     * Caches the <Identity_Value, Guid> pairs for this account
     *
     * @param cachedGUIDs - jsonObject of the Pairs
     */
    public void setCachedGUIDs(JSONObject cachedGUIDs) {
        if (cachedGUIDs == null) {
            return;
        }
        try {
            String cachedGuid = cachedGUIDs.toString();
            StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(config, Constants.CACHED_GUIDS_KEY),
                    cachedGuid);
            config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN,
                    "setCachedGUIDs:[" + cachedGuid + "]");
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Error persisting guid cache: " + t.toString());
        }
    }

    /**
     * @return - Cached Identity Keys for the account
     */
    public String getCachedIdentityKeysForAccount() {
        String cachedKeys = StorageHelper
                .getStringFromPrefs(context, config, Constants.SP_KEY_PROFILE_IDENTITIES, "");
        config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "getCachedIdentityKeysForAccount:" + cachedKeys);
        return cachedKeys;
    }

    /**
     * Returns the Guid Value corresponding to the given <Key, Value>
     *
     * @param key        - Identity Key e.g Email
     * @param identifier - Value corresponding to the Key e.g abc@gmail.com
     * @return - String value of Guid if any entry is saved with Key_Value
     */
    public String getGUIDForIdentifier(String key, String identifier) {
        if (key == null || identifier == null) {
            return null;
        }

        String cacheKey = key + "_" + identifier;
        JSONObject cache = getCachedGUIDs();
        try {
            String cachedGuid = cache.getString(cacheKey);
            config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN,
                    "getGUIDForIdentifier:[Key:" + key + ", value:" + cachedGuid + "]");
            return cachedGuid;
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Error reading guid cache: " + t.toString());
            return null;
        }
    }

    public boolean isAnonymousDevice() {
        JSONObject cachedGUIDs = getCachedGUIDs();
        boolean isAnonymousDevice = cachedGUIDs.length() <= 0;
        config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN,
                "isAnonymousDevice:[" + isAnonymousDevice + "]");
        return isAnonymousDevice;
    }

    /**
     * Checks if any user was previously logged in using the legacy identity set{@link
     * Constants#LEGACY_IDENTITY_KEYS}for the
     * account.
     */
    public boolean isLegacyProfileLoggedIn() {
        JSONObject jsonObject = getCachedGUIDs();
        boolean isLoggedIn = jsonObject != null && jsonObject.length() > 0 && TextUtils
                .isEmpty(getCachedIdentityKeysForAccount());
        config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "isLegacyProfileLoggedIn:" + isLoggedIn);
        return isLoggedIn;
    }

    /**
     * Saves cached identity keys in the preference
     *
     * @param valueCommaSeparated - identity keys in comma separated format e.g. (Email,Phone)
     */
    public void saveIdentityKeysForAccount(final String valueCommaSeparated) {
        StorageHelper.putString(context, config, Constants.SP_KEY_PROFILE_IDENTITIES,
                valueCommaSeparated);
        config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "saveIdentityKeysForAccount:" + valueCommaSeparated);
    }

    private boolean isErrorDeviceId() {
        boolean isErrorDeviceId = deviceInfo.isErrorDeviceId();
        config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN,
                "isErrorDeviceId:[" + isErrorDeviceId + "]");
        return isErrorDeviceId;
    }
}