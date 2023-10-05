package com.clevertap.android.sdk.inapp

import android.content.SharedPreferences
import com.clevertap.android.sdk.StorageHelper

class SharedPreferencesMigration<T>(
    private val oldSharedPreferences: SharedPreferences,
    private val newSharedPreferences: SharedPreferences,
    private val valueType: Class<T>,
    private val condition: ((T) -> Boolean) = { true }
) {

    @Suppress("UNCHECKED_CAST")
    fun migrate() {

        val oldData = oldSharedPreferences.all
        val editor = newSharedPreferences.edit()

        for ((key, value) in oldData) {
            if (valueType.isInstance(value) && condition(value as T)
            ) {
                when (valueType) {
                    Boolean::class.java -> editor.putBoolean(key, value as Boolean)
                    Int::class.java -> editor.putInt(key, value as Int)
                    Long::class.java -> editor.putLong(key, value as Long)
                    Float::class.java -> editor.putFloat(key, value as Float)
                    String::class.java -> editor.putString(key, value as String)
                    else -> {
                        // Remove values of other types
                        editor.remove(key)
                    }
                }
            } else {
                // Remove values of other types
                editor.remove(key)
            }
        }

        // Apply changes to the new SharedPreferences
        StorageHelper.persist(editor)

        // clear old SharedPreferences
        oldSharedPreferences.edit().clear().apply()
    }
}


