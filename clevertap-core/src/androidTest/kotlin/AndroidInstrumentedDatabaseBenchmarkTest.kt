package com.clevertap.android.sdk.db.dao

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Instrumented database benchmark test for real Android devices.
 *
 * This test runs on actual Android devices/emulators and provides realistic
 * performance measurements that reflect real user experience.
 */
@RunWith(AndroidJUnit4::class)
class AndroidInstrumentedDatabaseBenchmarkTest {

    private lateinit var dbHelper: RealDeviceBenchmarkHelper
    private lateinit var context: Context

    companion object {
        private const val BENCHMARK_ITERATIONS = 1000 // Reduced for real device performance
        private const val BENCHMARK_RUNS = 3
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dbHelper = RealDeviceBenchmarkHelper(context)

        // Device info logging
        val deviceInfo = getDeviceInfo()
        println("=== Running on Real Android Device ===")
        println(deviceInfo)

        // Warm up database connections
        dbHelper.writableDatabase
        dbHelper.readableDatabase

        // Perform warmup operations
        performWriteOperation(dbHelper.writableDatabase, 0)
        performReadOperation(dbHelper.readableDatabase)

        // Clean up warmup data
        dbHelper.writableDatabase.execSQL("DELETE FROM device_benchmark_table")
    }

    @After
    fun tearDown() {
        dbHelper.close()
        context.deleteDatabase("real_device_benchmark.db")
    }

    @Test
    fun benchmark_real_device_database_access() {
        println("=== Real Android Device Database Benchmark ===")

        val results = DeviceBenchmarkResults()

        repeat(BENCHMARK_RUNS) { run ->
            println("Run ${run + 1}/$BENCHMARK_RUNS")

            // Clean database
            dbHelper.writableDatabase.execSQL("DELETE FROM device_benchmark_table")

            // Force garbage collection to minimize interference
            System.gc()
            Thread.sleep(100)

            // Benchmark 1: Mode switching approach
            val modeSwitchingTime = benchmarkModeSwitching()

            // Clean database and brief pause
            dbHelper.writableDatabase.execSQL("DELETE FROM device_benchmark_table")
            System.gc()
            Thread.sleep(100)

            // Benchmark 2: Write-only approach
            val writeOnlyTime = benchmarkWriteOnly()

            results.addRun(modeSwitchingTime, writeOnlyTime)
            println("  Run ${run + 1}: ModeSwitching=${modeSwitchingTime}ms, WriteOnly=${writeOnlyTime}ms")
        }

        results.printDeviceSummary()
    }

