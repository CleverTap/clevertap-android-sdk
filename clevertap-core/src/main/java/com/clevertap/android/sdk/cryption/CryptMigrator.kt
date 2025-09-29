package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_CS
import com.clevertap.android.sdk.Constants.INAPP_KEY
import com.clevertap.android.sdk.Constants.piiDBKeys
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.cryption.CryptHandler.EncryptionAlgorithm
import com.clevertap.android.sdk.cryption.EncryptionState.ENCRYPTED_AES
import com.clevertap.android.sdk.cryption.EncryptionState.ENCRYPTED_AES_GCM
import com.clevertap.android.sdk.cryption.EncryptionState.PLAIN_TEXT
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.utils.getStringOrNull
import com.clevertap.android.sdk.variables.repo.VariablesRepo
import org.json.JSONObject

internal data class CryptMigrator(
    private val logPrefix: String,
    private val configEncryptionLevel: Int,
    private val logger: ILogger,
    private val cryptHandler: CryptHandler,
    private val cryptRepository: CryptRepository,
    private val dataMigrationRepository: DataMigrationRepository,
    private val variablesRepo: VariablesRepo,
    private val dbAdapter: DBAdapter
) {

    companion object {
        const val MIGRATION_FAILURE_COUNT_KEY = "encryptionMigrationFailureCount"
        const val SS_IN_APP_MIGRATED = "ssInAppMigrated"
        const val UNKNOWN_LEVEL = -1
        const val MIGRATION_NOT_NEEDED = 0
        const val MIGRATION_NEEDED = 1
        const val MIGRATION_FIRST_UPGRADE = -1
    }

    /**
     * Handles the migration of encryption levels for stored data.
     *
     *
     * - Scenarios handled:
     *   1. **Fresh Install**: If the stored encryption level is unknown and the configured level is
     *      `EncryptionLevel.NONE`, no migration is needed.
     *   2. **Encryption Level Upgrade**: If the stored encryption level differs from the configured
     *      encryption level and migration failures are not recorded, migration is required.
     *   3. **Existing Failures**: If there are recorded migration failures, the process attempts to
     *      continue from where it left off.
     *   4. **SSInApp Key Fix**: v7.3.0- had incorrect migration for SSInApps, hence migration is required.
     *
     * - Updates:
     *   - Updates the stored encryption level to the current configuration.
     *   - Updates the migration failure count based on the success or failure of the migration.
     *
     */
    fun migrateEncryption() {
        val storedEncryptionLevel = cryptRepository.storedEncryptionLevel()
        val storedFailureCount = cryptRepository.migrationFailureCount()
        val isSSInAppDataMigrated = cryptRepository.isSSInAppDataMigrated()

        val migrationFailureCount = when {
            !isSSInAppDataMigrated -> MIGRATION_NEEDED // Migration incorrect for for SS InApps
            storedEncryptionLevel != configEncryptionLevel && storedFailureCount != -1 -> MIGRATION_NEEDED // Encryption level changed and upgrade to v2 already complete
            else -> storedFailureCount
        }

        cryptRepository.updateEncryptionLevel(configEncryptionLevel)

        if (migrationFailureCount == MIGRATION_NOT_NEEDED) {
            logger.verbose(
                logPrefix,
                "Migration not required: config-encryption-level $configEncryptionLevel, " +
                        "stored-encryption-level $storedEncryptionLevel"
            )
            return
        }

        logger.verbose(
            logPrefix,
            "Starting migration from encryption level $storedEncryptionLevel to $configEncryptionLevel " +
                    "with migrationFailureCount $migrationFailureCount and isSSInAppDataMigrated $isSSInAppDataMigrated"
        )

        val level = EncryptionLevel.fromInt(configEncryptionLevel)
        val storedLevel = EncryptionLevel.fromInt(storedEncryptionLevel)

        val migrationSuccess = handleAllMigrations(
            level = level,
            storedLevel = storedLevel,
            firstUpgrade = migrationFailureCount == -1
        )

        cryptRepository.updateIsSSInAppDataMigrated(migrationSuccess)
        cryptRepository.updateMigrationFailureCount(migrationSuccess)
    }

    private fun handleAllMigrations(
        level: EncryptionLevel,
        storedLevel: EncryptionLevel,
        firstUpgrade: Boolean,
    ): Boolean {
        // Order is imp, do not change.
        val cgkMigrationSuccess = migrateCachedGuidsKeyPref(
            level = level,
            firstUpgrade = firstUpgrade
        )
        val dbMigrationSuccess = migrateDBProfile(level = level)
        val inAppMigrationSuccess = migrateInAppData(level = level)

        migrateVariablesData(level = level, storedLevel = storedLevel)
        migrateInboxData(level = level, storedLevel = storedLevel)

        return cgkMigrationSuccess && dbMigrationSuccess && inAppMigrationSuccess
    }

    private fun migrateVariablesData(
        level: EncryptionLevel,
        storedLevel: EncryptionLevel
    ) : Boolean {
        val variablesData = variablesRepo.loadDataFromCache()
        if (variablesData != null) {
            // Automatically internally saved to correct level and state
            variablesRepo.storeDataInCache(variablesData)
        } else {
            logger.verbose("Skipping variable migration as there is no data")
        }
        return true
    }

    private fun migrateInboxData(
        level: EncryptionLevel,
        storedLevel: EncryptionLevel
    ) : Boolean {
        for (id in dataMigrationRepository.userProfilesInAccount().map { it.key }) {
            val messages = dbAdapter.getMessages(id)
            // Save function will automatically save it in encrypted form.
            dbAdapter.upsertMessages(messages)
        }
        return true
    }

    /**
     * This method migrates the encryption level of the value under cachedGUIDsKey stored in the shared preference file
     *
     * If decryption from AES to plain-text fails, this data is deleted
     * @param level - Level of encryption
     * @param firstUpgrade - Flag to indicate whether this is the first upgrade to v2
     * Returns true if migration was successful and false otherwise
     */
    private fun migrateCachedGuidsKeyPref(
        level: EncryptionLevel,
        firstUpgrade: Boolean,
    ): Boolean {
        logger.verbose(
            logPrefix,
            "Migrating encryption level for cachedGUIDsKey prefs"
        )
        val cgkString: String = if (firstUpgrade) {
            // translate from old format to new format, in new format we encrypt entire string
            val cgkJson: JSONObject = convertCachedGuidsToPlainText()
            val cgkLength = cgkJson.length()
            dataMigrationRepository.saveCachedGuidJsonLength(cgkLength)
            if (cgkLength == 0) {
                dataMigrationRepository.removeCachedGuidJson()
                return true
            }
            cgkJson.toString()
        } else {
            dataMigrationRepository.cachedGuidString() ?: return true
        }

        val migrationResult = performMigrationStep(level = level, data = cgkString)
        dataMigrationRepository.saveCachedGuidJson(migrationResult.data)
        logger.verbose(
            logPrefix,
            "Cached GUIDs migrated with success = $migrationResult.migrationSuccessful = ${migrationResult.data}"
        )
        return migrationResult.migrationSuccessful
    }


    /**
     * This method migrates converts the older format of cgk to an all plain text JSONObject
     * If decryption for any key-value fails that key is dropped forever
     *
     * The older format when encryption level is 1 was {Email_[]:__g... , Name_[]:__i...} -> Only the identifier was encrypted
     * The migrated format will encrypt the entire JSONObject and not just the identifier
     *
     * @return JSONObject with all plain text
     */
    private fun convertCachedGuidsToPlainText(): JSONObject {
        val cachedGuidJsonObj = dataMigrationRepository.cachedGuidJsonObject()
        val migratedGuidJsonObj = JSONObject()
        try {
            val keysIterator = cachedGuidJsonObj.keys()
            while (keysIterator.hasNext()) {
                val nextKey = keysIterator.next()
                val (key, identifier) = nextKey.split("_", limit = 2)
                val migrationResult = performMigrationStep(EncryptionLevel.NONE, identifier)
                if (migrationResult.migrationSuccessful) {
                    val cryptedKey = "${key}_${migrationResult.data}"
                    migratedGuidJsonObj.put(cryptedKey, cachedGuidJsonObj[nextKey])
                }
            }
        } catch (t: Throwable) {
            logger.verbose(
                logPrefix,
                "Error migrating format for cached GUIDs: Clearing and starting fresh $t"
            )
        }
        return migratedGuidJsonObj
    }

    /**
     * This method migrates the encryption level of the user profiles stored in the local db
     * Only pii data such as name, phone, email and identity are encrypted from the user profile, remaining are kept as is
     *
     * If decryption from AES to plain-text fails, this data point is deleted
     * @param level - Level of encryption
     * Returns true if migration was successful and false otherwise
     */
    private fun migrateDBProfile(level: EncryptionLevel): Boolean {
        logger.verbose(
            logPrefix,
            "Migrating encryption level for user profiles in DB"
        )

        val profiles = dataMigrationRepository.userProfilesInAccount()

        var migrationSuccessful = true
        for ((deviceID, profile) in profiles) {
            try {
                piiDBKeys.forEach { piiKey ->
                    profile.getStringOrNull(piiKey)?.let { value ->
                        val moveTo = if (EncryptionLevel.FULL_DATA == level) {
                            // Lets convert individual keys to plain text in case of FULL
                            // dataMigrationRepository.saveUserProfile() internally will correctly save the data completely encrypted (entire profile).
                            EncryptionLevel.NONE
                        } else {
                            level
                        }

                        val migrationResult =
                            performMigrationStep(level = moveTo, data = value)
                        migrationSuccessful =
                            migrationSuccessful && migrationResult.migrationSuccessful
                        profile.put(piiKey, migrationResult.data)
                    }
                }
                logger.verbose(
                    logPrefix,
                    "DB migrated with success = $migrationSuccessful = $profile"
                )
                if (dataMigrationRepository.saveUserProfile(deviceID, profile) <= -1L) {
                    migrationSuccessful = false
                }
            } catch (e: Exception) {
                logger.verbose(logPrefix, "Error migrating profile $deviceID: $e")
                migrationSuccessful = false
            }
        }

        return migrationSuccessful
    }


    /**
     * This method migrates the encryption level of the inapp data stored in the shared preferences file.
     * Migration(if needed) is always performed to AES_GCM
     *
     * If decryption from AES to plain-text fails, this data point is deleted
     * Returns true if migration was successful and false otherwise
     */
    private fun migrateInAppData(level: EncryptionLevel): Boolean {
        logger.verbose(logPrefix, "Migrating encryption for InAppData")
        var migrationSuccessful = true

        val migrateCode: (String) -> String? = { spData: String ->
            val result = performMigrationStep(level, spData)
            migrationSuccessful = migrationSuccessful && result.migrationSuccessful
            result.data
        }
        val keysToProcess = listOf(PREFS_INAPP_KEY_CS, INAPP_KEY)

        dataMigrationRepository.inAppDataFiles(keysToProcess, migrateCode)

        return migrationSuccessful
    }

    private fun performMigrationStep(
        level: EncryptionLevel,
        data: String
    ): MigrationResult {
        val currentState = getCurrentEncryptionState(data)
        val targetState = getFinalEncryptionState(level.shouldEncrypt())

        return transitionEncryptionState(currentState, targetState, data)
    }

    private fun transitionEncryptionState(
        currentState: EncryptionState,
        targetState: EncryptionState,
        data: String,
    ): MigrationResult {

        if (currentState == targetState) {
            // No migration needed if current state matches the final state
            return MigrationResult(data = data, migrationSuccessful = true)
        }

        return when (currentState) {
            ENCRYPTED_AES -> handleEncryptedAesTransition(targetState, data)
            ENCRYPTED_AES_GCM -> handleEncryptedAesGcmTransition(targetState, data)
            PLAIN_TEXT -> handlePlainTextTransition(targetState, data)
        }
    }

    /**
     * Handles the transition of data encrypted from AES to the specified target state.
     *
     * This function decrypts the input data using AES encryption and then transitions it to the
     * target encryption state:
     * - **AES_GCM**: Re-encrypts the decrypted data using AES-GCM encryption.
     * - **Plain Text**: Returns the decrypted data
     *
     * This function returns (null, true) when decryption from AES state fails. This indicates that the data must be deleted
     *
     * @param targetState The target encryption state to which the data should transition.
     * @param data The encrypted input data as a string.
     *
     * @return A `MigrationResult` containing the transformed data and whether the operation succeeded.
     */
    private fun handleEncryptedAesTransition(
        targetState: EncryptionState,
        data: String,
    ): MigrationResult {
        val decrypted = cryptHandler.decrypt(data, EncryptionAlgorithm.AES)
        return when (targetState) {
            ENCRYPTED_AES_GCM -> {
                val encrypted = decrypted?.let {
                    cryptHandler.encrypt(
                        it,
                        EncryptionAlgorithm.AES_GCM
                    )
                }
                MigrationResult(encrypted ?: decrypted, encrypted != null || decrypted == null)
            }

            PLAIN_TEXT -> {
                MigrationResult(decrypted ?: data, decrypted != null)
            }
            else -> {
                logger.verbose(logPrefix, "Invalid transition from ENCRYPTED_AES to $targetState")
                MigrationResult.failure(data)
            }
        }
    }

    /**
     * Handles the transition of data encrypted from AES-GCM to the specified target state.
     * Target State should will be PLAIN_TEXT
     *
     * This function decrypts the input data using AES-GCM algorithm
     *
     * @param targetState The target encryption state to which the data should transition.
     * @param data The encrypted input data as a string.
     *
     * @return A `MigrationResult` containing the transformed data and whether the operation succeeded.
     */
    private fun handleEncryptedAesGcmTransition(
        targetState: EncryptionState,
        data: String,
    ): MigrationResult {
        val decrypted = cryptHandler.decrypt(
            data,
            EncryptionAlgorithm.AES_GCM
        )
        return when (targetState) {
            PLAIN_TEXT -> {
                MigrationResult(decrypted ?: data, decrypted != null)
            }
            else -> {
                logger.verbose(logPrefix, "Invalid transition from ENCRYPTED_AES_GCM to $targetState")
                MigrationResult.failure(data)
            }
        }
    }

    /**
     * Handles the transition of plain text data to the specified target state.
     *
     * This function transitions the input plain text data to the desired encryption state:
     * - **AES_GCM**: Encrypts the data using AES-GCM encryption.
     *
     * @param targetState The target encryption state to which the data should transition.
     * @param data The input plain text data as a string.
     *
     * @return A `MigrationResult` containing the transformed data and whether the operation succeeded.
     */
    private fun handlePlainTextTransition(
        targetState: EncryptionState,
        data: String,
    ): MigrationResult {
        return when (targetState) {
            ENCRYPTED_AES_GCM -> {
                val encrypted = cryptHandler.encrypt(
                    data,
                    EncryptionAlgorithm.AES_GCM
                )
                MigrationResult(encrypted ?: data, encrypted != null)
            }
            else -> {
                logger.verbose(logPrefix, "Invalid transition from PLAIN_TEXT to $targetState")
                MigrationResult.failure(data)
            }
        }
    }


    private fun getFinalEncryptionState(encrypt: Boolean): EncryptionState {
        return if (encrypt) {
            ENCRYPTED_AES_GCM
        } else {
            PLAIN_TEXT
        }
    }

    private fun getCurrentEncryptionState(data: String): EncryptionState {
        return when {
            CryptHandler.isTextAESEncrypted(data) -> ENCRYPTED_AES
            CryptHandler.isTextAESGCMEncrypted(data) -> ENCRYPTED_AES_GCM
            else -> PLAIN_TEXT
        }
    }
}
