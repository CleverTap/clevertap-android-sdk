package com.clevertap.android.sdk.inapp

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ImpressionStoreTest : BaseTestCase() {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var impressionStore: ImpressionStore

    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)

        // Initialize ImpressionStore with the mocked context
        impressionStore = ImpressionStore(context, "accountId", "deviceId")
    }

    @Test
    fun testRead_WhenKeyExists_ShouldReturnListOfTimestamps() {
        val campaignId = "campaign123"
        val serializedData = "12345,67890" // Serialized data for testing

        // Mock the sharedPrefs method to return the SharedPreferences instance
        `when`(impressionStore.sharedPrefs()).thenReturn(sharedPreferences)

        // Mock SharedPreferences to return serializedData when getString is called
        `when`(
            sharedPreferences.getString(
                "${ImpressionStore.PREF_PREFIX}_$campaignId", ""
            )
        ).thenReturn(serializedData)

        // Call the read method
        val result = impressionStore.read(campaignId)

        // Assert that the result matches the expected list
        assertEquals(listOf(12345L, 67890L), result)
    }

    @Test
    fun testRead_WhenKeyDoesNotExist_ShouldReturnEmptyList() {
        val campaignId = "campaign456"

        // Mock sharedPrefs to return the SharedPreferences instance
        `when`(impressionStore.sharedPrefs()).thenReturn(sharedPreferences)

        // Mock SharedPreferences to return an empty string when getString is called
        `when`(
            sharedPreferences.getString(
                "${ImpressionStore.PREF_PREFIX}_$campaignId", ""
            )
        ).thenReturn("")

        // Call the read method
        val result = impressionStore.read(campaignId)

        // Assert that the result is an empty list
        assertEquals(emptyList(), result)
    }

    @Test
    fun testWrite_WhenKeyExists_ShouldUpdateListOfTimestamps() {
        val campaignId = "campaign789"
        val timestamp = 123456789L
        val serializedData = "98765,123456789" // Serialized data for testing

        // Mock sharedPrefs to return the SharedPreferences instance
        `when`(impressionStore.sharedPrefs()).thenReturn(sharedPreferences)

        // Mock SharedPreferences to return serializedData when getString is called
        `when`(
            sharedPreferences.getString(
                "${ImpressionStore.PREF_PREFIX}_$campaignId", ""
            )
        ).thenReturn(serializedData)

        // Mock the SharedPreferences.Editor
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(sharedPreferences.edit()).thenReturn(editor)

        // Mock the editor's putString method
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)

        // Call the write method
        impressionStore.write(campaignId, timestamp)

        // Verify that SharedPreferences is updated with the new serialized data
        verify(editor).putString(
            "${ImpressionStore.PREF_PREFIX}_$campaignId", "98765,123456789,123456789"
        )

        // Verify that apply method is called on the editor
        verify(editor).apply()
    }

    @Test
    fun testWrite_WhenKeyDoesNotExist_ShouldCreateNewListOfTimestamps() {
        val campaignId = "campaign1011"
        val timestamp = 123456789L

        // Mock sharedPrefs to return the SharedPreferences instance
        `when`(impressionStore.sharedPrefs()).thenReturn(sharedPreferences)

        // Mock SharedPreferences to return an empty string when getString is called
        `when`(
            sharedPreferences.getString(
                "${ImpressionStore.PREF_PREFIX}_$campaignId", ""
            )
        ).thenReturn("")

        // Mock the SharedPreferences.Editor
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(sharedPreferences.edit()).thenReturn(editor)

        // Mock the editor's putString method
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)

        // Call the write method
        impressionStore.write(campaignId, timestamp)

        // Verify that SharedPreferences is updated with the new serialized data
        verify(editor).putString(
            "${ImpressionStore.PREF_PREFIX}_$campaignId", "123456789"
        )

        // Verify that apply method is called on the editor
        verify(editor).apply()
    }

    @Test
    fun testClear_ShouldRemoveKeyFromSharedPreferences() {
        val campaignId = "campaign1011"

        // Mock sharedPrefs to return the SharedPreferences instance
        `when`(impressionStore.sharedPrefs()).thenReturn(sharedPreferences)

        // Mock the SharedPreferences.Editor
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(sharedPreferences.edit()).thenReturn(editor)

        // Mock the editor's remove method
        `when`(editor.remove(anyString())).thenReturn(editor)

        // Call the clear method
        impressionStore.clear(campaignId)

        // Verify that apply method is called on the editor
        verify(editor).apply()
    }

    @Test
    fun testSharedPrefs_WhenContextIsNonNull_ShouldReturnNonNull() {
        // Mock StorageHelper.getPreferences to return sharedPreferences
        `when`(StorageHelper.getPreferences(context, impressionStore.prefName)).thenReturn(
            sharedPreferences
        )

        // Call the sharedPrefs method
        val result = impressionStore.sharedPrefs()

        // Assert that the result is not null
        assertNotNull(result)
    }
}