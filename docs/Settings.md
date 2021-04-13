## CTGeofenceSettings parameters:

### LogLevel : 
Default is `DEBUG`

* **Logger.OFF** - Turns off sdk logging
* **Logger.INFO** - Prints info level sdk logging
* **Logger.DEBUG** - Prints debug level sdk logging
* **Logger.VERBOSE** - Prints verbose level sdk logging

### LocationFetchMode : 
Default is `FETCH_LAST_LOCATION_PERIODIC`

* **CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC** - This value will use Periodic work manager which will fetch **last known location** from OS periodically(use `setInterval()` to set the interval). Location fetched using this may be less accurate and might be null in case Location is turned off in the device settings, the device never recorded its location or Google Play services on the device have restarted. This will give better battery optimisation with less location accuracy.
* **CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC** - This value will use Periodic Receiver which will fetch **current device location** from OS. Accuracy and battery optimisation can vary from high to low based on interval, displacement and accuracy values provided in `CTGeofenceSettings`.

### LocationAccuracy:
Default is `ACCURACY_HIGH`. Applicable only for `FETCH_CURRENT_LOCATION_PERIODIC`

* **CTGeofenceSettings.ACCURACY_HIGH** - provides the most accurate location possible, which is computed using as many inputs as necessary (it enables GPS, Wi-Fi, and cell, and uses a variety of Sensors), and may cause significant battery drain.
* **CTGeofenceSettings.ACCURACY_MEDIUM** -  provides accurate location while optimising for power. Very rarely uses GPS. Typically uses a combination of Wi-Fi and cell information to compute device location.
* **CTGeofenceSettings.ACCURACY_LOW** - largely relies on cell towers and avoids GPS and Wi-Fi inputs, providing coarse (city-level) accuracy with minimal battery drain.

### Interval in milliseconds: 
Default is `30 minutes`. Applicable for both fetch modes.

* Values less than 30 minutes will be ignored by SDK. SDK will then handover this value to [Google Location API](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest#public-locationrequest-setinterval-long-millis)

### Fastest Interval in milliseconds: 
Default is `30 minutes`. Applicable only for `FETCH_CURRENT_LOCATION_PERIODIC`

* Values less than 30 minutes will be ignored by SDK. SDK will then handover this value to [Google Location API](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest#public-locationrequest-setfastestinterval-long-millis)

### SmallestDisplacement in meters: 
Default is `200 meters`. Applicable only for `FETCH_CURRENT_LOCATION_PERIODIC`

* Values less than 200 meters will be ignored by SDK. SDK will then handover this value to [Google Location API](https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest#public-locationrequest-setsmallestdisplacement-float-smallestdisplacementmeters)

### Geofence Monitoring Count: 
Default is `50`. 

* As per android documentation You can have multiple active geofences, with a limit of 100 per app, per device user. 
* SDK lets the App decide how many geofences they want to monitor through CleverTap by setting this value.
* Use values in the range of **1-100**

### EnableBackgroundLocationUpdates: 
Default is `true`. 

* When **true**, this will allow SDK to register background location updates through any of the above mentioned fetch modes.
* When **false**, this will inform SDK to fetch location only in foreground when the app is launched or through `triggerLocation()` and not to register background location updates through any of the above mentioned fetch modes.

### Geofence Notification Responsiveness in milliseconds:
Default is `0`.

* This can be used to set the notification responsiveness to a higher value. Doing so improves power consumption by increasing the latency of geofence alerts. For example, if you set a responsiveness value of five minutes your app only checks for an entrance or exit alert once every five minutes. Setting lower values doesn't necessarily mean that users are notified within that time period (for example, if you set a value of 5 seconds it may take a bit longer than that to receive the alert).