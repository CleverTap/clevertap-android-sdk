package com.clevertap.android.sdk.cryption

import androidx.annotation.RestrictTo

@Suppress("UnnecessaryAbstractClass") // intentional abstract base for AESCrypt/AESGCMCrypt; never instantiated directly
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class Crypt protected constructor() {
    abstract fun encryptInternal(plainText: String): String?
    abstract fun decryptInternal(cipherText: String): String?
}
