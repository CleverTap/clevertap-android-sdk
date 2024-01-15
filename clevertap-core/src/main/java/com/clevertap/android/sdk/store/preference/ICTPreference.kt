package com.clevertap.android.sdk.store.preference

/**
 * The `ICTPreference` interface defines a set of methods for interacting with shared preferences
 * to read and write various data types. It provides flexibility in handling preferences
 * for different data types such as strings, booleans, integers, longs, floats, string sets, and maps.
 *
 * <p>
 * The implementation of this interface should encapsulate the underlying storage mechanism
 * for shared preferences, allowing the CleverTap SDK to store and retrieve preference data
 * in a specific manner.
 * </p>
 */
interface ICTPreference {

    /**
     * Reads a string from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The string value associated with the key, or the default value if not found.
     */
    fun readString(key: String, default: String): String?

    /**
     * Reads a boolean from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The boolean value associated with the key, or the default value if not found.
     */
    fun readBoolean(key: String, default: Boolean): Boolean

    /**
     * Reads an integer from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The integer value associated with the key, or the default value if not found.
     */
    fun readInt(key: String, default: Int): Int

    /**
     * Reads a long from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The long value associated with the key, or the default value if not found.
     */
    fun readLong(key: String, default: Long): Long

    /**
     * Reads a float from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The float value associated with the key, or the default value if not found.
     */
    fun readFloat(key: String, default: Float): Float

    /**
     * Reads a string set from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The string set associated with the key, or the default value if not found.
     */
    fun readStringSet(key: String, default: Set<String>): Set<String>?

    /**
     * Reads all key-value pairs from the shared preferences.
     *
     * @return A map containing all key-value pairs in the shared preferences.
     */
    fun readAll(): Map<String, *>?

    /**
     * Writes a string to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The string value to be stored.
     */
    fun writeString(key: String, value: String)

    /**
     * Writes a boolean to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The boolean value to be stored.
     */
    fun writeBoolean(key: String, value: Boolean)

    /**
     * Writes an integer to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The integer value to be stored.
     */
    fun writeInt(key: String, value: Int)

    /**
     * Writes a long to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The long value to be stored.
     */
    fun writeLong(key: String, value: Long)

    /**
     * Writes a float to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The float value to be stored.
     */
    fun writeFloat(key: String, value: Float)

    /**
     * Writes a string set to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The string set value to be stored.
     */
    fun writeStringSet(key: String, value: Set<String>)

    /**
     * Writes a map to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The map value to be stored.
     */
    fun writeMap(key: String, value: Map<String, *>)

    /**
     * Writes a string to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The string value to be stored.
     */
    fun writeStringImmediate(key: String, value: String)

    /**
     * Writes a boolean to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The boolean value to be stored.
     */
    fun writeBooleanImmediate(key: String, value: Boolean)

    /**
     * Writes an integer to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The integer value to be stored.
     */
    fun writeIntImmediate(key: String, value: Int)

    /**
     * Writes a long to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The long value to be stored.
     */
    fun writeLongImmediate(key: String, value: Long)

    /**
     * Writes a float to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The float value to be stored.
     */
    fun writeFloatImmediate(key: String, value: Float)

    /**
     * Writes a string set to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The string set value to be stored.
     */
    fun writeStringSetImmediate(key: String, value: Set<String>)

    /**
     * Writes a map to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The map value to be stored.
     */
    fun writeMapImmediate(key: String, value: Map<String, *>)

    /**
     * Checks if the shared preferences are empty.
     *
     * @return `true` if no key-value pairs are present, otherwise `false`.
     */
    fun isEmpty(): Boolean

    /**
     * Gets the size (number of key-value pairs) of the shared preferences.
     *
     * @return The number of key-value pairs in the shared preferences.
     */
    fun size(): Int

    /**
     * Removes a key and its associated value from the shared preferences.
     *
     * @param key The key to be removed.
     */
    fun remove(key: String)

    /**
     * Removes a key and its associated value from the shared preferences immediately
     * (without applying changes asynchronously).
     *
     * @param key The key to be removed.
     */
    fun removeImmediate(key: String)

    /**
     * Changes the name of the shared preferences.
     *
     * @param prefName The new name for the shared preferences.
     */
    fun changePreferenceName(prefName: String)
}