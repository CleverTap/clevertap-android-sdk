package com.clevertap.android.sdk.store.preference

import androidx.annotation.RestrictTo

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ICTPreference {

    /**
     * Reads a string from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The string value associated with the key, or the default value if not found.
     */
    public fun readString(key: String, default: String): String?

    /**
     * Reads a boolean from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The boolean value associated with the key, or the default value if not found.
     */
    public fun readBoolean(key: String, default: Boolean): Boolean

    /**
     * Reads an integer from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The integer value associated with the key, or the default value if not found.
     */
    public fun readInt(key: String, default: Int): Int

    /**
     * Reads a long from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The long value associated with the key, or the default value if not found.
     */
    public fun readLong(key: String, default: Long): Long

    /**
     * Reads a float from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The float value associated with the key, or the default value if not found.
     */
    public fun readFloat(key: String, default: Float): Float

    /**
     * Reads a string set from the shared preferences.
     *
     * @param key The key associated with the value.
     * @param default The default value to return if the key is not present.
     * @return The string set associated with the key, or the default value if not found.
     */
    public fun readStringSet(key: String, default: Set<String>): Set<String>?

    /**
     * Reads all key-value pairs from the shared preferences.
     *
     * @return A map containing all key-value pairs in the shared preferences.
     */
    public fun readAll(): Map<String, *>?

    /**
     * Writes a string to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The string value to be stored.
     */
    public fun writeString(key: String, value: String)

    /**
     * Writes a boolean to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The boolean value to be stored.
     */
    public fun writeBoolean(key: String, value: Boolean)

    /**
     * Writes an integer to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The integer value to be stored.
     */
    public fun writeInt(key: String, value: Int)

    /**
     * Writes a long to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The long value to be stored.
     */
    public fun writeLong(key: String, value: Long)

    /**
     * Writes a float to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The float value to be stored.
     */
    public fun writeFloat(key: String, value: Float)

    /**
     * Writes a string set to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The string set value to be stored.
     */
    public fun writeStringSet(key: String, value: Set<String>)

    /**
     * Writes a map to the shared preferences.
     *
     * @param key The key associated with the value.
     * @param value The map value to be stored.
     */
    public fun writeMap(key: String, value: Map<String, *>)

    /**
     * Writes a string to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The string value to be stored.
     */
    public fun writeStringImmediate(key: String, value: String)

    /**
     * Writes a boolean to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The boolean value to be stored.
     */
    public fun writeBooleanImmediate(key: String, value: Boolean)

    /**
     * Writes an integer to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The integer value to be stored.
     */
    public fun writeIntImmediate(key: String, value: Int)

    /**
     * Writes a long to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The long value to be stored.
     */
    public fun writeLongImmediate(key: String, value: Long)

    /**
     * Writes a float to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The float value to be stored.
     */
    public fun writeFloatImmediate(key: String, value: Float)

    /**
     * Writes a string set to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The string set value to be stored.
     */
    public fun writeStringSetImmediate(key: String, value: Set<String>)

    /**
     * Writes a map to the shared preferences immediately (without applying changes asynchronously).
     *
     * @param key The key associated with the value.
     * @param value The map value to be stored.
     */
    public fun writeMapImmediate(key: String, value: Map<String, *>)

    /**
     * Checks if the shared preferences are empty.
     *
     * @return `true` if no key-value pairs are present, otherwise `false`.
     */
    public fun isEmpty(): Boolean

    /**
     * Gets the size (number of key-value pairs) of the shared preferences.
     *
     * @return The number of key-value pairs in the shared preferences.
     */
    public fun size(): Int

    /**
     * Removes a key and its associated value from the shared preferences.
     *
     * @param key The key to be removed.
     */
    public fun remove(key: String)

    /**
     * Removes a key and its associated value from the shared preferences immediately
     * (without applying changes asynchronously).
     *
     * @param key The key to be removed.
     */
    public fun removeImmediate(key: String)

    /**
     * Changes the name of the shared preferences.
     *
     * @param prefName The new name for the shared preferences.
     */
    public fun changePreferenceName(prefName: String)
}