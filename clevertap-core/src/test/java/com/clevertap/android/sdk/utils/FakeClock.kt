package com.clevertap.android.sdk.utils

import java.util.Date

class FakeClock(var timeMillis: Long = 1735686000000L/* 01.01.2025 */) : Clock {
    override fun currentTimeMillis(): Long {
        return timeMillis
    }

    override fun newDate(): Date {
        return Date(timeMillis)
    }

    fun advanceOneDay() {
        timeMillis += 86400000
    }
}
