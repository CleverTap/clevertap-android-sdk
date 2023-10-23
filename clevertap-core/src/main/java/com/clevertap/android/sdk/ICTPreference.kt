package com.clevertap.android.sdk

interface ICTPreference {

    fun readString(key: String, default: String): String?
    fun readBoolean(key: String, default: Boolean): Boolean
    fun readInt(key: String, default: Int): Int
    fun readLong(key: String, default: Long): Long
    fun readFloat(key: String, default: Float): Float
    fun readStringSet(key: String, default: Set<String>): Set<String>?
    fun readAll(): Map<String, *>?
    fun writeString(key: String, value: String)
    fun writeBoolean(key: String, value: Boolean)
    fun writeInt(key: String, value: Int)
    fun writeLong(key: String, value: Long)
    fun writeFloat(key: String, value: Float)
    fun writeStringSet(key: String, value: Set<String>)
    fun writeMap(key: String, value: Map<String, *>)
    fun writeStringImmediate(key: String, value: String)
    fun writeBooleanImmediate(key: String, value: Boolean)
    fun writeIntImmediate(key: String, value: Int)
    fun writeLongImmediate(key: String, value: Long)
    fun writeFloatImmediate(key: String, value: Float)
    fun writeStringSetImmediate(key: String, value: Set<String>)
    fun writeMapImmediate(key: String, value: Map<String, *>)
    fun isEmpty(): Boolean
    fun size(): Int
    fun remove(key: String)
    fun removeImmediate(key: String)
}