package com.clevertap.android.sdk

import android.location.Location
import java.util.concurrent.Future

internal abstract class BaseLocationManager {

    abstract fun _getLocation(): Location?

    abstract fun _setLocation(location: Location?): Future<*>?
}
