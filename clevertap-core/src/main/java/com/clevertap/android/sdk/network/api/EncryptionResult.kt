package com.clevertap.android.sdk.network.api

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class EncryptionResult

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class EncryptionSuccess(
    val data: String,
    val iv: String
) : EncryptionResult()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data object EncryptionFailure : EncryptionResult()