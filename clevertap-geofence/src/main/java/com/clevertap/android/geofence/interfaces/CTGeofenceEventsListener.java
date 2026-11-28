package com.clevertap.android.geofence.interfaces;

import androidx.annotation.NonNull;

import org.json.JSONObject;

/**
 * Callback interface to get geofence enter/exit events from OS.
 * <p>
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameters carry
 * androidx nullability annotations that match what the SDK passes (SDK-6396), so a Kotlin override
 * must declare the same nullability. Do not convert this file to Kotlin.
 */
public interface CTGeofenceEventsListener {

    /**
     * This method will be invoked when a user enters geofence. Given below is the JSON structure
     * for the event :
     * <pre>
     * {
     *    "id" : 500043 //geofenceUniqueID,
     *    "gcId" : 1 //geofenceClusterId,
     *    "gcName" : "geofenceClusterName",
     *    "lat" : 19.229493674459807 //geofence Latitude,
     *    "lng" : 72.82329440116882 //geofence Longitude,
     *    "r" : 200 //geofenceRadiusInMeters,
     *    "triggered_lat" : 19.229493674459807 //geofenceTriggeredLatitude,
     *    "triggered_lng" : 72.82329440116882 //geofenceTriggeredLongitude
     * }
     * </pre>
     *
     * @param geofenceEnteredEventProperties {@link JSONObject} containing geofence event details
     */
    void onGeofenceEnteredEvent(@NonNull JSONObject geofenceEnteredEventProperties);

    /**
     * This method will be invoked when a user exits geofence. Given below is the JSON structure
     * for the event :
     * <pre>
     * {
     *    "id" : 500043 //geofenceUniqueID,
     *    "gcId" : 1 //geofenceClusterId,
     *    "gcName" : "geofenceClusterName",
     *    "lat" : 19.229493674459807 //geofence Latitude,
     *    "lng" : 72.82329440116882 //geofence Longitude,
     *    "r" : 200 //geofenceRadiusInMeters,
     *    "triggered_lat" : 19.229493674459807 //geofenceTriggeredLatitude,
     *    "triggered_lng" : 72.82329440116882 //geofenceTriggeredLongitude
     * }
     * </pre>
     *
     * @param geofenceExitedEventProperties {@link JSONObject} containing geofence event details
     */
    void onGeofenceExitedEvent(@NonNull JSONObject geofenceExitedEventProperties);
}
