package com.clevertap.android.sdk.utils

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.repo.TAG_FILE_DOWNLOAD
import java.io.File
import java.io.FileOutputStream
import kotlin.Exception

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

    fun add(key: String, value: ByteArray) : Boolean {
        return try {
            addAndReturnFileInstance(key, value)
            true
        } catch (e : Exception){
            logger?.verbose("Error while adding file to disk. Key: $key, Value Size: ${value.size} bytes", e)
            false
        }
    }

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

    fun get(key: String): File? {
        val file = fetchFile(key)

        return if (file.exists()) {
            file
        } else {
            null
        }
    }

    fun remove(key: String): Boolean {
        val file = fetchFile(key)
        return if (file.exists()) {
            file.delete()
            true
        } else {
            false
        }
    }

    fun empty() : Boolean {
        return directory.deleteRecursively()
    }

    private fun fetchFile(key: String) : File {
        val filePath = "${directory}/${FILE_PREFIX}_${hashFunction(key)}"
        return File(filePath)
    }
}