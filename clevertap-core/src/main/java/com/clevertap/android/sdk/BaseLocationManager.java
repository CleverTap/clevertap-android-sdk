package com.clevertap.android.sdk;

import android.location.Location;
import java.util.concurrent.Future;

public abstract class BaseLocationManager {

    public abstract Location _getLocation();

    abstract Future<?> _setLocation(final Location location);
}
