package com.clevertap.android.geofence.interfaces;

import org.json.JSONObject;

/**
 * Callback interface to get geofence enter/exit events from OS.
 */
public interface CTGeofenceEventsListener {

    /**
     * This method will be invoked when a user enters geofence. Given below is the JSON structure
     * for the event :<br>
     * <code>
     * <dl><dt>{</dt>
     *    <dd>"id" : 500043 //geofenceUniqueID,</dd>
     *    <dd>"gcId" : 1 //geofenceClusterId,</dd>
     *    <dd>"gcName" : "geofenceClusterName",</dd>
     *    <dd>"lat" : 19.229493674459807 //geofence Latitude,</dd>
     *    <dd>"lng" : 72.82329440116882 //geofence Longitude,</dd>
     *    <dd>"r" : 200 //geofenceRadiusInMeters,</dd>
     *    <dd>"triggered_lat" : 19.229493674459807 //geofenceTriggeredLatitude,</dd>
     *    <dd>"triggered_lng" : 72.82329440116882 //geofenceTriggeredLongitude</dd>
     * }</dl>
     *  </code>
     *
     * @param geofenceEnteredEventProperties {@link JSONObject} containing geofence event details
     */
    void onGeofenceEnteredEvent(JSONObject geofenceEnteredEventProperties);

    /**
     * This method will be invoked when a user exits geofence. Given below is the JSON structure
     * for the event :<br>
     * <code>
     * <dl><dt>{</dt>
     *    <dd>"id" : 500043 //geofenceUniqueID,</dd>
     *    <dd>"gcId" : 1 //geofenceClusterId,</dd>
     *    <dd>"gcName" : "geofenceClusterName",</dd>
     *    <dd>"lat" : 19.229493674459807 //geofence Latitude,</dd>
     *    <dd>"lng" : 72.82329440116882 //geofence Longitude,</dd>
     *    <dd>"r" : 200 //geofenceRadiusInMeters,</dd>
     *    <dd>"triggered_lat" : 19.229493674459807 //geofenceTriggeredLatitude,</dd>
     *    <dd>"triggered_lng" : 72.82329440116882 //geofenceTriggeredLongitude</dd>
     * }</dl>
     *  </code>
     *
     * @param geofenceExitedEventProperties {@link JSONObject} containing geofence event details
     */
    void onGeofenceExitedEvent(JSONObject geofenceExitedEventProperties);
}
