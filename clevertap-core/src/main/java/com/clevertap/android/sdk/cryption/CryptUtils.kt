package com.clevertap.android.sdk.cryption

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants.CACHED_GUIDS_KEY
import com.clevertap.android.sdk.Constants.ENCRYPTION_FLAG_CGK_SUCCESS
import com.clevertap.android.sdk.Constants.ENCRYPTION_FLAG_DB_SUCCESS
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_FLAG_STATUS
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_LEVEL
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_MIGRATION
import com.clevertap.android.sdk.Constants.piiDBKeys
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.cryption.CryptHandler.EncryptionDataState
import com.clevertap.android.sdk.cryption.CryptHandler.EncryptionDataState.*
import com.clevertap.android.sdk.cryption.CryptUtils.MigrationStep
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.utils.CTJsonConverter
import org.json.JSONObject
import java.io.File

/**
 * This class is a utils class, mainly used to handle migration when encryption fails or encryption level is changed
 */
internal object CryptUtils {
    // todo - Add syntactic sugar,
    // todo - enums for states,
    // todo - constants where needed,
    // todo - better data structures and more readable
    // todo - fix logs
    // todo - better names for variables

    private fun isWorstStateNeeded(storedEncryptionLevel: Int, configEncryptionLevel: Int, encryptionAlgorithm: Int): Boolean {
        // todo - add more logic for worst state if required
        return (storedEncryptionLevel != configEncryptionLevel) || encryptionAlgorithm == 0
    }

    private fun getRequiredEncryptedDataState(configEncryptionLevel: Int): EncryptionDataState {
        return if (configEncryptionLevel == 0)
            ENCRYPTED_AES
        else
            ENCRYPTED_AES_GCM
    }

    private fun getCurrentEncryptedDataState(
        storedEncryptionLevel: Int,
        encryptionFlagStatus: Int,
        storedCurrentState: String?,
        configEncryptionLevel: Int,
        encryptionAlgorithm: Int,
        requiredState: EncryptionDataState
    ): MutableMap<String, EncryptionDataState> {
        return when {
            storedCurrentState == null && encryptionFlagStatus == -1 -> {
                // User has cleared the data / fresh install of app
                mutableMapOf(
                    "currentStateDb" to requiredState,
                    "currentStateCgk" to requiredState,
                    "currentStateInApp" to requiredState
                )
            }

            storedCurrentState == null -> {
                // First migration attempt from old to new
                // todo - verify below logic
                val currentStateCgk =
                    if ((encryptionFlagStatus and ENCRYPTION_FLAG_CGK_SUCCESS) != 0 && storedEncryptionLevel == 0) ENCRYPTED_AES else PLAIN_TEXT
                val currentStateDb =
                    if ((encryptionFlagStatus and ENCRYPTION_FLAG_DB_SUCCESS) != 0 && storedEncryptionLevel == 0) ENCRYPTED_AES else PLAIN_TEXT

                mutableMapOf(
                    "currentStateDb" to currentStateDb,
                    "currentStateCgk" to currentStateCgk,
                    "currentStateInApp" to ENCRYPTED_AES
                )
            }

            isWorstStateNeeded(
                storedEncryptionLevel,
                configEncryptionLevel,
                encryptionAlgorithm
            ) -> {
                when {
                    encryptionAlgorithm == 0 -> mutableMapOf(
                        "currentStateDb" to PLAIN_TEXT,
                        "currentStateCgk" to PLAIN_TEXT,
                        "currentStateInApp" to PLAIN_TEXT
                    )

                    configEncryptionLevel == 0 -> mutableMapOf(
                        "currentStateDb" to ENCRYPTED_AES_GCM,
                        "currentStateCgk" to ENCRYPTED_AES_GCM,
                        "currentStateInApp" to ENCRYPTED_AES_GCM
                    )

                    else -> mutableMapOf(
                        "currentStateDb" to ENCRYPTED_AES,
                        "currentStateCgk" to ENCRYPTED_AES,
                        "currentStateInApp" to ENCRYPTED_AES
                    )
                }
            }

            else -> {
                storedCurrentState.split(",").associate {
                    val (key, value) = it.split(":")
                    key to EncryptionDataState.fromState(value.toInt())
                }.toMutableMap()
            }
        }
    }



