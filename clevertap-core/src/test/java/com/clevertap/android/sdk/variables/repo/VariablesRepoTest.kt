package com.clevertap.android.sdk.variables.repo

import com.clevertap.android.sdk.db.DBEncryptionHandler
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class VariablesRepoTest {

    internal lateinit var variablesRepo: VariablesRepo
    internal lateinit var dbEncryptionHandler: DBEncryptionHandler

    @Before
    fun setup() {
        dbEncryptionHandler = mockk()
        variablesRepo = VariablesRepo(
            context = mockk(relaxed = true),
            accountId = "Account-id",
            dbEncryptionHandler = dbEncryptionHandler
        )
    }

    @After
    fun tearDown() {
        confirmVerified(dbEncryptionHandler)
        clearMocks(dbEncryptionHandler)
    }

    @Test
    fun `storeDataInCache - verify correct method calls`() {
        val inputData = "some-input-json-for-storing-vars"
        val encryptedData = "some-encrypted-data"
        every { dbEncryptionHandler.wrapDbData(data = any()) } returns encryptedData
        every { dbEncryptionHandler.unwrapDbData(data = any()) } returns inputData

        // Act
        variablesRepo.storeDataInCache(inputData)
        verify { dbEncryptionHandler.wrapDbData(data = inputData) }

        val loadedData = variablesRepo.loadDataFromCache()
        assertEquals(expected = inputData, actual = loadedData)
        verify { dbEncryptionHandler.unwrapDbData(data = any()) }
    }

    @Test
    fun `loadDataFromCache - verify correct method calls`() {
        val returnData = "some-decrypted-vars-data"
        every { dbEncryptionHandler.unwrapDbData(any()) } returns returnData

        // Act
        val varsCachedData = variablesRepo.loadDataFromCache()
        verify { dbEncryptionHandler.unwrapDbData(data = any()) }
        assertEquals(expected = returnData, actual = varsCachedData)
    }

    @Test
    fun `storeVariantsInCache - verify correct method calls`() {
        val inputData = "some-input-json-for-storing-vars"
        val encryptedData = "some-encrypted-data"
        every { dbEncryptionHandler.wrapDbData(data = any()) } returns encryptedData
        every { dbEncryptionHandler.unwrapDbData(data = any()) } returns inputData

        // Act
        variablesRepo.storeVariantsInCache(inputData)
        verify { dbEncryptionHandler.wrapDbData(data = inputData) }

        val loadedData = variablesRepo.loadDataFromCache()
        verify { dbEncryptionHandler.unwrapDbData(data = any()) }
        assertEquals(expected = inputData, actual = loadedData)
    }

    @Test
    fun `loadVariantsFromCache - verify correct method calls`() {
        val returnData = "some-decrypted-vars-data"
        every { dbEncryptionHandler.unwrapDbData(any()) } returns returnData

        // Act
        val abVariantsCachedData = variablesRepo.loadVariantsFromCache()
        verify { dbEncryptionHandler.unwrapDbData(data = any()) }
        assertEquals(expected = returnData, actual = abVariantsCachedData)
    }
}