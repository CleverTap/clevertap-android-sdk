package com.clevertap.android.sdk.login

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.cryption.ICryptHandler
import com.clevertap.android.sdk.utils.CTJsonConverter
import org.json.JSONObject
import java.util.Locale
import androidx.annotation.RestrictTo

/**
 * Handles saving and/or providing login related information.
 *
 * [cryptHandler] is only needed by the cached-GUID read/write paths; callers that only query login
 * state (e.g. [isLegacyProfileLoggedIn], [deviceIsMultiUser]) may omit it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class LoginInfoProvider(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val cryptHandler: ICryptHandler? = null
) {

    private val requiredCryptHandler: ICryptHandler
        get() = checkNotNull(cryptHandler) { "LoginInfoProvider was created without an ICryptHandler" }

    //Profile

    /**
     * Caches a single pair of <Identity_Value, Guid> for this account and updates the length of the cgk map
     *
     * @param guid       - guid of the user
     * @param key        - Identity Key e.g Email
     * @param identifier - Value corresponding to the Key e.g abc@gmail.com
     *                   Format in which the entries are saved
     *                   "Email_abc@gmail.com:Guid"
     */
    open fun cacheGUIDForIdentifier(guid: String?, key: String?, identifier: String?) {
        if (guid == null || key == null || identifier == null) {
            return
        }
        val cacheKey = "${key}_$identifier"
        val cache = decryptedCachedGUIDs
        if (cache.optString(cacheKey) == guid) {
            return
        }
        try {
            cache.put(cacheKey, guid)
            setCachedGUIDsAndLength(cache.toString(), cache.length())
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "Error caching guid: $t")
        }
    }

    /**
     * Removes value for PII(Email) pair of <Email_Value, Guid> for this account from shared prefs and updates the length of the cgk map
     *
     * @param guid       - guid of the user
     * @param key        - Identity Key e.g Email
     */
    open fun removeValueFromCachedGUIDForIdentifier(guid: String?, key: String?) {
        if (guid == null || key == null) {
            return
        }

        val cachedGuidJsonObj = decryptedCachedGUIDs
        try {
            val keyInLowerCase = key.lowercase(Locale.getDefault())
            val iterator = cachedGuidJsonObj.keys()
            while (iterator.hasNext()) {
                val nextJSONObjKey = iterator.next()
                val actualKeyInLowerCase = nextJSONObjKey.lowercase(Locale.getDefault())

                if (keyInLowerCase in actualKeyInLowerCase && cachedGuidJsonObj.getString(nextJSONObjKey) == guid) {
                    cachedGuidJsonObj.remove(nextJSONObjKey)
                    setCachedGUIDsAndLength(cachedGuidJsonObj.toString(), cachedGuidJsonObj.length())
                }
            }
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "Error removing cached key: $t")
        }
    }

    open fun deviceIsMultiUser(): Boolean {
        val deviceIsMultiUser = cachedGuidsLength > 1
        config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "deviceIsMultiUser:[$deviceIsMultiUser]")
        return deviceIsMultiUser
    }

    /**
     * All pairs of cached <Identity_Value, Guid> for this account in String format.
     */
    private val cachedGUIDStringFromPrefs: String?
        get() {
            val json = StorageHelper.getStringFromPrefs(context, config.accountId, Constants.CACHED_GUIDS_KEY, null)
            config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "getCachedGUIDs:[$json]")
            return json
        }

    /**
     * All pairs of cached <Identity_Value, Guid> for this account in json format after decryption.
     * If decryption fails, an empty JSONObject is returned
     */
    open val decryptedCachedGUIDs: JSONObject
        get() {
            val json = cachedGUIDStringFromPrefs?.let { requiredCryptHandler.decryptSafe(it) }
            return CTJsonConverter.toJsonObject(json, config.logger, config.accountId)
        }

    /**
     * Caches or removes the <Identity_Value, Guid> pairs for this account
     *
     * @param cachedGUIDs - jsonObject of the Pairs
     * @param cgkLength - length of the JSONObject
     */
    open fun setCachedGUIDsAndLength(cachedGUIDs: String?, cgkLength: Int) {
        if (cachedGUIDs == null) {
            return
        }
        val encrypted = if (EncryptionLevel.fromInt(config.encryptionLevel) != EncryptionLevel.NONE) {
            requiredCryptHandler.encryptSafe(cachedGUIDs).also {
                if (it == null) {
                    requiredCryptHandler.updateMigrationFailureCount(false)
                }
            }
        } else {
            null
        }
        val saveString = encrypted ?: cachedGUIDs
        StorageHelper.putInt(context, config.accountId, Constants.CACHED_GUIDS_LENGTH_KEY, cgkLength)
        config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "Storing size of cachedGUIDs: $cgkLength")
        if (cgkLength == 0) {
            removeCachedGuidFromSharedPrefs()
            return
        }
        StorageHelper.putString(context, config.accountId, Constants.CACHED_GUIDS_KEY, saveString)
        config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "setCachedGUIDs:[$cachedGUIDs]")
    }

    private val cachedGuidsLength: Int
        get() {
            val cgkLength =
                StorageHelper.getIntFromPrefs(context, config.accountId, Constants.CACHED_GUIDS_LENGTH_KEY, 0)
            config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "Retrieved size of cachedGUIDs: $cgkLength")
            return cgkLength
        }

    open fun removeCachedGuidFromSharedPrefs() {
        try {
            StorageHelper.remove(context, config.accountId, Constants.CACHED_GUIDS_KEY)
            config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "removeCachedGUIDs:[]")
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "Error removing guid cache: $t")
        }
    }

    /**
     * Cached Identity Keys for the account ("" when none are saved; SharedPreferences never stores null,
     * so the default is what an absent key reads as)
     */
    open val cachedIdentityKeysForAccount: String
        get() {
            val cachedKeys = StorageHelper.getStringFromPrefs(
                context, config.accountId, Constants.SP_KEY_PROFILE_IDENTITIES, ""
            ).orEmpty()
            config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "getCachedIdentityKeysForAccount:$cachedKeys")
            return cachedKeys
        }

    /**
     * Returns the Guid Value corresponding to the given <Key, Value>
     * If guid is not found, then it returns null
     * @param key        - Identity Key e.g Email
     * @param identifier - Value corresponding to the Key e.g abc@gmail.com
     * @return - String value of Guid if any entry is saved with Key_Value
     */
    open fun getGUIDForIdentifier(key: String?, identifier: String?): String? {
        if (key == null || identifier == null) {
            return null
        }
        val cacheKey = "${key}_$identifier"
        val cache = decryptedCachedGUIDs
        return try {
            val cachedGuid = cache.getString(cacheKey)
            config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "getGUIDForIdentifier:[Key:$key, value:$cachedGuid]")
            cachedGuid
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "Error reading guid cache: $t")
            null
        }
    }

    open val isAnonymousDevice: Boolean
        get() {
            val isAnonymousDevice = cachedGuidsLength == 0
            config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "isAnonymousDevice:[$isAnonymousDevice]")
            return isAnonymousDevice
        }

    /**
     * Checks if any user was previously logged in using the legacy identity set[Constants.LEGACY_IDENTITY_KEYS]
     * for the account.
     */
    open val isLegacyProfileLoggedIn: Boolean
        get() {
            val isLoggedIn = cachedGuidsLength > 0 && cachedIdentityKeysForAccount.isEmpty()
            config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "isLegacyProfileLoggedIn:$isLoggedIn")
            return isLoggedIn
        }

    /**
     * Saves cached identity keys in the preference
     *
     * @param valueCommaSeparated - identity keys in comma separated format e.g. (Email,Phone)
     */
    open fun saveIdentityKeysForAccount(valueCommaSeparated: String?) {
        StorageHelper.putString(context, config.accountId, Constants.SP_KEY_PROFILE_IDENTITIES, valueCommaSeparated)
        config.log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "saveIdentityKeysForAccount:$valueCommaSeparated")
    }
}
