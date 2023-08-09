package com.clevertap.android.sdk.cryption

import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


class CryptHandlerTest : BaseTestCase() {
    private lateinit var accountID: String
    private lateinit var cryptHandlerNone: CryptHandler
    private lateinit var cryptHandlerMedium: CryptHandler

    @Before
    override fun setUp() {

        super.setUp()
        mockkObject(CryptFactory.Companion)
        every { CryptFactory.getCrypt(CryptHandler.EncryptionAlgorithm.AES) } answers { MockAESCrypt() }

        accountID = "test_account_id"
        cryptHandlerNone = CryptHandler(0, CryptHandler.EncryptionAlgorithm.AES, accountID)
        cryptHandlerMedium = CryptHandler(1, CryptHandler.EncryptionAlgorithm.AES, accountID)
    }


    @Test
    fun `testEncryptAndDecrypt when encryptionLevelIsNone`() {
        val plainText = "dummy_decrypted"
        val key = "dummy_key"

        val encryptedText = cryptHandlerNone.encrypt(plainText, key)
        val decryptedText = cryptHandlerNone.decrypt(encryptedText!!, key)

        assertEquals(plainText, decryptedText)
    }

    @Test
    fun `testEncryptAndDecrypt when encryptionLevelIsMedium and key is valid`() {
        val plainText = "dummy_decrypted"
        val key = "cgk"

        val encryptedText = cryptHandlerMedium.encrypt(plainText, key)
        val decryptedText = cryptHandlerMedium.decrypt(encryptedText!!, key)

        assertEquals(plainText, decryptedText)
    }

    @Test
    fun `testEncryptAndDecrypt when encryptionLevelIsMedium and key is invalid`() {
        val plainText = "dummy_decrypted"
        val key = "dummy_key"

        val encryptedText = cryptHandlerMedium.encrypt(plainText, key)
        val decryptedText = cryptHandlerMedium.decrypt(encryptedText!!, key)

        assertEquals(plainText, decryptedText)
    }



    @Test
    fun `test Encrypt should return plaintext irrespective of the key when encryptionLevelIsNone `() {
        val plainText = "dummy_decrypted"
        var key = "dummy_key"

        var encryptedText = cryptHandlerNone.encrypt(plainText, key)
        assertEquals(encryptedText, plainText)

        key = "cgk"
        encryptedText = cryptHandlerNone.encrypt(plainText, key)
        assertEquals(encryptedText, plainText)
    }

    @Test
    fun `test Encrypt should only encrypt for required keys when encryptionLevelIsMedium `() {
        val plainText = "dummy_decrypted"
        val key = "dummy_key"
        val reqKeys =
            arrayListOf("cgk", "encryptionmigration", "Email", "Phone", "Identity", "Name")

        var actualEncryptedText = cryptHandlerMedium.encrypt(plainText, key)
        assertEquals(plainText, actualEncryptedText)

        val expectedEncryptedText = "[1,2,3]"
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
        val expectedDecryptedText = "dummy_decrypted"
        val key = "any_key"

        val decryptedText = cryptHandlerNone.decrypt(cipherText, key)
        assertEquals(expectedDecryptedText, decryptedText)
    }

    @Test
    fun `test Decrypt should decrypt only for required keys when encryptionLevelIsMedium`() {
        val cipherText =
            "[93, 125, -83, 116, -22, 82, -53, -67, 88, -87, -44, -32, 55, 86, 120, -53]"
        val expectedDecryptedText = "dummy_decrypted"
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
        val cipherText = "dummy_decrypted"
        val dummyKey = "cgk"

        val decryptedText = cryptHandlerMedium.decrypt(cipherText, dummyKey)
        assertEquals(cipherText, decryptedText)
    }

}

