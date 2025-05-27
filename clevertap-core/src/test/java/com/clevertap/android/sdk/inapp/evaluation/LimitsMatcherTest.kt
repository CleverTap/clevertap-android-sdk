package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class LimitsMatcherTest : BaseTestCase() {
    private lateinit var impressionManager: ImpressionManager
    private lateinit var triggerManager: TriggerManager
    private lateinit var limitsMatcher: LimitsMatcher

    @Before
    override fun setUp() {
        impressionManager = mockk(relaxed = true)
        triggerManager = mockk(relaxed = true)
        limitsMatcher = LimitsMatcher(impressionManager, triggerManager)
    }

    @Test
    fun `matchWhenLimits should return true when all limits are met`() {
        // Define your sample JSON limits here
        val jsonLimits = listOf(
            LimitAdapter(JSONObject(mapOf("type" to "session", "limit" to 5))),
            LimitAdapter(JSONObject(mapOf("type" to "minutes", "limit" to 10, "frequency" to 2)))
        )

        // Mock the behavior of the impressionManager
        every { impressionManager.perSession("campaign123") } returns 4
        every { impressionManager.perMinute("campaign123", 2) } returns 9

        val result = limitsMatcher.matchWhenLimits(jsonLimits, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return true when limits are empty`() {
        // Define your sample JSON limits here
        val jsonLimits = listOf<LimitAdapter>()

        val result = limitsMatcher.matchWhenLimits(jsonLimits, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when any limit is not met`() {
        // Define your sample JSON limits here
        val jsonLimits = listOf(
            LimitAdapter(JSONObject(mapOf("type" to "session", "limit" to 5))),
            LimitAdapter(JSONObject(mapOf("type" to "minutes", "limit" to 10, "frequency" to 2)))
        )

        // Mock the behavior of the impressionManager
        every { impressionManager.perSession("campaign123") } returns 6
        every { impressionManager.perMinute("campaign123", 2) } returns 9

        val result = limitsMatcher.matchWhenLimits(jsonLimits, "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is session and limit is not reached`() {
        every { impressionManager.perSession("campaign123") } returns 3
        val jsonLimit = listOf(LimitAdapter(JSONObject(mapOf("type" to "session", "limit" to 5))))
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is session and limit is reached`() {
        every { impressionManager.perSession("campaign123") } returns 7
        val jsonLimit = listOf(LimitAdapter(JSONObject(mapOf("type" to "session", "limit" to 5))))
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is minutes and limit is not reached`() {
        every { impressionManager.perMinute("campaign123", 2) } returns 3
        val jsonLimit = listOf(
            LimitAdapter(
                JSONObject(
                    mapOf(
                        "type" to "minutes", "limit" to 5, "frequency" to 2
                    )
                )
            )
        )
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is minutes and limit is reached`() {
        every { impressionManager.perMinute("campaign123", 2) } returns 6
        val jsonLimit = listOf(
            LimitAdapter(
                JSONObject(
                    mapOf(
                        "type" to "minutes", "limit" to 5, "frequency" to 2
                    )
                )
            )
        )
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is seconds and limit is not reached`() {
        every { impressionManager.perSecond("campaign123", 5) } returns 3
        val jsonLimit = listOf(
            LimitAdapter(
                JSONObject(
                    mapOf(
                        "type" to "seconds", "limit" to 5, "frequency" to 5
                    )
                )
            )
        )
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is seconds and limit is reached`() {
        every { impressionManager.perSecond("campaign123", 5) } returns 6
        val jsonLimit = listOf(
            LimitAdapter(
                JSONObject(
                    mapOf(
                        "type" to "seconds", "limit" to 5, "frequency" to 5
                    )
                )
            )
        )
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is hours and limit is not reached`() {
        every { impressionManager.perHour("campaign123", 2) } returns 3
        val jsonLimit = listOf(
            LimitAdapter(
                JSONObject(
                    mapOf(
                        "type" to "hours", "limit" to 5, "frequency" to 2
                    )
                )
            )
        )
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is hours and limit is reached`() {
        every { impressionManager.perHour("campaign123", 2) } returns 6
        val jsonLimit = listOf(
            LimitAdapter(
                JSONObject(
                    mapOf(
                        "type" to "hours", "limit" to 5, "frequency" to 2
                    )
                )
            )
        )
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is days and limit is not reached`() {
        every { impressionManager.perDay("campaign123", 1) } returns 3
        val jsonLimit = listOf(
            LimitAdapter(
                JSONObject(
                    mapOf(
                        "type" to "days", "limit" to 5, "frequency" to 1
                    )
                )
            )
        )
        val result = limitsMatcher.matchWhenLimits(jsonLimit, "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is days and limit is reached`() {
        every { impressionManager.perDay("campaign123", 1) } returns 6
        val jsonLimit =
            LimitAdapter(JSONObject(mapOf("type" to "days", "limit" to 5, "frequency" to 1)))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is weeks and limit is not reached`() {
        every { impressionManager.perWeek("campaign123", 1) } returns 3
        val jsonLimit =
            LimitAdapter(JSONObject(mapOf("type" to "weeks", "limit" to 5, "frequency" to 1)))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is weeks and limit is reached`() {
        every { impressionManager.perWeek("campaign123", 1) } returns 6
        val jsonLimit =
            LimitAdapter(JSONObject(mapOf("type" to "weeks", "limit" to 5, "frequency" to 1)))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is ever and limit is not reached`() {
        every { impressionManager.getImpressions("campaign123") } returns listOf(1, 2, 1, 2)
        val jsonLimit = LimitAdapter(JSONObject(mapOf("type" to "ever", "limit" to 5)))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is ever and limit is reached`() {
        every { impressionManager.getImpressions("campaign123") } returns listOf(1, 2)
        val jsonLimit = LimitAdapter(JSONObject(mapOf("type" to "ever", "limit" to 2)))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is onEvery and limit is not reached`() {
        every { triggerManager.getTriggers("campaign123") } returns 3
        val jsonLimit = LimitAdapter(JSONObject(mapOf("type" to "onEvery", "limit" to 3)))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertTrue(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is onEvery and limit is reached`() {
        every { triggerManager.getTriggers("campaign123") } returns 3
        val jsonLimit = LimitAdapter(JSONObject(mapOf("type" to "onEvery", "limit" to 2)))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return false when limit type is onExactly and limit is reached`() {
        every { triggerManager.getTriggers("campaign123") } returns 3
        val jsonLimit = LimitAdapter(JSONObject(mapOf("type" to "onExactly", "limit" to 2)))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertFalse(result)
    }

    @Test
    fun `matchWhenLimits should return true when limit type is onExactly and limit is not reached`() {
        every { triggerManager.getTriggers("campaign123") } returns 2
        val jsonLimit = LimitAdapter(JSONObject(mapOf("type" to "onExactly", "limit" to 2)))
        val result = limitsMatcher.matchWhenLimits(listOf(jsonLimit), "campaign123")
        assertTrue(result)
    }
}
