## Trigger Location

`triggerLocation()` - This method fetches last known location from OS (can be null) and delivers it to APP through CTLocationUpdatesListener. This also synchronises geofences based on location passed to CleverTap servers with throttling limit of minimum `30 minutes` and minimum displacement of `200 meters` between two syncs.

**Note:** Geofence SDK must be initialised before this method call or else `IllegalStateException` will be thrown

Below are two main use cases where this method can be used:

* Apps need last known location for their own use without implementing location feature in their app
* Apps can use it to maintain a tradeoff between location update interval and geofence synchronisation, For example.
If an app sets a location update interval of 6 hours and displacement of 2 km due to some reasons like battery optimisations but before the next location update also wants to sync geofences dynamically based on some smart logic then this can be handy. Note that throttling limit of 30 minutes and displacement of 200 meters between two syncs will be applied here.

![trigger_location_syncs](https://github.com/CleverTap/clevertap-android-sdk/blob/master/static/trigger_location.png)


