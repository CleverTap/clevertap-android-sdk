package com.clevertap.android.geofence.fakes;

import com.clevertap.android.geofence.CTGeofenceSettings;
import org.json.JSONException;
import org.json.JSONObject;

public class CTGeofenceSettingsFake {

    public static CTGeofenceSettings getSettings(JSONObject jsonObject) {
        try {
            return new CTGeofenceSettings.Builder()
                    .enableBackgroundLocationUpdates(jsonObject.getBoolean("last_bg_location_updates"))
                    .setLocationAccuracy((byte) jsonObject.getInt("last_accuracy"))
                    .setLocationFetchMode((byte) jsonObject.getInt("last_fetch_mode"))
                    .setLogLevel(jsonObject.getInt("last_log_level"))
                    .setGeofenceMonitoringCount(jsonObject.getInt("last_geo_count"))
                    .setId(jsonObject.getString("id"))
                    .setInterval(jsonObject.getLong("last_interval"))
                    .setFastestInterval(jsonObject.getLong("last_fastest_interval"))
                    .setSmallestDisplacement((float) jsonObject.getDouble("last_displacement"))
                    .build();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new CTGeofenceSettings.Builder().build();
    }

    public static JSONObject getSettingsJsonObject() {
        JSONObject jsonObject = null;
        try {

            jsonObject = new JSONObject("{\"last_accuracy\":1,\"last_fetch_mode\":1," +
                    "\"last_bg_location_updates\":true,\"last_log_level\":3,\"last_geo_count\":47," +
                    "\"last_interval\":1800000,\"last_fastest_interval\":1800000,\"last_displacement\":200," +
                    "\"id\":\"4RW-Z6Z-485Z\"}");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    public static String getSettingsJsonString() {
        String jsonObject = null;
        try {

            jsonObject = new JSONObject("{\"last_accuracy\":1,\"last_fetch_mode\":1," +
                    "\"last_bg_location_updates\":true,\"last_log_level\":3,\"last_geo_count\":47," +
                    "\"last_interval\":1800000,\"last_fastest_interval\":1800000,\"last_displacement\":200," +
                    "\"id\":\"4RW-Z6Z-485Z\",\"last_geo_notification_responsiveness\":0}").toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

}
