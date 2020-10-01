package com.clevertap.android.geofence;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter;
import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import com.clevertap.android.geofence.model.CTGeofence;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A task of type {@link CTGeofenceTask} responsible for Adding/Replacing(remove followed by add)
 * Geofences into file and OS
 */
class GeofenceUpdateTask implements CTGeofenceTask {

    private final Context context;

    @Nullable
    private final CTGeofenceAdapter ctGeofenceAdapter;

    private final JSONObject fenceList;

    @Nullable
    private OnCompleteListener onCompleteListener;

    GeofenceUpdateTask(Context context, @Nullable JSONObject fenceList) {
        this.context = context.getApplicationContext();
        this.fenceList = fenceList;
        ctGeofenceAdapter = CTGeofenceAPI.getInstance(this.context).getCtGeofenceAdapter();
    }

    /**
     * Initiates {@link GeofenceUpdateTask} execution
     */
    @WorkerThread
    @Override
    public void execute() {

        if (ctGeofenceAdapter == null) {
            return;
        }

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Executing GeofenceUpdateTask...");

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Reading previously registered geofences from file...");

        String oldFenceListString = FileUtils.readFromFile(context,
                FileUtils.getCachedFullPath(context, CTGeofenceConstants.CACHED_FILE_NAME));

        if (!oldFenceListString.trim().equals("")) {

            List<String> ctOldGeofenceIdList = null;
            JSONObject ctOldGeofenceObject = null;
            try {
                ctOldGeofenceObject = new JSONObject(oldFenceListString);
                ctOldGeofenceIdList = Utils.jsonToGeoFenceList(ctOldGeofenceObject);
            } catch (Exception e) {
                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Failed to read previously registered geofences from file");
                e.printStackTrace();
            }

            if (fenceList != null) {
                //remove previously added geofences
                ctGeofenceAdapter.removeAllGeofence(ctOldGeofenceIdList, new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o) {
                        // called on same calling thread
                        addGeofences(fenceList);
                    }
                });
            } else {
                // In case device reboot, boot receiver will pass null fenceList which simply means
                // read old fences from file and add back to Geofence Client
                addGeofences(ctOldGeofenceObject);
            }
        } else {
            // add new fences
            addGeofences(fenceList);
        }

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Finished executing GeofenceUpdateTask");
    }

    @Override
    public void setOnCompleteListener(@NonNull OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    /**
     * Extracts top n geofences from provided {@link JSONObject} as requested by User through
     * {@link com.clevertap.android.geofence.CTGeofenceSettings.Builder#setGeofenceMonitoringCount(int)}
     * and store it to file followed by registration to OS through
     * {@link GoogleGeofenceAdapter#addAllGeofence(List, OnSuccessListener)}
     * <br>Caller will be notified on registration of the geofences through {@link OnCompleteListener}
     *
     * @param geofenceObject json response containing list of geofences
     */
    @WorkerThread
    private void addGeofences(@Nullable JSONObject geofenceObject) {

        if (geofenceObject == null || ctGeofenceAdapter == null) {
            return;
        }

        int geofenceMoitoringCount = CTGeofenceSettings.DEFAULT_GEO_MONITOR_COUNT;
        CTGeofenceSettings geofenceSettings = CTGeofenceAPI.getInstance(context).getGeofenceSettings();

        if (geofenceSettings != null) {
            geofenceMoitoringCount = geofenceSettings.getGeofenceMonitoringCount();
        }

        JSONObject fenceSubList = new JSONObject();

        try {
            JSONArray geofenceObjectJSONArray = geofenceObject.getJSONArray(CTGeofenceConstants.KEY_GEOFENCES);

            if (geofenceMoitoringCount > geofenceObjectJSONArray.length()) {

                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Requested geofence monitoring count is greater than available count." +
                                " Setting request count to " + geofenceObjectJSONArray.length());

                geofenceMoitoringCount = geofenceObjectJSONArray.length();
            }

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Extracting Top " + geofenceMoitoringCount + " new geofences out of " +
                            geofenceObjectJSONArray.length() + "...");

            JSONArray jsonSubArray = Utils.subArray(geofenceObjectJSONArray,
                    0, geofenceMoitoringCount);
            fenceSubList.put(CTGeofenceConstants.KEY_GEOFENCES, jsonSubArray);

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Successfully created geofence sublist");
        } catch (Exception e) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to create geofence sublist");
            e.printStackTrace();
        }

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Writing " + geofenceMoitoringCount + " new geofences to file...");

        //add new geofences, this will overwrite old ones
        boolean writeJsonToFile = FileUtils.writeJsonToFile(context, FileUtils.getCachedDirName(context),
                CTGeofenceConstants.CACHED_FILE_NAME, fenceSubList);

        if (writeJsonToFile) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "New geofences successfully written to file");
        } else {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to write new geofences to file");
            if (CTGeofenceAPI.getInstance(context).getCleverTapApi() != null) {
                CTGeofenceAPI.getInstance(context)
                        .getCleverTapApi()
                        .pushGeoFenceError(CTGeofenceConstants.ERROR_CODE, "Failed to write new geofences to file");
            }
        }

        List<CTGeofence> ctGeofenceList = CTGeofence.from(fenceSubList);

        ctGeofenceAdapter.addAllGeofence(ctGeofenceList, new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                if (onCompleteListener != null) {
                    onCompleteListener.onComplete();
                }
            }
        });
    }
}
