package com.clevertap.android.sdk.cryption

abstract class Crypt protected constructor() {
    abstract fun encryptInternal(plainText: String): String?
    abstract fun decryptInternal(cipherText: String): String?
}