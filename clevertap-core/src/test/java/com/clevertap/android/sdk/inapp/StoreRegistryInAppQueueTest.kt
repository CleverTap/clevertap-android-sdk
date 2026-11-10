package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.store.preference.FileStore
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.toList
import com.clevertap.android.sdk.validation.ValidationResult
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
    fun `enqueueAll when save throws OutOfMemoryError drops the queue and reports 596`() {
        val reported = mutableListOf<ValidationResult>()
        val inAppQueue = StoreRegistryInAppQueue(storeRegistry, "Test") { reported.add(it) }
        every { mockInAppStore.readServerSideInApps() } returns listOf(JSONObject().put("id", 1))
        every { mockInAppStore.storeServerSideInApps(any()) } throws OutOfMemoryError()
        every { mockInAppStore.serverSideInAppsStoredLength() } returns 40

        inAppQueue.enqueueAll(listOf(JSONObject().put("id", 2)))

        verify { mockInAppStore.removeServerSideInApps() }
        assertEquals(1, reported.size)
        assertEquals(Constants.INAPP_QUEUE_SAVE_OOM_ERROR_CODE, reported[0].errorCode)
        assertEquals("${Constants.INAPP_QUEUE_SAVE_OOM_ERROR_MSG}, items=2, prevLen=40", reported[0].errorDesc)
    }

    @Test
    fun `dequeue when save throws OutOfMemoryError drops the queue and returns null`() {
        val reported = mutableListOf<ValidationResult>()
        val inAppQueue = StoreRegistryInAppQueue(storeRegistry, "Test") { reported.add(it) }
        every { mockInAppStore.readServerSideInApps() } returns listOf(JSONObject().put("id", 1))
        every { mockInAppStore.storeServerSideInApps(any()) } throws OutOfMemoryError()

        val dequeuedObject = inAppQueue.dequeue()

        assertNull(dequeuedObject)
        verify { mockInAppStore.removeServerSideInApps() }
        assertEquals(Constants.INAPP_QUEUE_SAVE_OOM_ERROR_CODE, reported.single().errorCode)
    }

    @Test
    fun `enqueue when read throws OutOfMemoryError drops the stored queue, reports 597 and keeps the new inApp`() {
        val reported = mutableListOf<ValidationResult>()
        val inAppQueue = StoreRegistryInAppQueue(storeRegistry, "Test") { reported.add(it) }
        val inApp = JSONObject().put("id", 2)
        every { mockInAppStore.readServerSideInApps() } throws OutOfMemoryError()
        every { mockInAppStore.serverSideInAppsStoredLength() } returns 40

        inAppQueue.enqueue(inApp)

        verify { mockInAppStore.removeServerSideInApps() }
        verify { mockInAppStore.storeServerSideInApps(listOf(inApp)) }
        assertEquals(Constants.INAPP_QUEUE_READ_OOM_ERROR_CODE, reported.single().errorCode)
        assertEquals("${Constants.INAPP_QUEUE_READ_OOM_ERROR_MSG}, len=40", reported.single().errorDesc)
    }

    @Test
    fun `dequeue when read throws OutOfMemoryError returns null`() {
        every { mockInAppStore.readServerSideInApps() } throws OutOfMemoryError()

        assertNull(inAppQueue.dequeue())
        verify { mockInAppStore.removeServerSideInApps() }
    }

    @Test
    fun `queue is still dropped when reporting throws`() {
        val inAppQueue = StoreRegistryInAppQueue(storeRegistry, "Test") { throw OutOfMemoryError() }
        every { mockInAppStore.readServerSideInApps() } returns emptyList()
        every { mockInAppStore.storeServerSideInApps(any()) } throws OutOfMemoryError()

        inAppQueue.enqueue(JSONObject().put("id", 1))

        verify { mockInAppStore.removeServerSideInApps() }
    }

    @Test
    fun `successful save does not report`() {
        val reported = mutableListOf<ValidationResult>()
        val inAppQueue = StoreRegistryInAppQueue(storeRegistry, "Test") { reported.add(it) }
        every { mockInAppStore.readServerSideInApps() } returns emptyList()

        inAppQueue.enqueue(JSONObject().put("id", 1))

        assertEquals(0, reported.size)
        verify(exactly = 0) { mockInAppStore.removeServerSideInApps() }
    }

    @Test
    fun `when prefs failed to load, queue is empty, nothing is deleted and 598 is reported once`() {
        val reported = mutableListOf<ValidationResult>()
        val inAppQueue = StoreRegistryInAppQueue(storeRegistry, "Test") { reported.add(it) }
        val loadFailure = IllegalStateException(OutOfMemoryError())
        every { mockInAppStore.readServerSideInApps() } throws loadFailure
        every { mockInAppStore.storeServerSideInApps(any()) } throws loadFailure

        inAppQueue.enqueue(JSONObject().put("id", 1))
        assertNull(inAppQueue.dequeue())
        assertEquals(0, inAppQueue.getQueueLength())

        verify(exactly = 0) { mockInAppStore.removeServerSideInApps() }
        assertEquals(Constants.INAPP_QUEUE_PREFS_LOAD_OOM_ERROR_CODE, reported.single().errorCode)
        assertEquals(Constants.INAPP_QUEUE_PREFS_LOAD_OOM_ERROR_MSG, reported.single().errorDesc)
    }

    @Test
    fun `when prefs failed to load on save only, enqueue does not throw`() {
        every { mockInAppStore.readServerSideInApps() } returns emptyList()
        every { mockInAppStore.storeServerSideInApps(any()) } throws IllegalStateException(OutOfMemoryError())

        inAppQueue.enqueue(JSONObject().put("id", 1))

        verify(exactly = 0) { mockInAppStore.removeServerSideInApps() }
    }

    @Test(expected = IllegalStateException::class)
    fun `IllegalStateException not caused by OutOfMemoryError is rethrown`() {
        every { mockInAppStore.readServerSideInApps() } throws IllegalStateException("other")

        inAppQueue.enqueue(JSONObject().put("id", 1))
    }

    @Test
    fun `getQueueLength returns correctLength`() {
        val jsonArray = JSONArray().put(JSONObject().put("key", "value"))
        every { mockInAppStore.readServerSideInApps() } returns jsonArray.toList()

        val queueLength = inAppQueue.getQueueLength()

        assertEquals(1, queueLength)
    }
}
