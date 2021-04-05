package com.clevertap.android.sdk.utils;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY_GROUP)
public class FileUtils {

    private final CleverTapInstanceConfig config;

    private final Context context;

    public FileUtils(@NonNull final Context context, @NonNull final CleverTapInstanceConfig config) {
        this.context = context;
        this.config = config;
    }

    public void deleteDirectory(String dirName) {
        if (TextUtils.isEmpty(dirName)) {
            return;
        }
        try {
            synchronized (FileUtils.class) {
                File file = new File(context.getFilesDir(), dirName);
                if (file.exists() && file.isDirectory()) {
                    String[] children = file.list();
                    for (String child : children) {
                        boolean deleted = new File(file, child).delete();
                        config.getLogger().verbose(config.getAccountId(),
                                "File" + child + " isDeleted:" + deleted);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(config.getAccountId(),
                    "writeFileOnInternalStorage: failed" + dirName + " Error:" + e.getLocalizedMessage());
        }
    }

    public void deleteFile(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return;
        }
        try {
            synchronized (FileUtils.class) {
                File file = new File(context.getFilesDir(), fileName);
                if (file.exists()) {
                    if (file.delete()) {
                        config.getLogger().verbose(config.getAccountId(), "File Deleted:" + fileName);
                    } else {
                        config.getLogger().verbose(config.getAccountId(), "Failed to delete file" + fileName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(config.getAccountId(),
                    "writeFileOnInternalStorage: failed" + fileName + " Error:" + e.getLocalizedMessage());
        }
    }

    public String readFromFile(String fileNameWithPath) throws IOException {

        String content = "";
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        //Make sure to use a try-catch statement to catch any errors
        try {
            //Make your FilePath and File
            String yourFilePath = context.getFilesDir() + "/" + fileNameWithPath;
            File yourFile = new File(yourFilePath);
            //Make an InputStream with your File in the constructor
            inputStream = new FileInputStream(yourFile);
            StringBuilder stringBuilder = new StringBuilder();
            //Check to see if your inputStream is null
            //If it isn't use the inputStream to make a InputStreamReader
            //Use that to make a BufferedReader
            //Also create an empty String
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            //Use a while loop to append the lines from the Buffered reader
            while ((receiveString = bufferedReader.readLine()) != null) {
                stringBuilder.append(receiveString);
            }
            //Close your InputStream and save stringBuilder as a String
            inputStream.close();
            content = stringBuilder.toString();
        } catch (Exception e) {
            config.getLogger()
                    .verbose(config.getAccountId(), "[Exception While Reading: " + e.getLocalizedMessage());
            //Log your error with Log.e
        }finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (inputStreamReader != null) {
                inputStreamReader.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        return content;
    }

    public void writeJsonToFile(String dirName,
            String fileName, JSONObject jsonObject) throws IOException {
        FileWriter writer = null;
        try {
            if (jsonObject == null || TextUtils.isEmpty(dirName) || TextUtils.isEmpty(fileName)) {
                return;
            }
            synchronized (FileUtils.class) {
                File file = new File(context.getFilesDir(), dirName);
                if (!file.exists()) {
                    if (!file.mkdir()) {
                        return;// if directory is not created don't proceed and return
                    }
                }

                File file1 = new File(file, fileName);
                writer = new FileWriter(file1, false);
                writer.append(jsonObject.toString());
                writer.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
            config.getLogger().verbose(config.getAccountId(),
                    "writeFileOnInternalStorage: failed" + e.getLocalizedMessage());
        }finally {
            if(writer != null){
                writer.close();
            }
        }
    }
}