package com.clevertap.android.sdk.usereventlogs

import com.clevertap.android.sdk.Utils.getNormalizedName

object UserEventLogTestData {
    object EventNames {
        const val TEST_EVENT = "TeS  T"
        const val TEST_EVENT_2 = "TeS  T 2"
        const val SIMPLE_TEST_EVENT = "test"

        // Map of event names to their normalized versions for easy access
        val eventNameToNormalizedMap = mapOf(
            TEST_EVENT to getNormalizedName(TEST_EVENT),
            TEST_EVENT_2 to getNormalizedName(TEST_EVENT_2),
            SIMPLE_TEST_EVENT to getNormalizedName(SIMPLE_TEST_EVENT)
        )

        val eventNames = setOf(TEST_EVENT, TEST_EVENT_2)
        val setOfActualAndNormalizedEventNamePair = eventNames.map {
            Pair(it, eventNameToNormalizedMap[it]!!)
        }.toSet()

        // Sample test data
        val sampleUserEventLogsForSameDeviceId = listOf(
            UserEventLog(
                eventName = TEST_EVENT,
                normalizedEventName = eventNameToNormalizedMap[TEST_EVENT]!!,
                firstTs = 1000L,
                lastTs = 1000L,
                countOfEvents = 1,
                deviceID = "dId"
            ),
            UserEventLog(
                eventName = TEST_EVENT_2,
                normalizedEventName = eventNameToNormalizedMap[TEST_EVENT_2]!!,
                firstTs = 2000L,
                lastTs = 2000L,
                countOfEvents = 2,
                deviceID = "dId"
            )
        )
        val sampleUserEventLogsForMixedDeviceId = listOf(
            UserEventLog(
                eventName = TEST_EVENT,
                normalizedEventName = eventNameToNormalizedMap[TEST_EVENT]!!,
                firstTs = 1000L,
                lastTs = 1000L,
                countOfEvents = 1,
                deviceID = "dId1"
            ),
            UserEventLog(
                eventName = TEST_EVENT_2,
                normalizedEventName = eventNameToNormalizedMap[TEST_EVENT_2]!!,
                firstTs = 2000L,
                lastTs = 2000L,
                countOfEvents = 2,
                deviceID = "dId2"
            )
        )
    }

    object TestTimestamps {
        const val SAMPLE_TIMESTAMP = 1000L
        const val SAMPLE_TIMESTAMP_2 = 2000L
    }

    object TestDeviceIds {
        const val SAMPLE_DEVICE_ID = "dId"
    }
}