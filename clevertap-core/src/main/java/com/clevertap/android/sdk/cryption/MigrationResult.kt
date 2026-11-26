package com.clevertap.android.sdk.cryption

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class MigrationResult(
    val data: String?,
    val migrationSuccessful: Boolean
) {
    companion object {
        fun failure(data: String?) = MigrationResult(data, false)
    }
}