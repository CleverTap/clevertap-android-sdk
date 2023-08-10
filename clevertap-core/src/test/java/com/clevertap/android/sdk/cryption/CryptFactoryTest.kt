package com.clevertap.android.sdk.cryption

import org.junit.*
import kotlin.test.*

class CryptFactoryTest {
    @Test
    fun `test getCrypt returns AESCrypt`() {
        val result = CryptFactory.getCrypt(CryptHandler.EncryptionAlgorithm.AES)
        assertNotNull(result)
        assertTrue(result is AESCrypt)
    }
}