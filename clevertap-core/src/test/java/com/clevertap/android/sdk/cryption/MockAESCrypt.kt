package com.clevertap.android.sdk.cryption

class MockAESCrypt : Crypt() {
    override fun encryptInternal(plainText: String, accountID: String): String? {
        return "dummy_encrypted"
    }

    override fun decryptInternal(cipherText: String, accountID: String): String? {
        return "dummy_decrypted"
    }

    override fun parseCipherText(cipherText: String): ByteArray? {
        return "[1,2,3]".toByteArray()
    }

}