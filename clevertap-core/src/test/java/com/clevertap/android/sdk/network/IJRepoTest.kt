package com.clevertap.android.sdk.network

import android.content.Context
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
class IJRepoTest : BaseTestCase() {

    private lateinit var ijRepo: IJRepo
    private val namespaceIJ = "IJ"

    @Before
    override fun setUp() {
        super.setUp()
        ijRepo = IJRepo(cleverTapInstanceConfig)

        // Clear any existing values before each test
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    @After
    fun tearDown() {
        // Clean up after tests
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    @Test
    fun `setI saves long value for I`() {
        // Verify that calling setI with a specific long value successfully saves
        // it to the shared preferences under the correct key ('KEY_I') and account ID namespace.

        // Arrange
        val testValue = 12345L
        val expectedKey = "comms_i:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setI(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(testValue, savedValue, "setI should save the long value with the correct key")
    }

    @Test
    fun `setI handles zero value`() {
        // Test that setI can correctly save the value 0L.

        // Arrange
        val testValue = 0L
        val expectedKey = "comms_i:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setI(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, -1)
        assertEquals(testValue, savedValue, "setI should be able to save 0L value")
    }

    @Test
    fun `setI handles negative value`() {
        // Test that setI can correctly save a negative long value.

        // Arrange
        val testValue = -9876L
        val expectedKey = "comms_i:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setI(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(testValue, savedValue, "setI should be able to save negative values")
    }

    @Test
    fun `setI handles MAX VALUE`() {
        // Test that setI can correctly save Long.MAX_VALUE.

        // Arrange
        val testValue = Long.MAX_VALUE
        val expectedKey = "comms_i:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setI(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(testValue, savedValue, "setI should be able to save Long.MAX_VALUE")
    }

    @Test
    fun `setI handles MIN VALUE`() {
        // Test that setI can correctly save Long.MIN_VALUE.

        // Arrange
        val testValue = Long.MIN_VALUE
        val expectedKey = "comms_i:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setI(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(testValue, savedValue, "setI should be able to save Long.MIN_VALUE")
    }

    @Test
    fun `setI updates existing value`() {
        // Verify that calling setI multiple times overwrites the previous value
        // for 'I' with the latest one.

        // Arrange
        val initialValue = 100L
        val updatedValue = 200L
        val expectedKey = "comms_i:${cleverTapInstanceConfig.accountId}"

        // Act - set initial value
        ijRepo.setI(appCtx, initialValue)

        // Verify initial value was set
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val firstValue = prefs.getLong(expectedKey, 0)
        assertEquals(initialValue, firstValue, "Initial value should be saved correctly")

        // Act - update the value
        ijRepo.setI(appCtx, updatedValue)

        // Assert
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(updatedValue, savedValue, "setI should update the existing value")
    }

    @Test
    fun `setI uses correct namespace`() {
        // Ensure that setI saves the value in the shared preferences file
        // corresponding to NAMESPACE_IJ.

        // Arrange
        val testValue = 12345L
        val expectedKey = "comms_i:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setI(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(testValue, savedValue, "setI should use the correct namespace (IJ)")

        // Verify it's not in the default namespace
        val defaultPrefs = appCtx.getSharedPreferences("wizrocket", Context.MODE_PRIVATE)
        val defaultValue = defaultPrefs.getLong(expectedKey, -9999)
        assertEquals(-9999, defaultValue, "Value should not be saved in the default namespace")
    }

    @Test
    fun `setJ saves long value for J`() {
        // Verify that calling setJ with a specific long value successfully saves
        // it to the shared preferences under the correct key ('KEY_J') and account ID namespace.

        // Arrange
        val testValue = 67890L
        val expectedKey = "comms_j:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setJ(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(testValue, savedValue, "setJ should save the long value with the correct key")
    }

    @Test
    fun `setJ handles zero value`() {
        // Test that setJ can correctly save the value 0L.

        // Arrange
        val testValue = 0L
        val expectedKey = "comms_j:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setJ(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, -1)
        assertEquals(testValue, savedValue, "setJ should be able to save 0L value")
    }

    @Test
    fun `setJ handles negative value`() {
        // Test that setJ can correctly save a negative long value.

        // Arrange
        val testValue = -5432L
        val expectedKey = "comms_j:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setJ(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(testValue, savedValue, "setJ should be able to save negative values")
    }

    @Test
    fun `setJ handles MAX VALUE`() {
        // Test that setJ can correctly save Long.MAX_VALUE.

        // Arrange
        val testValue = Long.MAX_VALUE
        val expectedKey = "comms_j:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setJ(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(testValue, savedValue, "setJ should be able to save Long.MAX_VALUE")
    }

    @Test
    fun `setJ handles MIN VALUE`() {
        // Test that setJ can correctly save Long.MIN_VALUE.

        // Arrange
        val testValue = Long.MIN_VALUE
        val expectedKey = "comms_j:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setJ(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(testValue, savedValue, "setJ should be able to save Long.MIN_VALUE")
    }

    @Test
    fun `setJ updates existing value`() {
        // Verify that calling setJ multiple times overwrites the previous value
        // for 'J' with the latest one.

        // Arrange
        val initialValue = 100L
        val updatedValue = 200L
        val expectedKey = "comms_j:${cleverTapInstanceConfig.accountId}"

        // Act - set initial value
        ijRepo.setJ(appCtx, initialValue)

        // Verify initial value was set
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val firstValue = prefs.getLong(expectedKey, 0)
        assertEquals(initialValue, firstValue, "Initial value should be saved correctly")

        // Act - update the value
        ijRepo.setJ(appCtx, updatedValue)

        // Assert
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(updatedValue, savedValue, "setJ should update the existing value")
    }

    @Test
    fun `setJ uses correct namespace`() {
        // Ensure that setJ saves the value in the shared preferences file
        // corresponding to NAMESPACE_IJ.

        // Arrange
        val testValue = 67890L
        val expectedKey = "comms_j:${cleverTapInstanceConfig.accountId}"

        // Act
        ijRepo.setJ(appCtx, testValue)

        // Assert
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val savedValue = prefs.getLong(expectedKey, 0)
        assertEquals(testValue, savedValue, "setJ should use the correct namespace (IJ)")

        // Verify it's not in the default namespace
        val defaultPrefs = appCtx.getSharedPreferences("wizrocket", Context.MODE_PRIVATE)
        val defaultValue = defaultPrefs.getLong(expectedKey, -9999)
        assertEquals(-9999, defaultValue, "Value should not be saved in the default namespace")
    }

    @Test
    fun `getI retrieves saved value`() {
        // Verify that getI returns the exact long value previously saved using setI.

        // Arrange
        val testValue = 9999L
        val expectedKey = "comms_i:${cleverTapInstanceConfig.accountId}"

        // Set up data directly in SharedPreferences
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        prefs.edit().putLong(expectedKey, testValue).apply()

        // Act
        val retrievedValue = ijRepo.getI(appCtx)

        // Assert
        assertEquals(testValue, retrievedValue, "getI should retrieve the previously saved value")
    }

    @Test
    fun `getI returns default when not set`() {
        // Test that getI returns the default value (0L) when no value for 'I'
        // has been saved using setI.

        // Act
        val retrievedValue = ijRepo.getI(appCtx)

        // Assert
        assertEquals(0L, retrievedValue, "getI should return 0L when no value has been set")
    }

    @Test
    fun `getI retrieves after app restart`() {
        // Verify that the value saved by setI can be retrieved by getI after
        // the application has been closed and reopened.
        // Note: We're simulating this by creating a new IJRepo instance

        // Arrange
        val testValue = 8888L

        // Set up data directly in SharedPreferences
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val expectedKey = "comms_i:${cleverTapInstanceConfig.accountId}"
        prefs.edit().putLong(expectedKey, testValue).apply()

        // Create a new IJRepo to simulate app restart
        val newIJRepo = IJRepo(cleverTapInstanceConfig)

        // Act
        val retrievedValue = newIJRepo.getI(appCtx)

        // Assert
        assertEquals(testValue, retrievedValue, "getI should retrieve the value after app restart")
    }

    @Test
    fun `getI does not interfere with J`() {
        // Ensure that calling getI does not affect the value stored for 'J'.

        // Arrange
        val iValue = 1111L
        val jValue = 2222L
        val expectedIKey = "comms_i:${cleverTapInstanceConfig.accountId}"
        val expectedJKey = "comms_j:${cleverTapInstanceConfig.accountId}"

        // Set up data directly in SharedPreferences
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(expectedIKey, iValue)
            .putLong(expectedJKey, jValue)
            .apply()

        // Act
        val retrievedIValue = ijRepo.getI(appCtx)

        // Verify J is still intact
        val jValueAfterGetI = prefs.getLong(expectedJKey, 0)

        // Assert
        assertEquals(iValue, retrievedIValue, "getI should return the correct I value")
        assertEquals(jValue, jValueAfterGetI, "J value should not be affected by getI call")
    }

    @Test
    fun `getJ retrieves saved value`() {
        // Verify that getJ returns the exact long value previously saved using setJ.

        // Arrange
        val testValue = 7777L
        val expectedKey = "comms_j:${cleverTapInstanceConfig.accountId}"

        // Set up data directly in SharedPreferences
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        prefs.edit().putLong(expectedKey, testValue).apply()

        // Act
        val retrievedValue = ijRepo.getJ(appCtx)

        // Assert
        assertEquals(testValue, retrievedValue, "getJ should retrieve the previously saved value")
    }

    @Test
    fun `getJ returns default when not set`() {
        // Test that getJ returns the default value (0L) when no value for 'J'
        // has been saved using setJ.

        // Act
        val retrievedValue = ijRepo.getJ(appCtx)

        // Assert
        assertEquals(0L, retrievedValue, "getJ should return 0L when no value has been set")
    }

    @Test
    fun `getJ retrieves after app restart`() {
        // Verify that the value saved by setJ can be retrieved by getJ after
        // the application has been closed and reopened.
        // Note: We're simulating this by creating a new IJRepo instance

        // Arrange
        val testValue = 6666L

        // Set up data directly in SharedPreferences
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        val expectedKey = "comms_j:${cleverTapInstanceConfig.accountId}"
        prefs.edit().putLong(expectedKey, testValue).apply()

        // Create a new IJRepo to simulate app restart
        val newIJRepo = IJRepo(cleverTapInstanceConfig)

        // Act
        val retrievedValue = newIJRepo.getJ(appCtx)

        // Assert
        assertEquals(testValue, retrievedValue, "getJ should retrieve the value after app restart")
    }

    @Test
    fun `getJ does not interfere with I`() {
        // Ensure that calling getJ does not affect the value stored for 'I'.

        // Arrange
        val iValue = 3333L
        val jValue = 4444L
        val expectedIKey = "comms_i:${cleverTapInstanceConfig.accountId}"
        val expectedJKey = "comms_j:${cleverTapInstanceConfig.accountId}"

        // Set up data directly in SharedPreferences
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(expectedIKey, iValue)
            .putLong(expectedJKey, jValue)
            .apply()

        // Act
        val retrievedJValue = ijRepo.getJ(appCtx)

        // Verify I is still intact
        val iValueAfterGetJ = prefs.getLong(expectedIKey, 0)

        // Assert
        assertEquals(jValue, retrievedJValue, "getJ should return the correct J value")
        assertEquals(iValue, iValueAfterGetJ, "I value should not be affected by getJ call")
    }

    @Test
    fun `clearIJ removes both I and J`() {
        // Verify that calling clearIJ removes both the saved values for 'I' and 'J'
        // from the shared preferences.

        // Arrange
        val iValue = 5555L
        val jValue = 6666L
        val expectedIKey = "comms_i:${cleverTapInstanceConfig.accountId}"
        val expectedJKey = "comms_j:${cleverTapInstanceConfig.accountId}"

        // Set up data directly in SharedPreferences
        val prefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(expectedIKey, iValue)
            .putLong(expectedJKey, jValue)
            .apply()

        // Act
        ijRepo.clearIJ(appCtx)

        // Assert
        val afterClearPrefs = appCtx.getSharedPreferences("WizRocket_$namespaceIJ", Context.MODE_PRIVATE)
        assertFalse(afterClearPrefs.contains(expectedIKey), "I key should be removed after clearIJ")
        assertFalse(afterClearPrefs.contains(expectedJKey), "J key should be removed after clearIJ")
    }

    @Test
    fun `clearIJ after setting I and J`() {
        // Test that after setting both I and J, calling clearIJ results in both getI
        // and getJ returning their default values (0L).
        
        // Arrange
        val iValue = 7777L
        val jValue = 8888L
        
        // Set both values
        ijRepo.setI(appCtx, iValue)
        ijRepo.setJ(appCtx, jValue)
        
        // Act
        ijRepo.clearIJ(appCtx)
        
        // Assert
        val retrievedIValue = ijRepo.getI(appCtx)
        val retrievedJValue = ijRepo.getJ(appCtx)
        
        assertEquals(0L, retrievedIValue, "I value should be 0L after clearIJ")
        assertEquals(0L, retrievedJValue, "J value should be 0L after clearIJ")
    }

    @Test
    fun `clearIJ after setting only I`() {
        // Test that after setting only I, calling clearIJ results in both getI
        // and getJ returning their default values (0L).
        
        // Arrange
        val iValue = 9999L
        
        // Set only I value
        ijRepo.setI(appCtx, iValue)
        
        // Make sure J is at default
        assertEquals(0L, ijRepo.getJ(appCtx), "J should be at default value before test")
        
        // Act
        ijRepo.clearIJ(appCtx)
        
        // Assert
        val retrievedIValue = ijRepo.getI(appCtx)
        val retrievedJValue = ijRepo.getJ(appCtx)
        
        assertEquals(0L, retrievedIValue, "I value should be 0L after clearIJ")
        assertEquals(0L, retrievedJValue, "J value should remain 0L after clearIJ")
    }

    @Test
    fun `clearIJ after setting only J`() {
        // Test that after setting only J, calling clearIJ results in both getI
        // and getJ returning their default values (0L).
        
        // Arrange
        val jValue = 9999L
        
        // Set only J value
        ijRepo.setJ(appCtx, jValue)
        
        // Make sure I is at default
        assertEquals(0L, ijRepo.getI(appCtx), "I should be at default value before test")
        
        // Act
        ijRepo.clearIJ(appCtx)
        
        // Assert
        val retrievedIValue = ijRepo.getI(appCtx)
        val retrievedJValue = ijRepo.getJ(appCtx)
        
        assertEquals(0L, retrievedIValue, "I value should remain 0L after clearIJ")
        assertEquals(0L, retrievedJValue, "J value should be 0L after clearIJ")
    }

    @Test
    fun `clearIJ when no values set`() {
        // Test that calling clearIJ when no values for 'I' or 'J' have been set
        // does not cause any errors and subsequent calls to getI and getJ still
        // return default values (0L).
        
        // Verify both values start at default
        assertEquals(0L, ijRepo.getI(appCtx), "I should be at default value before test")
        assertEquals(0L, ijRepo.getJ(appCtx), "J should be at default value before test")
        
        // Act - clear both values
        ijRepo.clearIJ(appCtx)
        
        // Assert
        val retrievedIValue = ijRepo.getI(appCtx)
        val retrievedJValue = ijRepo.getJ(appCtx)
        
        assertEquals(0L, retrievedIValue, "I value should remain 0L after clearIJ")
        assertEquals(0L, retrievedJValue, "J value should remain 0L after clearIJ")
    }

    @Test
    fun `clearIJ persists changes`() {
        // Verify that the changes made by clearIJ (removing I and J) are persisted
        // and reflected after an app restart.
        
        // Arrange
        val iValue = 1234L
        val jValue = 5678L
        
        // Set both values
        ijRepo.setI(appCtx, iValue)
        ijRepo.setJ(appCtx, jValue)
        
        // Act - clear both values
        ijRepo.clearIJ(appCtx)
        
        // Create a new IJRepo to simulate app restart
        val newIJRepo = IJRepo(cleverTapInstanceConfig)
        
        // Assert
        val retrievedIValue = newIJRepo.getI(appCtx)
        val retrievedJValue = newIJRepo.getJ(appCtx)
        
        assertEquals(0L, retrievedIValue, "I value should be 0L after clearIJ and app restart")
        assertEquals(0L, retrievedJValue, "J value should be 0L after clearIJ and app restart")
    }

    @Test
    fun `Interactions between setI and setJ`() {
        // Verify that setting 'I' does not overwrite the value for 'J', and vice versa.
        
        // Arrange
        val initialIValue = 111L
        val initialJValue = 222L
        val updatedIValue = 333L
        val updatedJValue = 444L
        
        // Act - set both initial values
        ijRepo.setI(appCtx, initialIValue)
        ijRepo.setJ(appCtx, initialJValue)
        
        // Verify both values were set
        assertEquals(initialIValue, ijRepo.getI(appCtx), "Initial I value should be set")
        assertEquals(initialJValue, ijRepo.getJ(appCtx), "Initial J value should be set")
        
        // Update only I
        ijRepo.setI(appCtx, updatedIValue)
        
        // Assert J remains unchanged
        assertEquals(updatedIValue, ijRepo.getI(appCtx), "I value should be updated")
        assertEquals(initialJValue, ijRepo.getJ(appCtx), "J value should remain unchanged when I is updated")
        
        // Update only J
        ijRepo.setJ(appCtx, updatedJValue)
        
        // Assert I remains unchanged
        assertEquals(updatedIValue, ijRepo.getI(appCtx), "I value should remain unchanged when J is updated")
        assertEquals(updatedJValue, ijRepo.getJ(appCtx), "J value should be updated")
    }
}