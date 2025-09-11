package com.clevertap.android.sdk.cryption

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig.KEY_ENCRYPTION_LEVEL
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.cryption.CryptMigrator.Companion.MIGRATION_FAILURE_COUNT_KEY
import com.clevertap.android.sdk.cryption.CryptMigrator.Companion.MIGRATION_FIRST_UPGRADE
import com.clevertap.android.sdk.cryption.CryptMigrator.Companion.SS_IN_APP_MIGRATED
import com.clevertap.android.sdk.cryption.CryptMigrator.Companion.UNKNOWN_LEVEL

const val ENCRYPTION_KEY = "EncryptionKey"

interface ICryptRepository {
    fun storedEncryptionLevel(): Int
    fun isSSInAppDataMigrated(): Boolean
    fun updateIsSSInAppDataMigrated(migrated: Boolean)
    fun migrationFailureCount(): Int
    fun localEncryptionKey(): String?
    fun updateLocalEncryptionKey(key: String)
    fun updateEncryptionLevel(configEncryptionLevel: Int)
    fun updateMigrationFailureCount(migrationSuccessful: Boolean)
}

class CryptRepository(
    val context: Context,
    val accountId: String
) : ICryptRepository {
    private var migrationFailureCount: Int = 0

    override fun isSSInAppDataMigrated() =
        StorageHelper.getBooleanFromPrefs(
            context,
            accountId, SS_IN_APP_MIGRATED
        )

    override fun updateIsSSInAppDataMigrated(migrated: Boolean) {
        StorageHelper.putBoolean(
            context,
            accountId,
            SS_IN_APP_MIGRATED,
            migrated
        )
    }

    override fun storedEncryptionLevel() =
        StorageHelper.getIntFromPrefs(
            context,
            accountId,
            KEY_ENCRYPTION_LEVEL,
            UNKNOWN_LEVEL
        )

    override fun migrationFailureCount() = StorageHelper.getIntFromPrefs(
        context,
        accountId,
        MIGRATION_FAILURE_COUNT_KEY,
        MIGRATION_FIRST_UPGRADE
    )

    override fun localEncryptionKey(): String? {
        val encodedKey = StorageHelper.getString(context, ENCRYPTION_KEY, null)
        return encodedKey
    }

    override fun updateLocalEncryptionKey(key: String) {
        StorageHelper.putString(context, ENCRYPTION_KEY, key)
    }

    override fun updateEncryptionLevel(configEncryptionLevel: Int) {
        StorageHelper.putInt(
            context,
            accountId,
            KEY_ENCRYPTION_LEVEL,
            configEncryptionLevel
        )
    }

    override fun updateMigrationFailureCount(migrationSuccessful: Boolean) {
        migrationFailureCount = if (migrationSuccessful) {
            0
        } else {
            migrationFailureCount + 1
        }

        Logger.v(
            accountId,
            "Updating migrationFailureCount to $migrationFailureCount"
        )

        StorageHelper.putInt(
            context,
            accountId,
            MIGRATION_FAILURE_COUNT_KEY,
            migrationFailureCount
        )
    }

}