package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_CS
import com.clevertap.android.sdk.Constants.PREFS_INAPP_KEY_SS
import com.clevertap.android.sdk.Constants.piiDBKeys
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.utils.getStringOrNull
import org.json.JSONObject

internal data class CryptMigrator(
    private val logPrefix: String,
    private val configEncryptionLevel: Int,
    private val logger: ILogger,
    private val cryptHandler: CryptHandler,
    private val cryptRepository: CryptRepository,
    private val dataMigrationRepository: DataMigrationRepository
) {

    companion object {
        const val MIGRATION_FAILURE_COUNT_KEY = "encryptionMigrationFailureCount"
        const val UNKNOWN_LEVEL = -1
        const val MIGRATION_NOT_NEEDED = 0
        const val MIGRATION_NEEDED = 1
    }

    fun migrateEncryption() {
        val storedEncryptionLevel = cryptRepository.storedEncryptionLevel()

        val migrationFailureCount = when {
            storedEncryptionLevel == UNKNOWN_LEVEL && configEncryptionLevel == EncryptionLevel.NONE.intValue() -> {
                cryptRepository.updateMigrationFailureCount(false)
                MIGRATION_NOT_NEEDED
            }

            storedEncryptionLevel != configEncryptionLevel -> MIGRATION_NEEDED
            else -> cryptRepository.migrationFailureCount()
        }

        cryptRepository.updateEncryptionLevel(configEncryptionLevel)

        if (migrationFailureCount == 0) {
            logger.verbose(
                logPrefix,
                "Migration not required: config-encryption-level $configEncryptionLevel, stored-encryption-level $storedEncryptionLevel"
            )
            return
        }

        logger.verbose(
            logPrefix,
            "Starting migration from encryption level $storedEncryptionLevel to $configEncryptionLevel"
        )
        val migrationSuccess = handleAllMigrations(configEncryptionLevel == EncryptionLevel.MEDIUM.intValue())
        cryptRepository.updateMigrationFailureCount(migrationSuccess)
    }

    private fun handleAllMigrations(encrypt: Boolean): Boolean {
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
        logger.verbose(
            logPrefix,
            "Migrating encryption level for cachedGUIDsKey prefs"
        )

        val cachedGuidJsonObj = dataMigrationRepository.cachedGuidJsonObject()
        val newGuidJsonObj = JSONObject()
        var migrationSuccessful = true

        try {
            val keysIterator = cachedGuidJsonObj.keys()
            while (keysIterator.hasNext()) {
                val nextKey = keysIterator.next()
                val (key, identifier) = nextKey.split("_", limit = 2)
                val migrationResult =
                    performMigrationStep(encrypt, identifier)
                migrationSuccessful = migrationSuccessful && migrationResult.migrationSuccessful
                val cryptedKey = "${key}_${migrationResult.data}"
                newGuidJsonObj.put(cryptedKey, cachedGuidJsonObj[nextKey])
            }
            if (cachedGuidJsonObj.length() > 0) {
                dataMigrationRepository.saveCachedGuidJsonObject(newGuidJsonObj)
                logger.verbose(
                    logPrefix,
                    "Cached GUIDs migrated successfully: [${newGuidJsonObj}]"
                )
            }
        } catch (t: Throwable) {
            logger.verbose(logPrefix, "Error migrating cached GUIDs: $t")
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
                        val migrationResult =
                            performMigrationStep(encrypt, value)
                        migrationSuccessful =
                            migrationSuccessful && migrationResult.migrationSuccessful
                        profile.put(piiKey, migrationResult.data)
                    }
                }
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
     * Returns true if migration was successful and false otherwise
     */
    private fun migrateInAppData(): Boolean {
        logger.verbose(logPrefix, "Migrating encryption for InAppData")
        var migrationSuccessful = true

        dataMigrationRepository.inAppDataFiles().forEach { prefs ->
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

    private fun performMigrationStep(
        encrypt: Boolean,
        data: String
    ): MigrationResult {
        val currentState = getCurrentEncryptionState(data)
        val finalState = getFinalEncryptionState(encrypt)

        if (currentState == finalState) {
            // No migration needed if current state matches the final state
            return MigrationResult(data = data, migrationSuccessful = true)
        }

        return try {
            currentState.transitionTo(
                targetState = finalState,
                data = data,
                cryptHandler = cryptHandler
            )
        } catch (e: Exception) {
            logger.verbose(logPrefix, "Migration step failed for data: $e")
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

}
