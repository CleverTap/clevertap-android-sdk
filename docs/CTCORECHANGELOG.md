## CleverTap Android SDK CHANGE LOG

### Version 4.2.0 (July 15, 2021)
* Adds public methods for suspending/discarding & resuming InApp Notifications
* Adds public methods to increment/decrement values set via User properties
* Adds a new public method `getCleverTapID(OnInitCleverTapIDListener)` to get CleverTap ID through `OnInitCleverTapIDListener` callback
* Deprecated `SyncListener` interface and will be removed in future versions, use `getCleverTapID(OnInitCleverTapIDListener)` instead
* Deprecated `getCleverTapAttributionIdentifier()` method and will be removed in future versions, use `getCleverTapID(OnInitCleverTapIDListener)` instead
* Adds new `CleverTapAPI.LogLevel.VERBOSE` level for debugging
* Fixes App Inbox UI for Android Tablets
* Fixes `recordScreen()` NPE crash
* Fixes a few Strict Mode Read violations that caused ANRs
* Other performance improvements and improved logging

### Version 4.1.1 (May 4, 2021)
* Adds `setFirstTabTitle` method to set the name of the first tab in App Inbox
* Adds `pushChargedEvent` to `CTWebInterface` class to allow raising Charged Event from JS
* Removes a `NoClassDefFoundError` raised in Android Kitkat (v4.4) - #168
* Removes a `NullPointerException` raised while handling `InstallReferrerClient` - #166
* Other bug fixes

### Version 4.1.0 (April 13, 2021)
* Adds support for Android 11
* Reduces the SDK size and added performance improvements
* Removes the deprecated Product Experiences (Screen AB/Dynamic Variables) related code
* Removes support for JCenter
* Fixes a bug where Xiaomi, Huawei, Baidu and other push service tokens were not switched to new profiles when using `onUserLogin`

### Version 4.0.4 (Mar 2, 2021)
* Fixed FCM token refresh issue when multiple Firebase Projects are integrated in the application.
If you're using multiple Firebase projects in your app, use this version instead of v4.0.0 ~ v4.0.3

### Version 4.0.3 (Feb 22, 2021)
* Fixed product config & other crashes ([#127](https://github.com/CleverTap/clevertap-android-sdk/issues/127) , [#132](https://github.com/CleverTap/clevertap-android-sdk/issues/132) , [#147](https://github.com/CleverTap/clevertap-android-sdk/issues/147)) 

### Version 4.0.2 (December 28, 2020)
* Fix for In-apps/Inbox not rendering in v4.0.1 on using configurable Clevertap Identities feature introduced in v4.0.1

### Version 4.0.1 (November 30, 2020)
* Adds support for configurable CleverTap identifiers.
* Adds deprecation warnings for Product A/B Tests public methods.
* Fix for multiple App Launched and App Installed events in the first session.
* Fixes crash which occurred due to wrong classification of some mobile devices as tablets #116.
* Optimized proguard rules for better obfuscation.

### Version 4.0.0 (October 1, 2020)

* Adds support for Android 10 and AndroidX support libraries.
* This is a major release, please find the list of all [changes here](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTV4CHANGES.md)