package com.clevertap.android.sdk.inapp.matchers

import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LimitsMatcherTest : BaseTestCase() {

    private lateinit var impressionManager: ImpressionManager
    private lateinit var triggerManager: TriggerManager
    private lateinit var limitsMatcher: LimitsMatcher

    @Before
    override fun setUp() {
        impressionManager = mock(ImpressionManager::class.java)
        triggerManager = mock(TriggerManager::class.java)
        limitsMatcher = LimitsMatcher(impressionManager, triggerManager)
    }

    @Test
    fun `matchWhenLimits should return true when all limits are met`() {
        // Define your sample JSON limits here
        val jsonLimits = listOf(
            JSONObject(mapOf("type" to "session", "limit" to 5)),
            JSONObject(mapOf("type" to "minutes", "limit" to 10, "frequency" to 2))
        )

        // Mock the behavior of the impressionManager
        `when`(impressionManager.perSession("campaign123")).thenReturn(4)
        `when`(impressionManager.perMinute("campaign123", 2)).thenReturn(9)

        val result = limitsMatcher.matchWhenLimits(jsonLimits, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when any limit is not met`() {
        // Define your sample JSON limits here
        val jsonLimits = listOf(
            JSONObject(mapOf("type" to "session", "limit" to 5)),
            JSONObject(mapOf("type" to "minutes", "limit" to 10, "frequency" to 2))
        )

        // Mock the behavior of the impressionManager
        `when`(impressionManager.perSession("campaign123")).thenReturn(6)
        `when`(impressionManager.perMinute("campaign123", 2)).thenReturn(9)

        val result = limitsMatcher.matchWhenLimits(jsonLimits, "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is session and limit is not reached`() {
        `when`(impressionManager.perSession("campaign123")).thenReturn(3)
        val jsonLimit = listOf(JSONObject(mapOf("type" to "session", "limit" to 5)))
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is session and limit is reached`() {
        `when`(impressionManager.perSession("campaign123")).thenReturn(7)
        val jsonLimit = listOf(JSONObject(mapOf("type" to "session", "limit" to 5)))
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is minutes and limit is not reached`() {
        `when`(impressionManager.perMinute("campaign123", 2)).thenReturn(3)
        val jsonLimit =
            listOf(JSONObject(mapOf("type" to "minutes", "limit" to 5, "frequency" to 2)))
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is minutes and limit is reached`() {
        `when`(impressionManager.perMinute("campaign123", 2)).thenReturn(6)
        val jsonLimit =
            listOf(JSONObject(mapOf("type" to "minutes", "limit" to 5, "frequency" to 2)))
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is seconds and limit is not reached`() {
        `when`(impressionManager.perSecond("campaign123", 5)).thenReturn(3)
        val jsonLimit =
            listOf(JSONObject(mapOf("type" to "seconds", "limit" to 5, "frequency" to 5)))
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is seconds and limit is reached`() {
        `when`(impressionManager.perSecond("campaign123", 5)).thenReturn(6)
        val jsonLimit =
            listOf(JSONObject(mapOf("type" to "seconds", "limit" to 5, "frequency" to 5)))
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is hours and limit is not reached`() {
        `when`(impressionManager.perHour("campaign123", 2)).thenReturn(3)
        val jsonLimit = listOf(JSONObject(mapOf("type" to "hours", "limit" to 5, "frequency" to 2)))
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is hours and limit is reached`() {
        `when`(impressionManager.perHour("campaign123", 2)).thenReturn(6)
        val jsonLimit = listOf(JSONObject(mapOf("type" to "hours", "limit" to 5, "frequency" to 2)))
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is days and limit is not reached`() {
        `when`(impressionManager.perDay("campaign123", 1)).thenReturn(3)
        val jsonLimit = listOf(JSONObject(mapOf("type" to "days", "limit" to 5, "frequency" to 1)))
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is days and limit is reached`() {
        `when`(impressionManager.perDay("campaign123", 1)).thenReturn(6)
        val jsonLimit = JSONObject(mapOf("type" to "days", "limit" to 5, "frequency" to 1))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is weeks and limit is not reached`() {
        `when`(impressionManager.perWeek("campaign123", 1)).thenReturn(3)
        val jsonLimit = JSONObject(mapOf("type" to "weeks", "limit" to 5, "frequency" to 1))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is weeks and limit is reached`() {
        `when`(impressionManager.perWeek("campaign123", 1)).thenReturn(6)
        val jsonLimit = JSONObject(mapOf("type" to "weeks", "limit" to 5, "frequency" to 1))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is ever and limit is not reached`() {
        `when`(impressionManager.getImpressions("campaign123")).thenReturn(listOf(1, 2, 1, 2))
        val jsonLimit = JSONObject(mapOf("type" to "ever", "limit" to 5))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is ever and limit is reached`() {
        `when`(impressionManager.getImpressions("campaign123")).thenReturn(listOf(1, 2))
        val jsonLimit = JSONObject(mapOf("type" to "ever", "limit" to 2))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchLimit should return true when limit type is onEvery and limit is not reached`() {
        `when`(triggerManager.getTriggers("campaign123")).thenReturn(3)
        val jsonLimit = JSONObject(mapOf("type" to "onEvery", "limit" to 3))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is onEvery and limit is reached`() {
        `when`(triggerManager.getTriggers("campaign123")).thenReturn(3)
        val jsonLimit = JSONObject(mapOf("type" to "onEvery", "limit" to 2))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is onExactly and limit is reached`() {
        `when`(triggerManager.getTriggers("campaign123")).thenReturn(3)
        val jsonLimit = JSONObject(mapOf("type" to "onExactly", "limit" to 2))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is onExactly and limit is not reached`() {
        `when`(triggerManager.getTriggers("campaign123")).thenReturn(2)
        val jsonLimit = JSONObject(mapOf("type" to "onExactly", "limit" to 2))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertTrue(result)
    }
}