    @Test
    fun benchmark_real_device_transaction_performance() {
        println("=== Real Device Transaction Performance ===")

        // Transactions often show clearer differences on real devices
        val transactionSwitchingTime = measureTimeMillis {
            dbHelper.writableDatabase.beginTransaction()
            try {
                repeat(150) { iteration ->
                    if (iteration % 3 == 0) {
                        performReadOperation(dbHelper.readableDatabase)
                    } else {
                        performWriteOperation(dbHelper.writableDatabase, iteration)
                    }
                }
                dbHelper.writableDatabase.setTransactionSuccessful()
            } finally {
                dbHelper.writableDatabase.endTransaction()
            }
        }

        // Clean database
        dbHelper.writableDatabase.execSQL("DELETE FROM device_benchmark_table")
        System.gc()
        Thread.sleep(100)

        val transactionWriteOnlyTime = measureTimeMillis {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                repeat(150) { iteration ->
                    if (iteration % 3 == 0) {
                        performReadOperation(db)
                    } else {
                        performWriteOperation(db, iteration)
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        println("Real Device Transaction Results:")
        println("Mode Switching: ${transactionSwitchingTime}ms")
        println("Write Only: ${transactionWriteOnlyTime}ms")

        val improvement = if (transactionSwitchingTime > 0) {
            ((transactionSwitchingTime - transactionWriteOnlyTime).toDouble() / transactionSwitchingTime * 100)
        } else 0.0

        println("Performance difference: ${improvement.toInt()}%")

        // On real devices, transaction behavior should show clearer patterns
        println("Note: Transaction performance differences are typically more pronounced on real devices")
    }

    @Test
    fun benchmark_real_device_complex_operations() {
        println("=== Real Device Complex Operations ===")

        // Populate with more realistic dataset
        populateRealisticTestData(500)

        val complexSwitchingTime = measureTimeMillis {
            repeat(50) { iteration ->
                if (iteration % 2 == 0) {
                    performComplexReadQuery(dbHelper.readableDatabase, iteration)
                } else {
                    performComplexWriteWithUpdate(dbHelper.writableDatabase, iteration)
                }
            }
        }

        // Reset data
        dbHelper.writableDatabase.execSQL("DELETE FROM device_benchmark_table")
        populateRealisticTestData(500)
        System.gc()
        Thread.sleep(100)

        val complexWriteOnlyTime = measureTimeMillis {
            val db = dbHelper.writableDatabase
            repeat(50) { iteration ->
                if (iteration % 2 == 0) {
                    performComplexReadQuery(db, iteration)
                } else {
                    performComplexWriteWithUpdate(db, iteration)
                }
            }
        }

        println("Complex Operations - Mode Switching: ${complexSwitchingTime}ms")
        println("Complex Operations - Write Only: ${complexWriteOnlyTime}ms")

        val improvement = if (complexSwitchingTime > 0) {
            ((complexSwitchingTime - complexWriteOnlyTime).toDouble() / complexSwitchingTime * 100)
        } else 0.0

        println("Complex operations performance difference: ${improvement.toInt()}%")
    }

    // Benchmark implementation methods
    private fun benchmarkModeSwitching(): Long {
        return measureTimeMillis {
            repeat(BENCHMARK_ITERATIONS) { iteration ->
                if (iteration % 2 == 0) {
                    performReadOperation(dbHelper.readableDatabase)
                } else {
                    performWriteOperation(dbHelper.writableDatabase, iteration)
                }
            }
        }
    }

    private fun benchmarkWriteOnly(): Long {
        val db = dbHelper.writableDatabase
        return measureTimeMillis {
            repeat(BENCHMARK_ITERATIONS) { iteration ->
                if (iteration % 2 == 0) {
                    performReadOperation(db)
                } else {
                    performWriteOperation(db, iteration)
                }
            }
        }
    }

    private fun populateRealisticTestData(count: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            repeat(count) { index ->
                val values = android.content.ContentValues().apply {
                    put("event_name", "UserAction_${index % 20}")
                    put("event_value", (index * 1.5).toInt())
                    put("user_category", "Category_${index % 10}")
                    put("session_id", "Session_${index / 50}")
                    put("event_timestamp", System.currentTimeMillis() - (count - index) * 1000)
                }
                db.insert("device_benchmark_table", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun performComplexReadQuery(db: SQLiteDatabase, iteration: Int): List<DeviceQueryResult> {
        val results = mutableListOf<DeviceQueryResult>()

        // Complex analytical query similar to real analytics
        val query = """
            SELECT 
                user_category,
                session_id,
                COUNT(*) as event_count,
                AVG(event_value) as avg_value,
                MAX(event_timestamp) as latest_event,
                MIN(event_timestamp) as earliest_event
            FROM device_benchmark_table 
            WHERE event_value > ? 
                AND event_name LIKE ? 
                AND event_timestamp > ?
            GROUP BY user_category, session_id
            HAVING COUNT(*) >= 2
            ORDER BY avg_value DESC, event_count DESC
            LIMIT 10
        """.trimIndent()

        db.rawQuery(
            query,
            arrayOf(
                (iteration * 5).toString(),
                "%UserAction%",
                (System.currentTimeMillis() - 3600000).toString() // Last hour
            )
        ).use { cursor ->
            while (cursor.moveToNext()) {
                results.add(
                    DeviceQueryResult(
                        userCategory = cursor.getString(0),
                        sessionId = cursor.getString(1),
                        eventCount = cursor.getInt(2),
                        avgValue = cursor.getDouble(3),
                        latestEvent = cursor.getLong(4),
                        earliestEvent = cursor.getLong(5)
                    )
                )
            }
        }
        return results
    }

    private fun performComplexWriteWithUpdate(db: SQLiteDatabase, iteration: Int): Int {
        // Insert new record
        val values = android.content.ContentValues().apply {
            put("event_name", "ComplexEvent_${iteration}")
            put("event_value", iteration * 3 + (iteration % 15))
            put("user_category", "UpdatedCategory_${iteration % 8}")
            put("session_id", "Session_${iteration / 10}")
            put("event_timestamp", System.currentTimeMillis() + iteration)
        }
        db.insert("device_benchmark_table", null, values)

        // Update existing records (realistic scenario)
        val updateValues = android.content.ContentValues().apply {
            put("event_value", iteration * 2)
        }
        return db.update(
            "device_benchmark_table",
            updateValues,
            "user_category = ? AND event_value < ?",
            arrayOf("Category_${iteration % 10}", (iteration * 2).toString())
        )
    }

    private fun performReadOperation(db: SQLiteDatabase): List<DeviceBenchmarkRecord> {
        val records = mutableListOf<DeviceBenchmarkRecord>()
        db.query(
            "device_benchmark_table",
            null,
            "event_timestamp > ? AND event_value BETWEEN ? AND ?",
            arrayOf(
                (System.currentTimeMillis() - 600000).toString(), // Last 10 minutes
                "5",
                "500"
            ),
            null,
            null,
            "event_timestamp DESC",
            "20"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                records.add(
                    DeviceBenchmarkRecord(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        eventName = cursor.getString(cursor.getColumnIndexOrThrow("event_name")),
                        eventValue = cursor.getInt(cursor.getColumnIndexOrThrow("event_value")),
                        userCategory = cursor.getString(cursor.getColumnIndexOrThrow("user_category")),
                        sessionId = cursor.getString(cursor.getColumnIndexOrThrow("session_id")),
                        eventTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("event_timestamp"))
                    )
                )
            }
        }
        return records
    }

    private fun performWriteOperation(db: SQLiteDatabase, iteration: Int): Long {
        val values = android.content.ContentValues().apply {
            put("event_name", "DeviceEvent_${iteration}_${System.nanoTime() % 1000}")
            put("event_value", iteration * 2 + (iteration % 12))
            put("user_category", "Category_${iteration % 8}")
            put("session_id", "Session_${iteration / 25}")
            put("event_timestamp", System.currentTimeMillis() + iteration)
        }
        return db.insert("device_benchmark_table", null, values)
    }

    private fun getDeviceInfo(): String {
        return """
Device: ${Build.MANUFACTURER} ${Build.MODEL}
Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
Hardware: ${Build.HARDWARE}
ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}
RAM: ${Runtime.getRuntime().maxMemory() / 1024 / 1024}MB max heap
        """.trimIndent()
    }

    // Results tracking for device testing
    private class DeviceBenchmarkResults {
        private val modeSwitchingTimes = mutableListOf<Long>()
        private val writeOnlyTimes = mutableListOf<Long>()

        fun addRun(modeSwitching: Long, writeOnly: Long) {
            modeSwitchingTimes.add(modeSwitching)
            writeOnlyTimes.add(writeOnly)
        }

        fun printDeviceSummary() {
            val avgModeSwitching = modeSwitchingTimes.average()
            val avgWriteOnly = writeOnlyTimes.average()

            println("\n=== Real Android Device Results ===")
            println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            println("Mode Switching Times: $modeSwitchingTimes")
            println("Write Only Times: $writeOnlyTimes")
            println("Average Mode Switching: ${avgModeSwitching.toLong()}ms")
            println("Average Write Only: ${avgWriteOnly.toLong()}ms")

            val improvement = ((avgModeSwitching - avgWriteOnly) / avgModeSwitching * 100)
            println("Average Performance Difference: ${improvement.toInt()}%")

            val consistentWins = modeSwitchingTimes.zip(writeOnlyTimes).count { (mode, write) ->
                write < mode
            }
            println("Runs where WriteOnly was faster: $consistentWins/${modeSwitchingTimes.size}")

            // Device-specific analysis
            analyzeDeviceResults(avgModeSwitching, avgWriteOnly)
        }

        private fun analyzeDeviceResults(avgModeSwitching: Double, avgWriteOnly: Double) {
            println("\n📱 REAL DEVICE ANALYSIS:")

            when {
                avgWriteOnly < avgModeSwitching * 0.9 -> {
                    println("✅ WriteOnly shows significant improvement (>10%)")
                    println("   - Mode switching overhead is measurable on this device")
                    println("   - Consistent writeOnly approach is beneficial")
                }
                avgWriteOnly < avgModeSwitching * 0.95 -> {
                    println("✅ WriteOnly shows moderate improvement (5-10%)")
                    println("   - Small but consistent benefit")
                }
                avgWriteOnly <= avgModeSwitching * 1.05 -> {
                    println("⚖️ Performance is roughly equivalent (±5%)")
                    println("   - Main benefit is code consistency, not performance")
                }
                else -> {
                    println("⚠️ WriteOnly is slower on this device")
                    println("   - Device-specific SQLite optimizations may favor mode switching")
                    println("   - Consider device characteristics and Android version")
                }
            }

            println("Device factors affecting results:")
            println("- Storage: ${getStorageType()}")
            println("- Memory pressure: ${getMemoryPressure()}")
            println("- CPU cores: ${Runtime.getRuntime().availableProcessors()}")
        }

        private fun getStorageType(): String {
            // Heuristic to determine storage type based on performance
            val testStart = System.currentTimeMillis()
            val testFile = java.io.File("/data/data/com.clevertap.android.sdk.test", "storage_test")
            testFile.parentFile?.mkdirs()
            testFile.writeText("test")
            testFile.delete()
            val testTime = System.currentTimeMillis() - testStart

            return when {
                testTime < 5 -> "Fast storage (likely UFS 3.0+)"
                testTime < 15 -> "Medium storage (UFS 2.x or fast eMMC)"
                else -> "Slower storage (eMMC)"
            }
        }

        private fun getMemoryPressure(): String {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val memoryUsage = (usedMemory.toDouble() / maxMemory * 100).toInt()

            return when {
                memoryUsage < 50 -> "Low ($memoryUsage%)"
                memoryUsage < 80 -> "Medium ($memoryUsage%)"
                else -> "High ($memoryUsage%)"
            }
        }
    }

    // Database helper for real device testing
    private class RealDeviceBenchmarkHelper(context: Context) : SQLiteOpenHelper(
        context, "real_device_benchmark.db", null, 1
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            // More realistic table structure for mobile analytics
            db.execSQL("""
                CREATE TABLE device_benchmark_table (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_name TEXT NOT NULL,
                    event_value INTEGER NOT NULL,
                    user_category TEXT NOT NULL,
                    session_id TEXT NOT NULL,
                    event_timestamp INTEGER NOT NULL
                )
            """.trimIndent())

            // Realistic indexes for mobile app analytics
            db.execSQL("CREATE INDEX idx_device_timestamp ON device_benchmark_table(event_timestamp)")
            db.execSQL("CREATE INDEX idx_device_category_session ON device_benchmark_table(user_category, session_id)")
            db.execSQL("CREATE INDEX idx_device_name_value ON device_benchmark_table(event_name, event_value)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS device_benchmark_table")
            onCreate(db)
        }
    }

    // Data classes for real device testing
    private data class DeviceBenchmarkRecord(
        val id: Long,
        val eventName: String,
        val eventValue: Int,
        val userCategory: String,
        val sessionId: String,
        val eventTimestamp: Long
    )

    private data class DeviceQueryResult(
        val userCategory: String,
        val sessionId: String,
        val eventCount: Int,
        val avgValue: Double,
        val latestEvent: Long,
        val earliestEvent: Long
    )
}