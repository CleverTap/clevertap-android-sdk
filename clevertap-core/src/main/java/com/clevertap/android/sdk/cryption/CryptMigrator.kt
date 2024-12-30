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
     * Migrates the encryption level of stored data for the current account ID.
     */
    fun migrateEncryption() {
        val configEncryptionLevel = config.encryptionLevel
        val storedEncryptionLevel = StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_LEVEL),
            -1
        )

        // Determine migration failure count:
        // 1. No migration needed for new installs with encryption level 0.
        // 2. Increment failure count if encryption levels differ.
        // 3. Otherwise, retain existing failure count.
        val migrationFailureCount = when {
            storedEncryptionLevel == -1 && configEncryptionLevel == 0 -> {
                cryptHandler.updateMigrationFailureCount(context, false)
                0
            }
            storedEncryptionLevel != configEncryptionLevel -> 1
            else -> StorageHelper.getInt(
                context,
                StorageHelper.storageKeyWithSuffix(config, "encryptionMigrationFailureCount"),
                1
            )
        }

        // Update stored encryption level to the current config value.
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_LEVEL),
            configEncryptionLevel
        )

        // If no migration is required, log and exit.
        if (migrationFailureCount == 0) {
            config.logger.verbose(
                config.accountId,
                "Migration not required: config-encryption-level $configEncryptionLevel, " +
                        "stored-encryption-level $storedEncryptionLevel"
            )
            return
        }

        // Log migration start and handle encryption/decryption as needed.
        config.logger.verbose(
            config.accountId,
            "Starting migration from encryption level $storedEncryptionLevel to $configEncryptionLevel"
        )
        val migrationSuccess = handleAllMigrations(configEncryptionLevel == 1)

        // Update migration status based on the operation outcome.
        cryptHandler.updateMigrationFailureCount(context, migrationSuccess)
    }


    private fun handleAllMigrations(encrypt: Boolean) : Boolean {
        return migrateCachedGuidsKeyPref(encrypt) &&
        migrateDBProfile(encrypt) &&
        migrateInAppData()
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
                val migrationResult = performMigrationStep(getFinalEncryptionState(encrypt), identifier)
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
                            val migrationResult = performMigrationStep(getFinalEncryptionState(encrypt), value)
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


    /**
     * This method migrates the encryption level of the inapp data stored in prefs. Migration(if needed) is always performed to AES_GCM
     *
     * Returns true if migration was successful and false otherwise
     */
    private fun migrateInAppData(): Boolean {

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
                    val migrationResult = performMigrationStep(getFinalEncryptionState(true), data)
                    migrationStatus = migrationStatus && migrationResult.migrationSuccessful

                    // Save the processed data back to SharedPreferences
                    inAppSharedPrefs.edit().putString(key, migrationResult.data).apply()
                }
            }
        }
        return migrationStatus
    }

    private fun performMigrationStep(finalState: EncryptionState, data: String): MigrationResult {
        var processedData: String? = data
        when {
            CryptHandler.isTextAESEncrypted(data) -> {
                processedData = cryptHandler.decrypt(data, KEY_ENCRYPTION_MIGRATION, EncryptionAlgorithm.AES)
                if (finalState == EncryptionState.ENCRYPTED_AES_GCM && processedData != null) {
                    processedData = cryptHandler.encrypt(processedData, KEY_ENCRYPTION_MIGRATION, EncryptionAlgorithm.AES_GCM)
                }
            }
            CryptHandler.isTextAESGCMEncrypted(data) -> {
                if (finalState == EncryptionState.PLAIN_TEXT) {
                    processedData = cryptHandler.decrypt(data, KEY_ENCRYPTION_MIGRATION, EncryptionAlgorithm.AES_GCM)
                }
            }
            finalState == EncryptionState.ENCRYPTED_AES_GCM-> {
                // Data here is plain text
                processedData = cryptHandler.encrypt(data, KEY_ENCRYPTION_MIGRATION, EncryptionAlgorithm.AES_GCM)
            }
        }

        return MigrationResult(
            data = processedData ?: data,
            migrationSuccessful = processedData != null
        )
    }

    private fun getFinalEncryptionState(encrypt: Boolean): EncryptionState {
        return when(encrypt) {
            true -> EncryptionState.ENCRYPTED_AES_GCM
            false -> EncryptionState.PLAIN_TEXT
        }
    }

    data class MigrationResult(val data: String, val migrationSuccessful: Boolean)
    enum class EncryptionState {
        ENCRYPTED_AES,
        PLAIN_TEXT,
        ENCRYPTED_AES_GCM;
    }
}
