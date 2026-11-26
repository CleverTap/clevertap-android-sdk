package com.clevertap.android.sdk

import android.location.Location
import java.util.concurrent.Future

internal abstract class BaseLocationManager {

    abstract fun findBestLocation(): Location?

    abstract fun updateLocation(location: Location?): Future<*>?
}
