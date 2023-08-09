package com.clevertap.android.sdk.cryption

import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test


class CryptHandlerTest : BaseTestCase() {
    private lateinit var accountID: String
    private lateinit var cryptHandlerNone: CryptHandler
    private lateinit var cryptHandlerMedium: CryptHandler
    private lateinit var aesMock: AESCrypt

    @Before
    override fun setUp() {

        super.setUp()
//        mockStatic(CryptFactory::class.java).use {
        mockkObject(CryptFactory.Companion)
        every { CryptFactory.getCrypt(CryptHandler.EncryptionAlgorithm.AES) } answers { MockAESCrypt() }

        accountID = "test_account_id"
        cryptHandlerNone = CryptHandler(0, CryptHandler.EncryptionAlgorithm.AES, accountID)
        cryptHandlerMedium = CryptHandler(1, CryptHandler.EncryptionAlgorithm.AES, accountID)
//        }
    }


    @Test
    fun `testEncryptAndDecrypt when encryptionLevelIsNone`() {
        val plainText = "Test Text!"
        val key = "dummy_key"

        val encryptedText = cryptHandlerNone.encrypt(plainText, key)
        val decryptedText = cryptHandlerNone.decrypt(encryptedText!!, key)

        assertEquals(plainText, decryptedText)
    }

    @Test
    fun `testEncryptAndDecrypt when encryptionLevelIsMedium and key is valid`() {
        val plainText = "Test Text!"
        val key = "cgk"

        val encryptedText = cryptHandlerMedium.encrypt(plainText, key)
        val decryptedText = cryptHandlerMedium.decrypt(encryptedText!!, key)

        assertEquals(plainText, decryptedText)
    }

    @Test
    fun `testEncryptAndDecrypt when encryptionLevelIsMedium and key is invalid`() {
        val plainText = "Test Text!"
        val key = "dummy_key"

        val encryptedText = cryptHandlerMedium.encrypt(plainText, key)
        val decryptedText = cryptHandlerMedium.decrypt(encryptedText!!, key)

        assertEquals(plainText, decryptedText)
    }


    @Test
    fun `testEncryptAndDecrypt when key is valid and accountID(password) is different for encryption and decryption`() {
        val plainText = "Test Text!"
        val key = "cgk"

        val cryptHandlerMedium2 =
            CryptHandler(1, CryptHandler.EncryptionAlgorithm.AES, "test_account_id_2")

        val encryptedText = cryptHandlerMedium.encrypt(plainText, key)
        val decryptedText = cryptHandlerMedium2.decrypt(encryptedText!!, key)

        assertNotEquals(plainText, decryptedText)
    }


    @Test
    fun `test Encrypt should return plaintext irrespective of the key when encryptionLevelIsNone `() {
        val plainText = "Test Text!"
        var key = "dummy_key"

        var encryptedText = cryptHandlerNone.encrypt(plainText, key)
        assertEquals(encryptedText, plainText)

        key = "cgk"
        encryptedText = cryptHandlerNone.encrypt(plainText, key)
        assertEquals(encryptedText, plainText)
    }

    @Test
    fun `test Encrypt should only encrypt for required keys when encryptionLevelIsMedium `() {
        val plainText = "Test Text!"
        val key = "dummy_key"
        val reqKeys =
            arrayListOf("cgk", "encryptionmigration", "Email", "Phone", "Identity", "Name")

        var actualEncryptedText = cryptHandlerMedium.encrypt(plainText, key)
        assertEquals(plainText, actualEncryptedText)

        val expectedEncryptedText =
            "[93, 125, -83, 116, -22, 82, -53, -67, 88, -87, -44, -32, 55, 86, 120, -53]"
        for (mediumKey in reqKeys) {
            actualEncryptedText = cryptHandlerMedium.encrypt(plainText, mediumKey)
            assertEquals(expectedEncryptedText, actualEncryptedText)
        }
    }

    @Test
    fun `test Encrypt should return the sameText if already encrypted`() {
        val plainText =
            "[93, 125, -83, 116, -22, 82, -53, -67, 88, -87, -44, -32, 55, 86, 120, -53]"
        val key = "cgk"

        val actualEncryptedText = cryptHandlerMedium.encrypt(plainText, key)
        assertEquals(plainText, actualEncryptedText)
    }


    @Test
    fun `test Decrypt should decrypt for any key if cipher text is valid `() {
        val cipherText =
            "[93, 125, -83, 116, -22, 82, -53, -67, 88, -87, -44, -32, 55, 86, 120, -53]"
        val expectedDecryptedText = "Test Text!"
        val key = "any_key"

        val decryptedText = cryptHandlerNone.decrypt(cipherText, key)
        assertEquals(expectedDecryptedText, decryptedText)
    }

    @Test
    fun `test Decrypt should decrypt only for required keys when encryptionLevelIsMedium`() {
        val cipherText =
            "[93, 125, -83, 116, -22, 82, -53, -67, 88, -87, -44, -32, 55, 86, 120, -53]"
        val expectedDecryptedText = "Test Text!"
        val dummyKey = "dummy_key"
        val reqKeys =
            arrayListOf("cgk", "encryptionmigration", "Email", "Phone", "Identity", "Name")

        var decryptedText = cryptHandlerMedium.decrypt(cipherText, dummyKey)
        assertEquals(cipherText, decryptedText)

        for (mediumKey in reqKeys) {
            decryptedText = cryptHandlerMedium.decrypt(cipherText, mediumKey)
            assertEquals(expectedDecryptedText, decryptedText)
        }
    }

    @Test
    fun `test Decrypt should return sameText if already decrypted`() {
        val cipherText = "Test Text!"
        val dummyKey = "cgk"

        val decryptedText = cryptHandlerMedium.decrypt(cipherText, dummyKey)
        assertEquals(cipherText, decryptedText)
    }

    @Test
    fun `test Decrypt should return null if cipherText is invalid`() {
        // This cipher text will appear to be encrypted and hence decrypt internal will be called
        val cipherText = "[Test Text!]"
        val dummyKey = "cgk"

        val decryptedText = cryptHandlerMedium.decrypt(cipherText, dummyKey)
        assertNull(decryptedText)
    }
}

