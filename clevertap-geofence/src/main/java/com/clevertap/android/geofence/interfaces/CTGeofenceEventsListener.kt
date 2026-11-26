package com.clevertap.android.geofence.interfaces

import org.json.JSONObject

/**
 * Callback interface to get geofence enter/exit events from OS.
 */
public interface CTGeofenceEventsListener {

    /**
     * This method will be invoked when a user enters geofence. Given below is the JSON structure
     * for the event :
     * ```
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
     * ```
     *
     * @param geofenceEnteredEventProperties [JSONObject] containing geofence event details
     */
    public fun onGeofenceEnteredEvent(geofenceEnteredEventProperties: JSONObject)

    /**
     * This method will be invoked when a user exits geofence. Given below is the JSON structure
     * for the event :
     * ```
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
     * ```
     *
     * @param geofenceExitedEventProperties [JSONObject] containing geofence event details
     */
    public fun onGeofenceExitedEvent(geofenceExitedEventProperties: JSONObject)
}
