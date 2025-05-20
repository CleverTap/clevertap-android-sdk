package com.clevertap.android.geofence.fakes

import com.clevertap.android.geofence.CTGeofenceSettings
import org.json.JSONObject

object CTGeofenceSettingsFake {

    fun getSettings(jsonObject: JSONObject): CTGeofenceSettings {
        return CTGeofenceSettings.Builder()
            .enableBackgroundLocationUpdates(jsonObject.getBoolean("last_bg_location_updates"))
            .setLocationAccuracy(jsonObject.getInt("last_accuracy").toByte())
            .setLocationFetchMode(jsonObject.getInt("last_fetch_mode").toByte())
            .setLogLevel(jsonObject.getInt("last_log_level"))
            .setGeofenceMonitoringCount(jsonObject.getInt("last_geo_count"))
            .setId(jsonObject.getString("id"))
            .setInterval(jsonObject.getLong("last_interval"))
            .setFastestInterval(jsonObject.getLong("last_fastest_interval"))
            .setSmallestDisplacement(jsonObject.getDouble("last_displacement").toFloat())
            .build()
    }

    @JvmStatic
    val settingsJsonObject = JSONObject(
        """{
        "last_accuracy": 1,
        "last_fetch_mode": 1,
        "last_bg_location_updates": true,
        "last_log_level": 3,
        "last_geo_count": 47,
        "last_interval": 1800000,
        "last_fastest_interval": 1800000,
        "last_displacement": 200,
        "id": "4RW-Z6Z-485Z",
        "last_geo_notification_responsiveness": 0
    }"""
    )

    val settingsJsonString: String = settingsJsonObject.toString()
}
