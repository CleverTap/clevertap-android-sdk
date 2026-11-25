package com.clevertap.android.geofence

import android.content.Context
import android.text.TextUtils
import androidx.annotation.WorkerThread
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader

@Suppress("MemberVisibilityCanBePrivate")
internal object FileUtils {

    @JvmStatic
    @WorkerThread
    fun deleteDirectory(context: Context?, dirName: String?) {
        if (TextUtils.isEmpty(dirName) || context == null) {
            return
        }
        try {
            val file = File(context.filesDir, dirName!!)
            if (file.exists() && file.isDirectory) {
                val children = file.list()
                if (children != null) {
                    for (child in children) {
                        val isDeleted = if (File(file, child).delete()) "successfully deleted" else "failed to delete"
                        CTGeofenceAPI.getLogger().verbose(
                            CTGeofenceAPI.GEOFENCE_LOG_TAG,
                            "$child :$isDeleted"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger()
                .debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "deleteDirectory failed for dirName: $dirName", e)
        }
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun getCachedDirName(context: Context?): String {
        return CTGeofenceConstants.CACHED_DIR_NAME /*+ "_" + CTGeofenceAPI.getInstance(context).getAccountId()
                + "_" + CTGeofenceAPI.getInstance(context).getGuid()*/
    }

    @JvmStatic
    fun getCachedFullPath(context: Context?, fileName: String?): String {
        return getCachedDirName(context) + "/" + fileName
    }

    @JvmStatic
    @WorkerThread
    fun readFromFile(context: Context?, fileNameWithPath: String?): String {
        var content = ""
        //Make sure to use a try-catch statement to catch any errors
        try {
            //Make your FilePath and File
            val yourFilePath = context!!.filesDir.toString() + "/" + fileNameWithPath
            val yourFile = File(yourFilePath)

            if (!yourFile.exists()) {
                return content
            }
            //Make an InputStream with your File in the constructor
            val inputStream = FileInputStream(yourFile)
            val stringBuilder = StringBuilder()
            //Check to see if your inputStream is null
            //If it isn't use the inputStream to make a InputStreamReader
            //Use that to make a BufferedReader
            //Also create an empty String
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var receiveString: String?
            //Use a while loop to append the lines from the Buffered reader
            while (bufferedReader.readLine().also { receiveString = it } != null) {
                stringBuilder.append(receiveString)
            }
            //Close your InputStream and save stringBuilder as a String
            inputStream.close()
            content = stringBuilder.toString()
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger()
                .debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Exception while reading file: $fileNameWithPath", e)
        }
        return content
    }

    @JvmStatic
    @WorkerThread
    fun writeJsonToFile(
        context: Context?, dirName: String?, fileName: String?,
        jsonObject: JSONObject?
    ): Boolean {
        var isWriteSuccessful = false
        try {
            if (jsonObject == null || TextUtils.isEmpty(dirName) || TextUtils.isEmpty(fileName)) {
                return false
            }
            val file = File(context!!.filesDir, dirName!!)
            if (!file.exists()) {
                if (!file.mkdir()) {
                    return false
                }
            }

            val file1 = File(file, fileName!!)
            val writer = FileWriter(file1, false)
            writer.append(jsonObject.toString())
            writer.flush()
            writer.close()
            isWriteSuccessful = true
            CTGeofenceAPI.getLogger()
                .verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG, "$fileName: writeFileOnInternalStorage: successful")
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger()
                .debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "writeJsonToFile failed for fileName: $fileName", e)
        }
        return isWriteSuccessful
    }
}
