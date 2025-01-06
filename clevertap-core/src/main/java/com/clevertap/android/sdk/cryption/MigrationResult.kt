package com.clevertap.android.sdk.cryption

internal data class MigrationResult(
    val data: String?,
    val migrationSuccessful: Boolean
) {
    companion object {
        fun failure(data: String?) = MigrationResult(data, false)
    }
}