package com.clevertap.android.sdk.cryption

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants.CACHED_GUIDS_KEY
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_LEVEL
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_MIGRATION
import com.clevertap.android.sdk.Constants.piiDBKeys
import com.clevertap.android.sdk.cryption.CryptHandler.EncryptionAlgorithm
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.utils.CTJsonConverter
import org.json.JSONObject
import java.io.File

internal data class CryptMigrator(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val cryptHandler: CryptHandler,
    private val dbAdapter: DBAdapter
) {

    /**
     * This method migrates the encryption level of the stored data for the current account ID
     *
     */
    fun migrateEncryption() {
        val migrationFailureCount: Int
        val configEncryptionLevel = config.encryptionLevel
        val storedEncryptionLevel = StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_LEVEL),
            -1
        )

        // Nothing to migrate if a new app install and configEncryption level is 0, hence return
        // If encryption level is updated (0 to 1 or 1 to 0) then set status to all migrations failed (0)
        migrationFailureCount = if (storedEncryptionLevel == -1 && configEncryptionLevel == 0) {
            cryptHandler.updateMigrationFailureCount(context,false)
            0
        }
        else if (storedEncryptionLevel != configEncryptionLevel) {
            1
        } else {
            StorageHelper.getInt(
                context,
                StorageHelper.storageKeyWithSuffix(config, "encryptionMigrationFailureCount"),
                1
            )
        }
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_LEVEL),
            configEncryptionLevel
        )

        if (migrationFailureCount == 0) {
            config.logger.verbose(
                config.accountId,
                "Migration is not needed for config-encryption-level $configEncryptionLevel and stored-encryption-level $storedEncryptionLevel"
            )
            return
        }

        config.logger.verbose(
            config.accountId,
            "Migrating encryption level from $storedEncryptionLevel to $configEncryptionLevel"
        )

        // If configEncryptionLevel is one then encrypt otherwise decrypt
        val migrationSuccess = handleMigration(configEncryptionLevel == 1)
        cryptHandler.updateMigrationFailureCount(context, migrationSuccess)
    }

    private fun handleMigration(encrypt: Boolean) : Boolean {
        return migrateCachedGuidsKeyPref(encrypt) &&
        migrateDBProfile(encrypt) &&
        migrateInAppData(encrypt)
    }

    /**
     * This method migrates the encryption level of the value under cachedGUIDsKey stored in the shared preference file
     * Only the value of the identifier(eg: johndoe@gmail.com) is encrypted/decrypted for this key throughout the sdk
     *
     * @param encrypt - Flag to indicate the task to be either encryption or decryption
     * Returns true if migration was successful and false otherwise
     */
    private fun migrateCachedGuidsKeyPref(
        encrypt: Boolean
    ): Boolean {
        config.logger.verbose(
            config.accountId,
            "Migrating encryption level for cachedGUIDsKey prefs"
        )
        val json =
            StorageHelper.getStringFromPrefs(context, config, CACHED_GUIDS_KEY, null)
        val cachedGuidJsonObj = CTJsonConverter.toJsonObject(json, config.logger, config.accountId)
        val newGuidJsonObj = JSONObject()
        var migrationSuccessful = true
        try {
            val i = cachedGuidJsonObj.keys()
            while (i.hasNext()) {
                val nextJSONObjKey = i.next()
                val key = nextJSONObjKey.substringBefore("_")
                val identifier = nextJSONObjKey.substringAfter("_")
                val migrationResult = performMigrationStep(encrypt, identifier)
                migrationSuccessful = migrationSuccessful && migrationResult.migrationSuccessful
                val cryptedKey = "${key}_${migrationResult.data}"
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
            migrationSuccessful = false
        }
        return migrationSuccessful
    }

    /**
     * This method migrates the encryption level of the user profiles stored in the local db
     * Only pii data such as name, phone, email and identity are encrypted from the user profile, remaining are kept as is
     *
     * @param encrypt - Flag to indicate the task to be either encryption or decryption
     * Returns true if migration was successful and false otherwise
     */
    private fun migrateDBProfile(
        encrypt: Boolean
    ): Boolean {
        config.logger.verbose(
            config.accountId,
            "Migrating encryption level for user profiles in DB"
        )
        val profiles = dbAdapter.fetchUserProfilesByAccountId(config.accountId)

        var migrationSuccessful = true
        for (profileIterator in profiles) {
            val profile = profileIterator.value
            try {
                for (piiKey in piiDBKeys) {
                    if (profile.has(piiKey)) {
                        val value = profile[piiKey]
                        if (value is String) {
                            val migrationResult = performMigrationStep(encrypt, value)
                            migrationSuccessful = migrationSuccessful && migrationResult.migrationSuccessful
                            profile.put(piiKey, migrationResult.data)
                        }
                    }
                }
                if (dbAdapter.storeUserProfile(
                        config.accountId,
                        profileIterator.key,
                        profile
                    ) <= -1L
                )
                    migrationSuccessful = false
            } catch (e: Exception) {
                config.logger.verbose(
                    config.accountId,
                    "Error migrating local DB profile for $profileIterator.key: $e"
                )
                migrationSuccessful = false
            }
        }
        return migrationSuccessful
    }

    private fun migrateInAppData(
        encrypt: Boolean
    ): Boolean {

        config.logger.verbose(
            config.accountId,
            "Migrating encryption for InAppData"
        )
        var migrationStatus = true
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")

        // Fetch all SharedPreferences files starting with "inApp" and ending with the accountId
        val prefsFiles = sharedPrefsDir.listFiles { _, name ->
            name.startsWith("inApp") && name.endsWith(config.accountId + ".xml")
        }

        prefsFiles?.forEach { file ->
            val prefName = file.nameWithoutExtension
            val inAppSharedPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

            val keysToProcess = listOf("inapp_notifs_cs", "inapp_notifs_ss")

            // Process each key for encryption/decryption
            keysToProcess.forEach { key ->
                val data = inAppSharedPrefs.getString(key, null)

                // If data exists, attempt to encrypt or decrypt
                if (!data.isNullOrEmpty()) {
                    val migrationResult = performMigrationStep(encrypt, data)
                    migrationStatus = migrationStatus && migrationResult.migrationSuccessful

                    // Save the processed data back to SharedPreferences
                    inAppSharedPrefs.edit().putString(key, migrationResult.data).apply()
                }
            }
        }
        return migrationStatus
    }

    private fun performMigrationStep(finalState: Boolean, data: String): MigrationResult {
        var crypted: String? = data
        if(CryptHandler.isTextAESEncrypted(data)) {
            crypted = cryptHandler.decrypt(data, KEY_ENCRYPTION_MIGRATION, EncryptionAlgorithm.AES)
            if(finalState && crypted != null) {
                crypted = cryptHandler.encrypt(crypted, KEY_ENCRYPTION_MIGRATION, EncryptionAlgorithm.AES_GCM)
            }
        } else if (CryptHandler.isTextAESGCMEncrypted(data)) {
            if(!finalState) {
                crypted = cryptHandler.decrypt(data, KEY_ENCRYPTION_MIGRATION, EncryptionAlgorithm.AES_GCM)
            }
        } else {
            if(finalState) {
                crypted = cryptHandler.encrypt(data, KEY_ENCRYPTION_MIGRATION, EncryptionAlgorithm.AES_GCM)
            }
        }
        val migrationResult = if(crypted ==null)
            MigrationResult(data, false)
        else
            MigrationResult(crypted, true)

        return migrationResult
    }

    data class MigrationResult(val data: String, val migrationSuccessful: Boolean)
}
