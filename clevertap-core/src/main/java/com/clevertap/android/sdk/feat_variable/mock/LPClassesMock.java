package com.clevertap.android.sdk.feat_variable.mock;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.clevertap.android.sdk.feat_variable.extras.CollectionUtil;
import com.clevertap.android.sdk.feat_variable.extras.Constants;

public final class LPClassesMock {

    private static final String TAG = "LPClassesMock>";

    //Leanplum.hasStarted()
    public static Boolean hasStarted(){
        return true;
    }

    //LeanplumInternal.hasCalledStart()
    public static Boolean hasCalledStart(){
        return true;
    }

    //Log.exception(t);
    public static void exception(Throwable t){
        t.printStackTrace();
    }

    //Util.generateResourceNameFromId(resId)
    public static  String generateResourceNameFromId(int resId){
        return null;
    }

    public static int generateIdFromResourceName(String resourceName) {
        return 0;
        /*
        // Split resource name to extract folder and file name.
        String[] parts = resourceName.split("/");
        if (parts.length == 2) {
            Resources resources = Leanplum.getContext().getResources();
            // Type name represents folder where file is contained.
            String typeName = parts[0];
            String fileName = parts[1];
            String entryName = fileName;
            // Since fileName contains extension we have to remove it,
            // to be able to get resource id.
            String[] fileParts = fileName.split("\\.(?=[^\\.]+$)");
            if (fileParts.length == 2) {
                entryName = fileParts[0];
            }
            // Get identifier for a file in specified directory
            if (!TextUtils.isEmpty(typeName) && !TextUtils.isEmpty(entryName)) {
                return resources.getIdentifier(entryName, typeName, Leanplum.getContext().getPackageName());
            }
        }
        Log.d("Could not extract resource id from provided resource name: ", resourceName);
        return 0;
        */
    }

    // LeanplumInternal.maybeThrowException(..)
    public static void maybeThrowException(RuntimeException e) {
        if (Constants.isDevelopmentModeEnabled) {
            throw e;
        } else {
            Log.e(TAG,e.getMessage() + " This error is only thrown in development mode.", e);
        }
    }

    //Leanplum.getContext()
    public static Context getContext(){
        //todo : fixit
        return null;
    }


    //FileTransferManager.getInstance().downloadFile(stringValue, urlValue, onComplete, onComplete)
    public static void downloadFile(final String path, final String url, Runnable onResponse, Runnable onError) {}


    //FileTransferManager.getInstance().sendFilesNow(fileData, filenames, streams);
    public static void sendFilesNow(List<JSONObject> fileData, final List<String> filenames, final List<InputStream> streams) {}

    //Util.isSimulator()
    public static boolean isSimulator() {
        String model = android.os.Build.MODEL.toLowerCase(Locale.getDefault());
        return model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("sdk");
    }

    //APIConfig.getInstance().token()
    public static  String token(){
        return "";
    }

    //Utils.multiIndex
    public static <T> T multiIndex(Map<?, ?> map, Object... indices) {
        if (map == null) {
            return null;
        }
        Object current = map;
        for (Object index : indices) {
            if (!((Map<?, ?>) current).containsKey(index)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(index);
        }
        return CollectionUtil.uncheckedCast(current);
    }

    //Leanplum.hasStartedAndRegisteredAsDeveloper()
    public static boolean hasStartedAndRegisteredAsDeveloper() {
        return true; //LeanplumInternal.hasStartedAndRegisteredAsDeveloper();
    }

    //ActionManager.getInstance().getDefinitions().getActionDefinitionMaps()
    public static Map<String, Object> getActionDefinitionMaps(){
        return  new HashMap<>();
    }

}
