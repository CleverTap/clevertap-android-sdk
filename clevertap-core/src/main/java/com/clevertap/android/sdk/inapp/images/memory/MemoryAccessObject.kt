package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.clevertap.android.sdk.inapp.images.hasValidBitmap
import java.io.ByteArrayOutputStream
import java.io.File
/**
 * Represents the types of transformations that can be applied to stored data.
 *
 * @param <A> The type of the transformed data.
 */
sealed class MemoryDataTransformationType<A> {
    /**
     * Transforms data into a [Bitmap] object.
     */
    object ToBitmap : MemoryDataTransformationType<Bitmap>()
    /**
     * Transforms data into a [ByteArray].
     */
    object ToByteArray : MemoryDataTransformationType<ByteArray>()
    /**
     * Transforms data into a [File] object.
     */
    object ToFile : MemoryDataTransformationType<File>()
}

/**
 * Converts a [File] to a [Bitmap] if the file contains a valid bitmap.
 *
 * @param file The file to convert.
 * @return The converted [Bitmap], or null if the file is invalid or cannot be decoded.
 */
val fileToBitmap: (file: File?) -> Bitmap? = { file ->
    if (file != null && file.hasValidBitmap()) {
        BitmapFactory.decodeFile(file.absolutePath)
    } else {
        null
    }
}
/**
 * Reads the contents of a [File] into a [ByteArray].
 *
 * @param file The file to read.
 * @return The [ByteArray] containing the file's contents, or null if the file is null.
 */
val fileToBytes: (file: File?) -> ByteArray? = { file ->
    file?.readBytes()
}
/**
 * Decodes a [ByteArray] into a [Bitmap].
 *
 * @param bytes The [ByteArray] to decode.
 * @return The decoded [Bitmap], or null if the byte array cannot be decoded.
 */
val bytesToBitmap: (bytes: ByteArray) -> Bitmap? = {
    BitmapFactory.decodeByteArray(
        it,
        0,
        it.size
    )
}
/**
 * Compresses a [Bitmap] into a [ByteArray] in PNG format.
 *
 * @param bitmap The [Bitmap] to compress.
 * @return The compressed [ByteArray], or null if the bitmap is null.
 */
val bitmapToBytes: (bitmap: Bitmap?) -> ByteArray? = {
    it?.let {
        val stream = ByteArrayOutputStream()
        it.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    }
}
/**
 * An interface for accessing and managing data in memory and on disk.
 *
 * @param <T> The type of data being stored.
 */
interface MemoryAccessObject<T> {

    /**
     * Fetches a value from in-memory cache by key.
     * @param key The key to search for.
     * @return A [Pair] containing the value and file if found, or null otherwise.
     */
    fun fetchInMemory(key: String): Pair<T, File>?

    /**
     * Fetches a value from in-memory cache by key and transforms it.
     * @param key The key to search for.
     * @param transformTo The transformation identifier.
     * @return The transformed value, or null if not found.
     */
    fun <A> fetchInMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType<A>): A?

    /**
     * Fetches a value from disk memory by key and transforms it.
     * @param key The key to search for.
     * @param transformTo The transformation identifier.
     * @return The transformed value, or null if not found.
     */
    fun <A> fetchDiskMemoryAndTransform(key: String, transformTo: MemoryDataTransformationType<A>): A?

    /**
     * Fetches a file from disk memory by key.
     * @param key The key to search for.
     * @return The [File] if found, or null otherwise.
     */
    fun fetchDiskMemory(key: String): File?

    /**
     * Saves a value to in-memory cache.
     * @param key The key to save the data under.
     * @param data The data to save.
     * @return True if the save was successful, false otherwise.
     */
    fun saveInMemory(key: String, data: Pair<T, File>): Boolean

    /**
     * Saves data to disk memory.
     * @param key The key to save the data under.
     * @param data The data to save as a byte array.
     * @return The saved [File] if successful, or null otherwise.
     */
    fun saveDiskMemory(key: String, data: ByteArray): File

    /**
     * Removes a file from disk memory by key.
     * @param key The key to remove the data for.
     * @return True if the removal was successful, false otherwise.
     */
    fun removeDiskMemory(key: String): Boolean

    /**
     * Removes a file from in-memory by key.
     * @param key The key to remove the data for.
     * @return True if the removal was successful, false otherwise.
     */
    fun removeInMemory(key: String): Pair<T, File>?
}
