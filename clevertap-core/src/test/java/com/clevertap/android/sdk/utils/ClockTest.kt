package com.clevertap.android.sdk.utils

import org.junit.*
import kotlin.test.assertTrue

class ClockTest {

    @Test
    fun `SYSTEM currentTimeMillis returns actual system time`() {
        val systemClock = Clock.SYSTEM

        val result = systemClock.currentTimeMillis()

        assertTrue(result > 0)
    }

    @Test
    fun `SYSTEM newDate returns actual system date`() {
        val systemClock = Clock.SYSTEM

        val result = systemClock.newDate()

        assertTrue(result.time > 0)
    }

    @Test
    fun `currentTimeSeconds returns expected value`() {
        val systemClock = Clock.SYSTEM

        val result = systemClock.currentTimeSeconds()

        assertTrue(result > 0)
    }
}