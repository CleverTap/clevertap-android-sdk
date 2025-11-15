package com.clevertap.android.sdk

import android.location.Location
import java.util.concurrent.Future

internal interface BaseLocationManager {
    fun _getLocation(): Location?

    fun _setLocation(location: Location?): Future<*>?
}
