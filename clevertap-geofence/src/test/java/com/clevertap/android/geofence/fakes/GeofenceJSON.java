package com.clevertap.android.geofence.fakes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GeofenceJSON {

    public static final String GEOFENCE_JSON_STRING = "{\n" +
            "  \"geofences\": [\n" +
            "    {\n" +
            "      \"id\": 310001,\n" +
            "      \"lat\": 19.092962,\n" +
            "      \"lng\": 72.849717,\n" +
            "      \"r\": 500,\n" +
            "      \"gcId\": 31,\n" +
            "      \"gcName\": \"GeoFence Cluster Details\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": 310002,\n" +
            "      \"lat\": 19.092962,\n" +
            "      \"lng\": 72.849717,\n" +
            "      \"r\": 500,\n" +
            "      \"gcId\": 31,\n" +
            "      \"gcName\": \"GeoFence Cluster Details\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    public static JSONObject getEmptyGeofence() {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject("{\"geofences\": []}");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    public static JSONObject getEmptyJson() {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject("{}");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    public static JSONObject getFirst() {
        JSONArray jsonArray = null;
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject();
            jsonArray = new JSONArray();
            jsonArray.put(getGeofence().getJSONArray("geofences").getJSONObject(0));

            jsonObject.put("geofences", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    public static JSONArray getFirstFromGeofenceArray() {
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray();
            jsonArray.put(getGeofence().getJSONArray("geofences").getJSONObject(0));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonArray;
    }

    public static JSONObject getGeofence() {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(GEOFENCE_JSON_STRING);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    public static JSONArray getGeofenceArray() {
        JSONArray jsonArray = null;
        try {
            jsonArray = getGeofence().getJSONArray("geofences");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonArray;
    }

    public static String getGeofenceString() {
        return GEOFENCE_JSON_STRING;
    }

    public static JSONArray getLastFromGeofenceArray() {
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray();
            jsonArray.put(getGeofence().getJSONArray("geofences").getJSONObject(1));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonArray;
    }

}
