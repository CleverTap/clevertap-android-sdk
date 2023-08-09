package com.clevertap.android.sdk.cryption

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants.CACHED_GUIDS_KEY
import com.clevertap.android.sdk.Constants.ENCRYPTION_FLAG_ALL_SUCCESS
import com.clevertap.android.sdk.Constants.ENCRYPTION_FLAG_CGK_SUCCESS
import com.clevertap.android.sdk.Constants.ENCRYPTION_FLAG_DB_SUCCESS
import com.clevertap.android.sdk.Constants.ENCRYPTION_FLAG_FAIL
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_FLAG_STATUS
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_LEVEL
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_MIGRATION
import com.clevertap.android.sdk.Constants.piiDBKeys
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.utils.CTJsonConverter
import org.json.JSONObject

object CryptUtils {

    /**
     * This method migrates the encryption level of the stored data for the current account ID
     *
     * @param context - The Android context
     * @param config  - The [CleverTapInstanceConfig] object
     * @param cryptHandler - The [CryptHandler] object
     * @param dbAdapter - The [DBAdapter] object
     */
    @JvmStatic
    fun migrateEncryptionLevel(
        context: Context,
        config: CleverTapInstanceConfig,
        cryptHandler: CryptHandler,
        dbAdapter: DBAdapter
    ) {

        val encryptionFlagStatus: Int
        val configEncryptionLevel = config.encryptionLevel
        val storedEncryptionLevel = StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_LEVEL),
            -1
        )

        // Nothing to migrate if a new app install and configEncryption level is 0, hence return
        // If encryption level is updated (0 to 1 or 1 to 0) then set status to all migrations failed (0)
        encryptionFlagStatus = if (storedEncryptionLevel == -1 && configEncryptionLevel == 0)
            return
        else if (storedEncryptionLevel != configEncryptionLevel) {
            ENCRYPTION_FLAG_FAIL
        } else {
            StorageHelper.getInt(
                context,
                StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_FLAG_STATUS),
                ENCRYPTION_FLAG_FAIL
            )
        }
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_LEVEL),
            configEncryptionLevel
        )

        if (encryptionFlagStatus == ENCRYPTION_FLAG_ALL_SUCCESS) {
            config.logger.verbose(
                config.accountId,
                "Encryption flag status is 100% success, no need to migrate"
            )
            cryptHandler.encryptionFlagStatus = ENCRYPTION_FLAG_ALL_SUCCESS
            return
        }

        config.logger.verbose(
            config.accountId,
            "Migrating encryption level from $storedEncryptionLevel to $configEncryptionLevel with current flag status $encryptionFlagStatus"
        )

        // If configEncryptionLevel is one then encrypt otherwise decrypt
        migrateEncryption(
            configEncryptionLevel == 1,
            context,
            config,
            cryptHandler,
            encryptionFlagStatus,
            dbAdapter
        )
    }

    /**
     * This method migrates the encryption level. There are currently 2 migrations required.
     * The migration strategy is such that even if one entry fails in one of the 2 migrations, flag bit for that migration is set to 0
     * and reattempted during the next instance creation
     *
     * @param encrypt - Flag to indicate the task to be either encryption or decryption
     * @param config  - The [CleverTapInstanceConfig] object
     * @param context - The Android context
     * @param cryptHandler - The [CryptHandler] object
     * @param encryptionFlagStatus - Current value of the flag
     * @param dbAdapter - The [dbAdapter] object
     */
    private fun migrateEncryption(
        encrypt: Boolean,
        context: Context,
        config: CleverTapInstanceConfig,
        cryptHandler: CryptHandler,
        encryptionFlagStatus: Int,
        dbAdapter: DBAdapter
    ) {
        // And operation checks if the required bit is set or not
        var cgkFlag = encryptionFlagStatus and ENCRYPTION_FLAG_CGK_SUCCESS
        if (cgkFlag == ENCRYPTION_FLAG_FAIL)
            cgkFlag = migrateCachedGuidsKeyPref(encrypt, config, context, cryptHandler)

        var dbFlag = encryptionFlagStatus and ENCRYPTION_FLAG_DB_SUCCESS
        if (dbFlag == ENCRYPTION_FLAG_FAIL)
            dbFlag = migrateDBProfile(encrypt, config, cryptHandler, dbAdapter)

        val updatedFlagStatus = cgkFlag or dbFlag

        config.logger.verbose(
            config.accountId,
            "Updating encryption flag status to $updatedFlagStatus"
        )
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_FLAG_STATUS),
            updatedFlagStatus
        )
        cryptHandler.encryptionFlagStatus = updatedFlagStatus
    }

    /**
     * This method migrates the encryption level of the value under cachedGUIDsKey stored in the shared preference file
     * Only the value of the identifier(eg: johndoe@gmail.com) is encrypted/decrypted for this key throughout the sdk
     *
     * @param encrypt - Flag to indicate the task to be either encryption or decryption
     * @param config  - The [CleverTapInstanceConfig] object
     * @param context - The Android context
     * @param cryptHandler - The [CryptHandler] object
     * Returns the status of cgk migration
     */
    private fun migrateCachedGuidsKeyPref(
        encrypt: Boolean,
        config: CleverTapInstanceConfig,
        context: Context,
        cryptHandler: CryptHandler
    ): Int {
        config.logger.verbose(
            config.accountId,
            "Migrating encryption level for cachedGUIDsKey prefs"
        )
        val json =
            StorageHelper.getStringFromPrefs(context, config, CACHED_GUIDS_KEY, null)
        val cachedGuidJsonObj = CTJsonConverter.toJsonObject(json, config.logger, config.accountId)
        val newGuidJsonObj = JSONObject()
        var migrationStatus = ENCRYPTION_FLAG_CGK_SUCCESS
        try {
            val i = cachedGuidJsonObj.keys()
            while (i.hasNext()) {
                val nextJSONObjKey = i.next()
                val key = nextJSONObjKey.substringBefore("_")
                val identifier = nextJSONObjKey.substringAfter("_")
                var crypted: String? =
                    if (encrypt)
                        cryptHandler.encrypt(identifier, key)
                    else
                        cryptHandler.decrypt(identifier, KEY_ENCRYPTION_MIGRATION)
                if (crypted == null) {
                    config.logger.verbose(
                        config.accountId,
                        "Error migrating $identifier in Cached Guid Key Pref"
                    )
                    crypted = identifier
                    migrationStatus = ENCRYPTION_FLAG_FAIL
                }
                val cryptedKey = "${key}_$crypted"
                newGuidJsonObj.put(cryptedKey, cachedGuidJsonObj[nextJSONObjKey])
            }
            if (cachedGuidJsonObj.length() > 0) {
                val cachedGuid = newGuidJsonObj.toString()
                StorageHelper.putString(
                    context,
                    StorageHelper.storageKeyWithSuffix(config, CACHED_GUIDS_KEY),
                    cachedGuid
                )
                config.logger.verbose(
                    config.accountId,
                    "setCachedGUIDs after migration:[$cachedGuid]"
                )
            }
        } catch (t: Throwable) {
            config.logger.verbose(config.accountId, "Error migrating cached guids: $t")
            migrationStatus = ENCRYPTION_FLAG_FAIL
        }
        return migrationStatus
    }


    /**
     * This method migrates the encryption level of the value under cachedGUIDsKey stored in the shared preference file
     * Only the value of the identifier(eg: johndoe@gmail.com) is encrypted/decrypted for this key throughout the sdk
     *
     * @param encrypt - Flag to indicate the task to be either encryption or decryption
     * @param config  - The [CleverTapInstanceConfig] object
     * @param cryptHandler - The [CryptHandler] object
     * @param dbAdapter - The [dbAdapter] object
     * Returns the status of db migration
     */
    private fun migrateDBProfile(
        encrypt: Boolean,
        config: CleverTapInstanceConfig,
        cryptHandler: CryptHandler,
        dbAdapter: DBAdapter
    ): Int {
        config.logger.verbose(config.accountId, "Migrating encryption level for user profile in DB")
        var migrationStatus = ENCRYPTION_FLAG_DB_SUCCESS
        val profile =
            dbAdapter.fetchUserProfileById(config.accountId) ?: return ENCRYPTION_FLAG_DB_SUCCESS
        try {
            for (piiKey in piiDBKeys) {
                if (profile.has(piiKey)) {
                    val value = profile[piiKey]
                    if (value is String) {
                        var crypted = if (encrypt)
                            cryptHandler.encrypt(value, piiKey)
                        else
                            cryptHandler.decrypt(value, KEY_ENCRYPTION_MIGRATION)
                        if (crypted == null) {
                            config.logger.verbose(
                                config.accountId,
                                "Error migrating $piiKey entry in db profile"
                            )
                            crypted = value
                            migrationStatus = ENCRYPTION_FLAG_FAIL
                        }
                        profile.put(piiKey, crypted)
                    }
                }
            }
            if (dbAdapter.storeUserProfile(config.accountId, profile) == -1L)
                migrationStatus = ENCRYPTION_FLAG_FAIL
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "Error migrating local DB profile: $e")
            migrationStatus = ENCRYPTION_FLAG_FAIL
        }
        return migrationStatus
    }

    /**
     * This method migrates the encryption level of the value under cachedGUIDsKey stored in the shared preference file
     * Only the value of the identifier(eg: johndoe@gmail.com) is encrypted/decrypted for this key throughout the sdk
     *
     * @param context - Context object
     * @param config  - The [CleverTapInstanceConfig] object
     * @param failedFlag - Indicates which encryption has failed
     * @param cryptHandler - The [CryptHandler] object
     * Returns the status of db migration
     */
    @JvmStatic
    fun updateEncryptionFlagOnFailure(
        context: Context,
        config: CleverTapInstanceConfig,
        failedFlag: Int,
        cryptHandler: CryptHandler
    ) {

        // This operation sets the bit for the required encryption fail to 0
        val updatedEncryptionFlag =
            (failedFlag xor cryptHandler.encryptionFlagStatus) and cryptHandler.encryptionFlagStatus
        config.logger.verbose(
            config.accountId,
            "Updating encryption flag status after error in $failedFlag to $updatedEncryptionFlag"
        )
        StorageHelper.putInt(
            context, StorageHelper.storageKeyWithSuffix(
                config,
                KEY_ENCRYPTION_FLAG_STATUS
            ),
            updatedEncryptionFlag
        )
        cryptHandler.encryptionFlagStatus = updatedEncryptionFlag

    }
}