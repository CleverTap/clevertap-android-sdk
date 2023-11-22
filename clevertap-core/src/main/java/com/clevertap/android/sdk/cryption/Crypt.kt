package com.clevertap.android.sdk.cryption

abstract class Crypt protected constructor() {
    abstract fun encryptInternal(plainText: String, accountID: String): String?
    abstract fun decryptInternal(cipherText: String, accountID: String): String?
    protected abstract fun parseCipherText(cipherText: String): ByteArray?
}