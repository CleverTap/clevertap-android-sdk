package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.store.preference.ICTPreference
import io.mockk.*
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImpressionStoreTest {

    private lateinit var ctPreference: ICTPreference
    private lateinit var impressionStore: ImpressionStore

    @Before
    fun setUp() {
        ctPreference = mockk(relaxed = true)
        impressionStore = ImpressionStore(ctPreference)
    }

    @Test
    fun `read returns empty list when no impressions are stored`() {
        // Arrange
        every { ctPreference.readString(any(), any()) } returns ""

        // Act
        val result = impressionStore.read("campaign123")

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `read returns list of impressions`() {
        // Arrange
        every { ctPreference.readString(any(), any()) } returns "123,456,789"

        // Act
        val result = impressionStore.read("campaign456")

        // Assert
        assertEquals(listOf(123L, 456L, 789L), result)
    }

    @Test
    fun `read returns list of impressions with custom delimiter`() {
        // Arrange
        every { ctPreference.readString(any(), any()) } returns "123;456;789"

        // Act
        val result = impressionStore.read("campaign456")

        // Assert
        assertEquals(listOf(), result)
    }

    @Test
    fun `write adds timestamp to the list of impressions`() {
        // Arrange
        every { ctPreference.readString(any(), any()) } returns "123,456"
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        impressionStore.write("campaign789", 987L)

        // Assert
        verify { ctPreference.writeString("__impressions_campaign789", "123,456,987") }
    }

    @Test
    fun `write adds timestamp to the list of empty impressions`() {
        // Arrange
        every { ctPreference.readString(any(), any()) } returns ""
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        impressionStore.write("campaign789", 987L)

        // Assert
        verify { ctPreference.writeString("__impressions_campaign789", "987") }
    }

    @Test
    fun `clear removes impressions for a campaign`() {
        // Arrange
        every { ctPreference.remove(any()) } just Runs

        // Act
        impressionStore.clear("campaign123")

        // Assert
        verify { ctPreference.remove("__impressions_campaign123") }
    }

    @Test
    fun `onChangeUser updates preference name in ctPreference`() {
        // Arrange
        val newPrefName = "${Constants.KEY_COUNTS_PER_INAPP}:deviceId123:accountId456"
        every { ctPreference.changePreferenceName(any()) } just Runs

        // Act
        impressionStore.onChangeUser("deviceId123", "accountId456")

        // Assert
        verify { ctPreference.changePreferenceName(newPrefName) }
    }
}