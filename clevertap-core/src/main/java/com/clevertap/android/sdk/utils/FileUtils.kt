package com.clevertap.android.sdk.utils

import android.content.Context
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

@WorkerThread
internal open class FileUtils(
    private val context: Context,
    private val config: CleverTapInstanceConfig
) {

    open fun deleteDirectory(dirName: String?) {
        if (TextUtils.isEmpty(dirName)) {
            return
        }
        try {
            synchronized(FileUtils::class.java) {
                val file = File(context.filesDir, dirName!!)
                if (file.exists() && file.isDirectory) {
                    val children = file.list()
                    for (child in children!!) {
                        val deleted = File(file, child).delete()
                        config.logger.verbose(
                            config.accountId,
                            "File$child isDeleted:$deleted"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            config.logger.verbose(
                config.accountId,
                "deleteDirectory: failed: $dirName", e
            )
        }
    }

    open fun deleteFile(fileName: String?) {
        if (TextUtils.isEmpty(fileName)) {
            return
        }
        try {
            synchronized(FileUtils::class.java) {
                val file = File(context.filesDir, fileName!!)
                if (file.exists()) {
                    if (file.delete()) {
                        config.logger.verbose(config.accountId, "File Deleted:$fileName")
                    } else {
                        config.logger.verbose(config.accountId, "Failed to delete file$fileName")
                    }
                }
            }
        } catch (e: Exception) {
            config.logger.verbose(
                config.accountId,
                "deleteFile: failed: $fileName", e
            )
        }
    }

    @Throws(IOException::class)
    open fun readFromFile(fileNameWithPath: String?): String {
        var content = ""
        var inputStream: InputStream? = null
        var inputStreamReader: InputStreamReader? = null
        var bufferedReader: BufferedReader? = null
        //Make sure to use a try-catch statement to catch any errors
        try {
            //Make your FilePath and File
            val yourFilePath = context.filesDir.toString() + "/" + fileNameWithPath
            val yourFile = File(yourFilePath)
            //Make an InputStream with your File in the constructor
            inputStream = FileInputStream(yourFile)
            val stringBuilder = StringBuilder()
            //Check to see if your inputStream is null
            //If it isn't use the inputStream to make a InputStreamReader
            //Use that to make a BufferedReader
            //Also create an empty String
            inputStreamReader = InputStreamReader(inputStream)
            bufferedReader = BufferedReader(inputStreamReader)
            var receiveString: String?
            //Use a while loop to append the lines from the Buffered reader
            while (bufferedReader.readLine().also { receiveString = it } != null) {
                stringBuilder.append(receiveString)
            }
            //Close your InputStream and save stringBuilder as a String
            inputStream.close()
            content = stringBuilder.toString()
        } catch (e: Exception) {
            config.logger
                .verbose(config.accountId, "readFromFile: failed for $fileNameWithPath", e)
            //Log your error with Log.e
        } finally {
            inputStream?.close()
            inputStreamReader?.close()
            bufferedReader?.close()
        }
        return content
    }

    @Throws(IOException::class)
    open fun writeJsonToFile(
        dirName: String?,
        fileName: String?, jsonObject: JSONObject?
    ) {
        var writer: FileWriter? = null
        try {
            if (jsonObject == null || TextUtils.isEmpty(dirName) || TextUtils.isEmpty(fileName)) {
                return
            }
            synchronized(FileUtils::class.java) {
                val file = File(context.filesDir, dirName!!)
                if (!file.exists()) {
                    if (!file.mkdir()) {
                        return  // if directory is not created don't proceed and return
                    }
                }

                val file1 = File(file, fileName!!)
                writer = FileWriter(file1, false)
                writer!!.append(jsonObject.toString())
                writer!!.flush()
            }
        } catch (e: Exception) {
            config.logger.verbose(
                config.accountId,
                "writeJsonToFile: failed for dir=$dirName, file=$fileName", e
            )
        } finally {
            writer?.close()
        }
    }
}
