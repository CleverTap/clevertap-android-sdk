package com.clevertap.android.sdk.cryption

import android.content.Context
import com.clevertap.android.sdk.Constants.KEY_ENCRYPTION_LEVEL
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.cryption.CryptMigrator.Companion.MIGRATION_FAILURE_COUNT_KEY
import com.clevertap.android.sdk.cryption.CryptMigrator.Companion.MIGRATION_FIRST_UPGRADE
import com.clevertap.android.sdk.cryption.CryptMigrator.Companion.UNKNOWN_LEVEL

interface ICryptRepository {
    fun storedEncryptionLevel(): Int
    fun migrationFailureCount(): Int
    fun updateEncryptionLevel(configEncryptionLevel: Int)
    fun updateMigrationFailureCount(migrationSuccessful: Boolean)
}

class CryptRepository(
    val context: Context,
    val accountId: String
) : ICryptRepository {
    private var migrationFailureCount: Int = 0

    override fun storedEncryptionLevel() =
        StorageHelper.getInt(
            context,
            StorageHelper.storageKeyWithSuffix(accountId, KEY_ENCRYPTION_LEVEL),
            UNKNOWN_LEVEL
        )

    override fun migrationFailureCount() = StorageHelper.getInt(
        context,
        StorageHelper.storageKeyWithSuffix(accountId, MIGRATION_FAILURE_COUNT_KEY),
        MIGRATION_FIRST_UPGRADE
    )

    override fun updateEncryptionLevel(configEncryptionLevel: Int) {
        StorageHelper.putInt(
            context,
            StorageHelper.storageKeyWithSuffix(accountId, KEY_ENCRYPTION_LEVEL),
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
            StorageHelper.storageKeyWithSuffix(accountId, CryptMigrator.MIGRATION_FAILURE_COUNT_KEY),
            migrationFailureCount
        )
    }

}