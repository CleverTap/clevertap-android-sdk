## Frequently Asked Questions

1. What is Play service APK error in logs?

   Current version of Geofence SDK requires device to have Play Services APK installed, enabled and up-to-date as describe [here](https://developers.google.com/android/guides/setup#ensure_devices_have_the_google_play_services_apk). In case if mentioned requirements not satisfied then SDK will silently catch the apk error and will printout error to logs and won't proceed further. You can add the same apk check on your end and then decide if you require to init `CTGeofenceAPI`. 

2. Does this sdk require to be compliant with any google policies ?

   Yes. Please check [location permissions section](https://support.google.com/googleplay/android-developer/answer/9888170?hl=en) and [background location access policy](https://support.google.com/googleplay/android-developer/answer/9799150?hl=en)
   
3. What is the update frequency for location pings to Server? Any triggers for update of geo-fence ?

   SDK pings server for update of geo-fence whenever Location is updated by OS and below condition is satisfied:<br>
   <i>When the interval between last ping and current ping is greater than `30 minutes` and displacement is greater than `200 meters`.  **Note** that `30 minutes` and `200 meters` are fixed values and can not be changed by APPs.</i>
   
4. When the location is updated by OS ?
   
   Location updates depends on below `CTGeofenceSettings` parameters:<br>
   * [LocationFetchMode](Settings.md#locationfetchmode-)
   * [LocationAccuracy](Settings.md#locationaccuracy)
   * [Interval in milliseconds](Settings.md#interval-in-milliseconds)
   * [Fastest Interval in milliseconds](Settings.md#fastest-interval-in-milliseconds)
   * [SmallestDisplacement in meters](Settings.md#smallestdisplacement-in-meters)
   * [EnableBackgroundLocationUpdates](Settings.md#enablebackgroundlocationupdates)
   
5. Why are Geofence event callbacks not being called by the SDK?

   SDK uses google's geofencing service to monitor geofences, that may not raise events in some situations as described [here](https://developer.android.com/training/location/geofencing#Troubleshooting) 
   
6. Why does the app not receive Push Notifications even though Geofence events are raised correctly?

   * First ensure that your CleverTap push notifications integration is working properly as described in [this guide](https://developer.clevertap.com/docs/android#section-push-notifications).<br>
   * For Android 6.0 or higher due to [Doze-Standby](https://developer.android.com/training/monitoring-device-state/doze-standby) and For Android 9.0 or higher due to [App standby buckets](https://developer.android.com/topic/performance/appstandby) network connectivity for apps gets deferred by some time as described [here in Network Column](https://developer.android.com/topic/performance/power/power-details) which prevents SDK to connect to CleverTap servers for raising notifications.

7. Why does the build fail for an app when minifying is enabled for gradle wrapper 8.0+ and android gradle plugin 8.0.0+?

   * This occurs due to change in behaviour in the AGP`
     When R8 traces the program it will try to handle all the classes, methods and fields that it finds in the part of the program it considers live. Earlier during this tracing, it threw a warning which allowed building the apk. But these are now converted into errors. Details [here](https://developer.android.com/build/releases/past-releases/agp-8-0-0-release-notes)
   * Upgrade to `com.clevertap.android:clevertap-android-sdk` v6.1.0 and `com.clevertap.android:clevertap-hms-sdk` v1.3.4 to fix this issue.