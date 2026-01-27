package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.inapp.store.preference.FileStore
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.toList
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StoreRegistryInAppQueueTest {

    private lateinit var storeRegistry: StoreRegistry
    private lateinit var mockInAppStore: InAppStore
    private lateinit var mockImpressionStore: ImpressionStore
    private lateinit var mockLegacyInAppStore: LegacyInAppStore
    private lateinit var mockFileStore: FileStore
    private lateinit var mockInAppAssetsStore: InAppAssetsStore

    private lateinit var inAppQueue: StoreRegistryInAppQueue

    @Before
    fun setup() {
        mockInAppStore = mockk(relaxed = true)
        mockImpressionStore = mockk(relaxed = true)
        mockLegacyInAppStore = mockk(relaxed = true)
        mockFileStore = mockk(relaxed = true)
        mockInAppAssetsStore = mockk(relaxed = true)
        storeRegistry = StoreRegistry(
            mockInAppStore,
            mockImpressionStore,
            mockLegacyInAppStore,
            mockInAppAssetsStore,
            mockFileStore
        )
        inAppQueue = StoreRegistryInAppQueue(storeRegistry, "Test")
    }

    @Test
    fun `enqueue single Object results in objectEnqueued`() {
        val jsonObject = JSONObject().put("key", "value")
        val fakeQueue = JSONArray().toList<JSONObject>()
        every { mockInAppStore.readServerSideInApps() } returns fakeQueue

        inAppQueue.enqueue(jsonObject)

        val expected = JSONArray().put(jsonObject)

        verify { mockInAppStore.storeServerSideInApps(expected.toList()) }
    }

    @Test
    fun `enqueue single Object when inAppStore is null`() {
        val jsonObject = JSONObject().put("key", "value")
        val storeRegistry = StoreRegistry(
            null,
            mockImpressionStore,
            mockLegacyInAppStore,
            mockInAppAssetsStore,
            mockFileStore
        )
        val inAppQueue = StoreRegistryInAppQueue(storeRegistry, "Test")

        inAppQueue.enqueue(jsonObject)

        confirmVerified(mockInAppStore)
    }

    @Test
    fun `enqueueAll multiple Objects results in objectsEnqueued`() {
        val jsonArray = JSONArray().put(JSONObject().put("key1", "value1"))
            .put(JSONObject().put("key2", "value2"))
        val fakeQueue = JSONArray().toList<JSONObject>()
        every { mockInAppStore.readServerSideInApps() } returns fakeQueue

        inAppQueue.enqueueAll(jsonArray.toList())

        val expected = JSONArray().put(jsonArray.getJSONObject(0)).put(jsonArray.getJSONObject(1))

        verify { mockInAppStore.storeServerSideInApps(expected.toList()) }
    }

    @Test
    fun `enqueueAll with mixed data types ignores non-JSONObject elements`() {
        val jsonArray = JSONArray()
            .put(JSONObject().put("key1", "value1"))
            .put("nonJsonObjectData")
            .put(JSONObject().put("key2", "value2"))
        val fakeQueue = JSONArray().toList<JSONObject>()
        every { mockInAppStore.readServerSideInApps() } returns fakeQueue

        inAppQueue.enqueueAll(jsonArray.toList())

        // Only valid JSONObjects should be stored in the queue
        val expected = JSONArray().put(jsonArray.getJSONObject(0)).put(jsonArray.getJSONObject(2))
        verify { mockInAppStore.storeServerSideInApps(expected.toList()) }
    }

    @Test
    fun `enqueueAll when argument json array is empty`() {
        val jsonArray = JSONArray().toList<JSONObject>()
        val fakeQueue = JSONArray().toList<JSONObject>()

        inAppQueue.enqueueAll(jsonArray)

        verify(exactly = 0) { mockInAppStore.storeServerSideInApps(fakeQueue) }
        verify(exactly = 0) { mockInAppStore.readServerSideInApps() }

    }

    @Test
    fun `dequeue when queue is not empty returns dequeuedObject`() {
        val jsonObject = JSONObject().put("key", "value")
        val fakeQueue = JSONArray().put(jsonObject).toList<JSONObject>()
        every { mockInAppStore.readServerSideInApps() } returns fakeQueue

        val dequeuedObject = inAppQueue.dequeue()

        verify { mockInAppStore.storeServerSideInApps(JSONArray().toList<JSONObject>()) }
        assertEquals(jsonObject.toString(), dequeuedObject?.toString())
    }

    @Test
    fun `dequeue when queue is empty returns null`() {
        val input = JSONArray().toList<JSONObject>()
        every { mockInAppStore.readServerSideInApps() } returns input

        val dequeuedObject = inAppQueue.dequeue()

        assertNull(dequeuedObject)
    }

    @Test
    fun `getQueueLength returns correctLength`() {
        val jsonArray = JSONArray().put(JSONObject().put("key", "value"))
        every { mockInAppStore.readServerSideInApps() } returns jsonArray.toList()

        val queueLength = inAppQueue.getQueueLength()

        assertEquals(1, queueLength)
    }
}
