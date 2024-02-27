<p align="center">
  <img src="https://github.com/CleverTap/clevertap-ios-sdk/blob/master/docs/images/clevertap-logo.png" height="220"/>
</p>

## 👋 Introduction
[(Back to top)](#-table-of-contents)

CleverTap Android Geofence SDK provides **Geofencing capabilities** to CleverTap Android SDK by using the Play Services Location library.

* If you haven't already configured your project for **CleverTap SDK**, follow the instructions [here](https://developer.clevertap.com/docs/android-quickstart-guide).
* If you haven't already configured your project for **Play Services**, follow the instructions [here](https://developers.google.com/android/guides/google-services-plugin).

## 🎉 Installation
[(Back to top)](#-table-of-contents)

Add the following dependencies to the `build.gradle`

```Groovy
implementation "com.clevertap.android:clevertap-geofence-sdk:1.3.0"
implementation "com.clevertap.android:clevertap-android-sdk:6.1.1" // 3.9.0 and above
implementation "com.google.android.gms:play-services-location:21.0.0"
implementation "androidx.work:work-runtime:2.7.1" // required for FETCH_LAST_LOCATION_PERIODIC
implementation "androidx.concurrent:concurrent-futures:1.1.0" // required for FETCH_LAST_LOCATION_PERIODIC
```
## 🔒 Permissions
[(Back to top)](#-table-of-contents)

In order to start using geofence in your app, the app will need below permissions in `AndroidManifest.xml` which is already added by SDK so you don’t have to add anything in manifest.
```XML
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
```
Before SDK initialization App will need to prompt users to grant below permissions at runtime. Since users can revoke permissions at any time from the app Settings screen, your app needs to check that it has the permissions it needs every time it runs.
See [Permissions](https://developer.android.com/preview/features/runtime-permissions.html) and [Location Permissions](https://developer.android.com/training/location/permissions#request-location-access-runtime) for more details.
Also if your app targets Android 12 or higher then you must also request the ACCESS_COARSE_LOCATION with ACCESS_FINE_LOCATION permission. You must include both permissions in a single runtime request.See [Android 12 location Permissions changes](https://developer.android.com/about/versions/12/behavior-changes-12#approximate-location) for more details.

```XML
android.permission.ACCESS_FINE_LOCATION
android.permission.ACCESS_BACKGROUND_LOCATION (Required only when requesting background location access on Android 10 (API level 29))
```

## 🚀 Initialization
[(Back to top)](#-table-of-contents)

`CTGeofenceAPI` needs an object of `CTGeofenceSettings` and the object of `CleverTapAPI` to be initialized in the following manner-

```java
CTGeofenceAPI.getInstance(getApplicationContext()).init(ctGeofenceSettings,cleverTapAPI);
```

`CTGeofenceSettings` object can be created in the following way -

```java
CTGeofenceSettings ctGeofenceSettings = new CTGeofenceSettings.Builder()
                .enableBackgroundLocationUpdates(bgLocation)//boolean to enable background location updates
                .setLogLevel(logLevel)//Log Level
                .setLocationAccuracy(accuracy)//byte value for Location Accuracy
                .setLocationFetchMode(fetchMode)//byte value for Fetch Mode
                .setGeofenceMonitoringCount(geofenceCount)//int value for number of Geofences CleverTap can monitor
                .setInterval(interval)//long value for interval in milliseconds
                .setFastestInterval(fastestInterval)//long value for fastest interval in milliseconds
                .setSmallestDisplacement(displacement)//float value for smallest Displacement in meters
                .setGeofenceNotificationResponsiveness(geofenceNotificationResponsiveness)// int value for geofence notification responsiveness in milliseconds
                .build();
 ```
**Note** - 

* CleverTapAPI needs to be initialized before CTGeofenceAPI object is created as the object of CleverTapAPI is required to initialize the CleverTap Android Geofence SDK.
* Runtime location permissions needed for SDK to work
* CTGeofenceAPI is automatically activated once the init() method has been called.
* CleverTap Android Geofence SDK will raise the “Geofence Cluster Entered” and “Geofence Cluster Exited” events automatically. The app cannot raise these methods manually.

## 📖 Settings parameters
[(Back to top)](#-table-of-contents)

Detailed info can be found [here](https://github.com/CleverTap/clevertap-android-sdk/tree/master/docs/Settings.md)


## 📍 Trigger Location
[(Back to top)](#-table-of-contents)

This method fetches last known location from OS (can be null) and delivers it to APP through `CTLocationUpdatesListener`. 
Detailed info can be found [here](https://github.com/CleverTap/clevertap-android-sdk/tree/master/docs/TriggerLocation.md)

```java
try {
    CTGeofenceAPI.getInstance(getApplicationContext()).triggerLocation();
} catch (IllegalStateException e) {
    // thrown when this method is called before geofence sdk initiaisation
}
```

## 📞 Callbacks/Listeners
[(Back to top)](#-table-of-contents)

CleverTap Android Geofence SDK provides 3 callbacks or listeners on the main thread to the app for more control to the developers.

### OnGeofenceApiInitializedListener

CleverTap Android Geofence SDK provides a callback on the main thread to notify that the CTGeofenceAPI class has been initialized. The callback can be used in the following way - 
```java
CTGeofenceAPI.getInstance(getApplicationContext())
       .setOnGeofenceApiInitializedListener(new CTGeofenceAPI.OnGeofenceApiInitializedListener() {
           @Override
           public void OnGeofenceApiInitialized() {
               //App is notified on the main thread that CTGeofenceAPI is initialized
           }
       });
 ```

### CTGeofenceEventsListener

CleverTap Android Geofence SDK provides a callback on the main thread to the app when the user enters or exits a Geofence. The callback can be used in the following way -
```java
CTGeofenceAPI.getInstance(getApplicationContext())
       .setCtGeofenceEventsListener(new CTGeofenceEventsListener() {
           @Override
           public void onGeofenceEnteredEvent(JSONObject jsonObject) {
                //Callback on the main thread when user enters Geofence with info in jsonObject             
           }

           @Override
           public void onGeofenceExitedEvent(JSONObject jsonObject) {
               //Callback on the main thread when user exits Geofence with info in jsonObject             
           }
       });
```

Structure of JSON Object on Geocluster Entered/Exited event :

```JSON
{
      "id" : 500043 //geofenceUniqueID,
      "gcId" : 1 //geofenceClusterId,
      "gcName" : "geofenceClusterName",
      "lat" : 19.229493674459807 //geofence Latitude,
      "lng" : 72.82329440116882 //geofence Longitude,
      "r" : 200 //geofenceRadiusInMeters,
      "triggered_lat" : 19.229493674459807 //geofenceTriggeredLatitude,
      "triggered_lng" : 72.82329440116882 //geofenceTriggeredLongitude
   }
 ```
 
### CTLocationUpdatesListener

CleverTap Android Geofence SDK provides a callback on the main thread to the app when Android OS provides a location update to the SDK. The callback can be used in the following way -
```java
CTGeofenceAPI.getInstance(getApplicationContext())
       .setCtLocationUpdatesListener(new CTLocationUpdatesListener() {
           @Override
           public void onLocationUpdates(Location location) {
              //New location on the main thread as provided by the Android OS
           }
       });
 ```

## ⏹️ Deactivation
[(Back to top)](#-table-of-contents)

If at any point you want to deactivate the CleverTap Android Geofence SDK, you can do so in the following way-

```java
CTGeofenceAPI.getInstance(getApplicationContext()).deactivate();
```

**Note:** Deactivation will remove all callbacks/listeners as well. 

## 📜 Proguard 
[(Back to top)](#-table-of-contents)

If you're using ProGuard to minify your app builds, use the following rules for a smooth working of the CleverTap Android Geofence SDK

```proguard
-keep class com.google.android.gms.common.*
-keep class com.google.android.gms.location.*
-keep class androidx.concurrent.futures.*
```

## 𝌡 Example Usage
[(Back to top)](#-table-of-contents)

A [demo application](https://github.com/CleverTap/clevertap-android-sdk/tree/master/sample)) showing CleverTap Android Geofence SDK integration.

## ❓ FAQ
[(Back to top)](#-table-of-contents)

FAQ can be found [here](https://github.com/CleverTap/clevertap-android-sdk/tree/master/docs/FAQ.md). 

## 🤝 Questions
[(Back to top)](#-table-of-contents)

If your question is not found in FAQ and you have other questions or concerns, you can reach out to the CleverTap support team by raising an issue from the CleverTap Dashboard.