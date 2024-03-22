package com.clevertap.android.sdk.db

import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QueueDataTest : BaseTestCase() {

    @Test
    fun test_updateCursorForDBObject_when_CalledWithCursorAndJson_should_UpdateCursorWithJsonEntries() {
        var queue: QueueData = QueueData(Table.EVENTS).also { it.lastId = "hello" }

        //if json is null the queue should remain the same
        queue.setDataFromDbObject(null)
        assertEquals("hello", queue.lastId)
        assertNull(queue.data)

        //if json is not of format {<string>:<JSONArray>}, it should cause an error and return queue with null data
        queue = QueueData(Table.EVENTS)
        var json = JSONObject().also { it.put("key", "value") }
        queue.setDataFromDbObject(json)
        assertNull(queue.data)
        assertNull(queue.lastId)

        //if json is of correct format, it will set the data on the queue and set its key as lastID
        queue = QueueData(Table.EVENTS)
        json = JSONObject()
        val sampleJsons = getSampleJsonArrayOfStrings(2)
        json.put("key", sampleJsons)
        queue.setDataFromDbObject(json)
        assertEquals("key", queue.lastId)
        val entries = queue.data
        assertEquals(sampleJsons.getString(0), entries?.getString(0))
        assertEquals(sampleJsons.getString(1), entries?.getString(1))
    }
}
