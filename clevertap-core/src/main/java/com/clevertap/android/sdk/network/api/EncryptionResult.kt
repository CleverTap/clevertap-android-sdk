package com.clevertap.android.sdk.network.api

internal sealed class EncryptionResult

internal data class EncryptionSuccess(
    val data: String,
    val iv: String
) : EncryptionResult()

internal data object EncryptionFailure : EncryptionResult()