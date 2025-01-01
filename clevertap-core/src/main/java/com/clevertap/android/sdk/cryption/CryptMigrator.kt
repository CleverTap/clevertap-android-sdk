package com.clevertap.android.sdk.cryption

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants.CACHED_GUIDS_KEY
import com.clevertap.android.sdk.Constants.INAPP_KEY
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_LEVEL
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_MIGRATION
import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_CS
import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_SS
import com.clevertap.android.sdk.Constants.piiDBKeys
import com.clevertap.android.sdk.cryption.CryptHandler.EncryptionAlgorithm
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.sdk.utils.getStringOrNull
import org.json.JSONObject
import java.io.File

internal data class CryptMigrator(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val cryptHandler: CryptHandler,
    private val dbAdapter: DBAdapter
) {

    companion object {
        private const val MIGRATION_FAILURE_COUNT_KEY = "encryptionMigrationFailureCount"
        private const val UNKNOWN_LEVEL = -1
        private const val MIGRATION_NOT_NEEDED = 0
        private const val MIGRATION_NEEDED = 1
        private const val MIGRATION_FIRST_UPGRADE = -1
    }

    fun migrateEncryption() {
        val configEncryptionLevel = config.encryptionLevel
        val storedEncryptionLevel = StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, KEY_ENCRYPTION_LEVEL),
            UNKNOWN_LEVEL
        )

        val storedFailureCount = StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, MIGRATION_FAILURE_COUNT_KEY),
            MIGRATION_FIRST_UPGRADE
        )

        val migrationFailureCount = when {
            // Fresh install
            storedEncryptionLevel == UNKNOWN_LEVEL && configEncryptionLevel == EncryptionLevel.NONE.intValue() -> {
                cryptHandler.updateMigrationFailureCount(context, false)
                MIGRATION_NOT_NEEDED
            }
            // Encryption level changed and upgrade to v2 already complete
            storedEncryptionLevel != configEncryptionLevel && storedFailureCount != -1 -> MIGRATION_NEEDED
            else -> storedFailureCount
        }

        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, KEY_ENCRYPTION_LEVEL),
            configEncryptionLevel
        )

        if (migrationFailureCount == MIGRATION_NOT_NEEDED) {
            config.logger.verbose(
                config.accountId,
                "Migration not required: config-encryption-level $configEncryptionLevel, " +
                        "stored-encryption-level $storedEncryptionLevel"
            )
            return
        }

        config.logger.verbose(
            config.accountId,
            "Starting migration from encryption level $storedEncryptionLevel to $configEncryptionLevel " +
                    "with migrationFailureCount $migrationFailureCount"
        )
        val migrationSuccess = handleAllMigrations(
            configEncryptionLevel == EncryptionLevel.MEDIUM.intValue(),
            migrationFailureCount == -1
        )
        cryptHandler.updateMigrationFailureCount(context, migrationSuccess)
    }

    private fun handleAllMigrations(encrypt: Boolean, firstUpgrade: Boolean): Boolean {
        return migrateCachedGuidsKeyPref(encrypt, firstUpgrade) &&
                migrateDBProfile(encrypt) &&
                migrateInAppData()
    }

    /**
     * This method migrates the encryption level of the value under cachedGUIDsKey stored in the shared preference file
     *
     * @param encrypt - Flag to indicate the task to be either encryption or decryption
     * @param firstUpgrade - Flag to indicate whether this is the first upgrade to v2
     * Returns true if migration was successful and false otherwise
     */
    private fun migrateCachedGuidsKeyPref(
        encrypt: Boolean,
        firstUpgrade: Boolean,
    ): Boolean {
        config.logger.verbose(
            config.accountId,
            "Migrating encryption level for cachedGUIDsKey prefs"
        )

        var cgkString = StorageHelper.getStringFromPrefs(context, config, CACHED_GUIDS_KEY, null)

        if (firstUpgrade) {
            cgkString =
                migrateFormatForCachedGuidsKeyPref(cgkString)
        }

        var migrationSuccessful = true

        val migrationResult = performMigrationStep(encrypt, cgkString)
        migrationSuccessful = migrationSuccessful && migrationResult.migrationSuccessful
        StorageHelper.putString(
            context,
            StorageHelper.storageKeyWithSuffix(config.accountId, CACHED_GUIDS_KEY),
            migrationResult.data
        )
        config.logger.verbose(
            config.accountId,
            "Cached GUIDs migrated successfully: [${cgkString}]"
        )
        return migrationSuccessful
    }


    /**
     * This method migrates the format of cachedGuidsKey
     * The older format when encryption level is 1 was {Email_[]:__g... , Name_[]:__i...} -> Only the identifier was encrypted
     * The migrated format will encrypt the entire JSONObject and not just the identifier
     *
     * @param cgkString - The string retrieved from the prefs that needs to be upgraded
     * Returns true if migration was successful and false otherwise
     */
    private fun migrateFormatForCachedGuidsKeyPref(
        cgkString: String,
    ): String {
        val cachedGuidJsonObj = CTJsonConverter.toJsonObject(cgkString, config.logger, config.accountId)
        val migratedGuidJsonObj = JSONObject()
        try {
            val keysIterator = cachedGuidJsonObj.keys()
            while (keysIterator.hasNext()) {
                val nextKey = keysIterator.next()
                val (key, identifier) = nextKey.split("_", limit = 2)
                val migrationResult = performMigrationStep(false, identifier)
                if (migrationResult.migrationSuccessful) {
                    val cryptedKey = "${key}_${migrationResult.data}"
                    migratedGuidJsonObj.put(cryptedKey, cachedGuidJsonObj[nextKey])
                }
            }
        } catch (t: Throwable) {
            config.logger.verbose(
                config.accountId,
                "Error migrating format for cached GUIDs: Clearing and starting fresh $t"
            )
        }
        return migratedGuidJsonObj.toString()
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
        for ((deviceID, profile) in profiles) {
            try {
                piiDBKeys.forEach { piiKey ->
                    profile.getStringOrNull(piiKey)?.let { value ->
                        val migrationResult =
                            performMigrationStep(encrypt, value)
                        migrationSuccessful =
                            migrationSuccessful && migrationResult.migrationSuccessful
                        profile.put(piiKey, migrationResult.data)
                    }
                }
                if (dbAdapter.storeUserProfile(config.accountId, deviceID, profile) <= -1L) {
                    migrationSuccessful = false
                }
            } catch (e: Exception) {
                config.logger.verbose(config.accountId, "Error migrating profile $deviceID: $e")
                migrationSuccessful = false
            }
        }

        return migrationSuccessful
    }


    /**
     * This method migrates the encryption level of the inapp data stored in the shared preferences file.
     * Migration(if needed) is always performed to AES_GCM
     *
     * Returns true if migration was successful and false otherwise
     */
    private fun migrateInAppData(): Boolean {
        config.logger.verbose(config.accountId, "Migrating encryption for InAppData")
        var migrationSuccessful = true

        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")

        // Fetch all SharedPreferences files starting with "inApp" and ending with the accountId
        val prefsFiles = sharedPrefsDir.listFiles { _, name ->
            // Check StoreProvider.constructStorePreferenceName() to check how the name is constructed
            name.startsWith(INAPP_KEY) && name.endsWith("${config.accountId}.xml")
        }

        prefsFiles?.forEach { file ->
            val prefName = file.nameWithoutExtension
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val keysToProcess = listOf(PREFS_INAPP_KEY_CS, PREFS_INAPP_KEY_SS)

            keysToProcess.forEach { key ->
                prefs.getString(key, null)?.let { data ->
                    val migrationResult = performMigrationStep(true, data)
                    migrationSuccessful = migrationSuccessful && migrationResult.migrationSuccessful
                    prefs.edit().putString(key, migrationResult.data).apply()
                }
            }
        }

        return migrationSuccessful
    }

    private fun performMigrationStep(encrypt: Boolean, data: String): MigrationResult {
        val currentState = getCurrentEncryptionState(data)
        val finalState = getFinalEncryptionState(encrypt)

        if (currentState == finalState) {
            // No migration needed if current state matches the final state
            return MigrationResult(data, true)
        }

        return try {
            return currentState.transitionTo(finalState, data, cryptHandler)
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "Migration step failed for data: $e")
            MigrationResult(data = data, migrationSuccessful = false)
        }
    }

    private fun getFinalEncryptionState(encrypt: Boolean): EncryptionState {
        return if (encrypt) {
            EncryptionState.ENCRYPTED_AES_GCM
        } else {
            EncryptionState.PLAIN_TEXT
        }
    }

    private fun getCurrentEncryptionState(data: String): EncryptionState {
        return when {
            CryptHandler.isTextAESEncrypted(data) -> EncryptionState.ENCRYPTED_AES
            CryptHandler.isTextAESGCMEncrypted(data) -> EncryptionState.ENCRYPTED_AES_GCM
            else -> EncryptionState.PLAIN_TEXT
        }
    }

    data class MigrationResult(val data: String, val migrationSuccessful: Boolean)

    /**
     * Enum representing encryption states and their transition logic.
     */
    enum class EncryptionState {
        ENCRYPTED_AES {
            override fun transitionTo(
                targetState: EncryptionState,
                data: String,
                cryptHandler: CryptHandler
            ): MigrationResult {
                val decrypted = cryptHandler.decrypt(data, KEY_ENCRYPTION_MIGRATION, EncryptionAlgorithm.AES)
                return when (targetState) {
                    ENCRYPTED_AES_GCM -> {
                        val encrypted = decrypted?.let {
                            cryptHandler.encrypt(
                                it,
                                KEY_ENCRYPTION_MIGRATION,
                                EncryptionAlgorithm.AES_GCM
                            )
                        }
                        MigrationResult(encrypted ?: decrypted ?: data, encrypted != null)
                    }

                    PLAIN_TEXT -> MigrationResult(decrypted ?: data, decrypted != null)
                    else -> throw IllegalArgumentException("Invalid transition from ENCRYPTED_AES to $targetState")
                }
            }
        },
        ENCRYPTED_AES_GCM {
            override fun transitionTo(
                targetState: EncryptionState,
                data: String,
                cryptHandler: CryptHandler
            ): MigrationResult {
                val decrypted = cryptHandler.decrypt(
                    data,
                    KEY_ENCRYPTION_MIGRATION,
                    EncryptionAlgorithm.AES_GCM
                )
                return when (targetState) {
                    PLAIN_TEXT -> MigrationResult(decrypted ?: data, decrypted != null)
                    else -> throw IllegalArgumentException("Invalid transition from ENCRYPTED_AES_GCM to $targetState")
                }
            }
        },
        PLAIN_TEXT {
            override fun transitionTo(
                targetState: EncryptionState,
                data: String,
                cryptHandler: CryptHandler
            ): MigrationResult {
                return when (targetState) {
                    ENCRYPTED_AES_GCM -> {
                        val encrypted = cryptHandler.encrypt(
                            data,
                            KEY_ENCRYPTION_MIGRATION,
                            EncryptionAlgorithm.AES_GCM
                        )
                        MigrationResult(encrypted ?: data, encrypted != null)
                    }
                    else -> throw IllegalArgumentException("Invalid transition from PLAIN_TEXT to $targetState")
                }
            }
        };

        abstract fun transitionTo(
            targetState: EncryptionState,
            data: String,
            cryptHandler: CryptHandler
        ): MigrationResult
    }
}
