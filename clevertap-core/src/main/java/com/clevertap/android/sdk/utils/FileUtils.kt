package com.clevertap.android.sdk.utils

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import androidx.annotation.RestrictTo

@WorkerThread
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class FileUtils(
    private val context: Context,
    private val config: CleverTapInstanceConfig
) {

    open fun deleteDirectory(dirName: String?) {
        if (dirName.isNullOrEmpty()) {
            return
        }
        try {
            synchronized(FileUtils::class.java) {
                val file = File(context.filesDir, dirName)
                if (file.exists() && file.isDirectory) {
                    // list() is null on an I/O error: that fails into the catch below, as before
                    for (child in checkNotNull(file.list())) {
                        val deleted = File(file, child).delete()
                        config.logger.verbose(config.accountId, "File$child isDeleted:$deleted")
                    }
                }
            }
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "deleteDirectory: failed: $dirName", e)
        }
    }

    open fun deleteFile(fileName: String?) {
        if (fileName.isNullOrEmpty()) {
            return
        }
        try {
            synchronized(FileUtils::class.java) {
                val file = File(context.filesDir, fileName)
                if (file.exists()) {
                    if (file.delete()) {
                        config.logger.verbose(config.accountId, "File Deleted:$fileName")
                    } else {
                        config.logger.verbose(config.accountId, "Failed to delete file$fileName")
                    }
                }
            }
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "deleteFile: failed: $fileName", e)
        }
    }

    /**
     * Reads the file at [fileNameWithPath] (relative to the app's files dir), joining its lines without
     * separators. Returns "" (and logs) when it can't be read.
     */
    open fun readFromFile(fileNameWithPath: String?): String = try {
        File("${context.filesDir}/$fileNameWithPath").bufferedReader().use { reader ->
            buildString { reader.forEachLine { append(it) } }
        }
    } catch (e: Exception) {
        config.logger.verbose(config.accountId, "readFromFile: failed for $fileNameWithPath", e)
        ""
    }

    open fun writeJsonToFile(dirName: String?, fileName: String?, jsonObject: JSONObject?) {
        if (jsonObject == null || dirName.isNullOrEmpty() || fileName.isNullOrEmpty()) {
            return
        }
        try {
            synchronized(FileUtils::class.java) {
                val dir = File(context.filesDir, dirName)
                if (!dir.exists() && !dir.mkdir()) {
                    return // if directory is not created don't proceed and return
                }
                FileWriter(File(dir, fileName), false).use { writer ->
                    writer.append(jsonObject.toString())
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            config.logger.verbose(config.accountId, "writeJsonToFile: failed for dir=$dirName, file=$fileName", e)
        }
    }
}
