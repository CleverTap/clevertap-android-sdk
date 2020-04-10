package com.clevertap.android.sdk;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static void writeJsonToFile(Context context, String dirName, String fileName, JSONObject jsonObject) throws Exception {
        try {
            if (jsonObject == null || TextUtils.isEmpty(dirName) || TextUtils.isEmpty(fileName))
                return;
            File file = new File(context.getFilesDir(), dirName);
            if (!file.exists()) {
                file.mkdir();//TODO @atul Shouldn't we use the returned boolean value to proceed?
            }

            File file1 = new File(file, fileName);
            FileWriter writer = new FileWriter(file1, false);
            writer.append(jsonObject.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "writeFileOnInternalStorage: failed" + e.getMessage());//TODO @atul Use our Logger
            throw e;
        }
    }

    public static String readFromFile(Context context, String fileNameWithPath) throws Exception {

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
            Log.d(TAG, "[Exception While Reading: " + fileNameWithPath + "]" + e.getMessage());//TODO @atul use our Logger
            throw e;//TODO @atul you are throwing exception but not catching it anywhere
            //Log your error with Log.e
        }
        return content;
    }
    //TODO @atul No exception is being thrown but method has a "throws"
    public static void deleteDirectory(Context context, String dirName) throws Exception {
        if (TextUtils.isEmpty(dirName) || context == null)
            return;
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
            Log.d(TAG, "writeFileOnInternalStorage: failed" + e.getMessage());//TODO @atul Use our Logger
            throw e;
        }
    }

    //TODO @atul this method can be void, why boolean?, and use our Logger
    public static boolean deleteFile(Context context, String fileName) throws Exception {
        if (TextUtils.isEmpty(fileName) || context == null)
            return false;
        try {
            File file = new File(context.getFilesDir(), fileName);
            if (file.exists()) {
                if (file.delete()) {
                    Log.d(TAG, "File Deleted:" + fileName);
                    return true;
                } else {
                    Log.d(TAG, "Failed to delete file" + fileName);
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "writeFileOnInternalStorage: failed" + e.getMessage());
            throw e;
        }
        return false;
    }
}