package com.clevertap.android.sdk

import com.clevertap.android.sdk.utils.Clock
import java.util.Date

// Test Clock implementation that allows controlling time
class TestClock(private var currentTime: Long = System.currentTimeMillis()) : Clock {
    override fun currentTimeMillis(): Long = currentTime
    override fun newDate(): Date = Date(currentTime)

    fun setCurrentTime(timeMillis: Long) {
        currentTime = timeMillis
    }

    fun advanceTime(deltaMillis: Long) {
        currentTime += deltaMillis
    }
}