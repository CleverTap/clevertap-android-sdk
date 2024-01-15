package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.store.preference.ICTPreference
import io.mockk.*
import org.json.JSONArray
import org.junit.*
import kotlin.test.assertEquals

class LegacyInAppStoreTest {

    private lateinit var ctPreference: ICTPreference
    private lateinit var legacyInAppStore: LegacyInAppStore
    private lateinit var inAppKey: String

    @Before
    fun setUp() {
        ctPreference = mockk(relaxed = true)
        legacyInAppStore = LegacyInAppStore(ctPreference, "accountId123")
        inAppKey = "${Constants.INAPP_KEY}:accountId123"
    }

    @Test
    fun `storeInApps writes JSONArray to ctPreference`() {
        // Arrange
        val inApps = JSONArray("[{\"id\":1},{\"id\":2}]")
        every { ctPreference.writeStringImmediate(inAppKey, any()) } just Runs

        // Act
        legacyInAppStore.storeInApps(inApps)

        // Assert
        verify { ctPreference.writeStringImmediate(inAppKey, "[{\"id\":1},{\"id\":2}]") }
    }

    @Test
    fun `readInApps returns JSONArray from ctPreference`() {
        // Arrange
        val storedInApps = "[{\"id\":3},{\"id\":4}]"
        every { ctPreference.readString(inAppKey, any()) } returns storedInApps

        // Act
        val result = legacyInAppStore.readInApps()

        // Assert
        assertEquals(storedInApps, result.toString())
    }

    @Test
    fun `readInApps returns empty JSONArray when ctPreference returns empty string`() {
        // Arrange
        every { ctPreference.readString(inAppKey, any()) } returns ""

        // Act
        val result = legacyInAppStore.readInApps()

        // Assert
        assertEquals(JSONArray(), result)
        assertEquals(0, result.length())
    }

    @Test
    fun `readInApps returns empty JSONArray when inAppKey not present`() {
        // Arrange
        every { ctPreference.readString(inAppKey, any()) } returns "[]"

        // Act
        val result = legacyInAppStore.readInApps()

        // Assert
        assertEquals(JSONArray(), result)
        assertEquals(0, result.length())
    }

    @Test
    fun `readInApps returns empty JSONArray when ctPreference returns malformed json`() {
        // Arrange
        every { ctPreference.readString(inAppKey, any()) } returns "[}{\"id\":3},{\"id\":4}{]"

        // Act
        val result = legacyInAppStore.readInApps()

        // Assert
        assertEquals(JSONArray(), result)
        assertEquals(0, result.length())
    }

    @Test
    fun `removeInApps removes entry from ctPreference`() {
        // Arrange
        every { ctPreference.remove(any()) } just Runs

        // Act
        legacyInAppStore.removeInApps()

        // Assert
        verify { ctPreference.remove(inAppKey) }
    }
}