    @JvmStatic
    fun migrateEncryption(
        context: Context,
        config: CleverTapInstanceConfig,
        cryptHandler: CryptHandler,
        dbAdapter: DBAdapter
    ) {
        val configEncryptionLevel = config.encryptionLevel
        val storedEncryptionLevel = StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_LEVEL),
            0
        )

        val storedCurrentState = StorageHelper.getString(
            context,
            StorageHelper.storageKeyWithSuffix(config, "currentEncryptionState"),
            null
        )

        val encryptionAlgorithm = StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, "encryptionAlgorithm"),
            0
        )

        val encryptionFlagStatus = StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(config, KEY_ENCRYPTION_FLAG_STATUS),
            -1
        )
        val requiredState = getRequiredEncryptedDataState(configEncryptionLevel)
        val currentStateMap = getCurrentEncryptedDataState(
            storedEncryptionLevel,
            encryptionFlagStatus,
            storedCurrentState,
            configEncryptionLevel,
            encryptionAlgorithm,
            requiredState
        )

        performMigrationSteps(
            currentStateMap,
            requiredState,
            config,
            context,
            cryptHandler,
            dbAdapter
        )
    }


    fun interface MigrationStep {
        fun execute(
            encrypt: Boolean,
            algorithm: Boolean,
            config: CleverTapInstanceConfig,
            context: Context,
            cryptHandler: CryptHandler,
            dbAdapter: DBAdapter
        ): Boolean
    }

    data class MigrationStepDetails(
        val encrypt: Boolean,
        val algorithm: Boolean,
        val step: Int
    )

    private fun performMigrationSteps(
        currentStateMap: MutableMap<String, EncryptionDataState>,
        requiredState: EncryptionDataState,
        config: CleverTapInstanceConfig,
        context: Context,
        cryptHandler: CryptHandler,
        dbAdapter: DBAdapter
    ) {
        // Define migration handlers for each key
        val migrationHandlers = createMigrationHandlers()

        var completeMigrationSuccess = true
        // Iterate through the current state and perform migration steps
        currentStateMap.forEach { (key, currentState) ->
            val requiredSteps = currentState.state xor requiredState.state
            var updatedState = currentState.state
            var stepSuccess = true

            // Find the appropriate handler and perform migration steps
            migrationHandlers[key]?.let { handler ->
                val steps = generateMigrationSteps(currentState, requiredState)

                // Execute each step based on the migration conditions
                for (stepDetails in steps) {
                    if (requiredSteps and stepDetails.step != 0 && stepSuccess) {
                        stepSuccess = executeMigrationStep(
                            handler, stepDetails, config, context, cryptHandler, dbAdapter
                        )
                        if (stepSuccess) {
                            updatedState = updatedState xor stepDetails.step
                        }
                    }
                }
            }

            // Track migration success/failure
            if (updatedState != requiredState.state) completeMigrationSuccess = false
            currentStateMap[key] = EncryptionDataState.fromState(updatedState)
        }

        // Store updated states and handle success/failure
        storeUpdatedState(currentStateMap, config, context)

        cryptHandler.currentEncryptionState = currentStateMap

        if (completeMigrationSuccess) {
            // Update encryption level and algorithm in shared preferences
           storeEncryptionSettings(config, context)
        }
    }

    // Creates the migration handlers
    private fun createMigrationHandlers(): Map<String, MigrationStep> {
        return mapOf(
            "currentStateCgk" to MigrationStep { encrypt, algorithm, config, context, cryptHandler, _ ->
                migrateCachedGuidsKeyPref(encrypt, algorithm, config, context, cryptHandler)
            },
            "currentStateDb" to MigrationStep { encrypt, algorithm, config, _, cryptHandler, dbAdapter ->
                migrateDBProfile(encrypt, algorithm, config, cryptHandler, dbAdapter)
            },
            "currentStateInApp" to MigrationStep { encrypt, algorithm, config, context, cryptHandler, _ ->
                migrateInAppData(encrypt, algorithm, config, context, cryptHandler)
            }
        )
    }

    private fun generateMigrationSteps(currentState: EncryptionDataState, requiredState: EncryptionDataState): List<MigrationStepDetails> {
        return listOf(
            MigrationStepDetails(currentState > requiredState, false, ENCRYPTED_AES.state),
            MigrationStepDetails(currentState < requiredState, true, ENCRYPTED_AES_GCM.state)
        )
    }

    // Executes the migration step and returns the success status
    private fun executeMigrationStep(
        handler: MigrationStep,
        stepDetails: MigrationStepDetails,
        config: CleverTapInstanceConfig,
        context: Context,
        cryptHandler: CryptHandler,
        dbAdapter: DBAdapter
    ): Boolean {
        return handler.execute(
            encrypt = stepDetails.encrypt,
            algorithm = stepDetails.algorithm,
            config = config,
            context = context,
            cryptHandler = cryptHandler,
            dbAdapter = dbAdapter
        )
    }

    // Stores the updated state map into persistent storage
    private fun storeUpdatedState(currentStateMap: MutableMap<String, EncryptionDataState>, config: CleverTapInstanceConfig, context: Context) {
        val serializedMap = currentStateMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        StorageHelper.putString(
            context,
            StorageHelper.storageKeyWithSuffix(config, "currentEncryptionState"),
            serializedMap
        )
    }

    // Updates encryption settings based on migration success
    private fun storeEncryptionSettings(config: CleverTapInstanceConfig, context: Context) {
        // Add logic to update encryption level and algorithm in shared preferences or wherever necessary
        // This can involve updating `SharedPreferences` based on `completeMigrationSuccess`
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
        algorithm: Boolean,
        config: CleverTapInstanceConfig,
        context: Context,
        cryptHandler: CryptHandler
    ): Boolean {
        // todo - add use of correct algorithm if needed or remove
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
        encrypt: Boolean,
        algorithm: Boolean,
        config: CleverTapInstanceConfig,
        cryptHandler: CryptHandler,
        dbAdapter: DBAdapter
    ): Boolean {
        // todo - add use of correct algorithm if needed or remove
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
                                migrationStatus = false
                            }
                            profile.put(piiKey, crypted)
                        }
                    }
                }
                if (dbAdapter.storeUserProfile(
                        config.accountId,
                        profileIterator.key,
                        profile
                    ) <= -1L
                )
                    migrationStatus = false
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

    private fun migrateInAppData(
        encrypt: Boolean,
        algorithm: Boolean,
        config: CleverTapInstanceConfig,
        context: Context,
        cryptHandler: CryptHandler
    ): Boolean {

        config.logger.verbose(
            config.accountId,
            "Migrating encryption for InAppData"
        )
        // todo - add use of correct algorithm if needed or remove
        var migrationStatus = true
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")

        // Fetch all SharedPreferences files starting with "inApp" and ending with the accountId
        val prefsFiles = sharedPrefsDir.listFiles { _, name ->
            name.startsWith("inApp") && name.endsWith(config.accountId + ".xml")
        }

        prefsFiles?.forEach { file ->
            val prefName = file.nameWithoutExtension
            val inAppSharedPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

            // todo - replace with constants
            val keysToProcess = listOf("inapp_notifs_cs", "inapp_notifs_ss")

            // Process each key for encryption/decryption
            keysToProcess.forEach { key ->
                val data = inAppSharedPrefs.getString(key, null)

                // If data exists, attempt to encrypt or decrypt
                if (!data.isNullOrEmpty()) {
                    val processedData =
                        if (encrypt) cryptHandler.encrypt(data) else cryptHandler.decrypt(data)

                    // Check if encryption/decryption was successful
                    if (processedData == null) {
                        migrationStatus = false // Mark failure if the result is null
                    } else {
                        // Save the processed data back to SharedPreferences
                        inAppSharedPrefs.edit().putString(key, processedData).apply()
                    }
                }
            }
        }
        return migrationStatus
    }
}