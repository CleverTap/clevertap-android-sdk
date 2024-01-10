package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DBManagerTest : BaseTestCase() {

    private lateinit var dbManager: DBManager
    private lateinit var dbManagerSpy: DBManager
    private lateinit var instanceConfig: CleverTapInstanceConfig
    private lateinit var lockManager: CTLockManager
    private lateinit var dbAdapter: DBAdapter
    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, "accountId", "accountToken")
        lockManager = CTLockManager()
        dbManager = DBManager(instanceConfig, lockManager)
        dbManagerSpy = Mockito.spy(dbManager)
        dbAdapter = DBAdapter(appCtx, instanceConfig)
    }

    @After
    fun deleteDb() {
        dbManager.loadDBAdapter(appCtx).deleteDB()
        dbManagerSpy.loadDBAdapter(appCtx).deleteDB()
        dbAdapter.deleteDB()
    }

    @Test
    @Ignore("DBAdapter should be refactored to not depend on System.currentTimeMillis(). Until then testing cleaning of stale events is not possible")
    fun test_loadDBAdapter_when_CalledWithContext_should_ReturnDBAdapterInstanceWithCleanedUpTables() {
        //assertion : some tables of dbadapter already have some entries

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table ->
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            println(
                "before call, entries for table: ${table.tableName} = ${
                    dbAdapter.fetchEvents(
                        table,
                        Int.MAX_VALUE
                    )
                }"
            )
        }

        //test
        val dAdp = dbManager.loadDBAdapter(appCtx)

        //validate
        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table ->
            val entries = dAdp.fetchEvents(table, Int.MAX_VALUE)
            println("after call, entries for table: ${table.tableName} = $entries")
            assertNull(entries)
        }
    }

    @Test
    fun test_clearQueues_when_called_should_ClearEventsAndProfileEventsTable() {
        //assertion : some tables of dbadapter already have some entries
        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS).forEach { table ->
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            println(
                "before call, entries for table: ${table.tableName} = ${
                    dbAdapter.fetchEvents(
                        table,
                        Int.MAX_VALUE
                    )
                }"
            )
        }

        //test
        dbManager.clearQueues(appCtx)

        //validate
        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS).forEach { table ->
            val entries = dbAdapter.fetchEvents(table, Int.MAX_VALUE)
            println("after call, entries for table: ${table.tableName} = $entries")
            assertNull(entries)
        }
    }

    @Test
    fun test_getPushNotificationViewedQueuedEvents_when_called_should_ReturnResponseFromGetQueueQueueFunction() {
        dbManagerSpy.getPushNotificationViewedQueuedEvents(appCtx, Int.MAX_VALUE, null)
        Mockito.verify(dbManagerSpy, Mockito.times(1))
            .getQueue(appCtx, Table.PUSH_NOTIFICATION_VIEWED, Int.MAX_VALUE, null)
    }

    @Test
    fun test_getQueue_when_calledWithNullPreviousQueue_should_returnAQueueWithEntries() {
        // if previous queue is null then a new queue will be created
        // the data of such queue will be : isEmpty=false | data= array of all the entries in table | lastID = last id of entry
        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table ->
            //assertion : adding entries in each table
            val dbAdapter = dbManager.loadDBAdapter(appCtx)
            val sampleEntries = listOf(
                JSONObject("""{"key":"value1"}"""),
                JSONObject("""{"key":"value2"}"""),
                JSONObject("""{"key":"value3"}"""),
                JSONObject("""{"key":"value4"}""")
            )
            sampleEntries.forEach { dbAdapter.storeObject(it, table) }

            //test
            val queue: QueueData = dbManager.getQueue(appCtx, table, batchSize = Int.MAX_VALUE, previousQueue = null)

            //validate: all entries in the table are available in the queue
            assertEquals("value1", queue.data?.getJSONObject(0)?.getString("key"))
            assertEquals("value2", queue.data?.getJSONObject(1)?.getString("key"))
            assertEquals("value3", queue.data?.getJSONObject(2)?.getString("key"))
            assertEquals("value4", queue.data?.getJSONObject(3)?.getString("key"))
            assertEquals("4", queue.lastId)
            assertEquals(table, queue.table)
        }
    }

    @Test
    fun test_getQueue_when_calledWithNotNullQueue_should_returnAQueueWithEntries() {
        // if previousQueue is not null then all the entries from the table till previousQueue.lastId will be removed
        // add only the remaining items as data of the new queue

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table ->
            //assertion : adding entries in each table
            val dbAdapter = dbManager.loadDBAdapter(appCtx)
            val sampleEntries = listOf(
                JSONObject("""{"key":"value1"}"""),
                JSONObject("""{"key":"value2"}"""),
                JSONObject("""{"key":"value3"}"""),
                JSONObject("""{"key":"value4"}""")
            )
            sampleEntries.forEach { dbAdapter.storeObject(it, table) }

            //assertion : previous queue has some entries
            val prevQueue = QueueData(Table.EVENTS).also {
                it.table = table
                it.data = sampleEntries.filterIndexed { index, _ -> index <= 1 }
                    .toJSONArray() //jsonarray of first 2 sample entries
                it.lastId = "2"
                println("previous queue data: ${it.data}")
            }

            //test
            val queue: QueueData = dbManager.getQueue(appCtx, table, batchSize = Int.MAX_VALUE, prevQueue)
            println("new queue data: ${queue.data}")

            //validate : only remainig entries are available in new queue
            assertEquals("value3", queue.data?.getJSONObject(0)?.getString("key"))
            assertEquals("value4", queue.data?.getJSONObject(1)?.getString("key"))

            assertEquals("4", queue.lastId)
            assertEquals(table, queue.table)
        }
    }

    @Test
    fun test_getQueuedDBEvents_when_CalledWithAQueue_should_returnFilledQueueWithEitherEventsOrProfileEventsData() {
        //CASE1 : if  events table is not empty, will return a filled queue with entries from events table

        //assertion: load entries in "events" table
        val dbManagerAdapter = dbManager.loadDBAdapter(appCtx)

        var sampleEntries = listOf(
            JSONObject("""{"key":"e1"}"""),
            JSONObject("""{"key":"e2"}"""),
            JSONObject("""{"key":"e3"}"""),
        )
        sampleEntries.forEach { dbManagerAdapter.storeObject(it, Table.EVENTS) }

        //test
        var queue = dbManager.getQueuedDBEvents(appCtx, Int.MAX_VALUE, null)

        //validate
        assertEquals(Table.EVENTS, queue.table)
        assertEquals("e1", queue.data?.getJSONObject(0)?.getString("key"))
        assertEquals("e2", queue.data?.getJSONObject(1)?.getString("key"))
        assertEquals("e3", queue.data?.getJSONObject(2)?.getString("key"))
        assertEquals("3", queue.lastId)

        //CASE2 : if events table is empty and profile events table is not empty, will load a queue from profile events

        //assertion:  clean events table and load entries in "profile events" table
        dbManagerAdapter.cleanupEventsFromLastId(Long.MAX_VALUE.toString(), Table.EVENTS)
        sampleEntries = listOf(
            JSONObject("""{"key":"p1"}"""),
            JSONObject("""{"key":"p2"}"""),
            JSONObject("""{"key":"p3"}"""),
        )
        sampleEntries.forEach { dbManagerAdapter.storeObject(it, Table.PROFILE_EVENTS) }

        //test
        queue = dbManager.getQueuedDBEvents(appCtx, Int.MAX_VALUE, queue)
        //validate
        assertEquals(Table.PROFILE_EVENTS, queue.table)
        assertEquals("p1", queue.data?.getJSONObject(0)?.getString("key"))
        assertEquals("p2", queue.data?.getJSONObject(1)?.getString("key"))
        assertEquals("p3", queue.data?.getJSONObject(2)?.getString("key"))
        assertEquals("3", queue.lastId)

        //CASE3 if  both "events" and "profile" events is  empty, will return empty queue
        //assertion:  clean events table and "profile events" table
        dbManagerAdapter.cleanupEventsFromLastId(Long.MAX_VALUE.toString(), Table.EVENTS)
        dbManagerAdapter.cleanupEventsFromLastId(Long.MAX_VALUE.toString(), Table.PROFILE_EVENTS)

        queue = dbManager.getQueuedDBEvents(appCtx, Int.MAX_VALUE, queue)
        //validate
        assertTrue(queue.isEmpty)
    }

    @Test
    fun test_getQueuedEvents_when_called_should_callOtherFunctions() {
        dbManagerSpy.getQueuedEvents(appCtx, Int.MAX_VALUE, null, EventGroup.PUSH_NOTIFICATION_VIEWED)
        Mockito.verify(dbManagerSpy, Mockito.times(1))
            .getPushNotificationViewedQueuedEvents(appCtx, Int.MAX_VALUE, null)

        arrayOf(EventGroup.REGULAR).forEach {
            val spy = Mockito.spy(dbManager)
            spy.getQueuedEvents(appCtx, Int.MAX_VALUE, null, it)
            Mockito.verify(spy, Mockito.times(1)).getQueuedDBEvents(appCtx, Int.MAX_VALUE, null)
            spy.loadDBAdapter(appCtx).deleteDB()
        }
    }

    @Test
    fun test_queueEventToDB_when_called_should_storeDataInEventOrProfileEventTable() {
        val json = JSONObject().also { it.put("name", "a1") }

        //test
        dbManager.queueEventToDB(appCtx, json, Constants.PROFILE_EVENT)

        //validate
        dbAdapter.fetchEvents(Table.PROFILE_EVENTS, Int.MAX_VALUE).let {
            println(" entries : $it")
            assertNotNull(it)
            assertEquals(1, it.length())
            assertEquals("a1", it.getJSONArray("1").getJSONObject(0).getString("name"))

        }

        //test
        dbManager.queueEventToDB(appCtx, json, Constants.PROFILE_EVENT + 1)

        //validate
        dbAdapter.fetchEvents(Table.EVENTS, Int.MAX_VALUE).let {
            println(" entries : $it")
            assertNotNull(it)
            assertEquals(1, it.length())
            assertEquals("a1", it.getJSONArray("1").getJSONObject(0).getString("name"))

        }
    }

    @Test
    fun test_queuePushNotificationViewedEventToDB_when_Called_should_StoreDataInPushNotificationViewedTable() {
        val json = JSONObject().also { it.put("name", "a1") }

        //test
        dbManager.queuePushNotificationViewedEventToDB(appCtx, json)

        //validate
        dbAdapter.fetchEvents(Table.PUSH_NOTIFICATION_VIEWED, Int.MAX_VALUE).let {
            println(" entries : $it")
            assertNotNull(it)
            assertEquals(1, it.length())
            assertEquals("a1", it.getJSONArray("1").getJSONObject(0).getString("name"))

        }
    }

    private fun List<JSONObject>.toJSONArray(): JSONArray {
        val arr = JSONArray()
        this.forEach { arr.put(it) }
        return arr
    }
}