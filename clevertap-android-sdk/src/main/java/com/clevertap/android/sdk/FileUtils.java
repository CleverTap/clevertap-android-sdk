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
                file.mkdir();
            }

            File file1 = new File(file, fileName);
            FileWriter writer = new FileWriter(file1, false);
            writer.append(jsonObject.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "writeFileOnInternalStorage: failed" + e.getMessage());
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
            Log.d(TAG, "[Exception While Reading: " + fileNameWithPath + "]" + e.getMessage());
            throw e;
            //Log your error with Log.e
        }
        return content;
    }

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
            Log.d(TAG, "writeFileOnInternalStorage: failed" + e.getMessage());
            throw e;
        }
    }
}