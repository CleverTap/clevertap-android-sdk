package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.cryption.EncryptionLevel
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DBEncryptionHandlerTest {

    internal val cryptHandler = mockk<CryptHandler>(relaxed = true)
    internal val logger = TestLogger()
    lateinit var dbEncryptionHandler: DBEncryptionHandler

    private fun initHandler(level: EncryptionLevel = EncryptionLevel.NONE) {
        dbEncryptionHandler = DBEncryptionHandler(cryptHandler, logger, level)
    }

    @After
    fun tearDown() {
        confirmVerified(cryptHandler)
        clearMocks(cryptHandler)
    }

    @Test
    fun `unwrapDbData - null data does not call decryptSafe`() {
        val nullData: String? = null

        initHandler(EncryptionLevel.NONE)
        dbEncryptionHandler.unwrapDbData(data = nullData)
        verify(exactly = 0) { cryptHandler.decryptSafe(any()) }

        initHandler(EncryptionLevel.MEDIUM)
        dbEncryptionHandler.unwrapDbData(data = nullData)
        verify(exactly = 0) { cryptHandler.decryptSafe(any()) }

        initHandler(EncryptionLevel.FULL_DATA)
        dbEncryptionHandler.unwrapDbData(data = nullData)
        verify(exactly = 0) { cryptHandler.decryptSafe(any()) }
    }

    @Test
    fun `unwrapDbData - level NONE - non null data calls decryptSafe method from CryptHandler`() {
        val plainText = "Some text"
        every { cryptHandler.decryptSafe(any()) } returns plainText

        initHandler(EncryptionLevel.NONE)
        val output1 = dbEncryptionHandler.unwrapDbData(data = plainText)
        assertEquals(plainText, output1)
        verify(exactly = 1) { cryptHandler.decryptSafe(any()) }
    }

    @Test
    fun `unwrapDbData - level MEDIUM - non null data calls decryptSafe method from CryptHandler`() {
        val inputText = "Some text"
        every { cryptHandler.decryptSafe(any()) } returns inputText
        initHandler(EncryptionLevel.MEDIUM)
        val output2 = dbEncryptionHandler.unwrapDbData(data = inputText)
        assertEquals(inputText, output2)
        verify(exactly = 1) { cryptHandler.decryptSafe(inputText) }
    }

    @Test
    fun `unwrapDbData - level FULL_DATA - non null data calls decryptSafe method from CryptHandler`() {
        val inputText = "Some text"
        every { cryptHandler.decryptSafe(any()) } returns inputText
        initHandler(EncryptionLevel.FULL_DATA)
        val output3 = dbEncryptionHandler.unwrapDbData(data = inputText)
        assertEquals(inputText, output3)
        verify(exactly = 1) { cryptHandler.decryptSafe(inputText) }
    }

    @Test
    fun `wrapDbData - wraps data with encryption only for encryptionLevel = FULL_DATA`() {
        val plainText = "Some text"
        val encryptedText = "${Constants.AES_GCM_PREFIX}some-encrypted-text${Constants.AES_GCM_SUFFIX}"
        every { cryptHandler.encryptSafe(any()) } returns encryptedText

        // Verify there is no encryption done for level NONE
        initHandler(EncryptionLevel.NONE)
        val op1 = dbEncryptionHandler.wrapDbData(plainText)
        assertEquals(op1, plainText)
        verify(exactly = 0) { cryptHandler.encryptSafe(any()) }

        // Verify there is no encryption done for level MEDIUM
        initHandler(EncryptionLevel.MEDIUM)
        val op2 = dbEncryptionHandler.wrapDbData(plainText)
        assertEquals(op2, plainText)
        verify(exactly = 0) { cryptHandler.encryptSafe(any()) }

        // Verify there is encryption done for level FULL_DATA
        initHandler(EncryptionLevel.FULL_DATA)
        val op3 = dbEncryptionHandler.wrapDbData(plainText)
        assertEquals(op3, encryptedText)
        verify(exactly = 1) { cryptHandler.encryptSafe(plainText) }
    }

    @Test
    fun `isInCorrectEncryptionFormat returns right value for different encryption levels`() {
        val plainText = "plainTextData"
        val encryptedText = "${Constants.AES_GCM_PREFIX}some-encrypted-text${Constants.AES_GCM_SUFFIX}"

        initHandler(EncryptionLevel.NONE)
        assertTrue(dbEncryptionHandler.isInCorrectEncryptionFormat(plainText))
        assertFalse(dbEncryptionHandler.isInCorrectEncryptionFormat(encryptedText))

        initHandler(EncryptionLevel.MEDIUM)
        assertTrue(dbEncryptionHandler.isInCorrectEncryptionFormat(plainText))
        assertFalse(dbEncryptionHandler.isInCorrectEncryptionFormat(encryptedText))

        initHandler(EncryptionLevel.FULL_DATA)
        assertFalse(dbEncryptionHandler.isInCorrectEncryptionFormat(plainText))
        assertTrue(dbEncryptionHandler.isInCorrectEncryptionFormat(encryptedText))
    }
}