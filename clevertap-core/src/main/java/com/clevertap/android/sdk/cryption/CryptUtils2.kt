package com.clevertap.android.sdk.cryption

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants.CACHED_GUIDS_KEY
import com.clevertap.android.sdk.Constants.ENCRYPTION_FLAG_FAIL
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_FLAG_STATUS
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_LEVEL
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_MIGRATION
import com.clevertap.android.sdk.Constants.piiDBKeys
import com.clevertap.android.sdk.cryption.CryptHandler.EncryptionAlgorithm
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.utils.CTJsonConverter
import org.json.JSONObject

/**
 * configvalue => 0,1
 * storedEncryptionLevel => -1, 0, 1
 * api level device => >23, 23
 *
 *
 *
 *
 *
 * configvalue 1 Scenarios >>>>>>>>>
 * storedEncryptionLevel = -1
 * first app launch -> plain to aes_gcm
 * -> successful () : stored = 1, algo = AES_GCM, migration success = true
 * -> failure () : stored = 1, algo = AES_GCM, migration success = false
 *
 * storedEncryptionLevel = 0
 * app has plain text -> plain to aes_gcm (client flipped enc bit)
 * -> successful () : stored = 1, algo = AES_GCM, migration success = true
 * -> failure () : stored = 1, algo = AES_GCM, migration success = false
 *
 * storedEncryptionLevel = 1
 * two scenarios -> SDK update: AES -> AES_GCM |||| previous failure -> run full migration
 * -> successful () : stored = 1, algo = AES_GCM, migration success = true
 * -> failure () : stored = 1, algo = AES_GCM, migration success = false
 *
 *
 *
 * configvalue 0 Scenarios >>>>>>>>>>
 * storedEncryptionLevel = -1
 * First sdk launch -> no-op
 * -> successful () : stored = 0, algo = AES_GCM, migration success = true
 *
 * storedEncryptionLevel = 0
 * two cases => (migration success = true) -> noop ||||| (migration success = false) -> run full migration
 * -> successful () : stored = 0, algo = AES_GCM, migration success = true
 * -> failure () : stored = 0, algo = AES_GCM, migration success = false
 *
 * storedEncryptionLevel = 1
 * client wants to remove encryption ||||| 2 cases => 1. sdk has upgraded -> AES to PLAIN |||| 2. flipped bit to remove enc -> AESGCM to PLAIN
 * -> successful () : stored = 0, algo = AES_GCM, migration success = true
 * -> failure () : stored = 0, algo = AES_GCM, migration success = false
 *
 */
