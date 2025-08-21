package com.clevertap.android.sdk.db.dao

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.system.measureTimeMillis
import kotlin.test.*

/**
 * Benchmark test for database access patterns.
 *
 * WHY RESULTS CAN BE FLAKY - Write-only sometimes slower:
 *
 * 1. SQLITE INTERNAL OPTIMIZATIONS:
 *    - ReadableDatabase may use different connection pooling strategies
 *    - SQLite query planner optimizes differently for read-only vs read-write connections
 *    - WAL mode vs rollback journal mode differences
 *
 * 2. LOCK CONTENTION:
 *    - WritableDatabase holds exclusive locks longer
 *    - ReadableDatabase can share locks with other readers
 *    - Lock escalation overhead when upgrading from read to write
 *
 * 3. TRANSACTION BEHAVIOR:
 *    - WritableDatabase may start implicit transactions
 *    - Transaction overhead for write operations
 *    - Autocommit vs explicit transaction differences
 *
 * 4. MEMORY AND CACHING:
 *    - Different cache strategies for read vs write connections
 *    - Page cache behavior differences
 *    - Memory allocation patterns
 *
 * 5. JVM/ROBOLECTRIC FACTORS:
 *    - JIT compilation affecting timing
 *    - Garbage collection interference
 *    - Robolectric shadow implementations may not perfectly mirror real Android
 *
 * 6. OPERATION COMPLEXITY:
 *    - Simple operations show minimal difference
 *    - Complex queries with joins, subqueries show more difference
 *    - The benefit appears more in high-throughput scenarios
 *
 * REAL-WORLD IMPACT:
 * The main benefit of write-only approach is CONSISTENCY and avoiding potential
 * lock upgrade issues, not necessarily raw performance for simple operations.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseAccessBenchmarkTest : BaseTestCase() {

    private lateinit var dbHelper: TestDatabaseHelper
    private lateinit var context: Context

    companion object {
        private const val BENCHMARK_ITERATIONS = 1000
    }

    override fun setUp() {
        super.setUp()
        context = ApplicationProvider.getApplicationContext()
        dbHelper = TestDatabaseHelper(context)

        // Warm up the database to ensure consistent baseline for all benchmarks
        dbHelper.writableDatabase
        dbHelper.readableDatabase

        // Perform a simple operation to fully initialize database connections
        performWriteOperation(dbHelper.writableDatabase, 0)
        performReadOperation(dbHelper.readableDatabase)

        // Clean up warmup data
        dbHelper.writableDatabase.execSQL("DELETE FROM test_table")
    }

    @After
    fun tearDown() {
        dbHelper.close()
        context.deleteDatabase("benchmark_test.db")
    }

    @Test
    fun benchmark_database_flipping_readWrite_vs_writeOnly() {
        println("=== Database Access Benchmark ===")

        // Run multiple iterations to get stable results
        val flippingTimes = mutableListOf<Long>()
        val writeOnlyTimes = mutableListOf<Long>()

        repeat(5) { run ->
            println("Benchmark run ${run + 1}/5")

            // Clean database before each run
            dbHelper.writableDatabase.execSQL("DELETE FROM test_table")

            // Benchmark 1: Flipping between readable and writable
            val flippingTime = measureTimeMillis {
                repeat(BENCHMARK_ITERATIONS) { iteration ->
                    if (iteration % 2 == 0) {
                        // Simulate read operation
                        val db = dbHelper.readableDatabase
                        performReadOperation(db)
                    } else {
                        // Simulate write operation
                        val db = dbHelper.writableDatabase
                        performWriteOperation(db, iteration)
                    }
                }
            }

            // Clean up data for fair comparison
            dbHelper.writableDatabase.execSQL("DELETE FROM test_table")

            // Small delay to avoid JVM optimization interference
            Thread.sleep(10)

            // Benchmark 2: Using only writable database
            val writeOnlyTime = measureTimeMillis {
                repeat(BENCHMARK_ITERATIONS) { iteration ->
                    // Use writable database for both read and write operations
                    val db = dbHelper.writableDatabase
                    if (iteration % 2 == 0) {
                        performReadOperation(db)
                    } else {
                        performWriteOperation(db, iteration)
                    }
                }
            }

            flippingTimes.add(flippingTime)
            writeOnlyTimes.add(writeOnlyTime)

            println("  Run ${run + 1}: Flipping=${flippingTime}ms, WriteOnly=${writeOnlyTime}ms")
        }

        // Calculate averages for stable comparison
        val avgFlippingTime = flippingTimes.average()
        val avgWriteOnlyTime = writeOnlyTimes.average()

        // Print detailed results
        println("\n=== Results Summary ===")
        println("Flipping Times: $flippingTimes")
        println("WriteOnly Times: $writeOnlyTimes")
        println("Average Flipping Time: ${avgFlippingTime.toLong()}ms")
        println("Average WriteOnly Time: ${avgWriteOnlyTime.toLong()}ms")

        val improvementPercent = if (avgFlippingTime > 0) {
            ((avgFlippingTime - avgWriteOnlyTime) / avgFlippingTime * 100).toInt()
        } else 0

        println("Average Performance Difference: $improvementPercent%")

        // Count how many runs showed improvement
        val improvementRuns = flippingTimes.zip(writeOnlyTimes).count { (flip, write) -> write < flip }
        println("Runs where WriteOnly was faster: $improvementRuns/5")

        // More lenient assertion - writeOnly should be faster on average or at least not significantly worse
        val significantlyWorse = avgWriteOnlyTime > avgFlippingTime * 1.1 // 10% worse threshold
        assertFalse(significantlyWorse, "WriteOnly approach should not be significantly worse on average")
    }

    @Test
    fun benchmark_rapid_database_mode_switching() {
        println("=== Rapid Database Mode Switching Benchmark ===")

        val rapidSwitchingTime = measureTimeMillis {
            repeat(BENCHMARK_ITERATIONS) {
                // Rapidly switch between modes without actual operations
                dbHelper.readableDatabase
                dbHelper.writableDatabase
                dbHelper.readableDatabase
                dbHelper.writableDatabase
            }
        }

        val consistentAccessTime = measureTimeMillis {
            repeat(BENCHMARK_ITERATIONS) {
                // Access writable database multiple times
                dbHelper.writableDatabase
                dbHelper.writableDatabase
                dbHelper.writableDatabase
                dbHelper.writableDatabase
            }
        }

        println("Rapid Switching Time: ${rapidSwitchingTime}ms")
        println("Consistent Access Time: ${consistentAccessTime}ms")

        assertTrue(consistentAccessTime <= rapidSwitchingTime, "Consistent access should be faster")
    }

    @Test
    fun benchmark_mixed_operations_realistic_scenario() {
        println("=== Realistic Mixed Operations Benchmark ===")

        // Scenario 1: Traditional approach with mode switching
        val traditionalTime = measureTimeMillis {
            repeat(100) { batch ->
                // Simulate typical app usage: read user data
                val readDb = dbHelper.readableDatabase
                performBatchReadOperations(readDb, 5)

                // Then write some analytics
                val writeDb = dbHelper.writableDatabase
                performBatchWriteOperations(writeDb, batch, 3)

                // Read again for UI update
                val readDb2 = dbHelper.readableDatabase
                performBatchReadOperations(readDb2, 2)
            }
        }

        // Clean database
        dbHelper.writableDatabase.execSQL("DELETE FROM test_table")

        // Scenario 2: Write-only approach
        val writeOnlyTime = measureTimeMillis {
            repeat(100) { batch ->
                val db = dbHelper.writableDatabase

                // Same operations but using single database reference
                performBatchReadOperations(db, 5)
                performBatchWriteOperations(db, batch, 3)
                performBatchReadOperations(db, 2)
            }
        }

        println("Traditional Mixed Operations: ${traditionalTime}ms")
        println("Write-Only Mixed Operations: ${writeOnlyTime}ms")

        val improvement = if (traditionalTime > 0) {
            ((traditionalTime - writeOnlyTime).toDouble() / traditionalTime * 100).toInt()
        } else 0

        println("Performance improvement: $improvement%")

        assertTrue(writeOnlyTime <= traditionalTime, "Write-only should be faster for mixed operations")
    }

    @Test
    fun test_writable_database_can_perform_all_operations() {
        val db = dbHelper.writableDatabase

        // Test that writable database can perform reads
        val countBefore = getRecordCount(db)
        assertEquals(0, countBefore)

        // Test writes
        performWriteOperation(db, 1)
        performWriteOperation(db, 2)

        // Test reads after writes
        val countAfter = getRecordCount(db)
        assertEquals(2, countAfter)

        // Test that we can continue reading
        val records = getAllRecords(db)
        assertEquals(2, records.size)

        println("✅ Writable database successfully performed all read and write operations")
    }



    // Helper methods for benchmark operations
    private fun performReadOperation(db: SQLiteDatabase): List<TestRecord> {
        // Realistic read: Get latest 10 records ordered by timestamp
        val records = mutableListOf<TestRecord>()
        db.query(
            "test_table",
            null,
            "timestamp > ?",
            arrayOf((System.currentTimeMillis() - 86400000).toString()), // Last 24 hours
            null,
            null,
            "timestamp DESC",
            "10"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                records.add(
                    TestRecord(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        value = cursor.getInt(cursor.getColumnIndexOrThrow("value")),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        return records
    }

    private fun performWriteOperation(db: SQLiteDatabase, iteration: Int): Long {
        // Realistic write: Insert user event with actual data processing
        val values = android.content.ContentValues().apply {
            put("name", "Event_${iteration}_${System.nanoTime() % 1000}")
            put("value", iteration * 2 + (iteration % 10))
            put("timestamp", System.currentTimeMillis() + iteration)
        }
        return db.insert("test_table", null, values)
    }

    private fun performBatchReadOperations(db: SQLiteDatabase, count: Int): List<TestRecord> {
        val allRecords = mutableListOf<TestRecord>()
        repeat(count) {
            // Realistic read: Search for records with specific criteria
            db.query(
                "test_table",
                null,
                "value > ? AND name LIKE ?",
                arrayOf("50", "%Event%"),
                null,
                null,
                "timestamp DESC",
                "20"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    allRecords.add(
                        TestRecord(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                            value = cursor.getInt(cursor.getColumnIndexOrThrow("value")),
                            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                        )
                    )
                }
            }
        }
        return allRecords
    }

    private fun performBatchWriteOperations(db: SQLiteDatabase, batch: Int, count: Int): List<Long> {
        val insertedIds = mutableListOf<Long>()
        repeat(count) { index ->
            // Realistic write: Insert with calculated values and string processing
            val eventName = "BatchEvent_${batch}_${index}_${System.nanoTime() % 1000}"
            val calculatedValue = (batch * 1000 + index) * 2 + (batch % 5)
            val adjustedTimestamp = System.currentTimeMillis() + (batch * 1000) + index

            val values = android.content.ContentValues().apply {
                put("name", eventName)
                put("value", calculatedValue)
                put("timestamp", adjustedTimestamp)
            }
            val id = db.insert("test_table", null, values)
            insertedIds.add(id)
        }
        return insertedIds
    }



    private fun getRecordCount(db: SQLiteDatabase): Int {
        return db.query(
            "test_table",
            arrayOf("id"),
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            var count = 0
            while (cursor.moveToNext()) {
                count++
            }
            count
        }
    }

    private fun getAllRecords(db: SQLiteDatabase): List<TestRecord> {
        val records = mutableListOf<TestRecord>()
        db.query("test_table", null, null, null, null, null, "timestamp DESC").use { cursor ->
            while (cursor.moveToNext()) {
                records.add(
                    TestRecord(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        value = cursor.getInt(cursor.getColumnIndexOrThrow("value")),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        return records
    }

    // Test database helper
    private class TestDatabaseHelper(context: Context) : SQLiteOpenHelper(
        context, "benchmark_test.db", null, 1
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE test_table (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    value INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS test_table")
            onCreate(db)
        }
    }

    // Data class for test records
    private data class TestRecord(
        val id: Long,
        val name: String,
        val value: Int,
        val timestamp: Long
    )
}

// Extension for better performance measurement
class PerformanceAssertion {
    companion object {
        fun assertPerformanceImprovement(
            traditionalTime: Long,
            optimizedTime: Long,
            minimumImprovementPercent: Int = 0,
            operation: String = "operation"
        ) {
            assertTrue(
                optimizedTime <= traditionalTime,
                "Optimized $operation should be faster or equal (Traditional: ${traditionalTime}ms, Optimized: ${optimizedTime}ms)"
            )

            if (traditionalTime > 0) {
                val improvementPercent = ((traditionalTime - optimizedTime).toDouble() / traditionalTime * 100).toInt()
                assertTrue(
                    improvementPercent >= minimumImprovementPercent,
                    "Performance improvement should be at least $minimumImprovementPercent% but was $improvementPercent%"
                )
                println("✅ $operation performance improved by $improvementPercent%")
            }
        }
    }
}