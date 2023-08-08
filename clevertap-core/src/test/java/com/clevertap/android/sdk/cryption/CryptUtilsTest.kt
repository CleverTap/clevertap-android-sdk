package com.clevertap.android.sdk.cryption


import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.cryption.CryptUtils.migrateEncryptionLevel
import com.clevertap.android.sdk.cryption.CryptUtils.updateEncryptionFlagOnFailure
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals


class CryptUtilsTest : BaseTestCase() {

    @Mock
    private lateinit var mockCryptHandler: CryptHandler

    @Mock
    private lateinit var mockDBAdapter: DBAdapter


    private lateinit var config: CleverTapInstanceConfig


    @Before
    override fun setUp() {
        super.setUp()
        config = CleverTapInstanceConfig.createInstance(application, "id", "token")
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `test updateEncryptionFlagOnFailure `() {
        val failedFlag = Constants.ENCRYPTION_FLAG_DB_SUCCESS
        val initialEncryptionFlagStatus = 7
        val updatedEncryptionFlag = 3

        `when`(mockCryptHandler.encryptionFlagStatus).thenReturn(initialEncryptionFlagStatus)
        updateEncryptionFlagOnFailure(appCtx, config, failedFlag, mockCryptHandler)

        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)
        verify(mockCryptHandler).encryptionFlagStatus = updatedEncryptionFlag
        assertEquals(updatedEncryptionFlag, sharedPreferences.getInt("encryptionFlagStatus:id", -1))
    }

    @Test
    fun `test migrateEncryptionLevel when encryption level is not present in prefs`() {
        config.setEncryptionLevel(CryptHandler.EncryptionLevel.NONE)
        StorageHelper.putInt(
            application,
            StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_LEVEL),
            -1
        )
        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)
        verifyNoMoreInteractions(mockCryptHandler, mockDBAdapter)
    }

    @Test
    fun `test migrateEncryptionLevel when config encryption level is 1 and stored encryption level is 0`() {
        config.setEncryptionLevel(CryptHandler.EncryptionLevel.MEDIUM)
        StorageHelper.putInt(
            application,
            StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_LEVEL),
            0
        )

        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)
        verify(mockCryptHandler).encryptionFlagStatus = 7
    }

    @Test
    fun `test migrateEncryptionLevel when config encryption level and stored encryption level are equal`() {
        config.setEncryptionLevel(CryptHandler.EncryptionLevel.MEDIUM)
        StorageHelper.putInt(
            application,
            StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_LEVEL),
            1
        )


        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)

        verify(mockCryptHandler).encryptionFlagStatus = 7
        verify(mockDBAdapter).fetchUserProfileById("id")
    }

    @Test
    fun `test migrateEncryptionLevel when config encryption level and stored encryption level are equal and flagStatus is 7`() {
        config.setEncryptionLevel(CryptHandler.EncryptionLevel.MEDIUM)
        StorageHelper.putInt(
            application,
            StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_LEVEL),
            1
        )
        StorageHelper.putInt(
            application,
            StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_FLAG_STATUS),
            7
        )

        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)

        verify(mockCryptHandler).encryptionFlagStatus = 7
        verifyNoMoreInteractions(mockDBAdapter, mockCryptHandler)
    }

}

