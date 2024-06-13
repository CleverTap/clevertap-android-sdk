package com.clevertap.android.sdk.inapp.images.memory

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.clevertap.android.sdk.inapp.images.hasValidBitmap
import java.io.ByteArrayOutputStream
import java.io.File

sealed class MemoryDataTransformationType<A> {
    object ToBitmap : MemoryDataTransformationType<Bitmap>()
    object ToByteArray : MemoryDataTransformationType<ByteArray>()
    object ToFile : MemoryDataTransformationType<File>()
}

val fileToBitmap: (file: File?) -> Bitmap? = { file ->
    if (file != null && file.hasValidBitmap()) {
        BitmapFactory.decodeFile(file.absolutePath)
    } else {
        null
    }
}
val fileToBytes: (file: File?) -> ByteArray? = { file ->
    file?.readBytes()
}
val bytesToBitmap: (bytes: ByteArray) -> Bitmap? = {
    BitmapFactory.decodeByteArray(
        it,
        0,
        it.size
    )
}
val bitmapToBytes: (bitmap: Bitmap?) -> ByteArray? = {
    it?.let {
        val stream = ByteArrayOutputStream()
        it.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    }
}

interface MemoryAccessObject<T> {

    /**
     * Fetches a value from in-memory cache by key.
     * @param key The key to search for.
     * @return A pair containing the value and file if found, or null otherwise.
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
     * @return The file if found, or null otherwise.
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
     * @return The saved file if successful, or null otherwise.
     */
    fun saveDiskMemory(key: String, data: ByteArray): File?

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
