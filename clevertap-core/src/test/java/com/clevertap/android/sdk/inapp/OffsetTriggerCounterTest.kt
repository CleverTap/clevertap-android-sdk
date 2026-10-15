package com.clevertap.android.sdk.inapp

import org.junit.Test
import kotlin.test.assertEquals

class OffsetTriggerCounterTest {

    private fun counterOf(vararg counts: Pair<String, Int>): TriggerCounting {
        val map = counts.toMap()
        return object : TriggerCounting {
            override fun getTriggers(campaignId: String): Int = map[campaignId] ?: 0
        }
    }

    @Test
    fun `reports live count plus default offset of one`() {
        val offset = OffsetTriggerCounter(counterOf("c1" to 4))
        assertEquals(5, offset.getTriggers("c1"))
    }

    @Test
    fun `unknown campaign reports the offset`() {
        val offset = OffsetTriggerCounter(counterOf())
        assertEquals(1, offset.getTriggers("missing"))
    }

    @Test
    fun `custom offset is applied`() {
        val offset = OffsetTriggerCounter(counterOf("c1" to 2), offset = 3)
        assertEquals(5, offset.getTriggers("c1"))
    }

    @Test
    fun `does not mutate the delegate`() {
        val delegate = counterOf("c1" to 7)
        val offset = OffsetTriggerCounter(delegate)
        offset.getTriggers("c1")
        offset.getTriggers("c1")
        assertEquals(7, delegate.getTriggers("c1")) // delegate untouched
    }
}
