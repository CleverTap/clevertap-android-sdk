## CleverTap Android SDK CHANGE LOG

### Version 4.5.2 (July 21, 2022)
* Fixes a bug for notification CTA deeplink - On clicking notification CTA, deeplink launches third party app instead of X app even though X app is capable of handling deeplink. For example, if X app is capable of handling https://google.com(sample link) but deeplink launches browser instead of X app.

### Version 4.5.1 (July 12, 2022)
* New Feature : You can now call `onUserLogin`, `incrementValue` and `decrementValue` method via WebView Interface.
* Improvement : Updated SSL Pinning Certificates

### Version 4.5.0 (June 3, 2022)
* `removeValueForKey()` in `CleverTapAPI` can now remove PII data like Email, Phone and Date Of Birth.
* Improved the `ActivityLifecycleCallback`’s `onPaused` logic so that it runs on the background thread to avoid any runtime issues. Fixes #221.
* Adds support to change credentials for the CleverTap Xiaomi Push SDK using `changeXiaomiCredentials`. Contribution PR #269.
* Adds support to enable/disable the CleverTap Xiaomi Push SDK using `enableXiaomiPushOn` method. CleverTap Xiaomi Push SDK can now be enabled/disabled for `ALL_DEVICES`, `XIAOMI_MIUI_DEVICES` and `NO_DEVICES`.
* Adds analytics support for upcoming CleverTap Direct Call Android SDK.
* Sets up CI/CD using Github Actions.
Note : If you are facing `ClassNotFoundException` "org.jacoco.agent.rt.internal_28bab1d.Offline" after updating to 4.5.0, Please update the SDK to v4.5.1

### Version 4.4.0 (December 20, 2021)
* Adds below new public APIs for smooth and easy integration of Custom Android Push Notifications Handling(FCM),Custom Push Amplification Handling and Push Templates
  * `CTFcmMessageHandler().createNotification(applicationContext, message)`
  * `CTFcmMessageHandler().processPushAmp(applicationContext, message)`
  * `CleverTapAPI.setNotificationHandler(notificationHandler)`
* Adds support for Firebase Cloud Messaging v21 and above

### Version 4.3.1 (November 25, 2021)
* Fixes a Strict Mode Read violation for low RAM devices

### Version 4.3.0 (November 2, 2021)
* Adds support for [apps targeting Android 12 (API 31)](https://developer.android.com/about/versions/12/behavior-changes-12)
  This version is compatible with all new Android 12 changes like Notification Trampolines, Pending Intents Mutability and Safer Component Exporting.
  For more information check out the [CleverTap documentation for Android 12 here](https://developer.clevertap.com/docs/android-push#android-12-changes)
* Deprecated `CTPushNotificationReceiver` and `CTNotificationIntentService` as a part of Notification Trampoline restrictions in Android 12 (API 31)
* Last version with support for custom FCM Sender ID for generating the FCM token
* Fixes a bug where `UTM Visited` event was not being raised on click of a direct deep link

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