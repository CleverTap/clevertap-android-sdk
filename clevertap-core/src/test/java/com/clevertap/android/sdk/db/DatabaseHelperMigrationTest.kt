package com.clevertap.android.sdk.db

import android.database.sqlite.SQLiteDatabase
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.db.Table.INBOX_MESSAGES
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DatabaseHelperMigrationTest : BaseTestCase() {

    private val dbName = "migration_test_db"
    private lateinit var dbHelper: DatabaseHelper

    @After
    fun cleanup() {
        if (this::dbHelper.isInitialized) {
            dbHelper.deleteDatabase()
        } else {
            // seeded but never opened via DatabaseHelper — wipe the raw file
            appCtx.deleteDatabase(dbName)
        }
    }

    /**
     * Seed a database file that mimics the schema a SDK ≤ 8.1.0 user would have
     * on disk — version 6, the v6 inboxMessages schema (no `source` column),
     * and no pending-action tables.
     */
    private fun seedV6Database(): SQLiteDatabase {
        val path = appCtx.getDatabasePath(dbName)
        path.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(path, null)
        db.version = 6
        db.execSQL(
            """
            CREATE TABLE ${INBOX_MESSAGES.tableName} (
                ${Column.ID} STRING NOT NULL,
                ${Column.DATA} TEXT NOT NULL,
                ${Column.WZRKPARAMS} TEXT NOT NULL,
                ${Column.CAMPAIGN} STRING NOT NULL,
                ${Column.TAGS} TEXT NOT NULL,
                ${Column.IS_READ} INTEGER NOT NULL DEFAULT 0,
                ${Column.EXPIRES} INTEGER NOT NULL,
                ${Column.CREATED_AT} INTEGER NOT NULL,
                ${Column.USER_ID} STRING NOT NULL
            );
            """.trimIndent()
        )
        return db
    }

    @Test
    fun `upgrade from v6 adds source column with V1 default and preserves existing rows`() {
        val seed = seedV6Database()
        seed.execSQL(
            "INSERT INTO ${INBOX_MESSAGES.tableName} " +
                "(${Column.ID}, ${Column.DATA}, ${Column.WZRKPARAMS}, ${Column.CAMPAIGN}, " +
                " ${Column.TAGS}, ${Column.IS_READ}, ${Column.EXPIRES}, ${Column.CREATED_AT}, " +
                " ${Column.USER_ID}) " +
                "VALUES ('m1', '{}', '{}', 'cp1', '', 0, 0, 0, 'user1');"
        )
        seed.close()

        val config = CleverTapInstanceConfig.createInstance(appCtx, "acc-1", "token")
        dbHelper = DatabaseHelper(appCtx, config.accountId, dbName, config.logger)

        // Opening a writable connection forces onUpgrade(6, 7).
        dbHelper.readableDatabase.use { upgraded ->
            val cursor = upgraded.rawQuery(
                "SELECT ${Column.ID}, ${Column.SOURCE} FROM ${INBOX_MESSAGES.tableName};",
                null
            )
            cursor.use {
                assertTrue(it.moveToFirst())
                assertEquals("m1", it.getString(0))
                assertEquals("V1", it.getString(1))
            }
        }
    }

    @Test
    fun `upgrade from v6 creates pending-action tables`() {
        seedV6Database().close()

        val config = CleverTapInstanceConfig.createInstance(appCtx, "acc-1", "token")
        dbHelper = DatabaseHelper(appCtx, config.accountId, dbName, config.logger)

        dbHelper.readableDatabase.use { upgraded ->
            val names = listOf(
                Table.INBOX_PENDING_DELETES.tableName,
                Table.INBOX_PENDING_READS.tableName
            )
            for (name in names) {
                val c = upgraded.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?;",
                    arrayOf(name)
                )
                c.use {
                    assertTrue(it.moveToFirst(), "Table $name should exist after V7 migration")
                }
            }
        }
    }

    @Test
    fun `fresh install creates inboxMessages with source column already present`() {
        val config = CleverTapInstanceConfig.createInstance(appCtx, "acc-1", "token")
        dbHelper = DatabaseHelper(appCtx, config.accountId, dbName, config.logger)

        dbHelper.readableDatabase.use { db ->
            val c = db.rawQuery("PRAGMA table_info(${INBOX_MESSAGES.tableName});", null)
            val columnNames = mutableListOf<String>()
            c.use {
                val nameIndex = it.getColumnIndexOrThrow("name")
                while (it.moveToNext()) columnNames.add(it.getString(nameIndex))
            }
            assertTrue(
                Column.SOURCE in columnNames,
                "source column must exist on fresh install"
            )
        }
    }
}
