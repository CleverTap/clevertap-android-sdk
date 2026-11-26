package com.clevertap.android.sdk.cryption

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class Crypt protected constructor() {
    abstract fun encryptInternal(plainText: String): String?
    abstract fun decryptInternal(cipherText: String): String?
}