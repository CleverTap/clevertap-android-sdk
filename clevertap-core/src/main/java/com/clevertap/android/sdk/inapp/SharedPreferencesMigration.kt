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


