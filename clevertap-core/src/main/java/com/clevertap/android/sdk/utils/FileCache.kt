package com.clevertap.android.sdk.utils

import com.clevertap.android.sdk.ILogger
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.UUID

class FileCache(
    private val directory: File,
    private val maxFileSizeKb: Int,
    private val logger: ILogger? = null,
    private val hashFunction: (key: String) -> String = { key ->
        UUID.nameUUIDFromBytes(key.toByteArray()).toString()
    }
) {

    companion object {
        //private const val DIGEST_ALGO = "SHA256"
        private const val FILE_PREFIX = "CT_FILE"
    }

    fun add(key: String, value: ByteArray) : Boolean {
        if (value.sizeInKb() > maxFileSizeKb) {
            return false
        }
        val file = fetchFile(key)

        if (file.exists()) {
            file.delete()
        }
        try {
            val newFile = fetchFile(key)
            val os = FileOutputStream(newFile)
            os.write(value)
            os.close()
        } catch (e: Exception) {
            logger?.verbose("Error in saving data to file", e)
            return false
        }

        return true
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