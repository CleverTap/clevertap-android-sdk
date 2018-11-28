## CHANGE LOG

### Version 3.3.3 (November 28, 2018)
* Fixes the bug which caused CTA buttons to not open the mentioned deeplink

### Version 3.3.2 (November 12, 2018)
* Fixes the app crash issue for Interstitial InApp notification template when not using ExoPlayer
* Fixes the bug empty buttons in Half Interstitial InApp notification template when no buttons are provided

### Version 3.3.1 (October 31, 2018)
* Fixes the issue for developers who override `InAppNotificationListener` methods

### Version 3.3.0 (October 26, 2018)
* Adds support for Native InApp Notifications
* Bug fixes and performance improvements

### Version 3.2.0 (September 04, 2018)
* Adds support to create multiple instances of CleverTap Android SDK
* Deprecated `CleverTapException`, `CleverTapMetaDataNotFoundException`, `CleverTapPermissionsNotSatisfied` and `InvalidEventNameException`
* Deprecated `CleverTapAPI.CHARGED_EVENT` use `cleverTapAPI.pushChargedEvent()`
* Deprecated `event`, `profile`, `session` and `data` methods, use respective `CleverTapAPI` methods
* Deprecated `CleverTapAPI.getInstance()` method. Use `CleverTapAPI.getDefaultInstance()` instead
* Added APIs for setting the SDK to offline and SSL Pinning

### Version 3.1.10 (May 19, 2018)
* Bug Fixes

### Version 3.1.9 (May 02, 2018)
* Methods for GDPR compliance
* New API for screen tracking
* New API for Android O channels with custom sound
* Various performance improvements

### Version 3.1.8 (December 22, 2017)
* Bug fixes and performance improvements

### Version 3.1.7 (October 08, 17)
* Fixes deep linking issue for Android versions less than Android N

### Version 3.1.6 (September 21, 2017)
* Adds Android Oreo support

### Version 3.1.4 (July 01, 2017)
* Custom sound and CTAs for Push Notifications
* Performance improvements

### Version 3.1.2 (January 31, 2017)
* Various performance enhancements

### Version 3.1.1 (December 15, 2016)
* Various performance enhancements

### Version 3.1.0 (October 20, 2016)
* Various performance enhancements

### Version 3.0.0 (August 31, 2016)
* Adds FCM support
* Updates GCM handling
* Please note the AndroidManifest.xml requirements have changed if using GCM, please refer to the [documentation](https://www.developer.clevertap.com/docs/android).

### Version 2.2.0 (July 20, 2016)
* Adds `onUserLogin` API to support multiple distinct user profiles per device
* Adds `getLocation` API

### Version 2.1.3 (June 24, 2016)
* Sending non primitive values for profile/event properties doesn’t abort the entire push (just skips that particular property).

### Version 2.1.2 (June 15, 2016)
* Updates to support Google Play privacy constraints

### Version 2.1.1 (June 07, 2016)
* Fixes InApp Notification blacklist bug

### Version 2.1.0 (May 08, 2016)
* Adds ability to receive InApp Notification button click callbacks with custom key-value pairs
* Adds support for dashboard analytics on specific InApp Notification button clicks

### Version 2.0.11 (April 21, 2016)
* Multi-value profile property handling improvements

### Version 2.0.10 (April 07, 2016)
* Adds support for Segment bundled integration
* Removes support for Segment webhook integration
* Improved Uninstall tracking support

### Version 2.0.9 (March 15, 2016)
* Adds support for migrating from Parse.com push notifications
* Adds support for multi-value (JSONArray) user profile properties
* Adds support for In-App Notification display frequency capping

Note: To support multi-value user profile properties, `CleverTap.profile.getProperty(key)` now returns an Object, rather than a String.

### Version 2.0.5 (January 09, 2016)
* Added updateLocation API: If your application is collecting location you can pass it to CleverTap for, among other things, more fine-grained geo-targeting and segmentation purposes.
* Added support for Segment webhook/server-side integration

### Version 2.0.4 (December 16, 2015)
* Fixed InApp Activity exclude feature

### Version 2.0.3 (December 09, 2015)
* Added SyncListener to notify application code of User Profile synchronization updates
* Added ability to push custom error events: `clevertap.event.pushError(String, int)`
* Removed `clevertap.profile.pushGraphUser(com.facebook...)`
* Requires update to Android Support Library, revision 23.1.1

### Version 2.0.1 (September 10, 2015)
* We’re now CleverTap! All the existing APIs have been changed from WizRocket to CleverTap.

