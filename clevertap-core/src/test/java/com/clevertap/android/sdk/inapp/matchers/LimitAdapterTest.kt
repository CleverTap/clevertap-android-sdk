package com.clevertap.android.sdk.inapp.matchers

import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals

class LimitAdapterTest : BaseTestCase() {


    @Test
    fun testLimitTypeFromString_InvalidType() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_TYPE, "invalid_type") }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val limitType = limitAdapter.limitType

        // Assert
        assertEquals(LimitType.Ever, limitType)
    }

    @Test
    fun testLimitValue() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_LIMIT, 5) }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val limit = limitAdapter.limit

        // Assert
        assertEquals(5, limit)
    }

    @Test
    fun testFrequencyValue() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_FREQUENCY, 10) }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val frequency = limitAdapter.frequency

        // Assert
        assertEquals(10, frequency)
    }

    @Test
    fun testLimitTypeFromString_ValidType_Ever() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_TYPE, "ever") }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val limitType = limitAdapter.limitType

        // Assert
        assertEquals(LimitType.Ever, limitType)
    }

    @Test
    fun testLimitTypeFromString_ValidType_Session() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_TYPE, "session") }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val limitType = limitAdapter.limitType

        // Assert
        assertEquals(LimitType.Session, limitType)
    }

    @Test
    fun testLimitTypeFromString_ValidType_Seconds() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_TYPE, "seconds") }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val limitType = limitAdapter.limitType

        // Assert
        assertEquals(LimitType.Seconds, limitType)
    }

    @Test
    fun testLimitTypeFromString_ValidType_Minutes() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_TYPE, "minutes") }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val limitType = limitAdapter.limitType

        // Assert
        assertEquals(LimitType.Minutes, limitType)
    }

    @Test
    fun testLimitTypeFromString_ValidType_Hours() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_TYPE, "hours") }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val limitType = limitAdapter.limitType

        // Assert
        assertEquals(LimitType.Hours, limitType)
    }

    @Test
    fun testLimitTypeFromString_ValidType_Days() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_TYPE, "days") }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val limitType = limitAdapter.limitType

        // Assert
        assertEquals(LimitType.Days, limitType)
    }

    @Test
    fun testLimitTypeFromString_ValidType_Weeks() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_TYPE, "weeks") }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val limitType = limitAdapter.limitType

        // Assert
        assertEquals(LimitType.Weeks, limitType)
    }

    @Test
    fun testLimitTypeFromString_ValidType_OnEvery() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_TYPE, "onEvery") }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val limitType = limitAdapter.limitType

        // Assert
        assertEquals(LimitType.OnEvery, limitType)
    }

    @Test
    fun testLimitTypeFromString_ValidType_OnExactly() {
        // Arrange
        val limitJSON = JSONObject().apply { put(Constants.KEY_TYPE, "onExactly") }
        val limitAdapter = LimitAdapter(limitJSON)

        // Act
        val limitType = limitAdapter.limitType

        // Assert
        assertEquals(LimitType.OnExactly, limitType)
    }
}