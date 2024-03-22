package com.clevertap.android.sdk.inapp

import android.content.Context
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.STORE_TYPE_IMPRESSION
import com.clevertap.android.sdk.STORE_TYPE_INAPP
import com.clevertap.android.sdk.STORE_TYPE_LEGACY_INAPP
import com.clevertap.android.sdk.StoreProvider
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StoreProviderTest {

    private lateinit var mockContext: Context
    private lateinit var mockCryptHandler: CryptHandler
    private lateinit var mockDeviceInfo: DeviceInfo
    private lateinit var storeProvider: StoreProvider

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockCryptHandler = mockk()
        mockDeviceInfo = mockk(relaxed = true)
        storeProvider = spyk(StoreProvider.getInstance())
    }

    @Test
    fun `provideInAppStore should create InAppStore with correct preferences`() {
        // Arrange
        val accountId = "testAccountId"
        val prefName = "${Constants.INAPP_KEY}:${mockDeviceInfo.deviceID}:$accountId"
        every { storeProvider.constructStorePreferenceName(STORE_TYPE_INAPP, mockDeviceInfo.deviceID, accountId) } returns prefName

        // Act
        val inAppStore = storeProvider.provideInAppStore(mockContext, mockCryptHandler, mockDeviceInfo.deviceID, accountId)

        // Assert
        verify { storeProvider.getCTPreference(mockContext, prefName) }
        assertEquals(InAppStore::class.java, inAppStore.javaClass)
    }

    @Test
    fun `provideImpressionStore should create ImpressionStore with correct preferences`() {
        // Arrange
        val accountId = "testAccountId"
        val prefName = "${Constants.KEY_COUNTS_PER_INAPP}:${mockDeviceInfo.deviceID}:$accountId"
        every { storeProvider.constructStorePreferenceName(STORE_TYPE_IMPRESSION, mockDeviceInfo.deviceID, accountId) } returns prefName

        // Act
        val impressionStore = storeProvider.provideImpressionStore(mockContext, mockDeviceInfo.deviceID, accountId)

        // Assert
        verify { storeProvider.getCTPreference(mockContext, prefName) }
        assertEquals(ImpressionStore::class.java, impressionStore.javaClass)
    }

    @Test
    fun `provideLegacyInAppStore should create LegacyInAppStore with correct preferences`() {
        // Arrange
        val accountId = "testAccountId"
        val prefName = Constants.CLEVERTAP_STORAGE_TAG
        every { storeProvider.constructStorePreferenceName(STORE_TYPE_LEGACY_INAPP) } returns prefName

        // Act
        val legacyInAppStore = storeProvider.provideLegacyInAppStore(mockContext, accountId)

        // Assert
        verify { storeProvider.getCTPreference(mockContext, prefName) }
        assertEquals(LegacyInAppStore::class.java, legacyInAppStore.javaClass)
    }

    @Test
    fun `constructStorePreferenceName should construct correct preference name for InApp`() {
        // Act
        val prefName = storeProvider.constructStorePreferenceName(STORE_TYPE_INAPP, "deviceId", "accountId")

        // Assert
        assertEquals("${Constants.INAPP_KEY}:deviceId:accountId", prefName)
    }

    @Test
    fun `constructStorePreferenceName should construct correct preference name for Impression`() {
        // Act
        val prefName = storeProvider.constructStorePreferenceName(STORE_TYPE_IMPRESSION, "deviceId", "accountId")

        // Assert
        assertEquals("${Constants.KEY_COUNTS_PER_INAPP}:deviceId:accountId", prefName)
    }

    @Test
    fun `constructStorePreferenceName should construct correct preference name for LegacyInApp`() {
        // Act
        val prefName = storeProvider.constructStorePreferenceName(STORE_TYPE_LEGACY_INAPP)

        // Assert
        assertEquals(Constants.CLEVERTAP_STORAGE_TAG, prefName)
    }
}
