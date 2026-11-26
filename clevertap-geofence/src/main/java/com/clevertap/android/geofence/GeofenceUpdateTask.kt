package com.clevertap.android.geofence

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.geofence.interfaces.CTGeofenceTask.OnCompleteListener
import com.clevertap.android.geofence.model.CTGeofence
import com.google.android.gms.tasks.OnSuccessListener
import org.json.JSONObject
import androidx.annotation.RestrictTo

/**
 * A task of type [CTGeofenceTask] responsible for Adding/Replacing(remove followed by add)
 * Geofences into file and OS
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class GeofenceUpdateTask(context: Context, private val fenceList: JSONObject?) : CTGeofenceTask {

    private val context: Context = context.applicationContext

    private val ctGeofenceAdapter: CTGeofenceAdapter? = CTGeofenceAPI.getInstance(this.context).ctGeofenceAdapter

    private var onCompleteListener: OnCompleteListener? = null

    /**
     * Initiates [GeofenceUpdateTask] execution
     */
    @WorkerThread
    override fun execute() {
        if (ctGeofenceAdapter == null) {
            return
        }

        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Executing GeofenceUpdateTask..."
        )

        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Reading previously registered geofences from file..."
        )

        val oldFenceListString = FileUtils.readFromFile(
            context,
            FileUtils.getCachedFullPath(context, CTGeofenceConstants.CACHED_FILE_NAME)
        )

        if (oldFenceListString.trim { it <= ' ' } != "") {
            var ctOldGeofenceIdList: List<String>? = null
            var ctOldGeofenceObject: JSONObject? = null
            try {
                ctOldGeofenceObject = JSONObject(oldFenceListString)
                ctOldGeofenceIdList = Utils.jsonToGeoFenceList(ctOldGeofenceObject)
            } catch (e: Exception) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to read previously registered geofences from file", e
                )
            }

            if (fenceList != null) {
                //remove previously added geofences
                ctGeofenceAdapter.removeAllGeofence(ctOldGeofenceIdList, OnSuccessListener {
                    // called on same calling thread
                    addGeofences(fenceList)
                })
            } else {
                // In case device reboot, boot receiver will pass null fenceList which simply means
                // read old fences from file and add back to Geofence Client
                addGeofences(ctOldGeofenceObject)
            }
        } else {
            // add new fences
            addGeofences(fenceList)
        }

        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Finished executing GeofenceUpdateTask"
        )
    }

    override fun setOnCompleteListener(onCompleteListener: OnCompleteListener) {
        this.onCompleteListener = onCompleteListener
    }

    /**
     * Extracts top n geofences from provided [JSONObject] as requested by User through
     * [com.clevertap.android.geofence.CTGeofenceSettings.Builder.setGeofenceMonitoringCount]
     * and store it to file followed by registration to OS through
     * [GoogleGeofenceAdapter.addAllGeofence]
     * Caller will be notified on registration of the geofences through [OnCompleteListener]
     *
     * @param geofenceObject json response containing list of geofences
     */
    @WorkerThread
    private fun addGeofences(geofenceObject: JSONObject?) {
        if (geofenceObject == null || ctGeofenceAdapter == null) {
            return
        }

        var geofenceMoitoringCount = CTGeofenceSettings.DEFAULT_GEO_MONITOR_COUNT
        val geofenceSettings = CTGeofenceAPI.getInstance(context).geofenceSettings

        if (geofenceSettings != null) {
            geofenceMoitoringCount = geofenceSettings.geofenceMonitoringCount
        }

        val fenceSubList = JSONObject()

        try {
            val geofenceObjectJSONArray = geofenceObject.getJSONArray(CTGeofenceConstants.KEY_GEOFENCES)

            if (geofenceMoitoringCount > geofenceObjectJSONArray.length()) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Requested geofence monitoring count is greater than available count." +
                            " Setting request count to " + geofenceObjectJSONArray.length()
                )

                geofenceMoitoringCount = geofenceObjectJSONArray.length()
            }

            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Extracting Top " + geofenceMoitoringCount + " new geofences out of " +
                        geofenceObjectJSONArray.length() + "..."
            )

            val jsonSubArray = Utils.subArray(
                geofenceObjectJSONArray,
                0, geofenceMoitoringCount
            )
            fenceSubList.put(CTGeofenceConstants.KEY_GEOFENCES, jsonSubArray)

            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Successfully created geofence sublist"
            )
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to create geofence sublist", e
            )
        }

        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Writing $geofenceMoitoringCount new geofences to file..."
        )

        //add new geofences, this will overwrite old ones
        val writeJsonToFile = FileUtils.writeJsonToFile(
            context, FileUtils.getCachedDirName(context),
            CTGeofenceConstants.CACHED_FILE_NAME, fenceSubList
        )

        if (writeJsonToFile) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "New geofences successfully written to file"
            )
        } else {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to write new geofences to file"
            )
            if (CTGeofenceAPI.getInstance(context).cleverTapApi != null) {
                CTGeofenceAPI.getInstance(context)
                    .cleverTapApi!!
                    .pushGeoFenceError(CTGeofenceConstants.ERROR_CODE, "Failed to write new geofences to file")
            }
        }

        val ctGeofenceList: List<CTGeofence> = CTGeofence.from(fenceSubList)

        ctGeofenceAdapter.addAllGeofence(ctGeofenceList, OnSuccessListener {
            if (onCompleteListener != null) {
                onCompleteListener!!.onComplete()
            }
        })
    }
}
