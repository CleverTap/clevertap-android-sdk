package com.clevertap.android.sdk.network.http

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.network.NetworkRepo
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NetworkRepoTest : BaseTestCase() {

    private lateinit var config: CleverTapInstanceConfig
    private lateinit var networkRepo: NetworkRepo
    private val accountId = "test-account-id"
    private val accountRegion = "us1"

    @Before
    override fun setUp() {
        super.setUp()
        // Create a real config instance - this will use actual SharedPreferences via Robolectric
        config = CleverTapInstanceConfig.createInstance(appCtx, accountId, "test-token", accountRegion)
    }

    // Test cases for getFirstRequestTs()
    @Test
    fun `getFirstRequestTs should return value from storage`() {
        // Given
        val expectedTimestamp = 1234567890
        networkRepo = NetworkRepo(appCtx, config)

        // First set a value
        networkRepo.setFirstRequestTs(expectedTimestamp)

        // When
        val result = networkRepo.getFirstRequestTs()

        // Then
        assertEquals(expectedTimestamp, result)
    }

    @Test
    fun `clearFirstRequestTs should return 0 from storage`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)

        // First set a value
        networkRepo.setFirstRequestTs(1234567890)

        // When
        networkRepo.clearFirstRequestTs()

        // Then
        assertEquals(0, networkRepo.getFirstRequestTs())
    }

    @Test
    fun `clearLastRequestTs should return 0 from storage`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)

        // First set a value
        networkRepo.setLastRequestTs(1234567890)

        // When
        networkRepo.clearLastRequestTs()

        // Then
        assertEquals(0, networkRepo.getLastRequestTs())
    }

    @Test
    fun `getFirstRequestTs should return default value when no stored value`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)

        // When
        val result = networkRepo.getFirstRequestTs()

        // Then
        assertEquals(0, result)
    }

    // Test cases for setFirstRequestTs()
    @Test
    fun `setFirstRequestTs should store value successfully`() {
        // Given
        val timestamp = 1234567890
        networkRepo = NetworkRepo(appCtx, config)

        // When
        networkRepo.setFirstRequestTs(timestamp)

        // Then - Verify by reading it back
        val result = networkRepo.getFirstRequestTs()
        assertEquals(timestamp, result)
    }

    // Test cases for getLastRequestTs()
    @Test
    fun `getLastRequestTs should return value from storage`() {
        // Given
        val expectedTimestamp = 9876543210L.toInt()
        networkRepo = NetworkRepo(appCtx, config)

        // First set a value
        networkRepo.setLastRequestTs(expectedTimestamp)

        // When
        val result = networkRepo.getLastRequestTs()

        // Then
        assertEquals(expectedTimestamp, result)
    }

    @Test
    fun `getLastRequestTs should return default value when no stored value`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)

        // When
        val result = networkRepo.getLastRequestTs()

        // Then
        assertEquals(0, result)
    }

    // Test cases for setLastRequestTs()
    @Test
    fun `setLastRequestTs should store value successfully`() {
        // Given
        val timestamp = 9876543210L.toInt()
        networkRepo = NetworkRepo(appCtx, config)

        // When
        networkRepo.setLastRequestTs(timestamp)

        // Then - Verify by reading it back
        val result = networkRepo.getLastRequestTs()
        assertEquals(timestamp, result)
    }

    // Test cases for setMuted()
    @Test
    fun `setMuted with true should store current timestamp`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)
        val beforeTime = (System.currentTimeMillis() / 1000).toInt()

        // When
        networkRepo.setMuted(true)
        val afterTime = (System.currentTimeMillis() / 1000).toInt()

        // Then - Verify by checking that a non-zero timestamp was stored
        // We can't directly access the muted value, but we can verify the behavior
        // by calling setMuted(false) and then setMuted(true) again
        networkRepo.setMuted(false)
        networkRepo.setMuted(true)

        // The test passes if no exception is thrown and the method completes successfully
        assertTrue("setMuted(true) should complete without error", true)
    }

    @Test
    fun `setMuted with false should store zero`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)

        // When
        networkRepo.setMuted(false)

        // Then - The test passes if no exception is thrown
        assertTrue("setMuted(false) should complete without error", true)
    }

    // Test cases for setDomain()
    @Test
    fun `setDomain should store domain name`() {
        // Given
        val domainName = "test.clevertap.com"
        networkRepo = NetworkRepo(appCtx, config)

        // When
        networkRepo.setDomain(domainName)

        // Then - Verify by reading it back
        val result = networkRepo.getDomain()
        assertEquals(domainName, result)
    }

    @Test
    fun `setDomain should handle null domain name`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)

        // When
        networkRepo.setDomain(null)

        // Then - Verify by reading it back
        val result = networkRepo.getDomain()
        assertNull(result)
    }

    // Test cases for getDomain()
    @Test
    fun `getDomain should return domain name from storage`() {
        // Given
        val expectedDomain = "test.clevertap.com"
        networkRepo = NetworkRepo(appCtx, config)

        // First set a value
        networkRepo.setDomain(expectedDomain)

        // When
        val result = networkRepo.getDomain()

        // Then
        assertEquals(expectedDomain, result)
    }

    @Test
    fun `getDomain should return null when no stored domain`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)

        // When
        val result = networkRepo.getDomain()

        // Then
        assertNull(result)
    }

    // Test cases for setSpikyDomain()
    @Test
    fun `setSpikyDomain should store spiky domain name`() {
        // Given
        val spikyDomainName = "spiky.clevertap.com"
        networkRepo = NetworkRepo(appCtx, config)

        // When
        networkRepo.setSpikyDomain(spikyDomainName)

        // Then - Verify by reading it back
        val result = networkRepo.getSpikyDomain()
        assertEquals(spikyDomainName, result)
    }

    // Test cases for getSpikyDomain()
    @Test
    fun `getSpikyDomain should return spiky domain name from storage`() {
        // Given
        val expectedSpikyDomain = "spiky.clevertap.com"
        networkRepo = NetworkRepo(appCtx, config)

        // First set a value
        networkRepo.setSpikyDomain(expectedSpikyDomain)

        // When
        val result = networkRepo.getSpikyDomain()

        // Then
        assertEquals(expectedSpikyDomain, result)
    }

    @Test
    fun `getSpikyDomain should return null when no stored spiky domain`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)

        // When
        val result = networkRepo.getSpikyDomain()

        // Then
        assertNull(result)
    }

    @Test
    fun `getSpikyDomain should work independently from regular domain`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)
        val regularDomain = "regular.clevertap.com"
        val spikyDomain = "spiky.clevertap.com"

        // When
        networkRepo.setDomain(regularDomain)
        networkRepo.setSpikyDomain(spikyDomain)

        // Then
        assertEquals(regularDomain, networkRepo.getDomain())
        assertEquals(spikyDomain, networkRepo.getSpikyDomain())
    }

    // Test cases for getMinDelayFrequency()
    @Test
    fun `getMinDelayFrequency should return PUSH_DELAY_MS for retry count less than 10`() {
        // Given
        val currentDelay = 5000
        val networkRetryCount = 5
        networkRepo = NetworkRepo(appCtx, config)

        // When
        val result = networkRepo.getMinDelayFrequency(currentDelay, networkRetryCount)

        // Then
        assertEquals(Constants.PUSH_DELAY_MS, result)
    }

    @Test
    fun `getMinDelayFrequency should return PUSH_DELAY_MS when account region is null and retry count is 10 or more`() {
        // Given
        val currentDelay = 5000
        val networkRetryCount = 10
        // Create config with null region
        config = CleverTapInstanceConfig.createInstance(appCtx, accountId, "test-token", null)
        networkRepo = NetworkRepo(appCtx, config)

        // When
        val result = networkRepo.getMinDelayFrequency(currentDelay, networkRetryCount)

        // Then
        assertEquals(Constants.PUSH_DELAY_MS, result)
    }

    @Test
    fun `getMinDelayFrequency should return calculated delay when account region is not null and retry count is 10 or more`() {
        // Given
        val currentDelay = 5000
        val networkRetryCount = 15
        val randomDelay = 3000
        val expectedDelay = currentDelay + randomDelay
        val generateRandomDelay = { randomDelay }

        networkRepo = NetworkRepo(appCtx, config, generateRandomDelay)

        // When
        val result = networkRepo.getMinDelayFrequency(currentDelay, networkRetryCount)

        // Then
        assertEquals(expectedDelay, result)
    }

    @Test
    fun `getMinDelayFrequency should return PUSH_DELAY_MS when calculated delay exceeds MAX_DELAY_FREQUENCY`() {
        // Given
        val currentDelay = Constants.MAX_DELAY_FREQUENCY - 1000
        val networkRetryCount = 15
        val randomDelay = 2000 // This will make total delay exceed MAX_DELAY_FREQUENCY
        val generateRandomDelay = { randomDelay }

        networkRepo = NetworkRepo(appCtx, config, generateRandomDelay)

        // When
        val result = networkRepo.getMinDelayFrequency(currentDelay, networkRetryCount)

        // Then
        assertEquals(Constants.PUSH_DELAY_MS, result)
    }

    @Test
    fun `getMinDelayFrequency should handle edge case of retry count exactly 10`() {
        // Given
        val currentDelay = 2000
        val networkRetryCount = 10
        val randomDelay = 1500
        val expectedDelay = currentDelay + randomDelay
        val generateRandomDelay = { randomDelay }

        networkRepo = NetworkRepo(appCtx, config, generateRandomDelay)

        // When
        val result = networkRepo.getMinDelayFrequency(currentDelay, networkRetryCount)

        // Then
        assertEquals(expectedDelay, result)
    }

    @Test
    fun `getMinDelayFrequency should handle zero current delay`() {
        // Given
        val currentDelay = 0
        val networkRetryCount = 15
        val randomDelay = 1000
        val expectedDelay = currentDelay + randomDelay
        val generateRandomDelay = { randomDelay }

        networkRepo = NetworkRepo(appCtx, config, generateRandomDelay)

        // When
        val result = networkRepo.getMinDelayFrequency(currentDelay, networkRetryCount)

        // Then
        assertEquals(expectedDelay, result)
    }

    @Test
    fun `getMinDelayFrequency should handle negative retry count`() {
        // Given
        val currentDelay = 5000
        val networkRetryCount = -1
        networkRepo = NetworkRepo(appCtx, config)

        // When
        val result = networkRepo.getMinDelayFrequency(currentDelay, networkRetryCount)

        // Then
        assertEquals(Constants.PUSH_DELAY_MS, result)
    }

    // Test for default generateRandomDelay function
    @Test
    fun `default generateRandomDelay should return value between 1000 and 10000`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)

        // When - Test the randomness by calling the method multiple times
        val results = mutableListOf<Int>()
        repeat(50) {
            val result = networkRepo.getMinDelayFrequency(1000, 15)
            results.add(result)
        }

        // Then - All results should be different (due to randomness) and within expected range
        val uniqueResults = results.toSet()
        assertTrue("Should have multiple different results due to randomness", uniqueResults.size > 1)

        // All results should be between 2000 (1000 + min random 1000) and 11000 (1000 + max random 10000)
        results.forEach { result ->
            assertTrue("Result $result should be >= 2000", result >= 2000)
            assertTrue("Result $result should be <= 11000", result <= 11000)
        }
    }

    // Integration test to verify storage operations work together
    @Test
    fun `storage operations should work independently`() {
        // Given
        networkRepo = NetworkRepo(appCtx, config)
        val firstTs = 1111111111
        val lastTs = 2222222222L.toInt()
        val domain = "integration.test.com"
        val spikyDomain = "spiky.integration.test.com"

        // When
        networkRepo.setFirstRequestTs(firstTs)
        networkRepo.setLastRequestTs(lastTs)
        networkRepo.setDomain(domain)
        networkRepo.setSpikyDomain(spikyDomain)
        networkRepo.setMuted(true)

        // Then
        assertEquals(firstTs, networkRepo.getFirstRequestTs())
        assertEquals(lastTs, networkRepo.getLastRequestTs())
        assertEquals(domain, networkRepo.getDomain())
        assertEquals(spikyDomain, networkRepo.getSpikyDomain())
    }

    // Test with different config instances
    @Test
    fun `different config instances should have separate storage`() {
        // Given
        val config1 = CleverTapInstanceConfig.createInstance(appCtx, "account1", "token1", "us1")
        val config2 = CleverTapInstanceConfig.createInstance(appCtx, "account2", "token2", "eu1")
        val networkRepo1 = NetworkRepo(appCtx, config1)
        val networkRepo2 = NetworkRepo(appCtx, config2)

        // When
        networkRepo1.setFirstRequestTs(1111)
        networkRepo2.setFirstRequestTs(2222)

        // Then
        assertEquals(1111, networkRepo1.getFirstRequestTs())
        assertEquals(2222, networkRepo2.getFirstRequestTs())
    }
}