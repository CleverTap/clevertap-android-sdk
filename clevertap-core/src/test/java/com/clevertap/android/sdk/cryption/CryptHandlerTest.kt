package com.clevertap.android.sdk.cryption

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.cryption.CryptHandler.EncryptionAlgorithm
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CryptHandlerTest {

    @MockK(relaxed = true)
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
            repository = repository,
            cryptFactory = cryptFactory
        )
        every { cryptFactory.getCryptInstance(any()) } returns crypt
    }

    @After
    fun teardown() {
        confirmVerified(repository, cryptFactory, crypt)
    }

    @Test
    fun `encryptSafe - plain text with AES_GCM algorithm - returns encrypted text`() {
        val plainText = "testPlainText"
        val encryptedText = "${Constants.AES_GCM_PREFIX}encryptedText${Constants.AES_GCM_SUFFIX}"

        every { crypt.encryptInternal(plainText) } returns encryptedText

        val result = cryptHandler.encryptSafe(plainText)

        assertEquals(encryptedText, result)
        verify { cryptFactory.getCryptInstance(CryptHandler.EncryptionAlgorithm.AES_GCM) }
        verify { crypt.encryptInternal(plainText) }
    }

    @Test
    @Ignore("We have made sure to never call this method on AES legacy encrypted text, so this use case is not needed")
    fun `encryptSafe - already AES encrypted text - returns same text without re-encryption`() {
        val encryptedText = "${Constants.AES_PREFIX}alreadyEncrypted${Constants.AES_SUFFIX}"

        val result = cryptHandler.encryptSafe(encryptedText)

        assertEquals(encryptedText, result)
        verify(exactly = 0) { crypt.encryptInternal(any()) }
    }

    @Test
    fun `encryptSafe - already AES_GCM encrypted text - returns same text without re-encryption`() {
        val encryptedText = "${Constants.AES_GCM_PREFIX}alreadyEncrypted${Constants.AES_GCM_SUFFIX}"

        val result = cryptHandler.encryptSafe(encryptedText)

        assertEquals(encryptedText, result)
        verify(exactly = 0) { crypt.encryptInternal(any()) }
    }

    @Test
    fun `encryptSafe - default algorithm uses AES_GCM`() {
        val plainText = "testPlainText"
        val encryptedText = "${Constants.AES_GCM_PREFIX}encryptedText${Constants.AES_GCM_SUFFIX}"

        every { crypt.encryptInternal(plainText) } returns encryptedText

        val result = cryptHandler.encryptSafe(plainText) // No algorithm specified

        assertEquals(encryptedText, result)
        verify { cryptFactory.getCryptInstance(CryptHandler.EncryptionAlgorithm.AES_GCM) }
        verify { crypt.encryptInternal(plainText) }
    }

    @Test
    fun `encryptSafe - encryption fails - returns null`() {
        val plainText = "testPlainText"

        every { crypt.encryptInternal(plainText) } returns null

        val result = cryptHandler.encryptSafe(plainText)

        assertEquals(null, result)
        verify { cryptFactory.getCryptInstance(CryptHandler.EncryptionAlgorithm.AES_GCM) }
        verify { crypt.encryptInternal(plainText) }
    }

    @Test
    fun `decryptWithAlgorithm - AES encrypted text - returns decrypted text`() {
        val cipherText = "${Constants.AES_PREFIX}encryptedText${Constants.AES_SUFFIX}"
        val decryptedText = "decryptedText"

        every { crypt.decryptInternal(cipherText) } returns decryptedText

        val result = cryptHandler.decryptWithAlgorithm(cipherText, CryptHandler.EncryptionAlgorithm.AES)

        assertEquals(decryptedText, result)
        verify { cryptFactory.getCryptInstance(CryptHandler.EncryptionAlgorithm.AES) }
        verify { crypt.decryptInternal(cipherText) }
    }

    @Test
    fun `decryptSafe - AES_GCM encrypted text - returns decrypted text`() {
        val cipherText = "${Constants.AES_GCM_PREFIX}encryptedText${Constants.AES_GCM_SUFFIX}"
        val decryptedText = "decryptedText"

        every { crypt.decryptInternal(cipherText) } returns decryptedText

        val result = cryptHandler.decryptSafe(cipherText)

        assertEquals(decryptedText, result)
        verify { cryptFactory.getCryptInstance(CryptHandler.EncryptionAlgorithm.AES_GCM) }
        verify { crypt.decryptInternal(cipherText) }
    }

    @Test
    fun `decryptSafe - plain text - returns same text without decryption`() {
        val plainText = "plainTextNotEncrypted"

        val result = cryptHandler.decryptSafe(plainText)

        assertEquals(plainText, result)
        verify(exactly = 0) { crypt.decryptInternal(any()) }
    }

    @Test
    fun `decryptSafe - default algorithm uses AES_GCM`() {
        val cipherText = "${Constants.AES_GCM_PREFIX}encryptedText${Constants.AES_GCM_SUFFIX}"
        val decryptedText = "decryptedText"

        every { crypt.decryptInternal(cipherText) } returns decryptedText

        val result = cryptHandler.decryptSafe(cipherText) // No algorithm specified

        assertEquals(decryptedText, result)
        verify { cryptFactory.getCryptInstance(CryptHandler.EncryptionAlgorithm.AES_GCM) }
        verify { crypt.decryptInternal(cipherText) }
    }

    @Test
    fun `decryptSafe - decryption fails - returns null`() {
        val cipherText = "${Constants.AES_GCM_PREFIX}encryptedText${Constants.AES_GCM_SUFFIX}"

        every { crypt.decryptInternal(cipherText) } returns null

        val result = cryptHandler.decryptSafe(cipherText)

        assertEquals(null, result)
        verify { cryptFactory.getCryptInstance(CryptHandler.EncryptionAlgorithm.AES_GCM) }
        verify { crypt.decryptInternal(cipherText) }
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
    fun updateMigrationFailureCount() {
        val migrationSuccessful = true

        cryptHandler.updateMigrationFailureCount(migrationSuccessful)

        verify { repository.updateMigrationFailureCount(migrationSuccessful) }
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

    @Test
    fun encryptSafe_defaultValues_checkCorrectEncryptionTypes() {
        val aesGcmEncryptedText = "${Constants.AES_GCM_PREFIX}testEncryptedText${Constants.AES_GCM_SUFFIX}"

        mockkObject(CryptHandler.Companion)

        cryptHandler.encryptSafe(aesGcmEncryptedText)
        verify { CryptHandler.isTextAESGCMEncrypted(aesGcmEncryptedText) }
        verify(exactly = 0) { cryptFactory.getCryptInstance(EncryptionAlgorithm.AES_GCM) }
        verify(exactly = 0) { crypt.encryptInternal(aesGcmEncryptedText) }

        unmockkObject(CryptHandler.Companion)
    }

    @Test
    fun decryptSafe_defaultValues_checkCorrectEncryptionTypes() {
        val testPlainText = "testPlainText"

        mockkObject(CryptHandler.Companion)

        cryptHandler.decryptSafe(testPlainText)
        verify { CryptHandler.isTextAESGCMEncrypted(testPlainText) }
        verify(exactly = 0) { cryptFactory.getCryptInstance(EncryptionAlgorithm.AES_GCM) }
        verify(exactly = 0) { crypt.decryptInternal(testPlainText) }

        unmockkObject(CryptHandler.Companion)
    }

}

