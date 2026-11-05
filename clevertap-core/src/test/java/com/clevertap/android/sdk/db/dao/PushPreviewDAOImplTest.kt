package com.clevertap.android.sdk.db.dao

import TestCryptHandler
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.cryption.ICryptHandler
import com.clevertap.android.sdk.db.Column
import com.clevertap.android.sdk.db.DBEncryptionHandler
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.db.Table
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class PushPreviewDAOImplTest : BaseTestCase() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var clock: TestClock
    private lateinit var dao: PushPreviewDAO

    private val nowSeconds = 1_793_896_379L
    private val table = Table.PUSH_PREVIEWS.tableName

    override fun setUp() {
        super.setUp()
        clock = TestClock(nowSeconds * 1000)
        dbHelper = DatabaseHelper(appCtx, "acc-1", "push_preview_test_db", TestLogger())
        dao = buildDao(DBEncryptionHandler(TestCryptHandler(), TestLogger(), EncryptionLevel.NONE))
    }

    @After
    fun cleanup() {
        dbHelper.deleteDatabase()
    }

    private fun buildDao(encryption: DBEncryptionHandler) =
        PushPreviewDAOImpl(dbHelper, TestLogger(), encryption, clock)

    private fun rowCount(): Int =
        dbHelper.readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use {
            it.moveToFirst()
            it.getInt(0)
        }

    private fun rawData(previewKey: String): String? =
        dbHelper.readableDatabase
            .query(table, arrayOf(Column.DATA), "${Column.PREVIEW_KEY} = ?", arrayOf(previewKey), null, null, null)
            .use { if (it.moveToFirst()) it.getString(0) else null }

    @Test
    fun `save then find returns the data`() {
        val data = """{"wzrk_inapp_type":"custom-html"}"""
        dao.save("k1", data, nowSeconds + 60, "1_2")

        assertEquals(data, dao.find("k1", nowSeconds))
    }

    @Test
    fun `find returns null for an unknown key`() {
        dao.save("k1", "{}", nowSeconds + 60, null)

        assertNull(dao.find("k2", nowSeconds))
    }

    @Test
    fun `find ignores a row once it has expired`() {
        dao.save("k1", "{}", nowSeconds + 10, null)

        assertEquals("{}", dao.find("k1", nowSeconds + 9))
        assertNull(dao.find("k1", nowSeconds + 10))
        assertNull(dao.find("k1", nowSeconds + 11))
    }

    @Test
    fun `saving the same key again keeps one row and refreshes its expiry`() {
        dao.save("k1", """{"v":1}""", nowSeconds + 10, null)
        dao.save("k1", """{"v":1}""", nowSeconds + 1000, null)

        assertEquals(1, rowCount())
        assertEquals("""{"v":1}""", dao.find("k1", nowSeconds + 500))
    }

    @Test
    fun `expired rows are removed on the next save`() {
        dao.save("old", "{}", nowSeconds + 1, null)
        clock.advanceTime(2_000)
        dao.save("new", "{}", nowSeconds + 1000, null)

        assertEquals(1, rowCount())
        assertNull(dao.find("old", clock.currentTimeSeconds()))
    }

    @Test
    fun `trim keeps only the newest fifty rows`() {
        for (i in 1..53) dao.save("k$i", "{}", nowSeconds + 1000, null)

        assertEquals(50, rowCount())
        for (i in 1..3) assertNull(dao.find("k$i", nowSeconds), "k$i must have been trimmed")
        for (i in 4..53) assertEquals("{}", dao.find("k$i", nowSeconds), "k$i must survive")
    }

    @Test
    fun `trim leaves exactly fifty rows alone`() {
        for (i in 1..50) dao.save("k$i", "{}", nowSeconds + 1000, null)

        assertEquals(50, rowCount())
        assertEquals("{}", dao.find("k1", nowSeconds))
    }

    @Test
    fun `trim leaves a small table alone`() {
        for (i in 1..20) dao.save("k$i", "{}", nowSeconds + 1000, null)

        assertEquals(20, rowCount())
    }

    @Test
    fun `data is written through the encryption handler and read back through it`() {
        val reversing = object : ICryptHandler {
            override fun encryptSafe(plainText: String): String = plainText.reversed()
            override fun decryptSafe(cipherText: String): String = cipherText.reversed()
            override fun encrypt(plainText: String): String = plainText.reversed()
            override fun decrypt(cipherText: String): String = cipherText.reversed()
            override fun updateMigrationFailureCount(migrationSuccessful: Boolean) = Unit
            override fun decryptWithAlgorithm(cipherText: String, algorithm: CryptHandler.EncryptionAlgorithm): String =
                cipherText.reversed()
        }
        val encryptingDao = buildDao(DBEncryptionHandler(reversing, TestLogger(), EncryptionLevel.FULL_DATA))

        encryptingDao.save("k1", """{"a":1}""", nowSeconds + 60, null)

        assertEquals("""}1:"a"{""", rawData("k1"))
        assertNotEquals("""{"a":1}""", rawData("k1"))
        assertEquals("""{"a":1}""", encryptingDao.find("k1", nowSeconds))
    }

    @Test
    fun `save on a broken table logs and does not throw`() {
        dbHelper.writableDatabase.execSQL("DROP TABLE $table")

        dao.save("k1", "{}", nowSeconds + 60, null)
        assertNull(dao.find("k1", nowSeconds))
    }
}
