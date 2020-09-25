package com.clevertap.android.sdk;

import android.content.Context;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.json.JSONObject;

public class FileUtils {

    public static void deleteDirectory(Context context, CleverTapInstanceConfig config, String dirName) {
        if (TextUtils.isEmpty(dirName) || context == null) {
            return;
        }
        try {
            File file = new File(context.getFilesDir(), dirName);
            if (file.exists() && file.isDirectory()) {
                String[] children = file.list();
                for (String child : children) {
                    new File(file, child).delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (config != null) {
                config.getLogger().verbose(config.getAccountId(),
                        "writeFileOnInternalStorage: failed" + dirName + " Error:" + e.getLocalizedMessage());
            }
        }
    }

    public static void deleteFile(Context context, CleverTapInstanceConfig config, String fileName) throws Exception {
        if (TextUtils.isEmpty(fileName) || context == null) {
            return;
        }
        try {
            File file = new File(context.getFilesDir(), fileName);
            if (file.exists()) {
                if (file.delete()) {
                    if (config != null) {
                        config.getLogger().verbose(config.getAccountId(), "File Deleted:" + fileName);
                    }
                } else {
                    if (config != null) {
                        config.getLogger().verbose(config.getAccountId(), "Failed to delete file" + fileName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (config != null) {
                config.getLogger().verbose(config.getAccountId(),
                        "writeFileOnInternalStorage: failed" + fileName + " Error:" + e.getLocalizedMessage());
            }
        }
    }

    public static String readFromFile(Context context, CleverTapInstanceConfig config, String fileNameWithPath)
            throws Exception {

        String content = "";
        //Make sure to use a try-catch statement to catch any errors
        try {
            //Make your FilePath and File
            String yourFilePath = context.getFilesDir() + "/" + fileNameWithPath;
            File yourFile = new File(yourFilePath);
            //Make an InputStream with your File in the constructor
            InputStream inputStream = new FileInputStream(yourFile);
            StringBuilder stringBuilder = new StringBuilder();
            //Check to see if your inputStream is null
            //If it isn't use the inputStream to make a InputStreamReader
            //Use that to make a BufferedReader
            //Also create an empty String
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString = "";
            //Use a while loop to append the lines from the Buffered reader
            while ((receiveString = bufferedReader.readLine()) != null) {
                stringBuilder.append(receiveString);
            }
            //Close your InputStream and save stringBuilder as a String
            inputStream.close();
            content = stringBuilder.toString();
        } catch (Exception e) {
            if (config != null) {
                config.getLogger()
                        .verbose(config.getAccountId(), "[Exception While Reading: " + e.getLocalizedMessage());
            }
            //Log your error with Log.e
        }
        return content;
    }

    public static void writeJsonToFile(Context context, CleverTapInstanceConfig config, String dirName,
            String fileName, JSONObject jsonObject) {
        try {
            if (jsonObject == null || TextUtils.isEmpty(dirName) || TextUtils.isEmpty(fileName)) {
                return;
            }
            File file = new File(context.getFilesDir(), dirName);
            if (!file.exists()) {
                if (!file.mkdir()) {
                    return;// if directory is not created don't proceed and return
                }
            }

            File file1 = new File(file, fileName);
            FileWriter writer = new FileWriter(file1, false);
            writer.append(jsonObject.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (config != null) {
                config.getLogger().verbose(config.getAccountId(),
                        "writeFileOnInternalStorage: failed" + e.getLocalizedMessage());
            }
        }
    }
}