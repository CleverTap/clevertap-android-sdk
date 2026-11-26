package com.clevertap.android.sdk.cryption

import androidx.annotation.RestrictTo

/**
 * Enum representing encryption states
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal enum class EncryptionState {
    ENCRYPTED_AES,
    ENCRYPTED_AES_GCM,
    PLAIN_TEXT;
}