package com.clevertap.android.geofence;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.json.JSONObject;

@SuppressWarnings("WeakerAccess")
public class FileUtils {

    @WorkerThread
    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void deleteDirectory(Context context, String dirName) {
        if (TextUtils.isEmpty(dirName) || context == null) {
            return;
        }
        try {
            File file = new File(context.getFilesDir(), dirName);
            if (file.exists() && file.isDirectory()) {
                String[] children = file.list();
                if (children != null) {
                    for (String child : children) {
                        String isDeleted = new File(file, child).delete() ? "successfully deleted"
                                : "failed to delete";
                        CTGeofenceAPI.getLogger().verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                child + " :" + isDeleted);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            CTGeofenceAPI.getLogger().verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "deleteFileOnInternalStorage: failed" + dirName + " Error:" + e.getLocalizedMessage());
        }
    }

    @SuppressWarnings("UnusedParameters")
    static String getCachedDirName(Context context) {
        return CTGeofenceConstants.CACHED_DIR_NAME /*+ "_" + CTGeofenceAPI.getInstance(context).getAccountId()
                + "_" + CTGeofenceAPI.getInstance(context).getGuid()*/;
    }

    static String getCachedFullPath(Context context, String fileName) {
        return getCachedDirName(context) + "/" + fileName;
    }

    @WorkerThread
    @NonNull
    static String readFromFile(Context context, String fileNameWithPath) {

        String content = "";
        //Make sure to use a try-catch statement to catch any errors
        try {
            //Make your FilePath and File
            String yourFilePath = context.getFilesDir() + "/" + fileNameWithPath;
            File yourFile = new File(yourFilePath);

            if (!yourFile.exists()) {
                return content;
            }
            //Make an InputStream with your File in the constructor
            InputStream inputStream = new FileInputStream(yourFile);
            StringBuilder stringBuilder = new StringBuilder();
            //Check to see if your inputStream is null
            //If it isn't use the inputStream to make a InputStreamReader
            //Use that to make a BufferedReader
            //Also create an empty String
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            //Use a while loop to append the lines from the Buffered reader
            while ((receiveString = bufferedReader.readLine()) != null) {
                stringBuilder.append(receiveString);
            }
            //Close your InputStream and save stringBuilder as a String
            inputStream.close();
            content = stringBuilder.toString();
        } catch (Exception e) {
            CTGeofenceAPI.getLogger()
                    .verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG, "[Exception While Reading: " + e.getLocalizedMessage());
        }
        return content;
    }

    @WorkerThread
    static boolean writeJsonToFile(Context context, String dirName, String fileName,
            @Nullable JSONObject jsonObject) {
        boolean isWriteSuccessful = false;
        try {
            if (jsonObject == null || TextUtils.isEmpty(dirName) || TextUtils.isEmpty(fileName)) {
                return false;
            }
            File file = new File(context.getFilesDir(), dirName);
            if (!file.exists()) {
                if (!file.mkdir()) {
                    return false;
                }
            }

            File file1 = new File(file, fileName);
            FileWriter writer = new FileWriter(file1, false);
            writer.append(jsonObject.toString());
            writer.flush();
            writer.close();

            isWriteSuccessful = true;
            CTGeofenceAPI.getLogger().verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG, fileName
                    + ": writeFileOnInternalStorage: successful");
        } catch (Exception e) {
            e.printStackTrace();
            CTGeofenceAPI.getLogger().verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "writeFileOnInternalStorage: failed" + e.getLocalizedMessage());
        }

        return isWriteSuccessful;
    }
}