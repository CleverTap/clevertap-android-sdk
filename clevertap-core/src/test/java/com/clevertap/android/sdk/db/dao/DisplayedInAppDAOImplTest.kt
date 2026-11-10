package com.clevertap.android.sdk.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.TestClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DisplayedInAppDAOImplTest {

    private lateinit var context: Context
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var logger: Logger
    private lateinit var dao: DisplayedInAppDAO

    private val dbName = "test_ct_displayed_inapp"
    private val mockTime = 1609459200000L // 2021-01-01 00:00:00

    private val inApp1 = DisplayedInAppData("inapp_1", """{"ti":1,"type":"interstitial"}""", 1609545600L)
    private val inApp2 = DisplayedInAppData("inapp_2", """{"ti":2,"type":"alert"}""", 1609632000L)

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        logger = mockk(relaxed = true)
        val config = CleverTapInstanceConfig.createInstance(context, "accountID", "token", "sk1")
        databaseHelper = DatabaseHelper(context, config.accountId, dbName, logger)
        dao = DisplayedInAppDAOImpl(databaseHelper, logger, TestClock(mockTime))
    }

    @After
    fun tearDown() {
        databaseHelper.close()
        context.deleteDatabase(dbName)
    }

    // ============================================
    // SAVE
    // ============================================

    @Test
    fun `save stores the in-app with its time to live`() {
        assertTrue(dao.save(inApp1))

        assertEquals(inApp1, dao.fetch(inApp1.inAppId))
    }

    @Test
    fun `save keeps only one row, the last saved in-app`() {
        assertTrue(dao.save(inApp1))
        assertTrue(dao.save(inApp2))

        assertNull(dao.fetch(inApp1.inAppId))
        assertEquals(inApp2, dao.fetch(inApp2.inAppId))
        assertEquals(1, rowCount())
    }

    @Test
    fun `save replaces the data and time to live of the same in-app`() {
        assertTrue(dao.save(inApp1))
        val updated = inApp1.copy(inAppData = """{"ti":1,"type":"cover"}""", timeToLive = inApp1.timeToLive + 60)

        assertTrue(dao.save(updated))

        assertEquals(updated, dao.fetch(inApp1.inAppId))
        assertEquals(1, rowCount())
    }

    @Test
    fun `save writes the creation time from the clock`() {
        assertTrue(dao.save(inApp1))

        databaseHelper.readableDatabase.rawQuery(
            "SELECT ${Column.CREATED_AT} FROM ${Table.DISPLAYED_INAPP.tableName};", null
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals(mockTime, it.getLong(0))
        }
    }

    @Test
    fun `save returns false when the memory threshold is exceeded`() {
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.belowMemThreshold() } returns false
        val dao = DisplayedInAppDAOImpl(dbHelper, logger)

        assertFalse(dao.save(inApp1))
        verify(exactly = 0) { dbHelper.writableDatabase }
    }

    @Test
    fun `save returns false and ends the transaction when the insert fails`() {
        val database = mockk<SQLiteDatabase>(relaxed = true)
        every { database.insertWithOnConflict(any(), any(), any(), any()) } throws SQLiteException()
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.belowMemThreshold() } returns true
        every { dbHelper.writableDatabase } returns database
        val dao = DisplayedInAppDAOImpl(dbHelper, logger)

        assertFalse(dao.save(inApp1))

        // Rolled back, so the old row is kept.
        verify(exactly = 1) { database.endTransaction() }
        verify(exactly = 0) { database.setTransactionSuccessful() }
    }

    // ============================================
    // FETCH
    // ============================================

    @Test
    fun `fetch returns null when nothing is saved`() {
        assertNull(dao.fetch(inApp1.inAppId))
    }

    @Test
    fun `fetch returns null for another in-app id`() {
        dao.save(inApp1)

        assertNull(dao.fetch(inApp2.inAppId))
    }

    @Test
    fun `fetch returns null when the read fails`() {
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every {
            dbHelper.readableDatabase.query(any(), any(), any(), any(), any(), any(), any(), any())
        } throws SQLiteException()
        val dao = DisplayedInAppDAOImpl(dbHelper, logger)

        assertNull(dao.fetch(inApp1.inAppId))
    }

    // ============================================
    // REMOVE / CLEAR
    // ============================================

    @Test
    fun `remove deletes the in-app and returns false when it is not there`() {
        dao.save(inApp1)

        assertTrue(dao.remove(inApp1.inAppId))
        assertNull(dao.fetch(inApp1.inAppId))
        assertFalse(dao.remove(inApp1.inAppId))
    }

    @Test
    fun `remove leaves another in-app alone`() {
        dao.save(inApp1)

        assertFalse(dao.remove(inApp2.inAppId))
        assertEquals(inApp1, dao.fetch(inApp1.inAppId))
    }

    @Test
    fun `remove returns false when the delete fails`() {
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.writableDatabase.delete(any(), any(), any()) } throws SQLiteException()
        val dao = DisplayedInAppDAOImpl(dbHelper, logger)

        assertFalse(dao.remove(inApp1.inAppId))
    }

    @Test
    fun `clearAll empties the table`() {
        dao.save(inApp1)

        assertTrue(dao.clearAll())
        assertEquals(0, rowCount())
        assertTrue(dao.clearAll()) // empty table is fine
    }

    @Test
    fun `clearAll returns false when the delete fails`() {
        val dbHelper = mockk<DatabaseHelper>(relaxed = true)
        every { dbHelper.writableDatabase.delete(any(), any(), any()) } throws SQLiteException()
        val dao = DisplayedInAppDAOImpl(dbHelper, logger)

        assertFalse(dao.clearAll())
    }

    @Test
    fun `the displayed in-app table is separate from the delayed in-apps table`() {
        val delayedDAO = DelayedLegacyInAppDAOImpl(databaseHelper, logger, Table.DELAYED_LEGACY_INAPPS)
        delayedDAO.insertBatch(listOf(DelayedLegacyInAppData("delayed_1", 5, "delayed")))
        dao.save(inApp1)

        assertNull(delayedDAO.fetchSingleInApp(inApp1.inAppId))
        assertNull(dao.fetch("delayed_1"))

        // Clearing delayed in-apps (done at every app start) does not touch the displayed in-app.
        delayedDAO.clearAll()
        assertEquals(inApp1, dao.fetch(inApp1.inAppId))
    }

    private fun rowCount(): Int =
        databaseHelper.readableDatabase.rawQuery(
            "SELECT count(*) FROM ${Table.DISPLAYED_INAPP.tableName};", null
        ).use {
            it.moveToFirst()
            it.getInt(0)
        }
}
