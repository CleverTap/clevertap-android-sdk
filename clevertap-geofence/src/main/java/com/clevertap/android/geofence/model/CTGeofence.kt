package com.clevertap.android.geofence.model

import com.clevertap.android.geofence.CTGeofenceAPI
import com.clevertap.android.geofence.CTGeofenceConstants
import org.json.JSONException
import org.json.JSONObject

/**
 * Wrapper over [com.google.android.gms.location.Geofence]
 */
internal class CTGeofence private constructor(builder: Builder) {

    class Builder internal constructor(internal val id: String) {

        internal var latitude = 0.0

        internal var longitude = 0.0

        internal var radius = 0

        internal var transitionType = 0

        internal fun build(): CTGeofence {
            return CTGeofence(this)
        }

        internal fun setLatitude(latitude: Double): Builder {
            this.latitude = latitude
            return this
        }

        internal fun setLongitude(longitude: Double): Builder {
            this.longitude = longitude
            return this
        }

        internal fun setRadius(radius: Int): Builder {
            this.radius = radius
            return this
        }

        internal fun setTransitionType(transitionType: Int): Builder {
            this.transitionType = transitionType
            return this
        }
    }

    val id: String = builder.id

    val latitude: Double = builder.latitude

    val longitude: Double = builder.longitude

    val radius: Int = builder.radius

    val transitionType: Int = builder.transitionType

    companion object {

        /**
         * Converts [JSONObject] containing an array of geofences to list of [CTGeofence]
         *
         * @param jsonObject containing an array of geofences
         * @return list of [CTGeofence]
         */
        @JvmStatic
        fun from(jsonObject: JSONObject): List<CTGeofence> {
            val geofenceList = ArrayList<CTGeofence>()

            try {
                val array = jsonObject.getJSONArray(CTGeofenceConstants.KEY_GEOFENCES)

                for (i in 0 until array.length()) {
                    val `object` = array.getJSONObject(i)
                    val geofence = Builder(`object`.getInt(CTGeofenceConstants.KEY_ID).toString())
                        .setLatitude(`object`.getDouble("lat"))
                        .setLongitude(`object`.getDouble("lng"))
                        .setRadius(`object`.getInt("r"))
                        .build()
                    geofenceList.add(geofence)
                }
            } catch (e: JSONException) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Could not convert JSON to GeofenceList", e
                )
            }

            return geofenceList
        }
    }
}
