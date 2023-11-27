package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.mockito.*
import org.mockito.Mockito.*

class InAppQueueTest {

    @Mock
    private lateinit var mockConfig: CleverTapInstanceConfig

    private lateinit var storeRegistry: StoreRegistry

    @Mock
    private lateinit var mockInAppStore: InAppStore

    private lateinit var inAppQueue: InAppQueue

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        storeRegistry = StoreRegistry(mockInAppStore)
        inAppQueue = InAppQueue(mockConfig, storeRegistry)
    }

    @Test
    fun `enqueue single Object results in objectEnqueued`() {
        val jsonObject = JSONObject().put("key", "value")
        val fakeQueue = JSONArray()
        `when`(mockInAppStore.readServerSideInApps()).thenReturn(fakeQueue)

        inAppQueue.enqueue(jsonObject)

        val expected = JSONArray().put(jsonObject)

        verify(mockInAppStore).storeServerSideInApps(expected)
    }

    @Test
    fun `enqueue single Object when inAppStore is null`() {
        val jsonObject = JSONObject().put("key", "value")
        val storeRegistry = StoreRegistry()
        val inAppQueue = InAppQueue(mockConfig, storeRegistry)

        inAppQueue.enqueue(jsonObject)

        verifyNoInteractions(mockInAppStore)
    }

    @Test
    fun `enqueueAll multiple Objects results in objectsEnqueued`() {
        val jsonArray = JSONArray().put(JSONObject().put("key1", "value1"))
            .put(JSONObject().put("key2", "value2"))
        val fakeQueue = JSONArray()
        `when`(mockInAppStore.readServerSideInApps()).thenReturn(fakeQueue)

        inAppQueue.enqueueAll(jsonArray)

        val expected = JSONArray().put(jsonArray.getJSONObject(0)).put(jsonArray.getJSONObject(1))

        verify(mockInAppStore).storeServerSideInApps(expected)
    }

    @Test
    fun `enqueueAll with mixed data types ignores non-JSONObject elements`() {
        val jsonArray = JSONArray()
            .put(JSONObject().put("key1", "value1"))
            .put("nonJsonObjectData")
            .put(JSONObject().put("key2", "value2"))
        val fakeQueue = JSONArray()
        `when`(mockInAppStore.readServerSideInApps()).thenReturn(fakeQueue)

        inAppQueue.enqueueAll(jsonArray)

        // Only valid JSONObjects should be stored in the queue
        val expected = JSONArray().put(jsonArray.getJSONObject(0)).put(jsonArray.getJSONObject(2))
        verify(mockInAppStore).storeServerSideInApps(expected)
    }

    @Test
    fun `enqueueAll when argument json array is empty`() {
        val jsonArray = JSONArray()
        val fakeQueue = JSONArray()
        `when`(mockInAppStore.readServerSideInApps()).thenReturn(fakeQueue)

        inAppQueue.enqueueAll(jsonArray)

        verify(mockInAppStore).storeServerSideInApps(fakeQueue)
    }
}