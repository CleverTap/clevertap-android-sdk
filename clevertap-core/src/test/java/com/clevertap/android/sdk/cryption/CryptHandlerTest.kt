package com.clevertap.android.sdk.cryption

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clevertap.android.sdk.Constants
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CryptHandlerTest {

    @MockK
    private lateinit var repository: CryptRepository

    @MockK
    private lateinit var cryptFactory: CryptFactory

    @MockK
    private lateinit var crypt: Crypt

    private lateinit var cryptHandler: CryptHandler

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        cryptHandler = CryptHandler(
            encryptionLevel = EncryptionLevel.MEDIUM,
            accountID = "testAccountId",
            repository = repository,
            cryptFactory = cryptFactory
        )
        every { cryptFactory.getCryptInstance(any()) } returns crypt
    }

    @Test
    fun encrypt_mediumEncryptionLevel_validKey() {
        val plainText = "testPlainText"
        val key = Constants.KEY_ENCRYPTION_EMAIL
        val encryptedText = "encryptedText"

        every { crypt.encryptInternal(plainText) } returns encryptedText

        val result = cryptHandler.encrypt(plainText, key)

        assertEquals(encryptedText, result)
    }

    @Test
    fun encrypt_mediumEncryptionLevel_invalidKey() {
        val plainText = "testPlainText"
        val key = "invalidKey"

        val result = cryptHandler.encrypt(plainText, key)

        assertEquals(plainText, result)
    }

    @Test
    fun encrypt_noneEncryptionLevel() {
        val plainText = "testPlainText"
        val key = Constants.KEY_ENCRYPTION_EMAIL
        val cryptHandler = CryptHandler(
            encryptionLevel = EncryptionLevel.NONE,
            accountID = "testAccountId",
            repository = repository,
            cryptFactory = cryptFactory
        )

        val result = cryptHandler.encrypt(plainText, key)

        assertEquals(plainText, result)
    }

    @Test
    fun decrypt_mediumEncryptionLevel_validKey() {
        val cipherText = "encryptedText"
        val key = Constants.KEY_ENCRYPTION_EMAIL
        val decryptedText = "decryptedText"

        every { crypt.decryptInternal(cipherText) } returns decryptedText

        val result = cryptHandler.decrypt(cipherText, key)

        assertEquals(decryptedText, result)
    }

    @Test
    fun decrypt_mediumEncryptionLevel_invalidKey() {
        val cipherText = "encryptedText"
        val key = "invalidKey"

        val result = cryptHandler.decrypt(cipherText, key)

        assertEquals(cipherText, result)
    }

    @Test
    fun decrypt_noneEncryptionLevel() {
        val cipherText = "encryptedText"
        val key = Constants.KEY_ENCRYPTION_EMAIL
        val decryptedText = "decryptedText"
        val cryptHandler = CryptHandler(
            encryptionLevel = EncryptionLevel.NONE,
            accountID = "testAccountId",
            repository = repository,
            cryptFactory = cryptFactory
        )

        every { crypt.decryptInternal(cipherText) } returns decryptedText

        val result = cryptHandler.decrypt(cipherText, key)

        assertEquals(decryptedText, result)
    }

    @Test
    fun isTextEncrypted_encryptedText() {
        val encryptedText = "${Constants.AES_PREFIX}testEncryptedText${Constants.AES_SUFFIX}"

        assertTrue(CryptHandler.isTextEncrypted(encryptedText))
    }

    @Test
    fun isTextEncrypted_plainText() {
        val plainText = "testPlainText"

        assertFalse(CryptHandler.isTextEncrypted(plainText))
    }

    @Test
    fun encrypt_withoutChecks() {
        val plainText = "testPlainText"
        val encryptedText = "encryptedText"

        every { crypt.encryptInternal(plainText) } returns encryptedText

        val result = cryptHandler.encrypt(plainText)

        assertEquals(encryptedText, result)
    }

    @Test
    fun decrypt_withoutChecks() {
        val cipherText = "encryptedText"
        val decryptedText = "decryptedText"

        every { crypt.decryptInternal(cipherText) } returns decryptedText

        val result = cryptHandler.decrypt(cipherText)

        assertEquals(decryptedText, result)
    }

    @Test
    fun updateMigrationFailureCount() {
        val migrationSuccessful = true

        cryptHandler.updateMigrationFailureCount(migrationSuccessful)

        verify { repository.updateMigrationFailureCount(migrationSuccessful) }
    }

    @Test
    fun encrypt_alreadyEncryptedText() {
        val encryptedText = "${Constants.AES_PREFIX}testEncryptedText${Constants.AES_SUFFIX}"
        val key = Constants.KEY_ENCRYPTION_EMAIL

        val result = cryptHandler.encrypt(encryptedText, key)

        assertEquals(encryptedText, result) // Should return the same encrypted text
    }

    @Test
    fun decrypt_notEncryptedText() {
        val plainText = "testPlainText"
        val key = Constants.KEY_ENCRYPTION_EMAIL

        val result = cryptHandler.decrypt(plainText, key)

        assertEquals(plainText, result) // Should return the same plain text
    }

    @Test
    fun isTextAESEncrypted_validAESEncryptedText() {
        val aesEncryptedText = "${Constants.AES_PREFIX}testEncryptedText${Constants.AES_SUFFIX}"

        assertTrue(CryptHandler.isTextAESEncrypted(aesEncryptedText))
    }

    @Test
    fun isTextAESEncrypted_invalidAESEncryptedText_missingPrefix() {
        val invalidAesEncryptedText = "testEncryptedText${Constants.AES_SUFFIX}"

        assertFalse(CryptHandler.isTextAESEncrypted(invalidAesEncryptedText))
    }

    @Test
    fun isTextAESEncrypted_invalidAESEncryptedText_missingSuffix() {
        val invalidAesEncryptedText = "${Constants.AES_PREFIX}testEncryptedText"

        assertFalse(CryptHandler.isTextAESEncrypted(invalidAesEncryptedText))
    }

    @Test
    fun isTextAESGCMEncrypted_validAESGCMEncryptedText() {
        val aesGcmEncryptedText = "${Constants.AES_GCM_PREFIX}testEncryptedText${Constants.AES_GCM_SUFFIX}"

        assertTrue(CryptHandler.isTextAESGCMEncrypted(aesGcmEncryptedText))
    }

    @Test
    fun isTextAESGCMEncrypted_invalidAESGCMEncryptedText_missingPrefix() {
        val invalidAesGcmEncryptedText = "testEncryptedText${Constants.AES_GCM_SUFFIX}"

        assertFalse(CryptHandler.isTextAESGCMEncrypted(invalidAesGcmEncryptedText))
    }

    @Test
    fun isTextAESGCMEncrypted_invalidAESGCMEncryptedText_missingSuffix() {
        val invalidAesGcmEncryptedText = "${Constants.AES_GCM_PREFIX}testEncryptedText"

        assertFalse(CryptHandler.isTextAESGCMEncrypted(invalidAesGcmEncryptedText))
    }

}

