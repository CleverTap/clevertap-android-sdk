package com.clevertap.android.sdk;

import android.location.Location;
import java.util.concurrent.Future;

abstract class BaseLocationManager {

    abstract Future<?> _setLocation(final Location location);
}
