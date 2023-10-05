package com.clevertap.android.sdk.inapp

import android.content.SharedPreferences
import com.clevertap.android.sdk.StorageHelper

/**
 * A utility class for migrating data between two instances of SharedPreferences with optional type filtering.
 *
 * @param oldSharedPreferences The source SharedPreferences from which data will be migrated.
 * @param newSharedPreferences The target SharedPreferences where data will be migrated to.
 * @param valueType The type of values to migrate. Use `Any::class.java` to migrate all types without filtering.
 * @param condition An optional condition function that filters values based on a custom condition.
 *                  When provided, only values that satisfy this condition will be migrated.
 *                  Defaults to a condition that allows all values when not specified.
 */
class SharedPreferencesMigration<T>(
    private val oldSharedPreferences: SharedPreferences,
    private val newSharedPreferences: SharedPreferences,
    private val valueType: Class<T>,
    private val condition: ((T) -> Boolean) = { true }
) {

    /**
     * Migrates data from the old SharedPreferences to the new SharedPreferences.
     *
     * This method iterates through all key-value pairs in the old SharedPreferences, applies optional
     * type filtering and condition, and migrates the eligible values to the new SharedPreferences.
     *
     * After migration, the changes are persisted in the new SharedPreferences, and the old SharedPreferences
     * is cleared to prevent data duplication.
     * @suppress("UNCHECKED_CAST")
     */
    @Suppress("UNCHECKED_CAST")
    fun migrate() {

        val oldData = oldSharedPreferences.all
        val editor = newSharedPreferences.edit()

        for ((key, value) in oldData) {
            if (valueType.isInstance(value) && condition(value as T)
            ) {
                when (valueType) {
                    Boolean::class.javaObjectType -> editor.putBoolean(key, value as Boolean)
                    Int::class.javaObjectType ->
                        editor.putInt(key, value as Int)

                    Long::class.javaObjectType -> editor.putLong(key, value as Long)
                    Float::class.javaObjectType -> editor.putFloat(key, value as Float)
                    String::class.javaObjectType -> editor.putString(key, value as String)
                    else -> {// case when valueType is Any
                        when (value) {
                            is Boolean -> editor.putBoolean(key, value)
                            is Int -> editor.putInt(key, value)
                            is Long -> editor.putLong(key, value)
                            is Float -> editor.putFloat(key, value)
                            is String -> editor.putString(key, value)
                        }
                    }
                }
            }
        }

        // Apply changes to the new SharedPreferences
        StorageHelper.persist(editor)

        // clear old SharedPreferences
        oldSharedPreferences.edit().clear().apply()
    }
}


