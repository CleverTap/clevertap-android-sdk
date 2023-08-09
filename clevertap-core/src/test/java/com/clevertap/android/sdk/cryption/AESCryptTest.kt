package com.clevertap.android.sdk.cryption

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.Assert.*


class AESCryptTest : BaseTestCase() {
    private lateinit var accountID : String
    private lateinit var aesCrypt : AESCrypt

    @Before
    override fun setUp() {
        super.setUp()
        accountID = "test_account_id"
        aesCrypt = AESCrypt()
    }


    @Test
    fun testEncryptAndDecrypt() {
        val plainText = "Test Text!"

        val encryptedText = aesCrypt.encryptInternal(plainText, accountID)
        assertNotNull(encryptedText)

        val decryptedText = aesCrypt.decryptInternal(encryptedText!!, accountID)
        assertEquals(plainText, decryptedText)
    }

    @Test
    fun `testEncryptAndDecrypt when key is valid and accountID(password) is different for encryption and decryption`() {
        val plainText = "Test Text!"

        val encryptedText = aesCrypt.encryptInternal(plainText,accountID)
        val decryptedText = aesCrypt.decryptInternal(encryptedText!!, "test_account_id2")

        assertNotEquals(plainText, decryptedText)
    }

    @Test
    fun `test encryptInternal`(){
        val plainText = "Test Text!"
        val expectedEncryptedText = "[93, 125, -83, 116, -22, 82, -53, -67, 88, -87, -44, -32, 55, 86, 120, -53]"

        val actualEncryptedText = aesCrypt.encryptInternal(plainText, accountID)
        assertEquals(expectedEncryptedText, actualEncryptedText)
    }


    @Test
    fun `test decryptInternal when cipherText is valid should return decrypted text`(){
        val cipherText = "[93, 125, -83, 116, -22, 82, -53, -67, 88, -87, -44, -32, 55, 86, 120, -53]"
        val expectedDecryptedText = "Test Text!"

        val actualDecryptedText = aesCrypt.decryptInternal(cipherText, accountID)
        assertEquals(expectedDecryptedText, actualDecryptedText)
    }

    @Test
    fun `test decryptInternal when cipherText is invalid should return null`(){
        val cipherText = "Invalid Cipher"

        val actualDecryptedText = aesCrypt.decryptInternal(cipherText, accountID)
        assertNull(actualDecryptedText)
    }
}

