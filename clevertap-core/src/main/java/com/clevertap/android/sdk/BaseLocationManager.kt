package com.clevertap.android.sdk

import android.location.Location
import java.util.concurrent.Future
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class BaseLocationManager {

    abstract fun findBestLocation(): Location?

    abstract fun updateLocation(location: Location?): Future<*>?
}
