package com.clevertap.android.sdk.inapp

import android.content.SharedPreferences
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import kotlin.test.assertEquals

class SharedPreferencesMigrationTest : BaseTestCase() {
    private val prefNameV1 = Constants.KEY_COUNTS_PER_INAPP
    private val prefNameV2 = "${Constants.KEY_COUNTS_PER_INAPP}:deviceId1"
    private val prefNameV3 = "${Constants.KEY_COUNTS_PER_INAPP}:deviceId1:acctId1"
    override fun setUp() {

        super.setUp()
    }

    @Test
    fun testMigrate() {
        // Mock old SharedPreferences data
        val oldData = mapOf(
            "key1" to "[1,1]", "key2" to 42, "key3" to true, "key4" to "value4",
            "key5" to "[1]", "key6" to "[1,1,1]"
        )

        val prefsV1 = StorageHelper
            .getPreferences(
                appCtx,
                prefNameV1
            )
        val prefsV3 = StorageHelper
            .getPreferences(
                appCtx,
                prefNameV3
            )

        storeMapToPreference(oldData, prefsV1.edit())

        assertEquals(6, prefsV1.all.size)
        assertEquals(0, prefsV3.all.size)

        SharedPreferencesMigration(
            prefsV1,
            prefsV3,
            String::class.java
        ) { it.split(",").size == 2 }.migrate()

        assertEquals(0, prefsV1.all.size)
        assertEquals(1, prefsV3.all.size)
        assertEquals("[1,1]", prefsV3.getString("key1", null))

    }