internal data class CryptUtils2(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val cryptHandler: CryptHandler,
    private val dbAdapter: DBAdapter
) {

    fun migrateEncryption() {

        // level mentioned in config, returns only 0 or 1
        val configEncryptionLevelAdjusted: Int = config.encryptionLevel
        val encryptData: Boolean = configEncryptionLevelAdjusted == 1

        // get (MyData) from prefs and check not null

        // state of current level in which data is encrypted
        val storedEncryptionLevel = StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_LEVEL),
            -1
        )

        val hasMigrationPassed = StorageHelper.getBoolean(
            context,
            StorageHelper.storageKeyWithSuffix(config, "migration-passed"),
            false
        )

        var mig: MigResult = MigResult(false)

        // First time SDK init scenario
        if (storedEncryptionLevel == -1) {
            mig = if (encryptData) {
                handleMigration(MigrationType.CONVERT_TO_ENCRYPTED_DATA)
            } else {
                MigResult(true)
            }
        }

        // Previous data is not encrypted
        if (storedEncryptionLevel == 0) {
            mig = if (encryptData) {
                handleMigration(MigrationType.CONVERT_TO_ENCRYPTED_DATA)
            } else {
                MigResult(true)
            }
        }

        if (storedEncryptionLevel == 1) {
            mig = if (encryptData) {
                handleMigration(MigrationType.CHANGE_ENCRYPTION_TYPE)
            } else {
                handleMigration(MigrationType.CONVERT_TO_PLAIN_TEXT)
            }
        }

        StorageHelper.putBoolean(
            context,
            StorageHelper.storageKeyWithSuffix(config, "migration-passed"),
            mig.hasPassed
        )

        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_LEVEL),
            configEncryptionLevelAdjusted
        )
    }

    private fun handleMigration(type: MigrationType) : MigResult {

        // 0, 3 always
        val encryptionFlagStatus = StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_FLAG_STATUS),
            ENCRYPTION_FLAG_FAIL
        )

        val migResult: MigResult
        when (type) {
            MigrationType.CONVERT_TO_ENCRYPTED_DATA -> {

                // Migrate shared prefs
                val x = migrateCachedGuidsKeyPref(true)

                // Migrate databases
                val y = migrateDBProfile(true)

                migResult = MigResult(x || y)
                // Migrate In-apps data, note: it is always saved encrypted
            }
            MigrationType.CONVERT_TO_PLAIN_TEXT -> {
                // Migrate shared prefs
                val x = migrateCachedGuidsKeyPref(false)

                // Migrate databases
                val y = migrateDBProfile(false)

                migResult = MigResult(x || y)

                // Migrate In-apps data, note: it is always saved encrypted
            }
            MigrationType.CHANGE_ENCRYPTION_TYPE -> {
                // Migrate shared prefs
                val x = migrateCachedGuidsKeyPref(true)

                // Migrate databases
                val b = migrateDBProfile(true)

                migResult = MigResult(x || b)

                // Migrate In-apps data, note: it is always saved encrypted
            }
        }

        return migResult
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
        var migrationStatus = true
        try {
            val i = cachedGuidJsonObj.keys()
            while (i.hasNext()) {
                val nextJSONObjKey = i.next()
                val key = nextJSONObjKey.substringBefore("_")
                val identifier = nextJSONObjKey.substringAfter("_")

                val algorithm = getAlgorithmToPerformFromDataType(identifier)

                var crypted: String? =
                    if (encrypt) {
                        cryptHandler.encrypt(identifier, key, algorithm)
                    } else {
                        cryptHandler.decrypt(identifier, KEY_ENCRYPTION_MIGRATION, algorithm)
                    }
                if (crypted == null) {
                    config.logger.verbose(
                        config.accountId,
                        "Error migrating $identifier in Cached Guid Key Pref"
                    )
                    crypted = identifier
                    migrationStatus = false
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
            migrationStatus = false
        }
        return migrationStatus
    }

    /**
     * This method migrates the encryption level of the user profiles stored in the local db
     * Only pii data such as name, phone, email and identity are encrypted from the user profile, remaining are kept as is
     *
     * @param encrypt - Flag to indicate the task to be either encryption or decryption
     * @param config  - The [CleverTapInstanceConfig] object
     * @param cryptHandler - The [CryptHandler] object
     * @param dbAdapter - The [dbAdapter] object
     * Returns the status of db migration
     */
    private fun migrateDBProfile(
        encrypt: Boolean
    ): Boolean {
        config.logger.verbose(
            config.accountId,
            "Migrating encryption level for user profiles in DB"
        )
        val profiles = dbAdapter.fetchUserProfilesByAccountId(config.accountId)

        var migrationStatus = true
        for (profileIterator in profiles) {
            val profile = profileIterator.value
            try {
                for (piiKey in piiDBKeys) {
                    if (profile.has(piiKey)) {
                        val value = profile[piiKey]
                        if (value is String) {

                            val algorithm = getAlgorithmToPerformFromDataType(value)

                            var crypted = if (encrypt) {
                                cryptHandler.encrypt(value, piiKey, algorithm)
                            } else {
                                cryptHandler.decrypt(value, KEY_ENCRYPTION_MIGRATION, algorithm)
                            }
                            if (crypted == null) {
                                config.logger.verbose(
                                    config.accountId,
                                    "Error migrating $piiKey entry in db profile"
                                )
                                crypted = value
                                migrationStatus = false
                            }
                            profile.put(piiKey, crypted)
                        }
                    }
                }
                val op = dbAdapter.storeUserProfile(
                    id = config.accountId,
                    deviceId = profileIterator.key,
                    obj = profile
                )
                if (op <= -1L) {
                    migrationStatus = false
                }
            } catch (e: Exception) {
                config.logger.verbose(
                    config.accountId,
                    "Error migrating local DB profile for $profileIterator.key: $e"
                )
                migrationStatus = false
            }
        }
        return migrationStatus
    }

    private fun getAlgorithmToPerformFromDataType(value: String): EncryptionAlgorithm {
        val type = if (CryptHandler.isTextAESEncrypted(value)) {
            10 // aes
        } else if (CryptHandler.isTextAESGCMEncrypted(value)) {
            20 // aesgcm
        } else {
            30 // plain
        }

        //return one or many steps
        return EncryptionAlgorithm.AES_GCM // todo fixme basis type,
    }
}

enum class MigrationType {
    CONVERT_TO_ENCRYPTED_DATA,
    CONVERT_TO_PLAIN_TEXT,
    CHANGE_ENCRYPTION_TYPE
}

data class EncryptionStatus(
    val prefs: Boolean,
    val db: Boolean,
    val inApps: Boolean
)

data class MigResult(
    val hasPassed: Boolean
)