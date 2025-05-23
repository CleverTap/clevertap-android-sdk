package com.clevertap.android.geofence.fakes

import org.json.JSONArray
import org.json.JSONObject

object GeofenceJSON {

    const val GEOFENCE_JSON_STRING = """
	{
	   "geofences":[
	      {
	         "id":310001,
	         "lat":19.092962,
	         "lng":72.849717,
	         "r":500,
	         "gcId":31,
	         "gcName":"GeoFence Cluster Details"
	      },
	      {
	         "id":310002,
	         "lat":19.092962,
	         "lng":72.849717,
	         "r":500,
	         "gcId":31,
	         "gcName":"GeoFence Cluster Details"
	      }
	   ]
	}
    """

    val geofence: JSONObject
        get() {
            return JSONObject(GEOFENCE_JSON_STRING)
        }

    val emptyGeofence: JSONObject
        get() {
            return JSONObject("{\"geofences\": []}")
        }

    val emptyJson: JSONObject
        get() {
            return JSONObject("{}")
        }

    val first: JSONObject
        get() {
            val jsonArray = JSONArray()
            val jsonObject = JSONObject()
            jsonArray.put(geofence.getJSONArray("geofences").getJSONObject(0))
            jsonObject.put("geofences", jsonArray)

            return jsonObject
        }

    val firstFromGeofenceArray: JSONArray
        get() {
            val jsonArray = JSONArray()
            jsonArray.put(geofence.getJSONArray("geofences").getJSONObject(0))

            return jsonArray
        }

    val geofenceArray: JSONArray
        get() {
            return geofence.getJSONArray("geofences")
        }

    val lastFromGeofenceArray: JSONArray
        get() {
            val jsonArray = JSONArray()
            jsonArray.put(geofence.getJSONArray("geofences").getJSONObject(1))

            return jsonArray
        }
}