    private fun storeMapToPreference(oldData: Map<String, Any>, editor: SharedPreferences.Editor) {
        oldData.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int ->
                    editor.putInt(key, value)

                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String ->
                    editor.putString(key, value)
            }
        }
        editor.commit()
    }

    @Test
    fun testMigrateWithDifferentDataTypes() {
        val oldData = mapOf(
            "key1" to "value1",
            "key2" to 42,
            "key3" to true,
            "key4" to 3.14f,
            "key5" to 1234567890L
        )

        val prefsV1 = StorageHelper.getPreferences(appCtx, prefNameV1)
        val prefsV3 = StorageHelper.getPreferences(appCtx, prefNameV3)

        // ======================Int========================
        storeMapToPreference(oldData, prefsV1.edit())

        assertEquals(5, prefsV1.all.size)
        assertEquals(0, prefsV3.all.size)

        SharedPreferencesMigration(prefsV1, prefsV3, Int::class.javaObjectType) { true }.migrate()

        assertEquals(0, prefsV1.all.size)
        assertEquals(1, prefsV3.all.size)
        assertEquals(42, prefsV3.getInt("key2", -1))

        // ======================Float========================
        prefsV3.edit().clear().apply()

        storeMapToPreference(oldData, prefsV1.edit())

        assertEquals(5, prefsV1.all.size)
        assertEquals(0, prefsV3.all.size)

        SharedPreferencesMigration(prefsV1, prefsV3, Float::class.javaObjectType).migrate()

        assertEquals(0, prefsV1.all.size)
        assertEquals(1, prefsV3.all.size)
        assertEquals(3.14f, prefsV3.getFloat("key4", -1f))

        // ======================Boolean========================
        prefsV3.edit().clear().apply()

        storeMapToPreference(oldData, prefsV1.edit())

        assertEquals(5, prefsV1.all.size)
        assertEquals(0, prefsV3.all.size)

        SharedPreferencesMigration(prefsV1, prefsV3, Boolean::class.javaObjectType).migrate()

        assertEquals(0, prefsV1.all.size)
        assertEquals(1, prefsV3.all.size)
        assertEquals(true, prefsV3.getBoolean("key3", false))
        // ======================Long========================
        prefsV3.edit().clear().apply()

        storeMapToPreference(oldData, prefsV1.edit())

        assertEquals(5, prefsV1.all.size)
        assertEquals(0, prefsV3.all.size)

        SharedPreferencesMigration(prefsV1, prefsV3, Long::class.javaObjectType).migrate()

        assertEquals(0, prefsV1.all.size)
        assertEquals(1, prefsV3.all.size)
        assertEquals(1234567890L, prefsV3.getLong("key5", -1))
    }

    @Test
    fun testMigrateWithCustomCondition() {
        val oldData = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
            "key4" to "lue4"
        )

        val prefsV1 = StorageHelper.getPreferences(appCtx, prefNameV1)
        val prefsV3 = StorageHelper.getPreferences(appCtx, prefNameV3)
        storeMapToPreference(oldData, prefsV1.edit())

        assertEquals(4, prefsV1.all.size)
        assertEquals(0, prefsV3.all.size)

        // Migrate only values starting with "value"
        SharedPreferencesMigration(
            prefsV1,
            prefsV3,
            String::class.javaObjectType
        ) { it.startsWith("value") }.migrate()

        assertEquals(0, prefsV1.all.size)
        assertEquals(3, prefsV3.all.size)
        assertEquals("value1", prefsV3.getString("key1", null))
        assertEquals("value2", prefsV3.getString("key2", null))
        assertEquals("value3", prefsV3.getString("key3", null))
    }

    @Test
    fun testMigrateWithEmptyOldSharedPreferences() {
        val prefsV1 = StorageHelper.getPreferences(appCtx, prefNameV1)
        val prefsV3 = StorageHelper.getPreferences(appCtx, prefNameV3)

        // Ensure that oldSharedPreferences is empty
        assertEquals(0, prefsV1.all.size)
        assertEquals(0, prefsV3.all.size)

        // Attempt migration with empty oldSharedPreferences
        SharedPreferencesMigration(prefsV1, prefsV3, String::class.javaObjectType).migrate()

        // Verify that nothing is migrated and both SharedPreferences remain empty
        assertEquals(0, prefsV1.all.size)
        assertEquals(0, prefsV3.all.size)
    }


    @Test
    fun testMigrateWithInvalidData() {
        val oldData = mapOf(
            "key1" to "invalid_json",
            "key2" to "42",
            "key3" to "true"
        )

        val prefsV1 = StorageHelper.getPreferences(appCtx, prefNameV1)
        val prefsV3 = StorageHelper.getPreferences(appCtx, prefNameV3)
        storeMapToPreference(oldData, prefsV1.edit())

        assertEquals(3, prefsV1.all.size)
        assertEquals(0, prefsV3.all.size)

        // Attempt migration with String::class.java and a condition
        SharedPreferencesMigration(
            prefsV1,
            prefsV3,
            String::class.javaObjectType
        ) { it.contains("42") }.migrate()

        // Verify that only valid data is migrated, and the rest is removed
        assertEquals(0, prefsV1.all.size)
        assertEquals(1, prefsV3.all.size)
        assertEquals("42", prefsV3.getString("key2", null))
    }

    @Test
    fun testMigrateWithNoCondition() {
        val oldData = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to 42
        )

        val prefsV1 = StorageHelper.getPreferences(appCtx, prefNameV1)
        val prefsV3 = StorageHelper.getPreferences(appCtx, prefNameV3)
        storeMapToPreference(oldData, prefsV1.edit())

        assertEquals(3, prefsV1.all.size)
        assertEquals(0, prefsV3.all.size)

        // Attempt migration with null condition
        SharedPreferencesMigration(prefsV1, prefsV3, String::class.javaObjectType).migrate()

        assertEquals(0, prefsV1.all.size)
        assertEquals(2, prefsV3.all.size)
        assertEquals("value2", prefsV3.getString("key2", null))
        assertEquals("value1", prefsV3.getString("key1", null))
    }

    @Test
    fun testMigrateWithAnyValueType() {
        val oldData = mapOf(
            "key1" to "value1",
            "key2" to 42,
            "key3" to true,
            "key4" to 3.14f
        )

        val prefsV1 = StorageHelper.getPreferences(appCtx, prefNameV1)
        val prefsV3 = StorageHelper.getPreferences(appCtx, prefNameV3)

        storeMapToPreference(oldData, prefsV1.edit())

        assertEquals(4, prefsV1.all.size)
        assertEquals(0, prefsV3.all.size)

        // Attempt migration with Any::class.java (all values should be migrated)
        SharedPreferencesMigration(prefsV1, prefsV3, Any::class.javaObjectType).migrate()

        // Verify that all data is migrated, regardless of type
        assertEquals(0, prefsV1.all.size)
        assertEquals(4, prefsV3.all.size)
        assertEquals("value1", prefsV3.getString("key1", null))
        assertEquals(42, prefsV3.getInt("key2", 0))
        assertEquals(true, prefsV3.getBoolean("key3", false))
        assertEquals(3.14f, prefsV3.getFloat("key4", 0f))
    }

}
