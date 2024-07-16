package com.clevertap.android.sdk.utils

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.repo.TAG_FILE_DOWNLOAD
import java.io.File
import java.io.FileOutputStream
import kotlin.Exception
/**
 * A simple disk-based storage utility for managing persistent key-value pairs, where the value is a [ByteArray].
 * This class provides methods for adding, retrieving, removing, and clearing stored files on disk.
 * It also enforces a maximum file size limit to manage storage usage.
 *
 * @param directory The directory on the device's storage where files will be stored.
 * @param maxFileSizeKb The maximum allowed file size in kilobytes for cached files.
 * @param logger An optional [ILogger] instance for debugging or tracking purposes.
 * @param hashFunction A function to generate a unique hash from the provided key. By default, it uses a [UrlHashGenerator].
 */
class DiskMemory(
    private val directory: File,
    private val maxFileSizeKb: Int,
    private val logger: ILogger? = null,
    internal val hashFunction: (key: String) -> String = UrlHashGenerator.hash()
) {

    companion object {
        //private const val DIGEST_ALGO = "SHA256"
        private const val FILE_PREFIX = "CT_FILE"
    }
    /**
     * Adds a file to the disk storage with the given key and value [ByteArray].
     *
     * @param key The unique key associated with the file.
     * @param value The [ByteArray] representing the file content.
     * @return `true` if the file was successfully stored, `false` otherwise.
     */
    fun add(key: String, value: ByteArray) : Boolean {
        return try {
            addAndReturnFileInstance(key, value)
            true
        } catch (e : Exception){
            logger?.verbose("Error while adding file to disk. Key: $key, Value Size: ${value.size} bytes", e)
            false
        }
    }
    /**
     * Adds a file to the disk storage and returns the [File] instance of the newly stored file.
     *
     * @param key The unique key associated with the file.
     * @param value The [ByteArray] representing the file content.
     * @return The [File] instance of the stored file.
     * @throws [IllegalArgumentException] if the file size exceeds the maximum limit.
     */
    fun addAndReturnFileInstance(key: String, value: ByteArray) : File {
        if (value.sizeInKb() > maxFileSizeKb) {
            remove(key = key)
            throw IllegalArgumentException("File size exceeds the maximum limit of $maxFileSizeKb")
        }
        val file = fetchFile(key)

        if (file.exists()) {
            file.delete()
        }
        val newFile = fetchFile(key)
        logger?.verbose(TAG_FILE_DOWNLOAD,"mapped file path - ${newFile.absoluteFile} to key - $key")
        val os = FileOutputStream(newFile)
        os.write(value)
        os.close()
        return newFile

    }
    /**
     * Retrieves the stored file associated with the given key.
     *
     * @param key The unique key associated with the file.
     * @return The [File] instance of the stored file if found, `null` otherwise.
     */
    fun get(key: String): File? {
        val file = fetchFile(key)

        return if (file.exists()) {
            file
        } else {
            null
        }
    }
    /**
     * Removes the stored file associated with the given key.
     *
     * @param key The unique key associated with the file.
     * @return `true` if the file exists, `false` if it didn't exist.
     */
    fun remove(key: String): Boolean {
        val file = fetchFile(key)
        return if (file.exists()) {
            file.delete()
            true
        } else {
            false
        }
    }
    /**
     * Clears all files within the storage directory.
     *
     * @return `true` if the directory was successfully deleted, `false` otherwise.
     */
    fun empty() : Boolean {
        return directory.deleteRecursively()
    }
    /**
     * Generates the file path for a stored file based on the provided key.
     *
     * @param key The unique key associated with the file.
     * @return The [File] instance representing the stored file path.
     */
    private fun fetchFile(key: String) : File {
        val filePath = "${directory}/${FILE_PREFIX}_${hashFunction(key)}"
        return File(filePath)
    }
}