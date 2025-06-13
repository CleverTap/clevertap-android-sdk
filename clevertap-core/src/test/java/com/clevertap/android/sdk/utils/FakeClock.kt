package com.clevertap.android.sdk.utils

import java.util.Date

class FakeClock(var timeMillis: Long) : Clock {
    override fun currentTimeMillis(): Long {
        return timeMillis
    }

    override fun newDate(): Date {
        return Date(timeMillis)
    }
}
