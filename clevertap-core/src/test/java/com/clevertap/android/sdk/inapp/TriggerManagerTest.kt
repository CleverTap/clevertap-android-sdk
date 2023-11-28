package com.clevertap.android.sdk.inapp

import android.content.Context
import android.content.SharedPreferences
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.mockito.*
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*

class TriggerManagerTest : BaseTestCase() {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    @Mock
    private lateinit var deviceInfo: DeviceInfo

    private lateinit var triggerManager: TriggerManager

    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)

        triggerManager = TriggerManager(context, "123456", deviceInfo)

        // Mock the sharedPrefs method to return the SharedPreferences instance
        `when`(triggerManager.sharedPrefs()).thenReturn(sharedPreferences)

        // Mock the SharedPreferences.Editor
        `when`(sharedPreferences.edit()).thenReturn(editor)
    }

    @Test
    fun testGetTriggers_WhenNoTriggersStored_ShouldReturnZero() {
        val campaignId = "campaign123"
        val storageKey = triggerManager.getTriggersKey(campaignId)
        // Mock the behavior for SharedPreferences.getInt to return 0 when no triggers are stored
        `when`(sharedPreferences.getInt(storageKey, 0)).thenReturn(0)

        val result = triggerManager.getTriggers(campaignId)

        // Verify that the expected methods were called when getting triggers
        verify(sharedPreferences).getInt(storageKey, 0)

        // Verify the result
        assert(result == 0)
    }

    @Test
    fun testGetTriggers_WhenTriggersStored_ShouldReturnStoredValue() {
        val campaignId = "campaign123"
        val storedTriggers = 5

        // Mock the behavior for SharedPreferences.getInt to return storedTriggers when triggers are stored
        `when`(sharedPreferences.getInt(triggerManager.getTriggersKey(campaignId), 0)).thenReturn(
            storedTriggers
        )

        val result = triggerManager.getTriggers(campaignId)

        // Verify that the expected methods were called when getting triggers
        verify(sharedPreferences).getInt(triggerManager.getTriggersKey(campaignId), 0)

        // Verify the result
        assert(result == storedTriggers)
    }

    @Test
    fun testIncrement_WhenTriggersExist_ShouldIncrementTriggers() {
        val campaignId = "campaign123"
        val storedTriggers = 5

        // Mock the editor's putInt method
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)

        // Mock the behavior for SharedPreferences.getInt to return storedTriggers initially
        `when`(sharedPreferences.getInt(triggerManager.getTriggersKey(campaignId), 0)).thenReturn(
            storedTriggers
        )

        triggerManager.increment(campaignId)

        // Verify that the expected methods were called when incrementing triggers
        verify(sharedPreferences).getInt(triggerManager.getTriggersKey(campaignId), 0)
        verify(editor).putInt(triggerManager.getTriggersKey(campaignId), storedTriggers + 1)
        verify(editor).apply()
    }

    @Test
    fun testIncrement_WhenNoTriggersExist_ShouldSetTriggersTo1() {
        val campaignId = "campaign123"

        // Mock the editor's putInt method
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)

        // Mock the behavior for SharedPreferences.getInt to return 0 initially (no triggers exist)
        `when`(sharedPreferences.getInt(triggerManager.getTriggersKey(campaignId), 0)).thenReturn(0)

        triggerManager.increment(campaignId)

        // Verify that the expected methods were called when incrementing triggers
        verify(sharedPreferences).getInt(triggerManager.getTriggersKey(campaignId), 0)
        verify(editor).putInt(triggerManager.getTriggersKey(campaignId), 1)
        verify(editor).apply()
    }

    @Test
    fun testRemoveTriggers_WhenTriggersExist_ShouldRemoveTriggers() {
        val campaignId = "campaign123"

        // Mock the editor's remove method
        `when`(editor.remove(anyString())).thenReturn(editor)

        triggerManager.removeTriggers(campaignId)

        // Verify that the expected methods were called when removing triggers
        verify(editor).remove(triggerManager.getTriggersKey(campaignId))
        verify(editor).apply()
    }
}